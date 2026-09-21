/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AppLayout } from "./components/AppLayout";
import { LanguageProvider } from "./context/LanguageContext";
import { PersonalizedTourModal } from "./components/PersonalizedTourModal";
import { AboutPage } from "./pages/AboutPage";
import { ConstellationPage } from "./pages/ConstellationPage";
import { ContributePage } from "./pages/ContributePage";
import { HomePage } from "./pages/HomePage";
import { LocationDetailsPage } from "./pages/LocationDetailsPage";
import { MapPage } from "./pages/MapPage";

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
export default function App() {
  return (
    <LanguageProvider>
      <BrowserRouter>
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
      </BrowserRouter>
    </LanguageProvider>
  );
}
