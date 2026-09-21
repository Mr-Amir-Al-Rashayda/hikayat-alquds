import React from "react";
import { AboutPanel } from "../components/AboutPanel";
import { useInterfaceLanguage } from "../context/LanguageContext";

export const AboutPage: React.FC = () => {
  const { isArabic } = useInterfaceLanguage();
  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-serif font-black text-3xl text-brand-olive">{isArabic ? "عن حكاية القدس" : "About Hikayat AlQuds"}</h1>
        <p className="text-xs text-brand-muted font-serif italic">
          {isArabic ? "دليل شخصي ذكي للقدس · مسار Q GUIDE 2026" : "AI-powered personalized guidance for Jerusalem · Q GUIDE 2026."}
        </p>
      </div>
      <AboutPanel />
    </div>
  );
};
