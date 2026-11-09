# -*- coding: utf-8 -*-
"""
活力橙主题（手电筒）桌面图标生成（v1）：全新设计。
设计：橙色对角渐变底 + 白色手电筒侧视剪影（竖置：梯形灯头 + 圆角筒身 + 侧按钮）
      + 向上的暖黄光束（柔光渐隐）+ 金色小星星点缀。
输出：
  drawable-xxxhdpi/ic_launcher_torch_fg.png  (432px adaptive 前景)
  drawable-xxxhdpi/ic_launcher_torch_bg.png  (432px adaptive 背景，深橙纯色)
  mipmap-xxxhdpi/ic_launcher_torch.png       (192px legacy 圆角方块)
  tools/preview_launcher_torch.png           (三视图预览)
"""
import math
import os
from PIL import Image, ImageDraw, ImageFilter

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(BASE, "app", "src", "main", "res")

# ---- 配色 ----
ORANGE_TOP = (250, 140, 20)    # 深橙（渐变顶部）
ORANGE_BOT = (255, 183, 77)    # 亮橙（渐变底部）
GLOW = (255, 205, 120)         # 中央柔光
WHITE = (255, 255, 255)
BEAM = (255, 244, 200)         # 光束暖白
BEAM_FADE = (255, 226, 150)    # 光束末端
BUTTON = (240, 120, 10)        # 筒身按钮
GOLD = (255, 214, 90)          # 点缀金星
S = 864


def diag_gradient(size, top, bot):
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


def rounded_poly(draw, pts, radius, fill):
    """用放大+圆角近似画圆角多边形（这里直接画多边形+端点圆即可）"""
    draw.polygon(pts, fill=fill)
    # 对每个顶点画圆柔化尖角
    for (x, y) in pts:
        pass  # 多边形已够平滑，不需要额外处理


def build(with_bg=True):
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))

    if with_bg:
        im.alpha_composite(diag_gradient((S, S), ORANGE_TOP, ORANGE_BOT))
        glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
        ImageDraw.Draw(glow).ellipse([192, 152, 672, 632], fill=GLOW + (70,))
        glow = glow.filter(ImageFilter.GaussianBlur(120))
        im.alpha_composite(glow)

    cx = S / 2
    # ---- 光束：从灯口向上张开、渐隐 ----
    beam = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    bd = ImageDraw.Draw(beam)
    # 灯口宽度约 150，顶部张开到 320
    bd.polygon(
        [(cx - 75, 268), (cx + 75, 268), (cx + 160, 64), (cx - 160, 64)],
        fill=BEAM + (215,),
    )
    # 光束边缘渐隐：叠加一层更大的低透明度光
    halo = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(halo).polygon(
        [(cx - 95, 272), (cx + 95, 272), (cx + 200, 56), (cx - 200, 56)],
        fill=BEAM_FADE + (90,),
    )
    halo = halo.filter(ImageFilter.GaussianBlur(26))
    beam = beam.filter(ImageFilter.GaussianBlur(6))
    # 光束垂直渐隐：顶部（远端）更透明
    fade = Image.new("L", (S, S), 255)
    fd = ImageDraw.Draw(fade)
    for y in range(56, 280):
        t = (y - 56) / (280 - 56)
        fd.line([(0, y), (S, y)], fill=int(90 + 165 * t))
    r, g, b, a = beam.split()
    a = Image.composite(a, Image.new("L", (S, S), 0), fade)
    beam = Image.merge("RGBA", (r, g, b, a))
    im.alpha_composite(halo)
    im.alpha_composite(beam)

    # ---- 灯头：梯形（上窄下更窄？实际手电灯头朝前更宽）——灯口朝上：上宽下窄 ----
    d = ImageDraw.Draw(im)
    # 灯头梯形：顶部（灯口）宽 220，底部（接筒身）宽 150，高 90
    d.polygon(
        [(cx - 110, 276), (cx + 110, 276), (cx + 75, 366), (cx - 75, 366)],
        fill=WHITE,
    )
    # 灯口反光环（深橙内圈线）
    d.rounded_rectangle([cx - 96, 282, cx + 96, 300], radius=9, fill=(255, 235, 200, 255))

    # ---- 筒身：圆角矩形 ----
    d.rounded_rectangle([cx - 62, 360, cx + 62, 640], radius=42, fill=WHITE)
    # 筒身细节：按钮
    d.rounded_rectangle([cx - 16, 420, cx + 16, 470], radius=12, fill=BUTTON)
    # 筒身底部两条装饰线
    d.rounded_rectangle([cx - 40, 566, cx + 40, 578], radius=6, fill=(255, 220, 170, 255))
    d.rounded_rectangle([cx - 40, 590, cx + 40, 602], radius=6, fill=(255, 220, 170, 255))

    # ---- 点缀金星 ----
    def sparkle(x, y, r, color):
        pts = []
        for i in range(8):
            ang = math.radians(i * 45)
            rad = r if i % 2 == 0 else r * 0.32
            pts.append((x + rad * math.cos(ang), y + rad * math.sin(ang)))
        d.polygon(pts, fill=color)

    sparkle(206, 218, 40, GOLD)
    sparkle(668, 620, 28, WHITE + (230,))
    d.ellipse([676, 200, 700, 224], fill=WHITE + (190,))

    return im


def main():
    master_full = build(True)
    master_fg = build(False)

    fg_path = os.path.join(RES, "drawable-xxxhdpi", "ic_launcher_torch_fg.png")
    bg_path = os.path.join(RES, "drawable-xxxhdpi", "ic_launcher_torch_bg.png")
    legacy_path = os.path.join(RES, "mipmap-xxxhdpi", "ic_launcher_torch.png")

    master_fg.resize((432, 432), Image.LANCZOS).save(fg_path)
    Image.new("RGBA", (432, 432), ORANGE_TOP + (255,)).save(bg_path)

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
    prev.save(os.path.join(BASE, "tools", "preview_launcher_torch.png"))

    print("launcher torch icons generated:")
    for p in (fg_path, bg_path, legacy_path):
        print(" -", os.path.relpath(p, BASE))


if __name__ == "__main__":
    main()
