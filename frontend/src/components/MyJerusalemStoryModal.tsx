import React, { useEffect, useState } from "react";
import { Award, BookHeart, Download, Footprints, MapPin, MessageSquareText, Share2, X } from "lucide-react";
import { locations } from "../locationsData";
import { MyJerusalemStory } from "../types";
import { PROGRESS_EVENT, readAudioListened, readMemoriesUncovered, readQuizCompleted } from "../services/progress";
import { useInterfaceLanguage } from "../context/LanguageContext";

function readVisited(): string[] {
  try {
    const value: unknown = JSON.parse(window.localStorage.getItem("hikaya.visited") ?? "[]");
    return Array.isArray(value) ? value.filter((item): item is string => typeof item === "string") : [];
  } catch {
    return [];
  }
}

function readTourProgress() {
  try {
    const value: unknown = JSON.parse(window.localStorage.getItem("hikaya-quds.itinerary") ?? "null");
    if (!value || typeof value !== "object") return { totalWalkingMinutes: 0 };
    const itinerary = value as { totalWalkingMinutes?: unknown };
    const totalWalkingMinutes = typeof itinerary.totalWalkingMinutes === "number" && Number.isFinite(itinerary.totalWalkingMinutes)
      ? Math.max(0, Math.round(itinerary.totalWalkingMinutes))
      : 0;
    return { totalWalkingMinutes };
  } catch {
    return { totalWalkingMinutes: 0 };
  }
}

function buildStory(isArabic: boolean): MyJerusalemStory {
  const exploredLocationIds = readVisited().filter((id) => locations.some((location) => location.id === id));
  const tour = readTourProgress();
  const completedQuizLocationIds = readQuizCompleted();
  const listenedLocationIds = readAudioListened();
  const heritageSitesDiscovered = new Set([...exploredLocationIds, ...completedQuizLocationIds, ...listenedLocationIds])
    .size;
  const count = exploredLocationIds.length;
  return {
    generatedAt: new Date().toISOString(),
    exploredLocationIds,
    completedQuizLocationIds,
    listenedLocationIds,
    oralMemoriesUncovered: readMemoriesUncovered().length,
    totalWalkingMinutes: tour.totalWalkingMinutes,
    heritageSitesDiscovered,
    badge: isArabic
      ? count >= locations.length && readQuizCompleted().length >= 4 ? "حارس حكاية القدس" : count >= 3 ? "راوي القدس" : "خطوة في القدس"
      : count >= locations.length && readQuizCompleted().length >= 4 ? "Guardian of Jerusalem’s Story" : count >= 3 ? "Jerusalem Storyteller" : "A Step into Jerusalem",
    reflection: isArabic
      ? count === 0 ? "كل حكاية تبدأ من باب؛ اختر أول حي لتبدأ حكايتك." : count >= locations.length ? "عبرت أبواب القدس وحاراتها؛ صارت الخريطة ذاكرةً تمشي معك." : `استكشفت ${count} من حارات القدس؛ وكل موقع حفظته يفتح طريقاً جديداً للحكاية.`
      : count === 0 ? "Every story begins at a gate; choose your first neighbourhood to begin." : count >= locations.length ? "You crossed Jerusalem’s gates and quarters; the map has become a memory you carry." : `You explored ${count} Jerusalem places; every site remembered opens another path into the story.`,
  };
}

export const MyJerusalemStoryModal: React.FC<{ open: boolean; onClose: () => void }> = ({ open, onClose }) => {
  const { isArabic } = useInterfaceLanguage();
  const [, refresh] = useState(0);

  useEffect(() => {
    if (!open) return;
    const update = () => refresh((value) => value + 1);
    window.addEventListener(PROGRESS_EVENT, update);
    window.addEventListener("storage", update);
    return () => {
      window.removeEventListener(PROGRESS_EVENT, update);
      window.removeEventListener("storage", update);
    };
  }, [open]);

  const story = buildStory(isArabic);
  if (!open) return null;
  const explored = locations.filter((location) => story.exploredLocationIds.includes(location.id));
  const exploredNames = explored.map((item) => isArabic ? item.arabicName : item.name);

  const share = async () => {
    const title = isArabic ? "حكايتي في القدس" : "My Jerusalem Story";
    const text = `${title} — ${story.badge}\n${story.reflection}\n${exploredNames.join(isArabic ? "، " : ", ")}`;
    if (navigator.share) await navigator.share({ title, text }).catch(() => undefined);
    else await navigator.clipboard?.writeText(text);
  };

  const download = () => {
    const lang = isArabic ? "ar" : "en";
    const title = isArabic ? "حكايتي في القدس" : "My Jerusalem Story";
    const exploredLabel = isArabic ? "الحارات التي استكشفتها" : "Places explored";
    const empty = isArabic ? "ستبدأ قريباً" : "Your journey will begin soon";
    const stats = isArabic
      ? `مواقع مكتشفة: ${story.heritageSitesDiscovered} · مشي مخطط: ${story.totalWalkingMinutes} دقيقة · اختبارات مكتملة: ${story.completedQuizLocationIds.length} · روايات مسموعة: ${story.listenedLocationIds.length} · ذكريات اكتشفتها: ${story.oralMemoriesUncovered}`
      : `Heritage sites discovered: ${story.heritageSitesDiscovered} · Planned walking: ${story.totalWalkingMinutes} min · Quizzes completed: ${story.completedQuizLocationIds.length} · Narrations heard: ${story.listenedLocationIds.length} · Memories uncovered: ${story.oralMemoriesUncovered}`;
    const html = `<!doctype html><html lang="${lang}" dir="${isArabic ? "rtl" : "ltr"}"><meta charset="utf-8"><title>${title}</title><style>@font-face{font-family:Thmanyah;src:url('/fonts/thmanyah-sans-regular.woff2')}body{font-family:Thmanyah,serif;background:#f5f5f0;color:#2d2d2a;padding:48px}.card{max-width:720px;margin:auto;border:2px solid #d97706;border-radius:28px;padding:42px;background:white}h1{color:#5a5a40}small{color:#8a8a80}</style><div class="card"><small>HIKAYAT ALQUDS · Q GUIDE 2026</small><h1>${title}</h1><h2>${story.badge}</h2><p>${story.reflection}</p><h3>${exploredLabel}</h3><p>${exploredNames.join(" · ") || empty}</p><p>${stats}</p></div></html>`;
    const url = URL.createObjectURL(new Blob([html], { type: "text/html;charset=utf-8" }));
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = "hikayat-alquds-story.html";
    anchor.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm p-4 flex items-center justify-center" role="dialog" aria-modal="true" aria-label={isArabic ? "حكايتي في القدس" : "My Jerusalem Story"}>
      <div className="max-w-2xl w-full bg-white rounded-3xl border-2 border-brand-amber shadow-2xl overflow-hidden" dir={isArabic ? "rtl" : "ltr"}>
        <div className="bg-brand-olive text-white p-7 relative">
          <button type="button" onClick={onClose} className="absolute top-4 end-4 p-2 rounded-full bg-white/10" aria-label={isArabic ? "إغلاق" : "Close"}><X className="w-4 h-4" /></button>
          <div className="flex items-center gap-3"><img src="/logo-icon.png" alt="Hikayat AlQuds" className="h-12 w-12 rounded-xl bg-white/90 object-contain p-1 mix-blend-screen" /><BookHeart className="w-8 h-8 text-brand-amber" /></div>
          <p className="text-xs text-brand-amber mt-3">{isArabic ? "تذكار جولتك الرقمية · HIKAYAT ALQUDS" : "Your digital tour keepsake · HIKAYAT ALQUDS"}</p>
          <h2 className="font-serif font-black text-3xl">{isArabic ? "حكايتي في القدس" : "My Jerusalem Story"}</h2>
        </div>
        <div className="p-7 space-y-5">
          <div className="text-center"><Award className="w-10 h-10 mx-auto text-brand-amber" /><h3 className="font-serif font-black text-2xl text-brand-olive">{story.badge}</h3><p className="mt-1 text-[10px] font-mono uppercase tracking-wider text-brand-muted">{isArabic ? "انعكاس شخصي لجولتك" : "Your personalized tour reflection"}</p><p className="text-sm text-brand-text mt-2">“{story.reflection}”</p></div>
          <div className="bg-brand-bg rounded-2xl p-4">
            <p className="text-xs text-brand-muted mb-2">{isArabic ? "الحارات التي استكشفتها" : "Places explored"} ({explored.length}/{locations.length})</p>
            <div className="flex flex-wrap gap-2">{explored.length ? explored.map((item) => <span key={item.id} className="bg-white border border-brand-border rounded-full px-3 py-1 text-xs">{isArabic ? item.arabicName : item.name}</span>) : <span className="text-xs text-brand-muted">{isArabic ? "افتح صفحة حي ليُضاف إلى الحكاية." : "Open a place page to add it to your story."}</span>}</div>
          </div>
          <div className="grid grid-cols-2 gap-2 text-center text-xs sm:grid-cols-3"><Stat value={story.heritageSitesDiscovered} label={isArabic ? "مواقع مكتشفة" : "Sites discovered"} icon={<MapPin className="h-3 w-3" />} /><Stat value={story.totalWalkingMinutes} label={isArabic ? "دقائق مشي" : "Walking min"} icon={<Footprints className="h-3 w-3" />} /><Stat value={story.completedQuizLocationIds.length} label={isArabic ? "اختبارات" : "Quizzes"} /><Stat value={story.listenedLocationIds.length} label={isArabic ? "روايات مسموعة" : "Narrations"} /><Stat value={story.oralMemoriesUncovered} label={isArabic ? "ذكريات" : "Memories"} icon={<MessageSquareText className="w-3 h-3" />} /><Stat value={explored.length} label={isArabic ? "حارات" : "Quarters"} /></div>
          <p className="text-[10px] text-brand-muted">{isArabic ? "الاقتباس انعكاس شخصي مولّد من تقدّمك فقط؛ لا يضيف أي ادعاء تاريخي." : "This reflection is generated only from your progress; it adds no historical claim."}</p>
          <div className="flex flex-col gap-3 sm:flex-row"><button type="button" onClick={download} className="flex-1 bg-brand-olive text-white rounded-full py-3 text-xs font-bold inline-flex justify-center items-center gap-2"><Download className="w-4 h-4" />{isArabic ? "تحميل التذكار الرقمي" : "Download digital keepsake"}</button><button type="button" onClick={() => void share()} className="flex-1 border border-brand-border rounded-full py-3 text-xs font-bold inline-flex justify-center items-center gap-2"><Share2 className="w-4 h-4" />{isArabic ? "مشاركة على منصات التواصل" : "Share on social platforms"}</button></div>
        </div>
      </div>
    </div>
  );
};

const Stat: React.FC<{ value: number; label: string; icon?: React.ReactNode }> = ({ value, label, icon }) => <div className="bg-brand-bg rounded-xl p-3"><strong className="block text-2xl text-brand-olive">{value}</strong><span className="text-brand-muted inline-flex items-center justify-center gap-1">{icon}{label}</span></div>;
