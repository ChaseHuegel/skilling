export interface FormattedSegment {
    text: string
    color?: string
    bold?: boolean
    italic?: boolean
    underline?: boolean
    strikethrough?: boolean
}

const COLOR_MAP: Record<string, string> = {
    '0': '#000000', '1': '#0000AA', '2': '#00AA00', '3': '#00AAAA',
    '4': '#AA0000', '5': '#AA00AA', '6': '#FFAA00', '7': '#AAAAAA',
    '8': '#555555', '9': '#5555FF', 'a': '#55FF55', 'b': '#55FFFF',
    'c': '#FF5555', 'd': '#FF55FF', 'e': '#FFFF55', 'f': '#FFFFFF',
}

export function parseAmpersandCodes(text: string): FormattedSegment[] {
    const segments: FormattedSegment[] = []
    let current: FormattedSegment = { text: '' }
    let color = ''
    let bold = false
    let italic = false
    let underline = false
    let strikethrough = false

    for (let i = 0; i < text.length; i++) {
        if (text[i] === '&' && i + 1 < text.length) {
            const code = text[i + 1].toLowerCase()
            if (COLOR_MAP[code] !== undefined) {
                if (current.text) {
                    segments.push({ ...current })
                }
                color = COLOR_MAP[code]
                bold = false
                italic = false
                underline = false
                strikethrough = false
                current = { text: '', color }
                i++
                continue
            }
            if (code === 'l') { bold = true; current.bold = true; i++; continue }
            if (code === 'o') { italic = true; current.italic = true; i++; continue }
            if (code === 'n') { underline = true; current.underline = true; i++; continue }
            if (code === 'm') { strikethrough = true; current.strikethrough = true; i++; continue }
            if (code === 'r') {
                if (current.text) segments.push({ ...current })
                color = ''
                bold = false
                italic = false
                underline = false
                strikethrough = false
                current = { text: '' }
                i++
                continue
            }
        }
        current.text += text[i]
    }
    if (current.text) segments.push({ ...current })
    return segments
}

export function renderFormattedText(segments: FormattedSegment[]): string {
    return segments.map(s => {
        const styles: string[] = []
        if (s.color) styles.push(`color:${s.color}`)
        if (s.bold) styles.push('font-weight:bold')
        if (s.italic) styles.push('font-style:italic')
        if (s.underline) styles.push('text-decoration:underline')
        if (s.strikethrough) styles.push('text-decoration:line-through')
        if (s.underline && s.strikethrough) styles[styles.length - 1] = 'text-decoration:underline line-through'
        const style = styles.length > 0 ? ` style="${styles.join(';')}"` : ''
        return `<span${style}>${escapeHtml(s.text)}</span>`
    }).join('')
}

function escapeHtml(str: string): string {
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}
