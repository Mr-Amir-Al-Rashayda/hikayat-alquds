/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { lazy, Suspense } from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AppLayout } from "./components/AppLayout";
import { LanguageProvider, useInterfaceLanguage } from "./context/LanguageContext";

const HomePage = lazy(() => import("./pages/HomePage").then((module) => ({ default: module.HomePage })));
const MapPage = lazy(() => import("./pages/MapPage").then((module) => ({ default: module.MapPage })));
const LocationDetailsPage = lazy(() => import("./pages/LocationDetailsPage").then((module) => ({ default: module.LocationDetailsPage })));
const ContributePage = lazy(() => import("./pages/ContributePage").then((module) => ({ default: module.ContributePage })));
const ConstellationPage = lazy(() => import("./pages/ConstellationPage").then((module) => ({ default: module.ConstellationPage })));
const AboutPage = lazy(() => import("./pages/AboutPage").then((module) => ({ default: module.AboutPage })));
const PersonalizedTourModal = lazy(() => import("./components/PersonalizedTourModal").then((module) => ({ default: module.PersonalizedTourModal })));

/**
 * Routes for the Sprint 2 MVP flow:
 *
 *   /                   home
 *   /map                interactive map of the archived locations
 *   /constellation      the archive drawn as a web of shared themes
 *   /locations/:id      details, reviewed stories, AI narrative, memories
 *   /contribute         user contribution form
 *   /about              project background
 *   /explorer           legacy URL redirected to the current Jerusalem map
 *
 * Location ids are the same slugs used by the API and the content files
 * (for example 'muslim-quarter' and 'bab-al-amud'), so URLs stay readable.
 */
const RoutedApp: React.FC = () => {
  const { isArabic } = useInterfaceLanguage();
  return (
    <Suspense fallback={<div className="min-h-screen bg-brand-bg flex items-center justify-center text-sm text-brand-olive" role="status"><span className="w-5 h-5 rounded-full border-2 border-brand-amber border-t-transparent animate-spin me-2" />{isArabic ? "جارٍ فتح حكاية القدس…" : "Opening Hikayat AlQuds…"}</div>}>
      <Routes>
        <Route element={<AppLayout />}>
          <Route index element={<HomePage />} />
          <Route path="plan-tour" element={<PersonalizedTourModal />} />
          <Route path="map" element={<MapPage />} />
          <Route path="locations/:locationId" element={<LocationDetailsPage />} />
          <Route path="constellation" element={<ConstellationPage />} />
          <Route path="contribute" element={<ContributePage />} />
          <Route path="about" element={<AboutPage />} />
          <Route path="explorer" element={<Navigate to="/map" replace />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </Suspense>
  );
};

export default function App() {
  return (
    <LanguageProvider>
      <BrowserRouter>
        <RoutedApp />
      </BrowserRouter>
    </LanguageProvider>
  );
}
