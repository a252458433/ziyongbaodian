# -*- coding: utf-8 -*-
"""
柔光漫画 2.0 —— 六套主题桌面图标生成器（Pillow，无第三方依赖）。

统一构图（区别于旧版四套各自为政的 M 泡泡字 / 手电筒 / 「自」字）：
  背景：主题三段对角渐变 + 右上柔光晕 + 漫画网点 + 斜向速度线
  前景：米白圆角纸卡 + 打开的书（主题色描边/书页）+ 星芒 + 底部主题色胶囊
  另有 legacy 圆角方块合成图与预览拼图。

输出：
  res/drawable-xxxhdpi/ic_launcher_{key}_bg.png   432 adaptive 背景
  res/drawable-xxxhdpi/ic_launcher_{key}_fg.png   432 adaptive 前景
  res/mipmap-xxxhdpi/ic_launcher_{key}.png        192 legacy
  res/mipmap-anydpi-v26/ic_launcher_{key}.xml     adaptive-icon 声明
  design/UI美化_图标预览.png                     六套三视图拼图

用法：python tools/gen_launcher_ui.py
"""
import math
import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

BASE = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))
RES = os.path.join(BASE, "app", "src", "main", "res")
DESIGN = os.path.join(BASE, "design")
FG_DIR = os.path.join(RES, "drawable-xxxhdpi")
MIP_DIR = os.path.join(RES, "mipmap-xxxhdpi")
ADP_DIR = os.path.join(RES, "mipmap-anydpi-v26")
for d in (FG_DIR, MIP_DIR, ADP_DIR, DESIGN):
    os.makedirs(d, exist_ok=True)

S = 432          # adaptive 画布
LEGACY = 192     # legacy

# key -> (名称, 渐变三段, 主题深色, 强调色)
THEMES = [
    ("manga_pink", "漫画粉 · 即梦漫画", [(255, 134, 182), (249, 83, 143), (180, 92, 232)], (214, 61, 116), (249, 83, 143)),
    ("manga",      "石墨灰 · 沉溺漫画", [(250, 251, 253), (227, 232, 240), (196, 205, 220)], (74, 80, 92),   (74, 80, 92)),
    ("blue",       "品牌蓝 · 自用宝典", [(78, 139, 255), (59, 110, 246), (108, 107, 239)],  (36, 81, 216),  (59, 110, 246)),
    ("torch",      "活力橙 · 手电筒",   [(255, 179, 71), (247, 144, 9), (255, 123, 84)],    (201, 114, 0),  (247, 144, 9)),
    ("mint",       "薄荷青 · 青柠笔记", [(74, 224, 174), (16, 185, 129), (14, 148, 165)],   (11, 138, 98),  (16, 185, 129)),
    ("aurora",     "星野紫 · 星野笔记", [(169, 139, 255), (122, 90, 248), (79, 70, 229)],   (91, 63, 224),  (122, 90, 248)),
]


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def diag_gradient(size, stops):
    """左上 → 右下 三段线性渐变（用 numpy 风格的逐行插值，纯 Python 亦足够快）"""
    im = Image.new("RGB", (size, size))
    px = im.load()
    c1, c2, c3 = stops
    for y in range(size):
        for x in range(size):
            t = min(1.0, max(0.0, (x + y) / (2.0 * (size - 1))))
            px[x, y] = lerp(c1, c2, t / 0.5) if t < 0.5 else lerp(c2, c3, (t - 0.5) / 0.5)
    return im.convert("RGBA")


def add_glow(im, cx, cy, radius, color=(255, 255, 255), strength=0.42):
    """柔光圆晕：单独 alpha 蒙版高斯模糊后叠加"""
    size = im.size[0]
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.ellipse([cx - radius, cy - radius, cx + radius, cy + radius],
              fill=color + (int(255 * strength),))
    layer = layer.filter(ImageFilter.GaussianBlur(radius * 0.55))
    return Image.alpha_composite(im, layer)


def halftone(im, step=26, radius=3.2, alpha=26):
    """漫画网点纸"""
    size = im.size[0]
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for y in range(0, size + step, step):
        for x in range(0, size + step, step):
            ox = step / 2 if (y // step) % 2 else 0
            d.ellipse([x + ox - radius, y - radius, x + ox + radius, y + radius],
                      fill=(255, 255, 255, alpha))
    return Image.alpha_composite(im, layer)


def speedlines(im, alpha=30):
    size = im.size[0]
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for i in range(9):
        x = size * 0.62 + i * 22
        d.line([(x, -10), (x - 150, size + 10)], fill=(255, 255, 255, alpha), width=3)
    return Image.alpha_composite(im, layer)


def make_bg(key, stops, accent):
    im = diag_gradient(S, stops)
    im = add_glow(im, int(S * 0.82), int(S * 0.16), int(S * 0.34), strength=0.34)
    im = add_glow(im, int(S * 0.12), int(S * 0.92), int(S * 0.26), color=accent, strength=0.16)
    im = halftone(im)
    im = speedlines(im, alpha=22)
    im.save(os.path.join(FG_DIR, "ic_launcher_%s_bg.png" % key))
    return im


def star(d, cx, cy, r, fill, inner=0.24, points=4):
    pts = []
    for i in range(points * 2):
        ang = -math.pi / 2 + i * math.pi / points
        rr = r if i % 2 == 0 else r * inner
        pts.append((cx + rr * math.cos(ang), cy + rr * math.sin(ang)))
    d.polygon(pts, fill=fill)


def make_fg(key, stops, deep, accent):
    """adaptive 前景：安全区内（中心 66%）放纸卡 + 书 + 星芒"""
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    # 纸卡投影
    shadow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    box = (118, 126, 314, 318)
    sd.rounded_rectangle((box[0] + 6, box[1] + 10, box[2] + 6, box[3] + 10),
                         radius=52, fill=(20, 28, 45, 90))
    im = Image.alpha_composite(im, shadow.filter(ImageFilter.GaussianBlur(14)))

    d = ImageDraw.Draw(im)
    # 米白纸卡
    d.rounded_rectangle(box, radius=48, fill=(255, 255, 255, 255),
                        outline=(255, 255, 255, 210), width=3)
    d.rounded_rectangle((124, 132, 308, 312), radius=42,
                        outline=deep + (55,), width=2)

    cx, cy = 216, 216
    # 打开的书：左页 / 右页 / 书脊
    d.polygon([(cx, 176), (cx - 12, 168), (cx - 66, 168), (cx - 66, 258),
               (cx - 12, 258), (cx, 250)], fill=(255, 255, 255, 255), outline=deep)
    d.polygon([(cx, 176), (cx + 12, 168), (cx + 66, 168), (cx + 66, 258),
               (cx + 12, 258), (cx, 250)], fill=(238, 244, 255, 255), outline=deep)
    d.line([(cx, 176), (cx, 250)], fill=deep, width=5)
    # 书页横线
    for i in range(3):
        yy = 190 + i * 20
        d.line([(cx - 54, yy), (cx - 20, yy)], fill=accent + (210,), width=5)
        d.line([(cx + 20, yy), (cx + 54, yy)], fill=accent + (210,), width=5)
    # 主题色胶囊（书下方）
    d.rounded_rectangle((cx - 30, 272, cx + 30, 282), radius=5, fill=accent + (255,))
    # 星芒 / 小星
    star(d, 306, 140, 26, (255, 255, 255, 255))
    star(d, 130, 286, 13, (255, 255, 255, 235))
    star(d, 292, 300, 10, (255, 255, 255, 210))
    d.ellipse([122, 138, 132, 148], fill=(255, 255, 255, 230))

    im.save(os.path.join(FG_DIR, "ic_launcher_%s_fg.png" % key))
    return im


def make_legacy(key, bg, fg, accent):
    """legacy 192px：裁掉 adaptive 外圈后圆角方块合成"""
    b = bg.resize((LEGACY, LEGACY), Image.LANCZOS).convert("RGBA")
    f = fg.resize((LEGACY, LEGACY), Image.LANCZOS).convert("RGBA")
    out = Image.alpha_composite(b, f)
    mask = Image.new("L", (LEGACY, LEGACY), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, LEGACY - 1, LEGACY - 1),
                                           radius=44, fill=255)
    out.putalpha(mask)
    out.save(os.path.join(MIP_DIR, "ic_launcher_%s.png" % key))
    return out


def write_adaptive(key):
    path = os.path.join(ADP_DIR, "ic_launcher_%s.xml" % key)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n')
        f.write('<!-- 柔光漫画 2.0 · %s -->\n' % key)
        f.write('<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n')
        f.write('    <background android:drawable="@drawable/ic_launcher_%s_bg" />\n' % key)
        f.write('    <foreground android:drawable="@drawable/ic_launcher_%s_fg" />\n' % key)
        f.write('</adaptive-icon>\n')


def font(size, bold=False):
    for p in (r"C:\Windows\Fonts\msyhbd.ttc" if bold else r"C:\Windows\Fonts\msyh.ttc",
              r"C:\Windows\Fonts\arialbd.ttf", r"C:\Windows\Fonts\arial.ttf"):
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, size)
            except OSError:
                pass
    return ImageFont.load_default()


def circle_mask(size, radius=None):
    m = Image.new("L", (size, size), 0)
    ImageDraw.Draw(m).ellipse((0, 0, size - 1, size - 1), fill=255)
    return m


def squircle_mask(size, radius):
    m = Image.new("L", (size, size), 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, size - 1, size - 1), radius=radius, fill=255)
    return m


def build_preview(results):
    """六套主题 × 圆形/方圆形/legacy 三视图拼图"""
    cell = 168
    pad = 26
    label_h = 40
    cols = 3
    rows = len(THEMES)
    W = pad * 2 + cols * (cell + pad)
    H = pad * 2 + rows * (cell + label_h + pad) + 54
    canvas = Image.new("RGB", (W, H), (246, 248, 253))
    d = ImageDraw.Draw(canvas)
    d.text((pad, 14), "柔光漫画 2.0 · 六套主题桌面图标", font=font(28, True), fill=(27, 36, 52))
    d.text((pad, 48), "圆形遮罩 / 圆角方遮罩 / legacy 三视图", font=font(15), fill=(102, 112, 133))

    for r, (key, label, stops, deep, accent) in enumerate(THEMES):
        bg, fg, legacy = results[key]
        y0 = 84 + r * (cell + label_h + pad)
        d.text((pad, y0), label, font=font(18, True), fill=(27, 36, 52))
        y = y0 + label_h
        for c in range(cols):
            x = pad + c * (cell + pad)
            if c == 2:
                tile = legacy.resize((cell, cell), Image.LANCZOS)
            else:
                b = bg.resize((cell, cell), Image.LANCZOS).convert("RGBA")
                f = fg.resize((cell, cell), Image.LANCZOS).convert("RGBA")
                tile = Image.alpha_composite(b, f)
                m = circle_mask(cell) if c == 0 else squircle_mask(cell, int(cell * 0.24))
                tile.putalpha(m)
            canvas.paste(tile.convert("RGB"), (x, y), tile.getchannel("A"))
        d.rounded_rectangle((pad, y + cell + 6, pad + 46, y + cell + 12),
                            radius=3, fill=accent)
    out = os.path.join(DESIGN, "UI美化_图标预览.png")
    canvas.save(out)
    print("preview ->", out)


def main():
    results = {}
    for key, label, stops, deep, accent in THEMES:
        bg = make_bg(key, stops, accent)
        fg = make_fg(key, stops, deep, accent)
        legacy = make_legacy(key, bg, fg, accent)
        write_adaptive(key)
        results[key] = (bg, fg, legacy)
        print("built", key)
    build_preview(results)


if __name__ == "__main__":
    main()
