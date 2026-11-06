import { loadFont } from "@remotion/google-fonts/NotoSans";
import { loadFont as loadInter } from "@remotion/google-fonts/Inter";
import { AbsoluteFill, Sequence, interpolate, useCurrentFrame, useVideoConfig } from "remotion";
import type { ReactNode } from "react";
import { copy } from "./copy";
import { ArtifactCard, Bubble, Chips, KwelaChat, TimeHeader, TypingDots } from "./kwela";
import { Appear, KwelaIcon, Phone, Tap, useProgress } from "./phone";
import { DateChip, Incoming, Outgoing, PosterThumb, W, WhatsAppGroup } from "./whatsapp";

/**
 * Draft short cut (about 15 s), text only, Sheng captions. Script: docs/marketing/demo-video-script-sheng.md
 * Kwela screens use colours and sizes taken from real Pixel screenshots of the app.
 * The WhatsApp and share-sheet screens are drawn from memory: swap in a real screen recording when you have one.
 */

// Noto Sans is the app's own font, used inside the phone. Inter is the closest free match to the SF Pro look of Apple promos, used for the captions.
const { fontFamily } = loadFont("normal", { weights: ["400", "500", "600", "700", "900"], subsets: ["latin"] });
const { fontFamily: captionFont } = loadInter("normal", { weights: ["600", "700", "800"], subsets: ["latin"] });

const C = { bg: "#ffffff", ink: "#111318", accent: "#52a8a0", warm: "#e8590c" };

export const SHORT_CUT_SECONDS = 18.5;

/** Big caption. Pass `after` to bring in a second line `afterAt` seconds into the scene. */
const Caption = ({ children, after, afterAt = 2.2, bottom = false, size }: { children: ReactNode; after?: ReactNode; afterAt?: number; bottom?: boolean; size?: number }) => {
  const p = useProgress(0, 18);
  const { fps } = useVideoConfig();
  return (
    <div
      style={{
        position: "absolute",
        left: 64,
        right: 64,
        // TikTok covers about the top 150 px and bottom 330 px with its own UI.
        ...(bottom ? { bottom: 360 } : { top: 135 }),
        textAlign: "center",
        fontFamily: captionFont,
        fontWeight: 700,
        letterSpacing: "-0.035em",
        fontSize: size ?? (after ? 62 : 76),
        lineHeight: 1.08,
        color: C.ink,
      }}
    >
      <div style={{ opacity: p, translate: `0px ${interpolate(p, [0, 1], [30, 0])}px` }}>{children}</div>
      {after && <Appear delay={Math.round(afterAt * fps)} style={{ color: "#6b6f7a", marginTop: 10 }}>{after}</Appear>}
    </div>
  );
};

/** Drawn tick, instead of an emoji. */
const Tick = () => (
  <svg width="50" height="50" viewBox="0 0 64 64" style={{ verticalAlign: "-6px", margin: "0 8px" }}>
    <circle cx="32" cy="32" r="30" fill={C.accent} />
    <path d="M18 33 L28 43 L47 22" stroke="#fff" strokeWidth="7" fill="none" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const Hook = () => (
  <AbsoluteFill style={{ background: C.bg, justifyContent: "center", alignItems: "center" }}>
    <div style={{ fontFamily: captionFont, color: C.warm, fontWeight: 800, fontSize: 320, letterSpacing: "-0.06em" }}>2K</div>
    <Caption bottom>{copy.captions.hook}</Caption>
  </AbsoluteFill>
);

const ShareTarget = ({ label, color, kwela = false }: { label: string; color: string; kwela?: boolean }) => (
  <div style={{ width: 230, display: "flex", flexDirection: "column", alignItems: "center", gap: 18, color: "#e3e3e8", fontSize: 32 }}>
    {kwela ? <KwelaIcon size={132} /> : <div style={{ width: 132, height: 132, borderRadius: 66, background: color }} />}
    {label}
  </div>
);

const ShareFlow = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const t = frame / fps;
  const selected = t >= 1.1;
  const menu = t >= 1.9 && t < 3.1;
  const sheet = useProgress(Math.round(2.9 * fps), 14);
  return (
    <>
      <WhatsAppGroup selected={selected}>
        <DateChip>{copy.whatsapp.dateChip}</DateChip>
        <Incoming name={copy.whatsapp.messages[0].from} time={copy.whatsapp.messages[0].time}>
          <div style={{ fontSize: 38, padding: "0 12px 4px" }}>{copy.whatsapp.messages[0].text}</div>
        </Incoming>
        <div style={{ position: "relative" }}>
          {selected && <div style={{ position: "absolute", left: -24, right: -24, top: -9, bottom: -9, background: "rgba(0,168,132,0.28)" }} />}
          <Incoming name={copy.whatsapp.messages[1].from} time={copy.whatsapp.messages[1].time}>
            <div style={{ borderRadius: 18, overflow: "hidden", width: 760 }}><PosterThumb width={760} height={930} /></div>
            <div style={{ fontSize: 38, margin: "12px 12px 2px" }}>{copy.whatsapp.messages[1].text}</div>
          </Incoming>
        </div>
        <Outgoing time={copy.whatsapp.reply.time}>{copy.whatsapp.reply.text}</Outgoing>
      </WhatsAppGroup>
      {menu && (
        <div style={{ position: "absolute", top: 140, right: 36, width: 440, background: "#233138", borderRadius: 24, padding: "14px 0", color: W.text, fontSize: 42, zIndex: 40, boxShadow: "0 12px 40px rgba(0,0,0,0.5)" }}>
          {copy.whatsapp.menu.map((item) => (
            <div key={item} style={{ padding: "26px 44px", background: item === "Share" && t > 2.75 ? "rgba(255,255,255,0.12)" : "transparent" }}>{item}</div>
          ))}
        </div>
      )}
      {sheet > 0 && (
        <>
          <div style={{ position: "absolute", inset: 0, background: `rgba(0,0,0,${0.5 * sheet})`, zIndex: 60 }} />
          <div
            style={{
              position: "absolute",
              left: 0,
              right: 0,
              bottom: 0,
              height: 980,
              background: "#26282d",
              borderRadius: "90px 90px 0 0",
              padding: "28px 48px",
              translate: `0px ${interpolate(sheet, [0, 1], [980, 0])}px`,
              zIndex: 61,
              color: "#e3e3e8",
            }}
          >
            <div style={{ width: 90, height: 10, borderRadius: 5, background: "#5b5e66", margin: "0 auto 36px" }} />
            <div style={{ display: "flex", gap: 30, alignItems: "center", marginBottom: 44 }}>
              <div style={{ borderRadius: 30, overflow: "hidden" }}><PosterThumb width={170} height={170} /></div>
              <div>
                <div style={{ fontSize: 44, fontWeight: 500 }}>{copy.shareSheet.title}</div>
                <div style={{ fontSize: 32, color: "#a9abb4" }}>{copy.shareSheet.fileName}</div>
              </div>
            </div>
            <div style={{ display: "flex", justifyContent: "space-between" }}>
              <ShareTarget label={copy.shareSheet.targets[0]} color="#4b7bec" />
              <ShareTarget label={copy.shareSheet.targets[1]} color="#3f8f6b" />
              <ShareTarget label={copy.shareSheet.targets[2]} color="" kwela />
              <ShareTarget label={copy.shareSheet.targets[3]} color="#c08a2b" />
            </div>
          </div>
        </>
      )}
      <Tap x={540} y={760} at={Math.round(0.9 * fps)} />
      <Tap x={1000} y={185} at={Math.round(1.8 * fps)} />
      <Tap x={800} y={405} at={Math.round(2.8 * fps)} />
      <Tap x={552} y={1745} at={Math.round(4.0 * fps)} />
    </>
  );
};

const ChatFlow = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const t = frame / fps;
  const { chat, timeHeader } = copy.kwela;
  const file = chat.attachmentLabel;
  return (
    <KwelaChat pending={t < 0.8 ? file : undefined}>
      {t >= 0.8 && (
        <>
          <TimeHeader>{timeHeader}</TimeHeader>
          <Bubble mine attachment={file}>{chat.you}</Bubble>
        </>
      )}
      {((t >= 1.3 && t < 2.2) || (t >= 2.9 && t < 3.4)) && <TypingDots />}
      {t >= 2.2 && <Bubble>{chat.replies[0]}</Bubble>}
      {t >= 3.4 && <Bubble>{chat.replies[1]}</Bubble>}
      {t >= 4.1 && <Chips labels={[...chat.chips]} />}
    </KwelaChat>
  );
};

const CardsFlow = () => {
  const { fps } = useVideoConfig();
  return (
    <KwelaChat>
      <Appear><Bubble>{copy.kwela.cards.intro}</Bubble></Appear>
      {copy.kwela.cards.files.map((f, i) => (
        <ArtifactCard key={f.title} delay={Math.round((0.3 + i * 0.5) * fps)} title={f.title} job={f.job} preview={f.preview} />
      ))}
    </KwelaChat>
  );
};

const EndScene = () => {
  const p = useProgress(0, 18);
  return (
    <AbsoluteFill style={{ background: C.bg, justifyContent: "center", alignItems: "center" }}>
      <div style={{ opacity: p, scale: interpolate(p, [0, 1], [0.8, 1]) }}><KwelaIcon size={360} /></div>
      <div style={{ fontFamily: captionFont, color: C.ink, fontWeight: 700, letterSpacing: "-0.04em", fontSize: 104, marginTop: 40, opacity: p }}>Kwela</div>
      <Caption bottom>{copy.captions.end}</Caption>
    </AbsoluteFill>
  );
};

// Scene lengths in seconds. The phone stays on screen from the share scene to the cards, so only its screen changes.
const T = { hook: 2, share: 5, chat: 5.5, cards: 3.5, end: 2.5 };

export const ShortCut = () => {
  const { fps } = useVideoConfig();
  const f = (seconds: number) => Math.round(seconds * fps);
  const shareAt = T.hook;
  const chatAt = shareAt + T.share;
  const cardsAt = chatAt + T.chat;
  const endAt = cardsAt + T.cards;
  return (
    <AbsoluteFill style={{ background: C.bg, fontFamily }}>
      <Sequence from={0} durationInFrames={f(T.hook)}><Hook /></Sequence>

      <Sequence from={f(shareAt)} durationInFrames={f(T.share + T.chat + T.cards)}>
        <Phone>
          <Sequence from={0} durationInFrames={f(T.share)}><ShareFlow /></Sequence>
          <Sequence from={f(T.share)} durationInFrames={f(T.chat)}><ChatFlow /></Sequence>
          <Sequence from={f(T.share + T.chat)} durationInFrames={f(T.cards)}><CardsFlow /></Sequence>
        </Phone>
      </Sequence>

      <Sequence from={f(shareAt)} durationInFrames={f(T.share)}>
        <Caption after={copy.captions.share[1]} afterAt={2.9}>{copy.captions.share[0]}</Caption>
      </Sequence>
      <Sequence from={f(chatAt)} durationInFrames={f(T.chat)}>
        <Caption after={copy.captions.chat[1]} afterAt={3.4}>{copy.captions.chat[0]}</Caption>
      </Sequence>
      <Sequence from={f(cardsAt)} durationInFrames={f(T.cards)}>
        <Caption size={64}>{copy.captions.cards}<Tick /></Caption>
      </Sequence>

      <Sequence from={f(endAt)} durationInFrames={f(T.end)}><EndScene /></Sequence>
    </AbsoluteFill>
  );
};
