#!/usr/bin/env python3
"""Generates the four MVP avatar state animations (SMIL SVG) into artwork/avatar-states/.

States: ready, working, waiting, complete. Every animation loops seamlessly and
animates only transforms/opacity so frames cannot tear or leave artefacts.
Run: python3 artwork/build_avatar_states.py
"""
from pathlib import Path

OUT = Path(__file__).parent / "avatar-states"
INK, CREAM, SKIN, TEAL, SCARF, RED = "#302824", "#f7f5e9", "#49332d", "#18aa9d", "#85b4ed", "#e90037"
EASE = "0.45 0 0.55 1"
NECK = "200 300"

SHOULDERS = "M-8 398 C4 348 48 323 94 308 C125 298 154 289 200 288 C248 288 278 298 308 310 C356 329 390 352 410 400Z"
SCARF_D = ("M66 315 C83 301 94 289 104 276 C95 252 94 224 96 195 C100 133 126 82 172 64 C207 50 240 58 272 76 "
           "C306 96 323 127 326 165 C331 207 318 245 302 277 C291 300 278 320 259 337 C243 352 227 365 210 368 "
           "C190 369 174 359 158 350 C128 334 103 317 81 306Z")
FACE_D = ("M143 182 C148 150 169 120 195 113 C219 106 242 119 258 140 C275 161 280 185 277 211 C274 240 260 268 243 282 "
          "C225 297 201 299 181 287 C158 274 145 247 140 218 C138 205 139 194 143 182Z")
GLASSES_D = ("M119 165 C127 160 139 164 152 168 C170 174 183 177 197 175 C210 173 217 177 228 177 C244 178 260 170 278 170 "
             "C287 170 297 172 301 177 C305 182 300 187 296 192 L277 213 C269 222 262 227 254 226 C244 225 234 216 224 212 "
             "C216 208 209 207 202 211 C191 216 183 225 172 226 C158 227 148 221 141 213 C131 202 120 189 114 180 "
             "C110 174 113 168 119 165Z")


def spline(n):
    return ";".join([EASE] * (n - 1))


def times(ts):
    return ";".join(f"{t:g}" for t in ts)


def anim(kind, ts, values, dur, attr="transform", extra=""):
    """Looping eased animation. ts are keyTimes (0..1), values same length."""
    assert len(ts) == len(values) and ts[0] == 0 and ts[-1] == 1 and values[0] == values[-1], (ts, values)
    tag = "animateTransform" if kind else "animate"
    typ = f' type="{kind}"' if kind else ""
    return (f'<{tag} attributeName="{attr}"{typ} dur="{dur}s" repeatCount="indefinite" calcMode="spline" '
            f'keyTimes="{times(ts)}" keySplines="{spline(len(ts))}" values="{";".join(values)}"{extra}/>')


def rot(ts, degs, dur, pivot=NECK):
    return anim("rotate", ts, [f"{d:g} {pivot}" for d in degs], dur)


def move(ts, pts, dur):
    return anim("translate", ts, [f"{x:g} {y:g}" for x, y in pts], dur)


def mirror(d):
    """Mirror an absolute 'M/Q/L/C... Z' path across x=199 (swap x -> 398 - x)."""
    out, nums, cmd = [], [], ""
    toks = d.replace(",", " ").replace("M", " M ").replace("Q", " Q ").replace("L", " L ").replace("Z", " Z ").split()
    res, i = [], 0
    while i < len(toks):
        t = toks[i]
        if t in "MQLZ":
            res.append(t)
            i += 1
        else:
            res.append(f"{398 - float(t):g}")
            res.append(toks[i + 1])
            i += 2
    return " ".join(res)


def stroke(w, extra=""):
    return f'stroke="{CREAM}" stroke-width="{w}" stroke-linejoin="round" stroke-linecap="round" {extra}'


def head(anim_tags, face_anim=""):
    """Scarf + face + glasses move together about the neck so nothing separates.

    face_anim moves only the face and glasses inside the scarf (e.g. looking down).
    """
    return (f'<g>{anim_tags}'
            f'<path fill="{SCARF}" {stroke(10)} d="{SCARF_D}"/>'
            f'<g>{face_anim}'
            f'<path fill="{SKIN}" {stroke(6)} d="{FACE_D}"/>'
            f'<path fill="#17191a" stroke="#242120" stroke-width="10" stroke-linejoin="round" d="{GLASSES_D}"/>'
            f'<path fill="none" {stroke(6)} d="{GLASSES_D}"/></g></g>')


def hand(d):
    return f'<path fill="{SKIN}" {stroke(6)} d="{d}"/>'


def svg(title, desc, body):
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 400" role="img" aria-labelledby="t d">
<title id="t">{title}</title><desc id="d">{desc}</desc>
<defs><clipPath id="circle"><circle cx="200" cy="200" r="190"/></clipPath></defs>
<g clip-path="url(#circle)">
<path fill="{TEAL}" d="M0 0h400v400H0z"/>
{body}
</g>
<circle cx="200" cy="200" r="190" fill="none" stroke="{CREAM}" stroke-width="10"/>
</svg>
'''


def body_shape(anim_tags=""):
    return f'<g>{anim_tags}<path fill="{RED}" {stroke(10)} d="{SHOULDERS}"/></g>'


# ---------- ready: calm breathing, slow head sway ----------
def ready():
    breathe = move([0, .5, 1], [(0, 0), (0, 3), (0, 0)], 4)
    sway = rot([0, .5, 1], [-2, 2, -2], 8)
    # head follows the breath by wrapping the swaying head in a translating group
    inner = head(sway)
    return svg("Ready", "She faces forward, calm and available.",
               f'<g>{breathe}{body_shape()}{inner}</g>')


# ---------- working: reads, then turns a page ----------
LP = "M199 349 Q146 322 91 337 L91 395 Q150 383 199 405Z"
RP = mirror(LP)


def page_lines(side):
    # three faint text lines per page, following the page curve
    if side == "L":
        return "M108 352q34-6 69 8 M108 366q34-5 69 8 M108 380q34-5 69 8"
    return "M221 360q35-14 70-8 M221 374q35-14 70-8 M221 388q35-14 70-8"


def working():
    D = 6
    lean = rot([0, .35, .6, 1], [3, 6, 6, 3], D)
    gaze = move([0, .35, .6, 1], [(0, 14), (0, 20), (0, 20), (0, 14)], D)
    page_line_attr = f'fill="none" stroke="#92a9ad" stroke-width="4" stroke-linecap="round"'
    # right page turns about the spine: scaleX 1 -> 0 -> -1, lifting slightly mid-turn.
    flip_scale = anim("scale", [0, .5, .56, .62, .96, .98, 1], ["1 1", "1 1", "0 1.04", "-1 1", "-1 1", "1 1", "1 1"], D)
    flip_lift = anim("translate", [0, .5, .56, .62, 1], ["0 0", "0 0", "0 -8", "0 0", "0 0"], D)
    # once turned it matches the static left page; fade it, reset hidden, then show again
    flip_fade = anim(None, [0, .9, .95, .99, 1], ["1", "1", "0", "0", "1"], D, attr="opacity")
    flip = (f'<g transform="translate(199 0)"><g>{flip_scale}<g transform="translate(-199 0)"><g>{flip_lift}'
            f'<g>{flip_fade}<path fill="{CREAM}" stroke="{INK}" stroke-width="5" stroke-linejoin="round" d="{RP}"/>'
            f'<path d="{page_lines("R")}" {page_line_attr}/></g></g></g></g></g>')
    book = (f'<path fill="{CREAM}" stroke="{INK}" stroke-width="5" stroke-linejoin="round" d="{LP}"/>'
            f'<path d="{page_lines("L")}" {page_line_attr}/>'
            f'<path fill="{CREAM}" stroke="{INK}" stroke-width="5" stroke-linejoin="round" d="{RP}"/>'
            f'<path d="{page_lines("R")}" {page_line_attr}/>'
            f'<path d="M199 349V405" stroke="#92a9ad" stroke-width="4"/>')
    hl = hand("M72 398 Q70 366 84 352 Q95 342 108 350 L122 364 L122 398Z")
    hr = hand("M326 398 Q328 366 314 352 Q303 342 290 350 L276 364 L276 398Z")
    bob = move([0, .5, 1], [(0, 0), (0, 2), (0, 0)], 3)
    return svg("Working", "She leans over an open book and turns a page while the assistant works.",
               f'{body_shape()}{head(lean, gaze)}<g transform="translate(0 -44)"><g>{bob}{book}{flip}{hl}{hr}</g></g>')


# ---------- waiting for input: looks up, tilts, a bubble with pulsing dots ----------
def waiting():
    D = 3
    tilt = rot([0, .3, .7, 1], [0, -9, -9, 0], D)
    look = move([0, .3, .7, 1], [(0, 0), (0, -4), (0, -4), (0, 0)], D)
    dots = ""
    for i, cx in enumerate((284, 306, 328)):
        s = 0.18 * i  # stagger
        dots += (f'<circle cx="{cx}" cy="78" r="6" fill="{TEAL}"><animate attributeName="r" dur="{D}s" '
                 f'repeatCount="indefinite" calcMode="spline" keyTimes="0;{0.30 + s / 2:g};{0.45 + s / 2:g};1" '
                 f'keySplines="{spline(4)}" values="5;5;8;5"/></circle>')
    bubble_pop = anim("scale", [0, .22, .3, .9, 1], ["0", "0", "1", "1", "0"], D)
    bubble = (f'<g transform="translate(306 78)"><g>{bubble_pop}<g transform="translate(-306 -78)">'
              f'<path fill="{CREAM}" stroke="{INK}" stroke-width="5" stroke-linejoin="round" '
              f'd="M270 50 H342 Q356 50 356 64 V92 Q356 106 342 106 H304 L288 124 L290 106 H270 Q256 106 256 92 V64 Q256 50 270 50Z"/>'
              f'{dots}</g></g></g>')
    return svg("Waiting for input", "She looks up and tilts her head while a chat bubble pulses, inviting a reply.",
               f'<g>{look}{body_shape()}{head(tilt)}</g>{bubble}')


# ---------- complete: holds up the finished document, check draws in, sparkles ----------
def sparkle(cx, cy, r, delay):
    d = f"M{cx} {cy - r} Q{cx} {cy} {cx + r} {cy} Q{cx} {cy} {cx} {cy + r} Q{cx} {cy} {cx - r} {cy} Q{cx} {cy} {cx} {cy - r}Z"
    t0 = 0.30 + delay
    pop = anim("scale", [0, t0, t0 + .08, t0 + .25, 1], ["0", "0", "1", "0", "0"], 5)
    return (f'<g transform="translate({cx} {cy})"><g>{pop}<g transform="translate({-cx} {-cy})">'
            f'<path fill="{CREAM}" d="{d}"/></g></g></g>')


def complete():
    D = 5
    rise_ts = [0, .15, .22, .85, .95, 1]
    rise = anim("translate", rise_ts, ["0 130", "0 130", "0 0", "0 0", "0 130", "0 130"], D)
    nod = rot([0, .2, .3, .45, .85, .95, 1], [0, 0, 6, 0, 0, 0, 0], D)
    doc_d = "M126 296 H274 V430 H126Z"
    check_len = 60
    check = (f'<path d="M168 346 L192 370 L234 318" fill="none" stroke="{TEAL}" stroke-width="12" stroke-linecap="round" '
             f'stroke-linejoin="round" stroke-dasharray="{check_len + 20}" stroke-dashoffset="0">'
             f'<animate attributeName="stroke-dashoffset" dur="{D}s" repeatCount="indefinite" calcMode="spline" '
             f'keyTimes="0;.22;.38;1" keySplines="{spline(4)}" values="{check_len + 20};{check_len + 20};0;0"/></path>')
    doc = (f'<path fill="{CREAM}" stroke="{INK}" stroke-width="5" stroke-linejoin="round" d="{doc_d}"/>'
           f'<path d="M148 316h104" stroke="#92a9ad" stroke-width="4" stroke-linecap="round"/>{check}')
    hl = hand("M104 410 Q98 376 110 356 Q122 342 138 352 L146 366 L146 410Z")
    hr = hand("M296 410 Q302 376 290 356 Q278 342 262 352 L254 366 L254 410Z")
    props = f'<g>{rise}{doc}{hl}{hr}</g>'
    sparks = sparkle(92, 150, 20, 0) + sparkle(312, 120, 24, .05) + sparkle(76, 250, 14, .1) + sparkle(330, 232, 16, .15)
    return svg("Complete", "She holds up the finished document with a check mark and nods, with small sparkles.",
               f'{body_shape()}{head(nod)}{props}{sparks}')


STATES = {"ready": ready, "working": working, "waiting": waiting, "complete": complete}

if __name__ == "__main__":
    OUT.mkdir(exist_ok=True)
    for name, fn in STATES.items():
        (OUT / f"{name}.svg").write_text(fn())
        print("wrote", OUT / f"{name}.svg")
