"""Builds the Play Store shots in release/phone-screenshots/ (1080 x 1920): a caption and a phone frame
around a raw 1080 x 2400 emulator screenshot. The status bar is redrawn lower and pulled in from the
edges so the frame's rounded corners never cut the clock or the battery.

usage: python3 tools/store_shots.py <folder with the raw shots> [out folder]
Raw shots are named today.png, amount-sheet.png, habits-week.png, habit-detail.png, journal.png,
insights.png, w-week.png (home screen with the This week widget) and today-dark.png.
Needs Pillow."""
import os
import sys
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = sys.argv[1]
OUT = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ROOT, "release", "phone-screenshots")
FONT = os.path.join(ROOT, "app", "src", "main", "res", "font", "outfit.ttf")

shots = [
    ("today", "01-today.png", "Swipe to check off your day", "Done, partly done or skipped. Undo is one tap away.", False),
    ("amount-sheet", "02-log-amount.png", "Log part of a habit", "15 of 20 pages still counts toward your score.", False),
    ("habits-week", "03-habits-week.png", "Your week at a glance", "Every habit, every day, on one screen.", False),
    ("habit-detail", "04-habit-detail.png", "Streaks and a calendar", "Tap any past day to fix it.", False),
    ("journal", "05-journal.png", "A note for each day", "Write about the days that went well, and the ones that didn't.", False),
    ("insights", "06-insights.png", "See what's working", "Scores by day and by habit, for any dates you pick.", False),
    ("w-week", "07-widgets.png", "Widgets for your home screen", "Check habits off without opening the app.", False),
    ("today-dark", "08-today-dark.png", "Light or dark, your colors", "Pick the font, text size and accent color.", True),
]

W, H = 1080, 1920
FX, FY, FW, FH, BEZEL = 215, 470, 650, 1444, 14      # phone frame, as in the 1.0 store shots
SX, SY, SW, SH = FX + BEZEL, FY + BEZEL, FW - 2 * BEZEL, FH - 2 * BEZEL
OUTER_R, INNER_R = 62, 48
K = SW / 1080                                         # raw -> screen scale


def font(size, weight):
    f = ImageFont.truetype(FONT, size)
    f.set_variation_by_axes([weight])
    return f


def wrap(draw, text, f, width):
    lines, cur = [], ""
    for word in text.split():
        t = (cur + " " + word).strip()
        if draw.textlength(t, font=f) <= width or not cur:
            cur = t
        else:
            lines.append(cur)
            cur = word
    return lines + [cur]


def screen(raw):
    s = Image.open(raw).convert("RGB")
    bar_bg = s.getpixel((540, 4))
    body = s.crop((0, 80, 1080, 2400)).resize((SW, round(2320 * K)), Image.LANCZOS)
    out = Image.new("RGB", (SW, SH), bar_bg)
    out.paste(body, (0, SH - body.height))
    # status bar: clock and icons from the raw shot, pulled in from the corners
    clock = s.crop((20, 0, 300, 70)).resize((round(280 * K), round(70 * K)), Image.LANCZOS)
    icons = s.crop((820, 0, 1060, 70)).resize((round(240 * K), round(70 * K)), Image.LANCZOS)
    top = SH - body.height - round(70 * K) - 4
    out.paste(clock, (30, top))
    out.paste(icons, (SW - icons.width - 30, top))
    return out


for raw, out, title, sub, dark in shots:
    bg = (30, 26, 20) if dark else (240, 227, 200)
    tc = (243, 237, 228) if dark else (30, 26, 21)
    sc = (189, 179, 165) if dark else (92, 82, 69)
    img = Image.new("RGB", (W, H), bg)
    d = ImageDraw.Draw(img)

    tf, sf = font(72, 600), font(38, 400)
    y = 186
    for line in wrap(d, title, tf, 960):
        d.text((W / 2, y), line, font=tf, fill=tc, anchor="mt")
        y += 86
    y += 14
    for line in wrap(d, sub, sf, 900):
        d.text((W / 2, y), line, font=sf, fill=sc, anchor="mt")
        y += 50

    # soft shadow under the phone
    sh = Image.new("L", (W, H), 0)
    ImageDraw.Draw(sh).rounded_rectangle((FX, FY + 12, FX + FW, FY + FH + 12), OUTER_R, fill=90 if not dark else 160)
    sh = sh.filter(ImageFilter.GaussianBlur(28))
    img.paste((0, 0, 0) if dark else (120, 100, 70), (0, 0), sh)

    d = ImageDraw.Draw(img)
    d.rounded_rectangle((FX, FY, FX + FW - 1, FY + FH - 1), OUTER_R, fill=(58, 54, 50) if dark else (22, 20, 18))
    if dark:  # a thin light rim so the dark phone reads on the dark page
        d.rounded_rectangle((FX, FY, FX + FW - 1, FY + FH - 1), OUTER_R, outline=(70, 64, 56), width=2)
        d.rounded_rectangle((FX + 2, FY + 2, FX + FW - 3, FY + FH - 3), OUTER_R - 2, fill=(14, 13, 12))

    scr = screen(f"{RAW}/{raw}.png")
    k = 4
    m = Image.new("L", (SW * k, SH * k), 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, SW * k - 1, SH * k - 1), INNER_R * k, fill=255)
    img.paste(scr, (SX, SY), m.resize((SW, SH), Image.LANCZOS))
    img.save(f"{OUT}/{out}", optimize=True)
    print(out)
