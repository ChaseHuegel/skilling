# ISSUE-010: Minecraft Color Code Rendering + Lore Preview Plan

## Description

Proposal for rendering Minecraft `&` color codes inline in lore text
inputs and adding a realistic lore preview flyout in the skill editor.

## Current State

Lore lines in the web GUI are plain `<input>` text fields. Color codes
like `&a`, `&l`, `&o` are displayed as literal text. The backend
(`LegacyComponentSerializer.legacyAmpersand().deserialize()`) converts
these to styled Adventure `Component` objects for in-game display, but
the frontend has zero color code rendering infrastructure.

## Proposed Solution

### Phase 1: Color Code Utility

Create `src/utils/minecraftColors.ts` with functions to:

1. **`parseAmpersandCodes(text: string): FormattedSegment[]`**
   - Parse `&`-prefixed color/format codes into an array of segments
   - Each segment has: `{ text: string, color?: string, bold?: boolean, italic?: boolean, underline?: boolean, strikethrough?: boolean, obfuscated?: boolean }`
   - Handle all standard codes: `&0-&f` (colors), `&l` (bold), `&o` (italic),
     `&n` (underline), `&m` (strikethrough), `&k` (obfuscated), `&r` (reset)

2. **`renderFormattedText(segments: FormattedSegment[]): string`**
   - Output HTML string with inline styles for use with `v-html`
   - Map `&0`-`&f` to hex colors matching Minecraft's color palette
   - Map `&l` → `font-weight: bold`, `&o` → `font-style: italic`, etc.

### Phase 2: Inline Rendering in Lore Inputs

Replace plain `<input>` fields in `AbilitiesSection.vue` with a custom
`LoreLineEditor` component that:

- Shows the raw text in an editable `<textarea>` or `<input>` for editing
- Below/beside the input, renders a preview line using `v-html` with the
  parsed color codes
- The preview updates in real-time as the user types
- Uses a monospace font to match Minecraft's visual style

### Phase 3: Lore Preview Flyout

Create a `LorePreviewFlyout.vue` component:

- Appears as a popover/flyout when hovering or focusing a lore input
- Shows all lore lines rendered with full color/formatting
- Mimics the in-game item tooltip style:
  - Dark purple/purple background (`#1a1a2e` or similar)
  - White/colored text
  - Item name at top in the skill's accent color
  - Separator lines
- Positioned to avoid viewport overflow

### Phase 4: Integration

- Add `LorePreviewFlyout` to `AbilitiesSection.vue` lore section
- Consider adding a context toolbar with color code quick-insert buttons
  (`&a` `&l` `&o` `&7` `&c` `&e`, etc.)

## Minecraft Color Palette (for HTML mapping)

```
&0 → #000000  (black)       &8 → #555555  (dark gray)
&1 → #0000AA  (dark blue)   &9 → #5555FF  (blue)
&2 → #00AA00  (dark green)  &a → #55FF55  (green)
&3 → #00AAAA  (dark aqua)   &b → #55FFFF  (aqua)
&4 → #AA0000  (dark red)    &c → #FF5555  (red)
&5 → #AA00AA  (dark purple) &d → #FF55FF  (light purple)
&6 → #FFAA00  (gold)        &e → #FFFF55  (yellow)
&7 → #AAAAAA  (gray)        &f → #FFFFFF  (white)
```

## Risk

Low. All rendering is client-side only. No backend changes needed.
The `v-html` directive is used carefully (only with controlled input).
