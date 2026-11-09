# -*- coding: utf-8 -*-
"""
品牌蓝主题桌面图标生成（v1）：全新设计，与灰白/粉色 M 泡泡构图无关。
设计：深蓝对角渐变底 + 白色微软雅黑粗体「自」字（取自「自用宝典」）
      + 柔和投影 + 右上金色四芒星 + 左下小白点。
输出：
  drawable-xxxhdpi/ic_launcher_blue_fg.png  (432px adaptive 前景)
  drawable-xxxhdpi/ic_launcher_blue_bg.png  (432px adaptive 背景，深蓝纯色)
  mipmap-xxxhdpi/ic_launcher_blue.png       (192px legacy 圆角方块)
  tools/preview_launcher_blue.png           (三视图预览)
"""
import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(BASE, "app", "src", "main", "res")
FONT = r"C:\Windows\Fonts\msyhbd.ttc"

# ---- 配色 ----
BLUE_TOP = (30, 58, 160)       # 深海军蓝（渐变顶部）
BLUE_BOT = (61, 123, 255)      # 亮品牌蓝（渐变底部）
GLOW = (120, 170, 255)         # 中央柔光蓝
WHITE = (255, 255, 255)
SHADOW = (10, 24, 80)          # 字体投影（深藏蓝）
GOLD = (255, 211, 77)          # 金色四芒星
GOLD_DEEP = (255, 180, 40)


def diag_gradient(size, top, bot):
    """对角渐变（左上 top → 右下 bot）"""
    w, h = size
    im = Image.new("RGBA", size)
    px = im.load()
    for y in range(h):
        for x in range(w):
            t = (x + y) / (w + h - 2)
            px[x, y] = (
                int(top[0] + (bot[0] - top[0]) * t),
                int(top[1] + (bot[1] - top[1]) * t),
                int(top[2] + (bot[2] - top[2]) * t),
                255,
            )
    return im


def rounded_mask(size, radius):
    m = Image.new("L", size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size[0] - 1, size[1] - 1], radius=radius, fill=255)
    return m


def draw_sparkle(img, cx, cy, r, color, rot=0.0):
    """四芒星（菱形闪花）"""
    import math
    d = ImageDraw.Draw(img)
    pts = []
    for i in range(8):
        ang = math.radians(rot + i * 45)
        rad = r if i % 2 == 0 else r * 0.32
        pts.append((cx + rad * math.cos(ang), cy + rad * math.sin(ang)))
    d.polygon(pts, fill=color)


def build(with_card=True):
    """构建完整画面（legacy 用）或无底前景（adaptive fg 用）"""
    S = 864
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))

    if with_card:
        im.alpha_composite(diag_gradient((S, S), BLUE_TOP, BLUE_BOT))
        # 中央柔光
        glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
        ImageDraw.Draw(glow).ellipse([172, 132, 692, 652], fill=GLOW + (70,))
        glow = glow.filter(ImageFilter.GaussianBlur(120))
        im.alpha_composite(glow)

    # ---- 中央「自」字 ----
    f = ImageFont.truetype(FONT, 430)
    ch = "自"
    # 投影层
    sh = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(sh).text((S / 2, S / 2 - 18), ch, font=f, fill=SHADOW + (160,), anchor="mm")
    sh = sh.filter(ImageFilter.GaussianBlur(14))
    im.alpha_composite(sh)
    # 主体
    ImageDraw.Draw(im).text((S / 2, S / 2 - 36), ch, font=f, fill=WHITE, anchor="mm")

    # ---- 点缀：右上金色四芒星 + 下方小四芒星 + 左下白点 ----
    draw_sparkle(im, 660, 186, 52, GOLD)
    draw_sparkle(im, 700, 262, 26, GOLD_DEEP)
    draw_sparkle(im, 208, 668, 34, WHITE + (235,))
    d = ImageDraw.Draw(im)
    d.ellipse([150, 214, 178, 242], fill=WHITE + (200,))

    return im


def main():
    master_full = build(True)
    master_fg = build(False)

    fg_path = os.path.join(RES, "drawable-xxxhdpi", "ic_launcher_blue_fg.png")
    bg_path = os.path.join(RES, "drawable-xxxhdpi", "ic_launcher_blue_bg.png")
    legacy_path = os.path.join(RES, "mipmap-xxxhdpi", "ic_launcher_blue.png")

    master_fg.resize((432, 432), Image.LANCZOS).save(fg_path)
    Image.new("RGBA", (432, 432), BLUE_TOP + (255,)).save(bg_path)

    legacy = master_full.resize((192, 192), Image.LANCZOS)
    out = Image.new("RGBA", (192, 192), (0, 0, 0, 0))
    out.paste(legacy, (0, 0), rounded_mask((192, 192), 38))
    out.save(legacy_path)

    # ---- 预览：圆形遮罩 / 圆角方遮罩 / legacy ----
    comp = master_full.resize((432, 432), Image.LANCZOS)
    cm = Image.new("L", (432, 432), 0)
    ImageDraw.Draw(cm).ellipse([0, 0, 431, 431], fill=255)
    panel_r = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
    panel_r.paste(comp, (0, 0), cm)
    panel_sq = comp.copy()
    panel_sq.putalpha(rounded_mask((432, 432), 86))

    prev = Image.new("RGBA", (432 * 3 + 48 * 2, 432), (244, 244, 246, 255))
    prev.alpha_composite(panel_r, (0, 0))
    prev.alpha_composite(panel_sq, (432 + 48, 0))
    prev.alpha_composite(out.resize((432, 432), Image.LANCZOS), (432 * 2 + 96, 0))
    prev.save(os.path.join(BASE, "tools", "preview_launcher_blue.png"))

    print("launcher blue icons generated:")
    for p in (fg_path, bg_path, legacy_path):
        print(" -", os.path.relpath(p, BASE))


if __name__ == "__main__":
    main()
