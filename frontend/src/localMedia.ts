import { ApiMedia } from "./types";

/**
 * Media ids that have a copy stored in this repository. Exported because the
 * Android build reads the same map when it bundles offline photographs, so a
 * picture added here reaches both clients.
 */
export const LOCAL_MEDIA: Record<string, string> = {
  "amud-before": "/images/jerusalem/bab-al-amud-1925.jpg",
  "amud-after": "/images/jerusalem/bab-al-amud.jpg",
  "muslim-souq-2014": "/images/jerusalem/gallery/muslim-souq-2014.jpg",
  "muslim-street-2015": "/images/jerusalem/gallery/muslim-street-2015.jpg",
  "christian-sepulchre-2015": "/images/jerusalem/gallery/christian-sepulchre-2015.jpg",
  "christian-sepulchre-1880": "/images/jerusalem/gallery/christian-sepulchre-1880.jpg",
  "armenian-ceramics-2012": "/images/jerusalem/gallery/armenian-ceramics-2012.jpg",
  "maghariba-quarter-early20c": "/images/jerusalem/gallery/maghariba-quarter-early20c.jpg",
  "maghariba-site-2012": "/images/jerusalem/gallery/maghariba-site-2012.jpg",
  "sheikh-nashashibi-2010": "/images/jerusalem/gallery/sheikh-nashashibi-2010.jpg",
  "sheikh-aerial-1931": "/images/jerusalem/gallery/sheikh-aerial-1931.jpg",
  "silwan-1920": "/images/jerusalem/gallery/silwan-1920.jpg",
  "silwan-spring-2009": "/images/jerusalem/gallery/silwan-spring-2009.jpg",
  "tur-gethsemane-2011": "/images/jerusalem/gallery/at-tur-gethsemane-2011.jpg",
  "amud-khan-zait-2018": "/images/jerusalem/gallery/bab-khan-zait-2018.jpg",
  "muslim-via-dolorosa-2010": "/images/jerusalem/gallery/muslim-via-dolorosa-2010.jpg",
  "christian-muristan-fountain-2013": "/images/jerusalem/gallery/christian-muristan-fountain-2013.jpg",
  "christian-latin-patriarchate-2011": "/images/jerusalem/gallery/christian-latin-patriarchate-2011.jpg",
  "armenian-quarter-detail-2014": "/images/jerusalem/gallery/armenian-quarter-detail-2014.jpg",
  "armenian-patriarchate-street-2014": "/images/jerusalem/gallery/armenian-patriarchate-street-2014.jpg",
  "sheikh-umm-kamel-2009": "/images/jerusalem/gallery/sheikh-umm-kamel-2009.jpg",
  "silwan-ain-1907": "/images/jerusalem/gallery/silwan-ain-1907.jpg",
  "tur-gethsemane-olives-2016": "/images/jerusalem/gallery/at-tur-gethsemane-olives-2016.jpg",
  "tur-aerial-2013": "/images/jerusalem/gallery/at-tur-aerial-2013.jpg",
  "amud-gate-2022": "/images/jerusalem/gallery/bab-al-amud-2022.jpg",
  "amud-khan-zait-2019": "/images/jerusalem/gallery/bab-khan-zait-2019.jpg",
};

export const localMedia = (image: ApiMedia) =>
  LOCAL_MEDIA[image.id] ?? (image.pairRole === "cover" ? `/images/jerusalem/${image.locationId}.jpg` : null);

