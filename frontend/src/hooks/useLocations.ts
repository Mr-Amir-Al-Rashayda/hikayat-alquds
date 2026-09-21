import { useEffect, useState } from "react";
import { ApiLocation, DataSource } from "../types";
import { getLocations } from "../services/api";

interface LocationsState {
  locations: ApiLocation[];
  loading: boolean;
  source: DataSource;
  reason?: string;
}

/**
 * Loads the location list once and reports where it came from.
 *
 * Several pages need the same list, and each needs to know whether it is live
 * or sample data, so the fetch and the source flag travel together.
 */
export function useLocations(): LocationsState {
  const [state, setState] = useState<LocationsState>({
    locations: [],
    loading: true,
    source: "backend",
  });

  useEffect(() => {
    let active = true;

    getLocations()
      .then((result) => {
        if (!active) return;
        setState({
          locations: result.data,
          loading: false,
          source: result.source,
          reason: result.reason,
        });
      })
      .catch(() => {
        if (active) setState((previous) => ({ ...previous, loading: false }));
      });

    return () => {
      // Stops a state update if the page unmounts mid-request.
      active = false;
    };
  }, []);

  return state;
}
