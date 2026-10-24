# Kwela Material 3 color-role map

This map audits the current Android UI against [Material 3 color roles](https://m3.material.io/styles/color/roles). Use semantic `MaterialTheme.colorScheme` roles in custom UI. Let Material 3 components keep their built-in role mappings unless Kwela has a clear reason to override them.

Material groups its roles into primary, secondary, tertiary, error, surface, and outline. Accent color should carry the most important actions and selected states. Surface roles should carry most screen area, with container tones distinguishing cards, sheets, and dialogs. Every `on…` role belongs on its paired fill. The app uses wallpaper-derived dynamic colors on Android 12+ and the supplied static scheme below Android 12.

## Screen and component map

| Screen / component | Container or fill role | Text and icon roles | State / mapping notes |
| --- | --- | --- | --- |
| App root | `surface` | `onSurface` by default | Root surface replaces a generic background fill. |
| Scaffold content | `surface` | `onSurface` | Set explicitly to match the root and app bar. |
| Status and navigation system bars | `surface` | System bar icons use `onSurface` contrast, selected from the actual surface luminance | Compose updates bar colors and icon contrast from the active static or dynamic scheme. XML supplies matching light/dark fallback tokens before Compose starts. |
| Loading indicator | `surface` | `primary` indicator | Material 3 `CircularProgressIndicator` default. |
| Login brand mark | `primary` | `onPrimary` for the “K”; `onSurface` for wordmark | Primary is used as brand identity, not a second CTA. |
| Login heading | `surface` | `onSurface` | High-emphasis content. |
| Login instructions and data notice | `surface` | `onSurfaceVariant` | Supporting copy. |
| Email / OTP outlined fields | `surface` | `onSurface` input; `onSurfaceVariant` label and hint; `outline` border; `primary` focus and cursor; `error` invalid state | Keep `OutlinedTextField` defaults. Show an error state on the field itself if validation moves inline. |
| Continue / Sign in buttons | `primary` | `onPrimary` | Main login action. Material 3 `Button` defaults. |
| Login alternate action | `surface` | `primary` text | Low-emphasis `TextButton` default. |
| Top app bar | `surface` | `onSurface` title; `onSurfaceVariant` action icons by default | Material 3 app bar roles. |
| Bottom navigation bar | `surfaceContainer` | selected indicator `secondaryContainer` / selected icon `onSecondaryContainer`; unselected icon and labels `onSurfaceVariant` | Matches Material 3's navigation-area container role. Keep `NavigationBarItem` defaults so selected and unselected states remain distinct. |
| Empty-chat headline | `surface` | `onSurface` | High-emphasis welcome content. |
| Empty-chat supporting copy and section label | `surface` | `onSurfaceVariant` | Lower-emphasis explanatory content. |
| Recommended “Update my CV for a job” action | `primary` | `onPrimary` | Single high-emphasis action, subject to changing when the app knows whether the user has a CV. |
| Other quick-start actions | `surface` | `onSurface` labels; `outline` border | Outlined buttons are lower emphasis. Their chevrons inherit the button content color instead of adding another primary accent. |
| Conversation composer container | `surfaceContainer` | `onSurface` | Uses a distinct navigation/control region tone and replaces a shadow-only separator. |
| Composer outlined input | `surface` | `onSurface` input; `onSurfaceVariant` placeholder; `outline` border; `primary` focus | Material 3 `OutlinedTextField` defaults. |
| Attach icon button | `surfaceContainerLow` | `onSurfaceVariant` | Secondary utility action. |
| Send icon button | `primary` | `onPrimary`; disabled state from component defaults | High-emphasis submit action. |
| User message bubble | `primaryContainer` | `onPrimaryContainer` | Accent container identifies the user’s contribution. |
| Assistant message bubble | `surfaceContainerLow` | `onSurface` | Neutral container keeps assistant content readable without competing with user/action accents. |
| Attachment metadata in a message | bubble fill | `onSurfaceVariant` | Metadata remains subordinate to message content. |
| Active task card | `surfaceContainerLow` | `onSurface` title; `onSurfaceVariant` status; `primary` progress indicator | Progress uses primary to signal current work. |
| Cancel task action | `surfaceContainerLow` | `onSurfaceVariant` | Cancellation is secondary, not the screen’s primary action. |
| Files empty state | `surface` | `onSurface` heading; `onSurfaceVariant` explanation | Text hierarchy only. |
| Saved-document card | `surfaceContainerLow` | `onSurface` title; `onSurfaceVariant` metadata and file icon | Share and save actions use Material 3 text-button defaults (`primary`). |
| Document preview dialog | Material 3 dialog surface container | `onSurface` title; `onSurfaceVariant` body; `primary` confirm action | Keep `AlertDialog` defaults. |
| Settings section headings | `surface` | `onSurfaceVariant` | Section labels are lower emphasis than values. |
| Account and preference cards | `surfaceContainerLow` | `onSurface` values; `onSurfaceVariant` descriptions | Neutral grouped content. |
| Notifications switch | card surface | Material 3 switch defaults: `primary` selected state and matching `onPrimary`; neutral surface roles when off | Keep component defaults. |
| Privacy explanation | `surface` | `onSurfaceVariant` | Supporting information. |
| Sign-out button | `surface` | `onSurface` with `outline` border | Outlined medium-emphasis action; signing out does not delete data. |
| Delete-local-data action and confirmation | dialog/card surface | `error` text and icon; dialog title `onSurface`; body `onSurfaceVariant`; keep action `primary`; destructive confirm `error` | Error role is reserved for the destructive action and error states. |
| Generated PDF text | document-specific neutral ink | black / dark gray | Exported document typography is not an app UI component and does not inherit the app color scheme. |
| Android notification small icon | Android system surface | Android monochrome notification icon treatment | System renders the small icon; do not apply app theme colors to it. |

## Role inventory

| Role group | Current use | Deliberately not used yet |
| --- | --- | --- |
| Primary | `primary` / `onPrimary`: login, send, recommended CV action, brand mark, progress, and default text buttons. `primaryContainer` / `onPrimaryContainer`: user message bubble. | Avoid using primary for decorative arrows, metadata, or every icon. |
| Secondary | `secondaryContainer` / `onSecondaryContainer`: selected navigation indicator and icon through the Material component default. | `secondary` / `onSecondary` have no custom component yet. |
| Tertiary | None of the four roles is assigned to custom UI. | Reserve for a purposeful contrasting category or badge, not decoration. |
| Error | `error`: validation feedback and both delete-local-data controls. | `onError`, `errorContainer`, and `onErrorContainer` are available for filled error content but are not needed by current surfaces. |
| Surface | `surface` / `onSurface`: app canvas, system bars, top app bar, text, and neutral content. `onSurfaceVariant`: supporting content, metadata, secondary icons. `surfaceContainer`: navigation and composer regions. `surfaceContainerLow`: cards and assistant bubbles. `surfaceContainerHigh` and `surfaceContainerHighest`: Material component defaults such as dialogs, outlined fields, and off-state controls. | `background` / `onBackground` remain in the supplied generated scheme but are not used by custom UI. `surfaceVariant`, `surfaceDim`, `surfaceBright`, and `surfaceContainerLowest` are not explicitly assigned. |
| Outline | `outline`: outlined fields and buttons through component defaults. | `outlineVariant` is for decorative separators, none are drawn manually. |
| Inverse / other | Dialog scrim is supplied by the Material component. | `inverseSurface`, `inverseOnSurface`, `inversePrimary`, and fixed accent add-ons are unused because the app has no inverse snackbar or fixed-accent need. |

## Audit changes

- Replaced the composer's shadow-only separation with a semantic tonal container.
- Mapped the navigation bar and composer control region to `surfaceContainer`, matching Material 3's default navigation-area hierarchy.
- Kept the app canvas on the semantic `surface` role.
- Removed primary-colored decoration from secondary action chevrons, attachment metadata, and file icons.
- Colored both delete-action icon and text with `error`, including the confirmation action.
- Kept `primary` for the leading CV action and actual submit/sign-in actions.

