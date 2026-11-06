import { CanvasImage, Easing, interpolate, staticFile, useCurrentFrame, useVideoConfig } from "remotion";
import type { CSSProperties, ReactNode } from "react";

/** 0 to 1 over `duration` frames, starting at `delay`. */
export const useProgress = (delay = 0, duration = 14) => {
  const frame = useCurrentFrame();
  return interpolate(frame, [delay, delay + duration], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.bezier(0.16, 1, 0.3, 1),
  });
};

/** Seconds to frames, using the composition fps. */
export const useSeconds = () => {
  const { fps } = useVideoConfig();
  return (seconds: number) => Math.round(seconds * fps);
};

/** Fades and slides a child in. `delay` is in frames. */
export const Appear = ({ delay = 0, children, style }: { delay?: number; children: ReactNode; style?: CSSProperties }) => {
  const p = useProgress(delay);
  return (
    <div style={{ opacity: p, translate: `0px ${interpolate(p, [0, 1], [40, 0])}px`, ...style }}>{children}</div>
  );
};

// The phone's screen is drawn at the Pixel's native 1080x2400 and scaled down into the body.
export const NATIVE = { width: 1080, height: 2400 };
const BODY = { width: 640, bezel: 14 };
const SCREEN_WIDTH = BODY.width - BODY.bezel * 2;
const SCALE = SCREEN_WIDTH / NATIVE.width;
export const PHONE_HEIGHT = Math.round(NATIVE.height * SCALE) + BODY.bezel * 2;
export const PHONE_TOP = 450;

const StatusIcons = ({ color }: { color: string }) => (
  <div style={{ display: "flex", gap: 22, alignItems: "center" }}>
    <svg width="46" height="40" viewBox="0 0 30 22"><path d="M2 20 L28 20 L28 2 Z" fill={color} /></svg>
    <svg width="46" height="40" viewBox="0 0 28 22"><path d="M2 8 Q14 -2 26 8 L22 12 Q14 5 6 12 Z M9 15 Q14 11 19 15 L14 21 Z" fill={color} /></svg>
    <svg width="72" height="40" viewBox="0 0 40 22"><rect x="1" y="2" width="34" height="18" rx="4" fill="none" stroke={color} strokeWidth="2.5" /><rect x="4" y="5" width="22" height="12" rx="2" fill={color} /><rect x="36" y="8" width="3" height="6" rx="1" fill={color} /></svg>
  </div>
);

/** Status bar and gesture bar, at native size. */
export const SystemBars = ({ color = "#ffffff", time = "10:24" }: { color?: string; time?: string }) => (
  <>
    <div style={{ position: "absolute", top: 0, left: 0, right: 0, height: 110, padding: "0 64px", display: "flex", alignItems: "center", justifyContent: "space-between", color, fontSize: 40, fontWeight: 500, zIndex: 50 }}>
      <span>{time}</span>
      <StatusIcons color={color} />
    </div>
    <div style={{ position: "absolute", top: 34, left: 540 - 28, width: 56, height: 56, borderRadius: 28, background: "#000", zIndex: 51 }} />
    <div style={{ position: "absolute", bottom: 22, left: 540 - 141, width: 283, height: 9, borderRadius: 5, background: color, opacity: 0.85, zIndex: 50 }} />
  </>
);

/** A finger tap, in native coordinates. */
export const Tap = ({ x, y, at }: { x: number; y: number; at: number }) => {
  const frame = useCurrentFrame();
  const t = frame - at;
  if (t < 0 || t > 18) return null;
  return (
    <div
      style={{
        position: "absolute",
        left: x - 90,
        top: y - 90,
        width: 180,
        height: 180,
        borderRadius: 90,
        background: "rgba(255,255,255,0.5)",
        scale: interpolate(t, [0, 18], [0.4, 1.25]),
        opacity: interpolate(t, [0, 18], [0.9, 0]),
        zIndex: 99,
      }}
    />
  );
};

/** A Pixel-style body. Children are drawn at native 1080x2400. */
export const Phone = ({ children }: { children: ReactNode }) => {
  const p = useProgress(0, 18);
  const { bezel, width } = BODY;
  return (
    <div
      style={{
        position: "absolute",
        left: (1080 - width) / 2,
        top: PHONE_TOP,
        width,
        height: PHONE_HEIGHT,
        scale: interpolate(p, [0, 1], [0.96, 1]),
        opacity: p,
      }}
    >
      <div style={{ position: "absolute", right: -7, top: 290, width: 7, height: 130, borderRadius: 4, background: "#2b2d33" }} />
      <div style={{ position: "absolute", right: -7, top: 470, width: 7, height: 70, borderRadius: 4, background: "#2b2d33" }} />
      <div style={{ position: "absolute", inset: 0, borderRadius: 104, background: "#1b1c20", boxShadow: "0 30px 80px rgba(20,24,40,0.28), inset 0 0 0 2px #34363d" }} />
      <div style={{ position: "absolute", inset: bezel, borderRadius: 92, overflow: "hidden", background: "#000" }}>
        <div style={{ position: "absolute", left: 0, top: 0, width: NATIVE.width, height: NATIVE.height, transformOrigin: "0 0", scale: SCALE, fontFamily: "inherit" }}>{children}</div>
      </div>
    </div>
  );
};

export const KwelaIcon = ({ size }: { size: number }) => (
  <CanvasImage src={staticFile("kwela-avatar.svg")} style={{ width: size, height: size, borderRadius: size / 2 }} />
);
