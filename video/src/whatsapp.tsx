import { CanvasImage, staticFile } from "remotion";
import type { ReactNode } from "react";
import { copy } from "./copy";
import { SystemBars } from "./phone";

// Drawn from memory of WhatsApp on Android in dark mode. Replace with a real screen recording of the share flow.
export const W = {
  bg: "#0b141a",
  bar: "#1f2c34",
  incoming: "#202c33",
  outgoing: "#005c4b",
  text: "#e9edef",
  muted: "#8696a0",
  green: "#00a884",
  name: "#53bdeb",
};

export const PosterThumb = ({ width, height }: { width: number | string; height: number | string }) => (
  <div style={{ width, height, backgroundImage: `url(${staticFile("poster.png")})`, backgroundSize: "cover", backgroundPosition: "top center" }} />
);

const stroke = { fill: "none", stroke: W.text, strokeWidth: 1.8, strokeLinecap: "round", strokeLinejoin: "round" } as const;

const Back = () => <svg width="60" height="60" viewBox="0 0 24 24"><path d="M15 5l-7 7 7 7" {...stroke} /></svg>;
const Kebab = () => <svg width="60" height="60" viewBox="0 0 24 24" fill={W.text}><circle cx="12" cy="5" r="1.8" /><circle cx="12" cy="12" r="1.8" /><circle cx="12" cy="19" r="1.8" /></svg>;
const VideoCall = () => <svg width="62" height="62" viewBox="0 0 24 24"><rect x="3" y="7" width="12" height="10" rx="2" {...stroke} /><path d="M15 11l6-3v8l-6-3" {...stroke} /></svg>;
const Phone = () => <svg width="58" height="58" viewBox="0 0 24 24"><path d="M5 4h4l2 5-2.5 1.5a11 11 0 005 5L15 13l5 2v4a2 2 0 01-2 2A15 15 0 013 6a2 2 0 012-2z" {...stroke} /></svg>;
const Reply = () => <svg width="60" height="60" viewBox="0 0 24 24"><path d="M10 7L4 12l6 5v-3c5 0 8 1 10 5-1-6-4-9-10-9z" {...stroke} /></svg>;
const Star = () => <svg width="60" height="60" viewBox="0 0 24 24"><path d="M12 3l2.7 5.6 6.1.8-4.5 4.2 1.1 6-5.4-3-5.4 3 1.1-6L3.2 9.4l6.1-.8z" {...stroke} /></svg>;
const Forward = () => <svg width="60" height="60" viewBox="0 0 24 24"><path d="M14 7l6 5-6 5v-3c-5 0-8 1-10 5 1-6 4-9 10-9z" {...stroke} /></svg>;

export const Toolbar = ({ selected }: { selected: boolean }) => (
  <div style={{ position: "absolute", top: 110, left: 0, right: 0, height: 150, background: W.bar, display: "flex", alignItems: "center", padding: "0 30px", gap: 26, color: W.text, zIndex: 10 }}>
    <Back />
    {selected ? (
      <>
        <div style={{ fontSize: 48, fontWeight: 500, flex: 1 }}>1</div>
        <Reply /><Star /><Forward /><Kebab />
      </>
    ) : (
      <>
        <div style={{ width: 92, height: 92, borderRadius: 46, background: "#2a3942" }} />
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 42, fontWeight: 600 }}>{copy.whatsapp.groupName}</div>
          <div style={{ fontSize: 29, color: W.muted }}>{copy.whatsapp.groupMembers}</div>
        </div>
        <VideoCall /><Phone /><Kebab />
      </>
    )}
  </div>
);

export const DateChip = ({ children }: { children: ReactNode }) => (
  <div style={{ alignSelf: "center", background: "#182229", color: W.muted, fontSize: 28, padding: "10px 26px", borderRadius: 18 }}>{children}</div>
);

export const Incoming = ({ name, children, time }: { name: string; children: ReactNode; time: string }) => (
  <div style={{ alignSelf: "flex-start", background: W.incoming, borderRadius: "8px 26px 26px 26px", padding: "12px 14px 10px", maxWidth: 800, color: W.text }}>
    <div style={{ color: W.name, fontSize: 31, fontWeight: 600, margin: "2px 12px 8px" }}>{name}</div>
    {children}
    <div style={{ textAlign: "right", color: W.muted, fontSize: 25, margin: "4px 10px 0" }}>{time}</div>
  </div>
);

export const Outgoing = ({ children, time }: { children: ReactNode; time: string }) => (
  <div style={{ alignSelf: "flex-end", background: W.outgoing, borderRadius: "26px 8px 26px 26px", padding: "14px 24px 10px", maxWidth: 700, color: W.text, fontSize: 38 }}>
    {children}
    <div style={{ textAlign: "right", color: "#9bd1c6", fontSize: 25, marginTop: 2 }}>{time}</div>
  </div>
);

export const WhatsAppGroup = ({ selected, children }: { selected: boolean; children: ReactNode }) => (
  <div style={{ position: "absolute", inset: 0, background: W.bg, fontFamily: "inherit" }}>
    <SystemBars color="#ffffff" />
    <Toolbar selected={selected} />
    <div style={{ position: "absolute", top: 260, bottom: 280, left: 0, right: 0, padding: "24px 24px", display: "flex", flexDirection: "column", gap: 18, overflow: "hidden" }}>{children}</div>
    <div style={{ position: "absolute", left: 24, right: 24, bottom: 120, height: 126, display: "flex", alignItems: "center", gap: 18 }}>
      <div style={{ flex: 1, height: 126, borderRadius: 63, background: W.bar, display: "flex", alignItems: "center", padding: "0 34px", gap: 24, color: W.muted, fontSize: 42 }}>
        <svg width="56" height="56" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9" fill="none" stroke={W.muted} strokeWidth="1.6" /><circle cx="9" cy="10" r="1" fill={W.muted} /><circle cx="15" cy="10" r="1" fill={W.muted} /><path d="M8 14c2 2.5 6 2.5 8 0" stroke={W.muted} strokeWidth="1.6" fill="none" strokeLinecap="round" /></svg>
        <div style={{ flex: 1 }}>{copy.whatsapp.inputPlaceholder}</div>
      </div>
      <div style={{ width: 126, height: 126, borderRadius: 63, background: W.green, display: "flex", alignItems: "center", justifyContent: "center" }}>
        <svg width="60" height="60" viewBox="0 0 24 24"><rect x="9" y="3" width="6" height="11" rx="3" fill="#0b141a" /><path d="M6 11a6 6 0 0012 0M12 17v4" stroke="#0b141a" strokeWidth="1.8" fill="none" strokeLinecap="round" /></svg>
      </div>
    </div>
  </div>
);

export const ShareSheetPhone = ({ children }: { children: ReactNode }) => <>{children}</>;
export { CanvasImage };
