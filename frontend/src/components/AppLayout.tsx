import React, { useEffect, useState } from "react";
import { Link, NavLink, Outlet } from "react-router-dom";
import { BookHeart, Heart, Home, Info, Languages, MapPin, PenLine, Route, Waypoints } from "lucide-react";
import { useInterfaceLanguage } from "../context/LanguageContext";
import { MyJerusalemStoryModal } from "./MyJerusalemStoryModal";
import { OPEN_JERUSALEM_STORY_EVENT } from "../services/progress";

const NAV_ITEMS = [
  { to: "/", en: "Home", ar: "الرئيسية", icon: Home, end: true },
  { to: "/plan-tour", en: "Plan tour", ar: "صمّم جولتك", icon: Route, end: false },
  { to: "/map", en: "Map", ar: "الخريطة", icon: MapPin, end: false },
  { to: "/constellation", en: "Connections", ar: "الروابط", icon: Waypoints, end: false },
  { to: "/contribute", en: "Contribute", ar: "شارك حكاية", icon: PenLine, end: false },
  { to: "/about", en: "About", ar: "عن المنصة", icon: Info, end: false },
];

export const AppLayout: React.FC = () => {
  const { language, setLanguage, isArabic } = useInterfaceLanguage();
  const [storyOpen, setStoryOpen] = useState(false);

  useEffect(() => {
    const openStory = () => setStoryOpen(true);
    window.addEventListener(OPEN_JERUSALEM_STORY_EVENT, openStory);
    return () => window.removeEventListener(OPEN_JERUSALEM_STORY_EVENT, openStory);
  }, []);

  return (
    <div className="min-h-screen bg-brand-bg text-brand-text font-sans flex flex-col" dir={isArabic ? "rtl" : "ltr"}>
      <div className="h-2 w-full bg-gradient-to-r from-brand-olive via-brand-amber to-brand-olive" />
      <header className="max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 pt-5 pb-0">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <Link to="/" className="group flex max-w-[480px] items-center gap-3 sm:gap-4" aria-label="HIKAYAT ALQUDS | حكاية القدس">
            <picture className="shrink-0">
              <source media="(max-width: 520px)" srcSet="/logo-icon.png" />
              <img src="/logo-horizontal.png" alt="HIKAYAT ALQUDS | حكاية القدس" className="h-[68px] w-auto max-w-[150px] object-contain mix-blend-multiply sm:h-[78px] sm:max-w-[185px]" />
            </picture>
            <span className="h-9 w-px shrink-0 bg-brand-border-light" aria-hidden="true" />
            <p className="max-w-[13rem] text-xs leading-relaxed text-brand-muted sm:max-w-[16rem]">{isArabic ? "المرشد السياحي الرقمي الذكي للقدس — مسار Q GUIDE" : "The intelligent digital guide to Jerusalem — Q GUIDE track"}</p>
          </Link>
          <div className="flex items-center gap-2 self-start">
            <button type="button" onClick={() => setStoryOpen(true)} className="bg-brand-olive text-brand-bg text-[10px] font-bold px-3 py-2 rounded-full inline-flex items-center gap-1.5">
              <BookHeart className="w-3.5 h-3.5 text-brand-amber" /> {isArabic ? "إنشاء حكايتي في القدس" : "Generate My Jerusalem Story"}
            </button>
            <button type="button" onClick={() => setLanguage(language === "ar" ? "en" : "ar")} aria-label={isArabic ? "التبديل إلى الإنجليزية" : "Switch to Arabic"} className="bg-white border border-brand-border text-brand-olive text-[10px] font-bold px-3 py-2 rounded-full inline-flex items-center gap-1.5">
              <Languages className="w-3.5 h-3.5" /> {language === "ar" ? "EN" : "العربية"}
            </button>
            <span className="bg-brand-amber/10 text-brand-amber text-[10px] font-mono font-bold px-3 py-2 rounded-full border border-brand-amber/20">Q GUIDE 2026</span>
          </div>
        </div>
        <nav className="flex flex-wrap items-center gap-1 mt-5 border-b border-brand-border-light">
          {NAV_ITEMS.map(({ to, en, ar, icon: Icon, end }) => (
            <NavLink key={to} to={to} end={end} className={({ isActive }) => `pb-3 pt-1 text-[11px] font-serif font-bold border-b-2 px-3 transition-colors flex items-center gap-1.5 ${isActive ? "border-brand-olive text-brand-olive" : "border-transparent text-brand-muted hover:text-brand-olive"}`}>
              <Icon className="w-3.5 h-3.5" /> {isArabic ? ar : en}
            </NavLink>
          ))}
        </nav>
      </header>
      <main className="max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 flex-1"><Outlet /></main>
      <footer className="mt-8 py-8 text-center text-[10px] text-brand-muted font-mono tracking-widest space-y-1 border-t border-brand-border-light">
        <p>© 2026 HIKAYAT ALQUDS · حكاية القدس</p>
        <div className="flex justify-center items-center gap-1.5 text-brand-amber"><Heart className="w-3 h-3 fill-current" /> {isArabic ? "هاكاثون القدس · مسار Q GUIDE" : "Jerusalem Hackathon · Q GUIDE"}</div>
      </footer>
      <MyJerusalemStoryModal open={storyOpen} onClose={() => setStoryOpen(false)} />
    </div>
  );
};
