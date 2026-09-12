# SUBZERO Design System Specification

Status: v1, Phase 3. This is the contract every screen is built against. Tokens live in
`core:designsystem` (`SubzeroTheme.colors / typography / spacing / shapes / motion`); nothing in a
feature module uses a raw hex value, dp literal for spacing, or ad-hoc animation duration.

The reference image is the quality benchmark, not a template. What we take from it: deep navy
ground, a single icy accent used sparingly, glass-like layered cards with hairline borders, large
tabular financial numbers, calm density, restrained glow, motion that explains hierarchy.

---

## 1. Color

### 1.1 Semantic tokens

| Token | Dark | Light | Use |
|---|---|---|---|
| `background` | `#070B14` | `#F4F7FC` | page ground |
| `surface` | `#0E1526` | `#FFFFFF` | cards, sheets |
| `surfaceElevated` | `#141D33` | `#FFFFFF` | elevated cards, dialogs, bottom sheets |
| `surfaceGlass` | `surface @ 72%` | `surface @ 85%` | translucent cards over a glow |
| `surfaceSubtle` | `#FFFFFF @ 4%` | `#0B1220 @ 4%` | icon containers, chips (rest) |
| `outline` | `#7AA2FF @ 12%` | `#1E3A8A @ 12%` | 1dp hairline borders |
| `outlineStrong` | `#7AA2FF @ 28%` | `#1E3A8A @ 28%` | focused fields, selected chips |
| `outlineHighlight` | `#FFFFFF @ 6%` | transparent | 1dp top-edge sheen on cards |
| `accent` | `#4DA3FF` | `#1F6FEB` | primary actions, active nav, links |
| `accentBright` | `#5EE7FF` | `#0EA5E9` | glow, hero emphasis (sparingly) |
| `accentContainer` | `accent @ 14%` | `accent @ 12%` | accent icon containers |
| `onAccent` | `#05101F` | `#FFFFFF` | text on accent fills |
| `secondaryAccent` | `#6F7CFF` | `#5B5FE0` | AI identity, secondary glow |
| `textPrimary` | `#F2F6FF` | `#0B1220` | |
| `textSecondary` | `#9AA8C7` | `#4B5876` | supporting copy |
| `textTertiary` | `#5F6D8C` | `#8A96B3` | metadata, placeholders |
| `positive` / `positiveContainer` | `#3DDC97` / `@ 14%` | `#1B9E6A` / `@ 12%` | savings, success |
| `warning` / `warningContainer` | `#F5B84B` / `@ 14%` | `#B7791F` / `@ 12%` | price increases, attention |
| `danger` / `dangerContainer` | `#FF5C6C` / `@ 14%` | `#D64550` / `@ 12%` | destructive, errors |
| `scrim` | `#000000 @ 60%` | `#0B1220 @ 40%` | behind sheets and dialogs |

Status is never communicated by color alone: every positive/warning/danger use pairs with an icon or a word.

### 1.2 Gradients

| Token | Definition | Use |
|---|---|---|
| `heroGlow` | radial, `accentBright @ 18%` → transparent, centred top-right, radius ≈ 1.2× card width | hero card only |
| `cardSheen` | vertical, `#FFFFFF @ 4%` → `@ 0%` over the top 40% | glass and hero cards |
| `accentButton` | linear 135°, `accent` → `accentBright` | primary button fill |
| `onboardingGlow` | radial, `secondaryAccent @ 22%` → transparent, centred bottom | onboarding pages |
| `chartFill` | vertical, `accent @ 28%` → `@ 0%` | area under line charts |

Glow is allowed on: hero money, primary CTA, active nav indicator, AI identity, onboarding. Nowhere else.

### 1.3 Contrast

Text on `surface` (dark): primary 15.1:1, secondary 7.2:1, tertiary 4.6:1 (AA, used only at ≥ 13sp).
`accent` on `background`: 7.8:1. `onAccent` on `accent`: 8.9:1.

---

## 2. Typography

Typeface: **Inter** (variable, bundled, SIL OFL). Fallback: system sans-serif.
Money styles enable tabular figures (`tnum`) so digits keep constant width and animated values do not jitter.

| Style | Size / line | Weight | Letter-spacing | Use |
|---|---|---|---|---|
| `display` | 44 / 52 | 600 | −0.5 | onboarding titles |
| `headline` | 28 / 34 | 600 | −0.3 | screen titles |
| `title` | 20 / 26 | 500 | 0 | section titles, dialog titles |
| `body` | 16 / 24 | 400 | 0 | primary copy |
| `bodySmall` | 14 / 20 | 400 | 0 | secondary copy |
| `label` | 13 / 18 | 500 | 0 | buttons, chips, field labels |
| `caption` | 11 / 16 | 400 | +0.2 | metadata, nav labels |
| `moneyHero` | 44 / 52 | 700 | −1.0 | the dashboard number |
| `moneyLarge` | 28 / 34 | 600 | −0.5 | section totals, detail header |
| `money` | 16 / 24 | 500 | 0 | rows, lists |
| `moneySmall` | 13 / 18 | 500 | 0 | captions with amounts |

Weights used: 400, 500, 600, 700 only. All sizes are `sp` and scale with system font size; layouts are verified at 200%.

---

## 3. Spacing

4pt grid: `xxs 4 · xs 8 · sm 12 · md 16 · lg 20 · xl 24 · xxl 32 · xxxl 40 · huge 48`.
Screen gutter `20`. Card padding `16` (compact) / `20` (hero). Vertical rhythm between cards `12`, between sections `24`.
Minimum touch target 48×48dp; list rows are ≥ 64dp tall.

---

## 4. Shape

| Token | Radius | Use |
|---|---|---|
| `sm` | 10 | chips, small icon containers, text fields |
| `md` | 16 | standard cards, buttons |
| `lg` | 22 | hero card, dialogs |
| `xl` | 28 | bottom sheet top corners |
| `full` | pill | nav indicator, toggles, tags |

Icon containers: 44dp `sm`-rounded squares (list rows), 56dp `md` (detail header). Service monograms sit in these.

---

## 5. Depth

In dark mode shadows are invisible on near-black and cost GPU, so depth is built from layers:

| Level | Recipe |
|---|---|
| 0 ground | `background` |
| 1 card | `surface` + 1dp `outline` border + `cardSheen` |
| 1 glass | `surfaceGlass` + 1dp `outline` + `cardSheen` + `outlineHighlight` top edge |
| 2 hero | `surfaceElevated`→`surface` vertical gradient + `heroGlow` + 1dp `outlineStrong @ 60%` |
| 3 overlay (sheet, dialog) | `surfaceElevated` + `scrim` behind + 1dp `outline`; light mode adds a 12dp soft shadow |

---

## 6. Motion

### 6.1 Durations and easing

| Token | Duration | Easing | Use |
|---|---|---|---|
| `fast` | 120ms | standard | press feedback, toggles, chip selection |
| `standard` | 220ms | standard | state changes, tab switch, number transitions |
| `emphasized` | 350ms | emphasized-decelerate (enter) / emphasized-accelerate (exit) | screen push/pop, card expansion, sheets |
| `reveal` | 400ms | emphasized-decelerate | charts drawing, insight reveal |
| `stagger` | 40ms per item, capped at 6 items | | lists, insight cards |

Curves: standard `cubic(0.2, 0, 0, 1)`, emphasized-decelerate `cubic(0.05, 0.7, 0.1, 1)`, emphasized-accelerate `cubic(0.3, 0, 0.8, 0.15)`.
No springs with visible overshoot; the only spring is press scale (stiffness high, damping ratio 1).

**Reduced motion:** when the system animator duration scale is 0, all durations are 0 and stagger is removed. Content still appears; it just does not animate.

### 6.2 Navigation transitions

| Transition | Enter | Exit |
|---|---|---|
| Tab switch | fade in 220ms + scale 0.98→1 | fade out 220ms |
| Push (list → detail) | slide in from end 24dp + fade, 350ms | fade to 0.9 alpha, scale 1→0.98 |
| Pop | reverse of push | |
| Predictive back | follows gesture progress; commit finishes with pop | |
| Modal (add/edit) | slide up from bottom, 350ms, scrim fades | slide down |
| Onboarding → main | fade-through 350ms | |
| Shared element (row → detail header, Phase 5) | bounds transform 350ms emphasized | |

### 6.3 Micro-interactions

| Interaction | Behaviour |
|---|---|
| Button press | scale 0.97, 120ms; release springs back |
| Card press | scale 0.985 + border → `outlineStrong` |
| Toggle | thumb slides 220ms; track color crossfades |
| Bottom nav | pill indicator slides between items 220ms; icon tint crossfades |
| Money change | old value fades/slides up 8dp out, new value slides in from below, 220ms |
| Chart | bars grow from baseline / line draws left→right over 400ms on first appearance |
| List insert/remove | animateItem: fade + size, 220ms |
| Save success | check mark scales in (emphasized), summary card fades in, auto-return after 1.2s |

---

## 7. Components

Every component has previews for: dark, light, empty, populated, long text, large numbers, disabled/error where applicable, and 200% font scale.

| Component | Variants | States |
|---|---|---|
| `SubzeroButton` | primary (gradient), secondary (outline), ghost, danger | enabled, pressed, disabled (40% alpha), loading |
| `SubzeroCard` | standard, glass, hero | rest, pressed (when clickable) |
| `SubzeroTextField` | text, number, multiline; leading/trailing slot | rest, focused (`outlineStrong`), error (danger border + message), disabled |
| `SubzeroTopBar` | title only, title + back, title + actions | large title collapses on scroll (Phase 5) |
| `SubzeroBottomBar` | 5 items | selected (accent + pill), unselected |
| `SubzeroMoneyText` | hero, large, regular | animates on value change |
| `SubzeroSubscriptionRow` | with/without category tag, with status badge | rest, pressed, paused/canceled (muted) |
| `SubzeroInsightCard` | info, positive, warning | with/without action |
| `SubzeroServiceIcon` | monogram (deterministic color per name), category glyph | 44 / 56dp |
| `SubzeroChart` | bar, line | animated on first appearance |
| `SubzeroChip` | filter, choice | selected, unselected |
| `SubzeroSwitch` | | on, off, disabled |
| `SubzeroEmptyState` | with illustration slot + CTA | |
| `SubzeroErrorState` | inline, full-screen; retry action | |
| `SubzeroSkeleton` | row, card, hero | shimmer (disabled under reduced motion) |
| `SubzeroDialog` | confirm, destructive | |
| `SubzeroBottomSheet` | | drag handle, scrim |
| `SectionHeader` | title, title + action | |

### Monogram colors
The service monogram color is `hash(name) mod 8` over a fixed palette of 8 desaturated hues
(blue, violet, teal, green, amber, coral, rose, slate) at 22% container alpha with the hue at
full strength for the letter. Deterministic, so the same service always looks the same.

---

## 8. Iconography

Outlined style, 24dp, 1.75dp stroke feel. Source: Material Symbols core set for common actions
(home, list, calendar, settings, add, search, check, close, back, edit, delete, notifications,
chevron) plus hand-drawn SUBZERO vectors for identity glyphs (insights, pause, sparkle). One
object, `SubzeroIcons`, exposes all of them; features never import Material icons directly so
the style stays uniform.

---

## 9. Accessibility

- Every interactive element ≥ 48dp, with a role and a content description or visible label.
- Money is announced with currency and unit: "15 dollars 49 cents per month".
- Status conveyed with text or icon in addition to color.
- Text scales to 200% without clipping; long service names ellipsize at 1 line in rows, 2 in headers.
- Focus order follows visual order; bottom sheets trap focus.
- Reduced motion respected (see 6.1).

---

## 10. Responsive rules

- Content column max width 640dp, centred on tablets; cards stretch to it.
- Hero number scales down one step when the formatted value exceeds 12 characters.
- Bottom bar labels hide below 320dp width; icons remain with content descriptions.
- RTL: all horizontal paddings use `start`/`end`; chevrons and back arrows auto-mirror.
