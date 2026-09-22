package ps.hikayatalquds.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether the device currently has a usable internet connection.
 *
 * Used to decide whether to *attempt* a request and whether to offer a remote
 * photograph, never to decide what content exists - the archive is on the
 * device either way.
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext context: Context,
    @ps.hikayatalquds.di.ApplicationScope scope: CoroutineScope,
) {
    private val manager = context.getSystemService<ConnectivityManager>()

    val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            private val available = mutableSetOf<Network>()

            override fun onAvailable(network: Network) {
                available += network
                trySend(true)
            }

            override fun onLost(network: Network) {
                available -= network
                trySend(available.isNotEmpty())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        // A manager can be absent in a restricted profile; assume offline and
        // let requests fail honestly rather than crashing the app.
        if (manager == null) {
            trySend(false)
        } else {
            manager.registerNetworkCallback(request, callback)
            trySend(manager.hasInternet())
        }

        awaitClose { runCatching { manager?.unregisterNetworkCallback(callback) } }
    }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), false)

    private fun ConnectivityManager.hasInternet(): Boolean {
        val capabilities = getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

/**
 * Lets the API address be changed in Settings without rebuilding Retrofit.
 *
 * Retrofit fixes its base URL when it is built; this rewrites each outgoing
 * request against whatever address is configured now, so a reader can point the
 * app at a local backend, a deployment, or nothing at all.
 */
@Singleton
class BaseUrlInterceptor @Inject constructor() : Interceptor {

    @Volatile
    private var base: HttpUrl? = null

    /** Returns false when the text is not a usable http(s) address. */
    fun update(url: String?): Boolean {
        val normalised = url?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { if (it.endsWith("/")) it else "$it/" }
        if (normalised == null) {
            base = null
            return true
        }
        val parsed = normalised.toHttpUrlOrNull() ?: return false
        base = parsed
        return true
    }

    val isConfigured: Boolean get() = base != null

    override fun intercept(chain: Interceptor.Chain): Response {
        val target = base ?: throw NoApiConfiguredException()
        val request = chain.request()
        val rebuilt = request.url.newBuilder()
            .scheme(target.scheme)
            .host(target.host)
            .port(target.port)
            .encodedPath(target.encodedPath.trimEnd('/') + request.url.encodedPath)
            .build()
        return chain.proceed(request.newBuilder().url(rebuilt).build())
    }
}

/** Thrown when a request is made with no API address configured. */
class NoApiConfiguredException : java.io.IOException("No Hikayat AlQuds API address is configured.")
