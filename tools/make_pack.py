#!/usr/bin/env python3
"""
Erzeugt das Resourcepack "CasinoPack" für das Craft-Casino-Plugin.

  python3 tools/make_pack.py            -> pack/ und CasinoPack.zip

Alle Texturen werden prozedural gezeichnet (Pixel-Art), es werden keine
Mojang-Texturen kopiert.
"""
import json, math, os, random, shutil, zipfile
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "pack")
NS = "ccasino"
T = 32  # Kachelgröße
random.seed(7)

# ------------------------------------------------------------------ Farben
def rgb(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)

FELT, FELT_D, FELT_L = rgb("15704d"), rgb("0e5539"), rgb("1c8a5f")
PANEL, PANEL_L = rgb("15191c"), rgb("232a2e")
WOOD, WOOD_D, WOOD_L = rgb("6e4020"), rgb("3a1f0e"), rgb("8c5a30")
GOLD, GOLD_D, GOLD_L = rgb("f5c542"), rgb("b07a1e"), rgb("fff0b0")
RED, RED_D = rgb("c8283c"), rgb("7a0e1c")
BLK, BLK_L = rgb("1a1a1e"), rgb("34343c")
GRN = rgb("1e9e52")
WHITE = rgb("ffffff")
CREAM, CREAM_D = rgb("f4eee2"), rgb("d9cfbe")
CAB, CAB_D = rgb("7a1e3e"), rgb("2a0a18")
SKY, SKY_D = rgb("0b1630"), rgb("060c1c")
LED_BG = rgb("0a0a0c")

# ------------------------------------------------------------------ 3x5-Pixelschrift
FONT = {
 "A":"010101111101101","B":"110101110101110","C":"011100100100011","D":"110101101101110",
 "E":"111100110100111","F":"111100110100100","G":"011100101101011","H":"101101111101101",
 "I":"111010010010111","J":"001001001101010","K":"101101110101101","L":"100100100100111",
 "M":"101111111101101","N":"110101101101101","O":"010101101101010","P":"110101110100100",
 "Q":"010101101110011","R":"110101110101101","S":"011100010001110","T":"111010010010010",
 "U":"101101101101111","V":"101101101101010","W":"101101111111101","X":"101101010101101",
 "Y":"101101010010010","Z":"111001010100111",
 "0":"111101101101111","1":"010110010010111","2":"110001010100111","3":"110001010001110",
 "4":"101101111001001","5":"111100110001110","6":"011100111101111","7":"111001010010010",
 "8":"111101111101111","9":"111101111001110",
 "!":"010010010000010",".":"000000000000010","-":"000000111000000","+":"000010111010000",
 ":":"000010000010000","/":"001001010100100","x":"000101010101000"," ":"000000000000000",
 "?":"110001010000010","$":"011110010011110","<":"001010100010001",">":"100010001010100",
}

def text_w(s, sc=1):
    return (len(s) * 4 - 1) * sc

def text(img, s, x, y, col, sc=1, shadow=None):
    px = img.load()
    for ch in s.upper() if s not in ("x",) else s:
        g = FONT.get(ch, FONT["?"])
        for i, b in enumerate(g):
            if b == "1":
                gx, gy = i % 3, i // 3
                for dx in range(sc):
                    for dy in range(sc):
                        X, Y = x + gx * sc + dx, y + gy * sc + dy
                        if shadow and 0 <= X + sc < img.width and 0 <= Y + sc < img.height:
                            px[X + sc, Y + sc] = shadow if px[X + sc, Y + sc][3] else px[X + sc, Y + sc]
                        if 0 <= X < img.width and 0 <= Y < img.height:
                            px[X, Y] = col
        x += 4 * sc

def text_c(img, s, cx, y, col, sc=1, shadow=None):
    text(img, s, cx - text_w(s, sc) // 2, y, col, sc, shadow)

# ------------------------------------------------------------------ Grundformen
def tile(bg=FELT, noise=True, size=T):
    im = Image.new("RGBA", (size, size), bg)
    if noise:
        px = im.load()
        for _ in range(size * size // 9):
            x, y = random.randrange(size), random.randrange(size)
            f = random.choice((0.93, 1.06))
            r, g, b, a = px[x, y]
            px[x, y] = (min(255, int(r * f)), min(255, int(g * f)), min(255, int(b * f)), a)
    return im

def shade(c, f):
    if f > 1:
        t = f - 1
        return tuple(int(c[i] + (255 - c[i]) * t) for i in range(3)) + (c[3],)
    return tuple(int(c[i] * f) for i in range(3)) + (c[3],)

def disc(d, cx, cy, r, col):
    d.ellipse([cx - r, cy - r, cx + r - 1, cy + r - 1], fill=col)

def rrect(d, box, col, r=3, outline=None):
    d.rounded_rectangle(box, r, fill=col, outline=outline)

def vgrad(im, box, top, bot):
    x0, y0, x1, y1 = box
    d = ImageDraw.Draw(im)
    for y in range(y0, y1):
        t = (y - y0) / max(1, (y1 - y0 - 1))
        d.line([x0, y, x1 - 1, y], fill=tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(4)))

TEXTURES = {}  # name -> Image

def add(name, im):
    TEXTURES[name] = im

# ------------------------------------------------------------------ Basis-Kacheln
add("felt", tile(FELT))
add("panel", tile(PANEL))
add("wood", tile(WOOD))

def bulb(col, on):
    im = tile(WOOD)
    d = ImageDraw.Draw(im)
    disc(d, 16, 17, 10, WOOD_D)
    if on:
        disc(d, 16, 16, 9, shade(col, 0.55))
        disc(d, 16, 16, 7, col)
        disc(d, 16, 16, 4, shade(col, 1.45))
        d.rectangle([12, 11, 13, 12], fill=WHITE)
    else:
        disc(d, 16, 16, 7, shade(col, 0.28))
        disc(d, 15, 15, 3, shade(col, 0.4))
    return im

add("bulb_on", bulb(rgb("ffe08a"), True))
add("bulb_off", bulb(rgb("ffe08a"), False))
for i, c in enumerate(["ff4f6d", "4fc3ff", "6dff8e", "c77dff"]):
    add(f"bulb_c{i}", bulb(rgb(c), True))

# ------------------------------------------------------------------ Chips (Einsatzleiste)
CHIP_COLS = [rgb("2e6fd8"), rgb("c8283c"), rgb("1f8a4c"), rgb("7b3fc4"), rgb("222226")]
CHIP_LABELS = ["10", "50", "100", "500", "1K"]

def chip(col, label, sel, bg=PANEL):
    im = tile(bg)
    d = ImageDraw.Draw(im)
    lift = -2 if sel else 0
    if sel:
        disc(d, 16, 16 + lift, 15, GOLD)
        disc(d, 16, 16 + lift, 14, bg)
    disc(d, 16, 18 + lift, 12, (0, 0, 0, 140))
    disc(d, 16, 16 + lift, 12, shade(col, 0.55))
    disc(d, 16, 16 + lift, 11, col)
    stripe = GOLD if col == CHIP_COLS[4] else rgb("f2f2f2")
    for k in range(6):
        a = k * math.pi / 3
        for rr in range(8, 11):
            x, y = 16 + math.sin(a) * rr, 16 + lift - math.cos(a) * rr
            d.rectangle([int(x) - 1, int(y) - 1, int(x), int(y)], fill=stripe)
    disc(d, 16, 16 + lift, 7, shade(col, 0.7))
    disc(d, 16, 16 + lift, 6, shade(col, 1.12))
    text_c(im, label, 16, 14 + lift, WHITE, 1)
    return im

CHIP_COLS.append(rgb("e0a526"))
CHIP_LABELS.append("$")
for i in range(6):
    add(f"chip_{i}", chip(CHIP_COLS[i], CHIP_LABELS[i], False))
    add(f"chip_{i}_sel", chip(CHIP_COLS[i], CHIP_LABELS[i], True))
    add(f"chipf_{i}", chip(CHIP_COLS[i], CHIP_LABELS[i], False, FELT))

# ------------------------------------------------------------------ Knöpfe
def button(col, label, icon=None, bg=PANEL, sc=1):
    im = tile(bg)
    d = ImageDraw.Draw(im)
    rrect(d, [2, 4, 29, 29], (0, 0, 0, 150), 4)
    rrect(d, [2, 2, 29, 27], shade(col, 0.5), 4)
    vgrad(im, (4, 4, 28, 26), shade(col, 1.2), shade(col, 0.85))
    d = ImageDraw.Draw(im)
    d.line([5, 3, 26, 3], fill=shade(col, 1.45))
    if icon:
        icon(d, im)
    if label:
        text_c(im, label, 16, 22 if icon else 12, WHITE, sc if not icon else 1, shadow=shade(col, 0.4))
    return im

def ic_arrow_left(d, im):
    d.polygon([(8, 12), (16, 6), (16, 18)], fill=WHITE)
    d.rectangle([15, 10, 23, 14], fill=WHITE)

def ic_x(d, im):
    for i in range(-1, 2):
        d.line([10 + i, 7, 21 + i, 18], fill=WHITE, width=1)
        d.line([21 + i, 7, 10 + i, 18], fill=WHITE, width=1)

def ic_info(d, im):
    disc(d, 16, 12, 8, WHITE)
    d.rectangle([15, 11, 16, 16], fill=rgb("2e6fd8"))
    d.rectangle([15, 8, 16, 9], fill=rgb("2e6fd8"))

def ic_spin(d, im):
    d.arc([9, 5, 22, 18], 30, 330, fill=WHITE, width=2)
    d.polygon([(20, 3), (24, 9), (18, 9)], fill=WHITE)

def ic_plus_card(d, im):
    rrect(d, [8, 5, 17, 18], WHITE, 1)
    d.rectangle([20, 10, 25, 11], fill=WHITE)
    d.rectangle([22, 8, 23, 13], fill=WHITE)

def ic_hand(d, im):
    d.rectangle([10, 9, 21, 18], fill=WHITE)
    for k in range(4):
        d.rectangle([10 + k * 3, 5 - (k % 2), 11 + k * 3, 9], fill=WHITE)

def ic_x2(d, im):
    text_c(im, "x2", 16, 8, WHITE, 2)

def ic_deal(d, im):
    rrect(d, [7, 6, 16, 19], WHITE, 1)
    rrect(d, [14, 4, 23, 17], rgb("dde6ff"), 1)

def ic_repeat(d, im):
    d.arc([9, 5, 22, 18], 200, 520, fill=WHITE, width=2)
    d.polygon([(7, 9), (12, 9), (9, 13)], fill=WHITE)

def ic_trash(d, im):
    d.rectangle([10, 8, 21, 19], fill=WHITE)
    d.rectangle([8, 6, 23, 7], fill=WHITE)
    d.rectangle([14, 4, 17, 5], fill=WHITE)

add("btn_back", button(rgb("6e4020"), "ZURUCK", ic_arrow_left))
add("btn_close", button(rgb("7a1e2c"), "ENDE", ic_x))
add("btn_info", button(rgb("2e6fd8"), "INFO", ic_info))
add("btn_spin", button(RED, "DREH", ic_spin))
add("btn_spin_off", button(rgb("3c4440"), "...", None))
add("btn_hit", button(rgb("1f8a4c"), "KARTE", ic_plus_card, FELT))
add("btn_stand", button(RED, "HALT", ic_hand, FELT))
add("btn_double", button(rgb("7b3fc4"), "", ic_x2, FELT))
add("btn_double_off", button(rgb("3c4440"), "", ic_x2, FELT))
add("btn_deal", button(rgb("2e6fd8"), "GEBEN", ic_deal, FELT))
add("btn_repeat", button(rgb("2e6fd8"), "NOCHM", ic_repeat, FELT))
add("btn_clear", button(rgb("5a5a62"), "WEG", ic_trash, FELT))
add("btn_spin_felt", button(RED, "DREH", ic_spin, FELT))
add("btn_spin_felt_off", button(rgb("3c4440"), "...", None, FELT))

# Kontostand-Münze
def coin_tile():
    im = tile(PANEL)
    d = ImageDraw.Draw(im)
    for k, (x, y) in enumerate([(10, 22), (10, 18), (10, 14), (21, 20), (21, 16)]):
        d.ellipse([x - 7, y - 3, x + 7, y + 4], fill=GOLD_D)
        d.ellipse([x - 7, y - 4, x + 7, y + 3], fill=GOLD)
        d.line([x - 4, y - 2, x + 3, y - 2], fill=GOLD_L)
    return im
add("coin", coin_tile())

# ------------------------------------------------------------------ LED-Ziffern (7-Segment)
SEG = {"0": "abcdef", "1": "bc", "2": "abged", "3": "abgcd", "4": "fgbc", "5": "afgcd", "6": "afgedc",
       "7": "abc", "8": "abcdefg", "9": "abcdfg", "-": "g", "k": "fegbc_k", "m": "m", " ": ""}

def led(ch, col):
    im = tile(LED_BG, noise=False)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 31, 31], outline=rgb("202024"))
    dim = shade(col, 0.16)
    def seg(name, on):
        c = col if on else dim
        X0, X1, Y0, YM, Y1, w = 9, 22, 4, 15, 26, 3
        if name == "a": d.rectangle([X0 + 2, Y0, X1 - 2, Y0 + w - 1], fill=c)
        if name == "g": d.rectangle([X0 + 2, YM, X1 - 2, YM + w - 1], fill=c)
        if name == "d": d.rectangle([X0 + 2, Y1, X1 - 2, Y1 + w - 1], fill=c)
        if name == "f": d.rectangle([X0, Y0 + 2, X0 + w - 1, YM], fill=c)
        if name == "b": d.rectangle([X1 - w + 1, Y0 + 2, X1, YM], fill=c)
        if name == "e": d.rectangle([X0, YM + 2, X0 + w - 1, Y1], fill=c)
        if name == "c": d.rectangle([X1 - w + 1, YM + 2, X1, Y1], fill=c)
    if ch in ".x+km":
        for s in "abcdefg":
            seg(s, False)
        if ch == ".":
            d.rectangle([14, 25, 17, 28], fill=col)
        elif ch == "x":
            for i in range(9):
                d.rectangle([11 + i, 13 + i, 12 + i, 14 + i], fill=col)
                d.rectangle([19 - i, 13 + i, 20 - i, 14 + i], fill=col)
        elif ch == "+":
            d.rectangle([10, 14, 21, 16], fill=col)
            d.rectangle([14, 10, 16, 21], fill=col)
        elif ch == "k":
            d.rectangle([9, 4, 11, 28], fill=col)
            for i in range(12):
                d.rectangle([12 + i // 2 * 1 + i // 2, 15 - i, 13 + i, 16 - i], fill=col)
                d.rectangle([12 + i, 15 + i, 13 + i, 16 + i], fill=col)
        elif ch == "m":
            d.rectangle([7, 8, 9, 28], fill=col)
            d.rectangle([22, 8, 24, 28], fill=col)
            for i in range(7):
                d.rectangle([9 + i, 9 + i, 10 + i, 10 + i], fill=col)
                d.rectangle([22 - i - 1, 9 + i, 23 - i - 1, 10 + i], fill=col)
    else:
        on = SEG.get(ch, "")
        for s in "abcdefg":
            seg(s, s in on)
    # leichter Glanz
    d.line([1, 1, 30, 1], fill=(255, 255, 255, 20))
    return im

LED_CHARS = {"0": "0", "1": "1", "2": "2", "3": "3", "4": "4", "5": "5", "6": "6", "7": "7", "8": "8", "9": "9",
             "dot": ".", "x": "x", "k": "k", "m": "m", "plus": "+", "minus": "-", "blank": " "}
for cname, col in [("a", rgb("ffb238")), ("g", rgb("5cf08a")), ("r", rgb("ff4a4a"))]:
    for key, ch in LED_CHARS.items():
        add(f"led_{cname}_{key}", led(ch, col))

# ------------------------------------------------------------------ Banner (5 Kacheln breit)
def banner(name, label, col, bg):
    """5 Kacheln, je 2 Zeichen pro Kachel (so fallen keine Buchstaben in die Slot-Fugen)."""
    lab = label[:10]
    pad = 10 - len(lab)
    lab = " " * (pad // 2) + lab + " " * (pad - pad // 2)
    for i in range(5):
        im = Image.new("RGBA", (T, T), bg)
        d = ImageDraw.Draw(im)
        d.rectangle([0, 3, T - 1, 28], fill=(0, 0, 0, 210))
        d.line([0, 3, T - 1, 3], fill=col)
        d.line([0, 28, T - 1, 28], fill=col)
        for x in range(2, T, 6):
            d.point((x, 5), fill=shade(col, 0.6))
            d.point((x + 3, 26), fill=shade(col, 0.6))
        pair = lab[i * 2:i * 2 + 2]
        text_c(im, pair, 16, 9, col, 3, shadow=shade(col, 0.35))
        add(f"ban_{name}_{i}", im)

for name, label, col in [("win", "GEWONNEN!", rgb("5cf08a")), ("lose", "VERLOREN", rgb("ff5a5a")),
                         ("bj", "BLACKJACK!", GOLD), ("push", "UNENTSCH.", WHITE),
                         ("bust", "UBERKAUFT", rgb("ff5a5a")), ("dbust", "DEALERBUST", rgb("5cf08a")),
                         ("crash", "CRASH!", rgb("ff4a4a")), ("cash", "AUSGEZAHLT", rgb("5cf08a")),
                         ("jackpot", "JACKPOT!", GOLD), ("ready", "BEREIT", GOLD)]:
    banner(name, label, col, FELT)
    banner(name + "_s", label, col, SKY)

# ------------------------------------------------------------------ Lobby-Spielkacheln
def game_tile(col, draw):
    im = tile(FELT)
    d = ImageDraw.Draw(im)
    rrect(d, [1, 2, 30, 31], (0, 0, 0, 130), 4)
    rrect(d, [1, 1, 30, 29], GOLD_D, 4)
    vgrad(im, (3, 3, 29, 28), shade(col, 0.6), shade(col, 0.25))
    d = ImageDraw.Draw(im)
    draw(d, im)
    return im

def draw_slots(d, im):
    rrect(d, [6, 7, 25, 24], CREAM, 2)
    for i, c in enumerate([rgb("ff4a4a"), GOLD, rgb("ff4a4a")]):
        text(im, "7", 8 + i * 6, 12, c, 1)
    d.line([6, 15, 25, 15], fill=RED)
    disc(d, 28, 8, 2, RED)

def draw_roulette(d, im):
    disc(d, 16, 16, 11, WOOD_D)
    for k in range(12):
        a0, a1 = k * 30, k * 30 + 30
        d.pieslice([6, 6, 26, 26], a0, a1, fill=RED if k % 2 else BLK)
    disc(d, 16, 16, 5, WOOD_L)
    disc(d, 16, 16, 2, GOLD)
    disc(d, 22, 9, 2, WHITE)

def draw_bj(d, im):
    rrect(d, [6, 8, 17, 24], WHITE, 1)
    text(im, "A", 8, 10, BLK)
    rrect(d, [14, 6, 25, 22], WHITE, 1)
    text(im, "K", 16, 8, RED)
    d.polygon([(19, 14), (21, 17), (19, 20), (17, 17)], fill=RED)

def draw_crash(d, im):
    for x in range(5, 27):
        y = int(26 - ((math.exp((x - 5) / 9) - 1) * 4))
        if y > 4:
            d.rectangle([x, y, x, y + 1], fill=rgb("ffb238"))
    d.polygon([(22, 6), (27, 4), (25, 9)], fill=WHITE)
    d.rectangle([23, 6, 25, 8], fill=RED)

add("game_slots", game_tile(rgb("b0306a"), draw_slots))
add("game_roulette", game_tile(RED, draw_roulette))
add("game_blackjack", game_tile(rgb("2e6fd8"), draw_bj))
add("game_crash", game_tile(rgb("e0861e"), draw_crash))

# ------------------------------------------------------------------ Slots
def sym_img(i, win):
    im = tile(GOLD_L if win else CREAM, noise=False)
    vgrad(im, (0, 0, 32, 32), GOLD_L if win else CREAM, shade(GOLD, 1.1) if win else CREAM_D)
    d = ImageDraw.Draw(im)
    if i == 0:  # Beeren
        d.line([16, 6, 12, 14], fill=rgb("2f7a2a"), width=2)
        d.line([16, 6, 21, 15], fill=rgb("2f7a2a"), width=2)
        d.polygon([(16, 6), (22, 4), (20, 9)], fill=rgb("4caf50"))
        for cx, cy in [(11, 19), (21, 20)]:
            disc(d, cx, cy, 6, RED_D); disc(d, cx, cy, 5, rgb("e0283c")); disc(d, cx - 2, cy - 2, 1, WHITE)
    elif i == 1:  # Redstone
        for cx, cy, r in [(16, 16, 6), (9, 11, 3), (23, 10, 3), (10, 23, 3), (23, 22, 4)]:
            disc(d, cx, cy, r, rgb("7a0000")); disc(d, cx, cy, r - 1, rgb("ff2a2a"))
        disc(d, 14, 14, 1, rgb("ffb0b0"))
    elif i in (2, 3):  # Eisen / Gold
        base, hi, lo = (rgb("d8d8d8"), WHITE, rgb("8a8a8a")) if i == 2 else (GOLD, GOLD_L, GOLD_D)
        d.polygon([(6, 22), (10, 12), (26, 12), (22, 22)], fill=base)
        d.polygon([(10, 12), (13, 9), (28, 9), (26, 12)], fill=hi)
        d.polygon([(22, 22), (26, 12), (28, 9), (28, 17), (24, 24)], fill=lo)
        d.line([8, 21, 21, 21], fill=lo)
    elif i == 4:  # Smaragd
        d.polygon([(16, 4), (24, 9), (24, 23), (16, 28), (8, 23), (8, 9)], fill=rgb("0d7a35"))
        d.polygon([(16, 7), (21, 10), (21, 22), (16, 25), (11, 22), (11, 10)], fill=rgb("2ed16a"))
        d.polygon([(16, 7), (21, 10), (16, 13), (11, 10)], fill=rgb("9cf5bc"))
    elif i == 5:  # Diamant
        d.polygon([(6, 12), (11, 6), (21, 6), (26, 12), (16, 27)], fill=rgb("1a9fa8"))
        d.polygon([(8, 12), (12, 8), (20, 8), (24, 12), (16, 24)], fill=rgb("5ff0f0"))
        d.polygon([(12, 8), (20, 8), (18, 12), (14, 12)], fill=rgb("d0ffff"))
        d.line([8, 12, 24, 12], fill=rgb("1a9fa8"))
    elif i == 6:  # Netherstern
        pts = []
        for k in range(8):
            r = 13 if k % 2 == 0 else 5
            a = k * math.pi / 4
            pts.append((16 + math.sin(a) * r, 16 - math.cos(a) * r))
        d.polygon(pts, fill=rgb("c9b8ff"))
        pts2 = [(16 + (x - 16) * 0.6, 16 + (y - 16) * 0.6) for x, y in pts]
        d.polygon(pts2, fill=WHITE)
    if win:
        d.rectangle([0, 0, 31, 31], outline=GOLD_D)
    return im

for i in range(7):
    add(f"sym_{i}", sym_img(i, False))
    add(f"sym_{i}_win", sym_img(i, True))

def cab(kind=""):
    im = tile(CAB)
    vgrad(im, (0, 0, 32, 32), CAB, CAB_D)
    d = ImageDraw.Draw(im)
    if kind.startswith("arrow"):
        on = kind.endswith("_on")
        c = GOLD if on else RED
        if "_l" in kind:
            d.polygon([(14, 9), (26, 16), (14, 23)], fill=c)
        else:
            d.polygon([(18, 9), (6, 16), (18, 23)], fill=c)
    if kind == "dots":
        for x in range(4, 32, 8):
            disc(d, x, 16, 2, GOLD_D)
    return im

add("cab", cab())
add("cab_dots", cab("dots"))
add("cab_arrow_l", cab("arrow_l"))
add("cab_arrow_r", cab("arrow_r"))
add("cab_arrow_l_on", cab("arrow_l_on"))
add("cab_arrow_r_on", cab("arrow_r_on"))

def lever(part, knob):
    im = tile(FELT)
    d = ImageDraw.Draw(im)
    if part in ("top", "mid"):
        d.rectangle([14, 0 if part == "mid" else 16, 17, 31], fill=rgb("b8b8c0"))
        d.rectangle([14, 0 if part == "mid" else 16, 14, 31], fill=rgb("e8e8f0"))
        if knob:
            disc(d, 16, 16, 9, RED_D); disc(d, 16, 15, 8, rgb("e0283c")); disc(d, 13, 12, 2, WHITE)
    elif part == "base":
        d.rectangle([14, 0, 17, 12], fill=rgb("b8b8c0"))
        rrect(d, [9, 10, 22, 30], rgb("3a3a40"), 3)
        rrect(d, [10, 11, 21, 28], rgb("8a8a92"), 2)
    elif part == "empty":
        pass
    return im

add("lever_top", lever("top", True))
add("lever_top_empty", lever("empty", False))
add("lever_mid", lever("mid", False))
add("lever_mid_knob", lever("mid", True))
add("lever_base", lever("base", False))

def paytable():
    im = tile(FELT)
    d = ImageDraw.Draw(im)
    rrect(d, [4, 3, 27, 29], rgb("f4eee2"), 2)
    for y in range(7, 27, 4):
        d.line([7, y, 24, y], fill=rgb("9a8a70"))
    text(im, "x", 19, 4, RED)
    return im
add("paytable", paytable())

# ------------------------------------------------------------------ Roulette
RED_NUMS = {1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36}

def rnum(n, hl):
    col = GRN if n == 0 else (rgb("c41e32") if n in RED_NUMS else BLK)
    im = tile(col, noise=False)
    vgrad(im, (0, 0, 32, 32), shade(col, 1.15), shade(col, 0.8))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 31, 31], outline=GOLD if hl else shade(GOLD_D, 0.8))
    if hl:
        d.rectangle([1, 1, 30, 30], outline=GOLD)
    s = str(n)
    text_c(im, s, 16, 11, WHITE, 2, shadow=(0, 0, 0, 160))
    return im

for n in range(37):
    add(f"rn_{n}", rnum(n, False))
    add(f"rn_{n}_hl", rnum(n, True))

def pointer(up=False):
    im = tile(FELT)
    d = ImageDraw.Draw(im)
    if up:
        d.polygon([(16, 6), (26, 22), (6, 22)], fill=GOLD_D)
        d.polygon([(16, 9), (23, 20), (9, 20)], fill=GOLD)
    else:
        d.polygon([(6, 8), (26, 8), (16, 25)], fill=GOLD_D)
        d.polygon([(9, 10), (23, 10), (16, 22)], fill=GOLD)
    return im
add("pointer", pointer())
add("pointer_up", pointer(True))

def bet_tile(label, col=None, diamond=None):
    im = tile(FELT)
    d = ImageDraw.Draw(im)
    if col:
        rrect(d, [2, 2, 29, 29], col, 2)
    d.rounded_rectangle([1, 1, 30, 30], 3, outline=GOLD)
    if diamond:
        d.polygon([(16, 6), (25, 16), (16, 26), (7, 16)], fill=diamond)
        d.polygon([(16, 9), (22, 16), (16, 23), (10, 16)], fill=shade(diamond, 1.2))
    if label:
        text_c(im, label, 16, 13, WHITE, 1, shadow=(0, 0, 0, 160))
    return im

add("bet_red", bet_tile("", rgb("8a1424"), rgb("ff3a4e")))
add("bet_black", bet_tile("", rgb("0e0e12"), rgb("4a4a56")))
add("bet_zero", bet_tile("0", GRN))
add("bet_even", bet_tile("GERADE"))
add("bet_odd", bet_tile("UNGER."))
add("bet_low", bet_tile("1-18"))
add("bet_high", bet_tile("19-36"))
add("bet_dz1", bet_tile("1-12"))
add("bet_dz2", bet_tile("13-24"))
add("bet_dz3", bet_tile("25-36"))
add("bet_pick", bet_tile("ZAHL?", rgb("2a3a8a")))

# ------------------------------------------------------------------ Blackjack-Karten
SUIT_SHAPES = {
    "s": [" 1 ", "111", "111", " 1 "], "h": ["1 1", "111", "111", " 1 "],
    "d": [" 1 ", "111", "111", " 1 "], "c": [" 1 ", "111", " 1 ", "111"],
}
BIG = {
    "s": ["    1    ", "   111   ", "  11111  ", " 1111111 ", "111111111", "111111111", " 11 1 11 ", "    1    ", "   111   "],
    "h": [" 111 111 ", "111111111", "111111111", "111111111", " 1111111 ", "  11111  ", "   111   ", "    1    ", "         "],
    "d": ["    1    ", "   111   ", "  11111  ", " 1111111 ", "111111111", " 1111111 ", "  11111  ", "   111   ", "    1    "],
    "c": ["   111   ", "  11111  ", "   111   ", " 11 1 11 ", "111111111", " 11 1 11 ", "    1    ", "   111   ", "         "],
}

def paint(im, rows, x, y, col):
    px = im.load()
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch == "1":
                px[x + i, y + j] = col

def card(rank, suit):
    im = tile(FELT)
    d = ImageDraw.Draw(im)
    rrect(d, [5, 2, 27, 31], (0, 0, 0, 120), 2)
    rrect(d, [4, 0, 26, 30], rgb("8a8a8a"), 2)
    vgrad(im, (5, 1, 26, 30), WHITE, rgb("ebe7de"))
    col = rgb("d21e36") if suit in "hd" else rgb("141418")
    text(im, rank, 6, 3, col)
    paint(im, SUIT_SHAPES[suit], 6, 9, col)
    paint(im, BIG[suit], 12, 14, col)
    if rank in ("J", "Q", "K"):
        d = ImageDraw.Draw(im)
        d.rectangle([10, 11, 22, 26], outline=shade(col, 1.6))
    return im

RANKS = ["A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"]
for r in RANKS:
    for s in "shdc":
        add(f"card_{r.lower()}_{s}", card(r, s))

def card_back():
    im = tile(FELT)
    d = ImageDraw.Draw(im)
    rrect(d, [5, 2, 27, 31], (0, 0, 0, 120), 2)
    rrect(d, [4, 0, 26, 30], RED_D, 2)
    vgrad(im, (5, 1, 26, 30), rgb("b01e30"), rgb("7a0e1c"))
    d = ImageDraw.Draw(im)
    d.rectangle([7, 3, 23, 27], outline=GOLD)
    for y in range(5, 26, 4):
        for x in range(9, 22, 4):
            if ((x + y) // 4) % 2 == 0:
                d.rectangle([x, y, x + 1, y + 1], fill=(255, 255, 255, 70))
    disc(d, 15, 15, 4, GOLD)
    return im
add("card_back", card_back())

def card_empty():
    im = tile(FELT)
    d = ImageDraw.Draw(im)
    for x in range(5, 27, 3):
        d.point((x, 1), fill=shade(FELT, 1.35)); d.point((x, 29), fill=shade(FELT, 1.35))
    for y in range(1, 30, 3):
        d.point((4, y), fill=shade(FELT, 1.35)); d.point((26, y), fill=shade(FELT, 1.35))
    return im
add("card_empty", card_empty())

def label_tile(lbl, col=GOLD, bg=FELT):
    im = tile(bg)
    text_c(im, lbl, 16, 13, col, 1, shadow=(0, 0, 0, 150))
    return im
add("lbl_dealer", label_tile("DEALER"))
add("lbl_du", label_tile("DU"))
add("lbl_32", label_tile("3:2", GOLD))

# ------------------------------------------------------------------ Crash
def sky(stars):
    im = tile(SKY, noise=False)
    vgrad(im, (0, 0, 32, 32), SKY, SKY_D)
    d = ImageDraw.Draw(im)
    rnd = random.Random(stars * 13 + 5)
    for _ in range(stars):
        x, y = rnd.randrange(1, 31), rnd.randrange(1, 31)
        b = rnd.choice([WHITE, rgb("a9c4ff"), rgb("fff2b0")])
        d.point((x, y), fill=b)
        if rnd.random() < 0.3:
            d.point((x + 1, y), fill=shade(b, 0.5)); d.point((x, y + 1), fill=shade(b, 0.5))
    return im

add("sky", sky(0))
for k in range(4):
    add(f"sky_s{k}", sky(2 + k * 2))

def rocket(boost):
    im = sky(1)
    d = ImageDraw.Draw(im)
    d.polygon([(16, 2), (21, 9), (21, 24), (11, 24), (11, 9)], fill=rgb("e8e8f0"))
    d.polygon([(16, 2), (21, 9), (11, 9)], fill=RED)
    disc(d, 16, 14, 3, rgb("3a7ad8")); disc(d, 15, 13, 1, WHITE)
    d.polygon([(11, 18), (6, 26), (11, 24)], fill=RED)
    d.polygon([(21, 18), (26, 26), (21, 24)], fill=RED)
    d.line([12, 9, 12, 23], fill=WHITE)
    fl = [rgb("fff1b0"), rgb("ffd45c"), rgb("ff9a3c")]
    h = 7 if boost else 5
    d.polygon([(13, 25), (19, 25), (16, 25 + h)], fill=fl[1])
    d.polygon([(14, 25), (18, 25), (16, 24 + h - 2)], fill=fl[0])
    return im

add("rocket_0", rocket(False))
add("rocket_1", rocket(True))

def flame(k):
    im = sky(1)
    d = ImageDraw.Draw(im)
    rnd = random.Random(k)
    for i in range(14):
        x = 16 + rnd.randint(-6, 6)
        y = rnd.randint(0, 28)
        c = [rgb("fff1b0"), rgb("ffd45c"), rgb("ff9a3c"), rgb("ff5a2e")][min(3, y // 8)]
        r = max(1, 4 - y // 8)
        disc(d, x, y, r, c)
    return im
add("flame_0", flame(1))
add("flame_1", flame(2))

def boom():
    im = sky(0)
    d = ImageDraw.Draw(im)
    for r, c in [(15, rgb("ff5a2e")), (11, rgb("ff9a3c")), (7, rgb("ffd45c")), (3, rgb("fff1b0"))]:
        pts = []
        for k in range(16):
            rr = r if k % 2 == 0 else r * 0.6
            a = k * math.pi / 8
            pts.append((16 + math.sin(a) * rr, 16 - math.cos(a) * rr))
        d.polygon(pts, fill=c)
    return im
add("boom", boom())

def wide_button(name, label, col, icon_l, icon_r, bg=PANEL):
    """3 Kacheln: Symbol | Text | Symbol – jede Kachel für sich, damit nichts in die Fugen fällt."""
    for i in range(3):
        im = tile(bg)
        d = ImageDraw.Draw(im)
        x0, x1 = (2 if i == 0 else 0), (T - 3 if i == 2 else T - 1)
        d.rectangle([x0, 4, x1, 29], fill=(0, 0, 0, 150))
        d.rectangle([x0, 2, x1, 27], fill=shade(col, 0.5))
        vgrad(im, (x0 + (2 if i == 0 else 0), 4, x1 + 1 - (2 if i == 2 else 0), 26), shade(col, 1.25), shade(col, 0.85))
        d = ImageDraw.Draw(im)
        d.line([x0 + 2, 3, x1 - 2, 3], fill=shade(col, 1.5))
        if i == 1:
            text_c(im, label, 16, 12, WHITE, 1, shadow=shade(col, 0.4))
        else:
            (icon_l if i == 0 else icon_r)(d, im)
        add(f"{name}_{i}", im)

def ic_rocket(d, im):
    d.polygon([(16, 5), (20, 10), (20, 20), (12, 20), (12, 10)], fill=WHITE)
    d.polygon([(16, 5), (20, 10), (12, 10)], fill=RED)
    d.polygon([(14, 21), (18, 21), (16, 25)], fill=rgb("ffd45c"))

def ic_coin(d, im):
    disc(d, 16, 15, 8, GOLD_D); disc(d, 16, 14, 7, GOLD); text_c(im, "$", 16, 12, GOLD_D)

def ic_arrow_r(d, im):
    d.polygon([(11, 8), (21, 15), (11, 22)], fill=WHITE)

def ic_dots(d, im):
    for x in (11, 16, 21):
        disc(d, x, 15, 2, WHITE)

wide_button("wb_launch", "START", rgb("e0861e"), ic_rocket, ic_arrow_r)
wide_button("wb_cash", "CASH", rgb("1f8a4c"), ic_coin, ic_coin)
wide_button("wb_cash2", "CASH", rgb("35c46e"), ic_coin, ic_coin)
wide_button("wb_wait", "...", rgb("3c4440"), ic_dots, ic_dots)

def hist(col):
    im = tile(PANEL)
    d = ImageDraw.Draw(im)
    rrect(d, [3, 8, 28, 23], shade(col, 0.35), 3)
    rrect(d, [4, 9, 27, 22], shade(col, 0.7), 3)
    d.line([6, 10, 25, 10], fill=col)
    return im
add("hist_red", hist(rgb("ff4a4a")))
add("hist_green", hist(rgb("5cf08a")))
add("hist_gold", hist(GOLD))
add("hist_none", tile(PANEL))

# ------------------------------------------------------------------ GUI-Hintergründe (Titel-Glyphen)
GW, GH = 176, 222
def gui_bg(title, sub):
    im = Image.new("RGBA", (GW, GH), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    # Rahmen um den Truhenbereich (y 0..125)
    vgrad(im, (0, 0, GW, 126), WOOD_L, WOOD_D)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, GW - 1, 125], outline=rgb("1a0d05"))
    d.rectangle([5, 15, GW - 6, 122], outline=GOLD_D)
    # Kopfzeile
    for x in range(8, GW - 8, 8):
        d.point((x, 2), fill=rgb("ffe08a"))
    text(im, title, 8, 6, GOLD, 1, shadow=WOOD_D)
    text(im, sub, GW - 8 - text_w(sub), 6, rgb("f4eee2"), 1, shadow=WOOD_D)
    # Slot-Raster: Fugen zwischen den Kacheln, Löcher für die Items
    for r in range(6):
        for c in range(9):
            x, y = 7 + c * 18, 17 + r * 18
            fug = rgb("2a160a") if r in (0, 5) else rgb("0a3122")
            d.rectangle([x, y, x + 17, y + 17], fill=fug)
            d.rectangle([x + 1, y + 1, x + 16, y + 16], fill=(0, 0, 0, 0))
    # Trennlinie über der Steuerleiste
    d.line([7, 106, GW - 8, 106], fill=GOLD_D)
    # rechte Kante deckend lassen (Glyph-Breite = 176)
    for y in range(0, 126):
        if im.getpixel((GW - 1, y))[3] == 0:
            im.putpixel((GW - 1, y), rgb("1a0d05"))
    return im

GUIS = {
    "lobby": ("CRAFT CASINO", "LOBBY"),
    "slots": ("CRAFT CASINO", "SLOTS"),
    "roulette": ("CRAFT CASINO", "ROULETTE"),
    "picker": ("ROULETTE", "ZAHL WAHLEN"),
    "blackjack": ("CRAFT CASINO", "BLACKJACK"),
    "crash": ("CRAFT CASINO", "CRASH"),
}
GUI_CHARS = {}

# ------------------------------------------------------------------ Schreiben
def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=1, ensure_ascii=False)

def main():
    if os.path.exists(OUT):
        shutil.rmtree(OUT)
    a = os.path.join(OUT, "assets", NS)
    write_json(os.path.join(OUT, "pack.mcmeta"), {
        "pack": {"description": "§6Craft Casino §7Texturen", "min_format": 69, "max_format": 99}})

    # Item-Texturen + Modelle + Item-Definitionen
    for name, im in TEXTURES.items():
        p = os.path.join(a, "textures", "item", name + ".png")
        os.makedirs(os.path.dirname(p), exist_ok=True)
        im.save(p)
        write_json(os.path.join(a, "models", "item", name + ".json"),
                   {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})
        write_json(os.path.join(a, "items", name + ".json"),
                   {"model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}})

    # GUI-Hintergründe als Font-Glyphen
    providers = []
    for i, (key, (t, s)) in enumerate(GUIS.items()):
        ch = chr(0xE000 + i)
        GUI_CHARS[key] = ch
        p = os.path.join(a, "textures", "font", f"gui_{key}.png")
        os.makedirs(os.path.dirname(p), exist_ok=True)
        gui_bg(t, s).save(p)
        providers.append({"type": "bitmap", "file": f"{NS}:font/gui_{key}.png", "ascent": 13, "height": GH, "chars": [ch]})
    # negative Abstände: U+F001 = -1, F002 = -2, F003 = -4 ... (2er-Potenzen)
    adv = {chr(0xF001 + k): -(1 << k) for k in range(9)}
    providers.append({"type": "space", "advances": adv})
    write_json(os.path.join(a, "font", "gui.json"), {"providers": providers})

    # Zip
    zp = os.path.join(ROOT, "CasinoPack.zip")
    if os.path.exists(zp):
        os.remove(zp)
    with zipfile.ZipFile(zp, "w", zipfile.ZIP_DEFLATED) as z:
        for base, _, files in os.walk(OUT):
            for f in files:
                full = os.path.join(base, f)
                z.write(full, os.path.relpath(full, OUT))
    print(f"{len(TEXTURES)} Texturen, {len(GUIS)} Hintergründe -> {zp}")
    print("GUI-Zeichen:", {k: hex(ord(v)) for k, v in GUI_CHARS.items()})

if __name__ == "__main__":
    main()
