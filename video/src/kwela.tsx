import type { ReactNode } from "react";
import { copy } from "./copy";
import { Appear, KwelaIcon, SystemBars } from "./phone";

// Colours sampled from real Pixel screenshots of the app (dark theme); sizes are the app's dp x 2.625.
export const K = {
  bg: "#0d0e12",
  bubble: "#181a1f",
  card: "#1e2025",
  user: "#455270",
  onUser: "#dae3fc",
  text: "#e3e5ee",
  muted: "#a9abb4",
  primary: "#b9c6ea",
  tonal: "#353c4c",
  onTonal: "#b9bfd3",
  navSelected: "#23262c",
};

const BODY_TEXT = { fontSize: 42, lineHeight: "63px" } as const;

// The app's top bar: the Kwela avatar (56 dp) with the name under it, centred below the status bar.
export const Header = () => (
  <div style={{ position: "absolute", top: 142, left: 0, right: 0, display: "flex", flexDirection: "column", alignItems: "center", gap: 6 }}>
    <KwelaIcon size={147} />
    <div style={{ color: K.text, fontSize: 31, fontWeight: 500 }}>{copy.kwela.name}</div>
  </div>
);

/** A chat bubble. Mine are on the right in the primary container colour. */
export const Bubble = ({ mine = false, children, attachment, delay = 0 }: { mine?: boolean; children: ReactNode; attachment?: string; delay?: number }) => (
  <Appear delay={delay} style={{ display: "flex", justifyContent: mine ? "flex-end" : "flex-start" }}>
    <div
      style={{
        maxWidth: 830,
        background: mine ? K.user : K.bubble,
        color: mine ? K.onUser : K.text,
        borderRadius: 63,
        padding: "26px 42px",
        ...BODY_TEXT,
      }}
    >
      <div>{children}</div>
      {attachment && <div style={{ fontSize: 31, lineHeight: "40px", color: mine ? "#aab4cf" : K.muted, marginTop: 21 }}>{"\u{1F4CE}"} {attachment}</div>}
    </div>
  </Appear>
);

export const TimeHeader = ({ children }: { children: ReactNode }) => (
  <div style={{ textAlign: "center", color: K.muted, fontSize: 31, padding: "21px 0 0" }}>{children}</div>
);

/** The "Kwela is typing" bubble. */
export const TypingDots = ({ delay = 0 }: { delay?: number }) => (
  <Appear delay={delay}>
    <div style={{ display: "inline-flex", gap: 16, background: K.bubble, borderRadius: 63, padding: "42px 47px" }}>
      {[0.9, 0.35, 0.35].map((o, i) => <div key={i} style={{ width: 21, height: 21, borderRadius: 11, background: K.muted, opacity: o }} />)}
    </div>
  </Appear>
);

export const Chips = ({ labels, delay = 0 }: { labels: string[]; delay?: number }) => (
  <Appear delay={delay} style={{ display: "flex", flexWrap: "wrap", gap: 21, paddingLeft: 63 }}>
    {labels.map((l) => (
      <div key={l} style={{ background: K.user, color: K.onUser, borderRadius: 52, padding: "31px 42px", fontSize: 37, fontWeight: 500 }}>{l}</div>
    ))}
  </Appear>
);

const FileIcon = () => (
  <svg width="58" height="58" viewBox="0 0 24 24" fill="none" stroke={K.primary} strokeWidth="1.8" strokeLinejoin="round"><path d="M6 2h8l5 5v15H6z" /><path d="M14 2v5h5" /></svg>
);

/** A finished document, as a card in the chat. */
export const ArtifactCard = ({ title, job, preview, delay = 0 }: { title: string; job: string; preview: string; delay?: number }) => (
  <Appear delay={delay}>
    <div style={{ width: 955, background: K.card, borderRadius: 52, padding: "37px 21px 16px 42px", display: "flex", flexDirection: "column", gap: 21 }}>
      <div style={{ display: "flex", alignItems: "center", gap: 26 }}>
        <FileIcon />
        <div>
          <div style={{ color: K.text, fontSize: 37, fontWeight: 700, lineHeight: "50px" }}>{title}</div>
          <div style={{ color: K.muted, fontSize: 31, lineHeight: "42px" }}>{job}</div>
        </div>
      </div>
      <div style={{ color: K.muted, fontSize: 37, lineHeight: "52px", paddingRight: 21, display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical", overflow: "hidden" }}>{preview}</div>
      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
        <div style={{ background: K.tonal, color: K.onTonal, borderRadius: 53, height: 105, padding: "0 63px", display: "flex", alignItems: "center", fontSize: 37, fontWeight: 500 }}>{copy.kwela.cards.buttons[0]}</div>
        <div style={{ color: K.primary, padding: "0 42px", fontSize: 37, fontWeight: 500 }}>{copy.kwela.cards.buttons[1]}</div>
        <div style={{ color: K.primary, padding: "0 42px", fontSize: 37, fontWeight: 500 }}>{copy.kwela.cards.buttons[2]}</div>
      </div>
    </div>
  </Appear>
);

/** The chip that shows a file waiting to be sent, above the message box. */
export const AttachmentChip = ({ name }: { name: string }) => (
  <div style={{ display: "inline-flex", alignItems: "center", gap: 18, background: K.card, color: K.text, borderRadius: 42, padding: "21px 21px 21px 36px", fontSize: 34 }}>
    {"\u{1F4CE}"} {name}
    <div style={{ width: 52, height: 52, borderRadius: 26, background: K.tonal, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 34, color: K.onTonal }}>×</div>
  </div>
);

const ChatIcon = ({ filled }: { filled?: boolean }) => (
  <svg width="64" height="64" viewBox="0 0 24 24" fill={filled ? K.text : "none"} stroke={K.text} strokeWidth="1.7" strokeLinejoin="round"><path d="M4 4h16v12H8l-4 4z" /></svg>
);
const FolderIcon = () => (
  <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke={K.text} strokeWidth="1.7" strokeLinejoin="round"><path d="M3 6h7l2 2h9v11H3z" /></svg>
);

/** The whole Kwela chat screen: header, messages, message box and the two-tab bar. */
export const KwelaChat = ({ children, pending, draft }: { children?: ReactNode; pending?: string; draft?: string }) => (
  <div style={{ position: "absolute", inset: 0, background: K.bg, fontFamily: "inherit" }}>
    <SystemBars color="#ffffff" />
    <Header />
    <div style={{ position: "absolute", top: 372, left: 0, right: 0, height: 1538, overflow: "hidden", padding: "12px 21px", display: "flex", flexDirection: "column", gap: 21 }}>{children}</div>
    {pending && <div style={{ position: "absolute", left: 21, bottom: 456 }}><AttachmentChip name={pending} /></div>}
    <div style={{ position: "absolute", left: 21, right: 21, top: 1960, height: 147, background: K.bubble, borderRadius: 74, display: "flex", alignItems: "center", gap: 6 }}>
      <svg width="126" height="63" viewBox="0 0 24 24" style={{ marginLeft: 0 }}><g transform="translate(-1,0)"><circle cx="12" cy="12" r="9" fill="none" stroke={K.muted} strokeWidth="1.6" /><path d="M12 7.5v9M7.5 12h9" stroke={K.muted} strokeWidth="1.6" /></g></svg>
      <div style={{ color: draft ? K.text : K.muted, fontSize: 42 }}>{draft ?? copy.kwela.inputPlaceholder}</div>
    </div>
    <div style={{ position: "absolute", left: 42, right: 42, top: 2148, height: 168, background: K.bubble, borderRadius: 84 }}>
      <div style={{ position: "absolute", left: 176, top: 26, width: 167, height: 116, borderRadius: 58, background: K.navSelected, display: "flex", alignItems: "center", justifyContent: "center" }}><ChatIcon /></div>
      <div style={{ position: "absolute", left: 700, top: 52 }}><FolderIcon /></div>
    </div>
  </div>
);
