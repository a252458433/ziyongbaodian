# -*- coding: utf-8 -*-
"""
「漫画刷题」桌面图标生成 v2 —— 糖果泡泡风（参考用户提供的样图重做）。

设计构成（无吉祥物）：
  卡片底：米白 #F7F2F6 圆角方块（legacy 自带圆角；adaptive 用纯色背景层）
  主视觉：淡蓝径向渐变大圆 + 竖向高光条纹 + 边缘月牙光
  主体：粗墨紫描边的「M」泡泡字（粉白渐变 + 中心高光 + 底部软阴影）
  点缀：墨紫描边闪光星（4/5 角）、粉色渐变实心星、蓝/粉渐变气泡球、小圆点
  字母 M 取自「漫画」Manga

输出：
  app/src/main/res/drawable-xxxhdpi/ic_launcher_manga_fg.png   432x432 adaptive 前景
  app/src/main/res/drawable-xxxhdpi/ic_launcher_manga_bg.png   432x432 adaptive 背景（米白纯色）
  app/src/main/res/mipmap-xxxhdpi/ic_launcher_manga.png        192x192 legacy 圆角方块
  tools/preview_launcher_manga.png                             效果预览拼图
"""
import math
import os

from PIL import Image, ImageDraw, ImageFilter, ImageChops, ImageFont

BASE = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))
RES = os.path.join(BASE, "app", "src", "main", "res")
BASE_SIZE = 432

# ---- 配色（v4 双版本：石墨灰 / 漫画粉，随应用内主题切换）----
# 键名沿用历史变量名。M_TOP/M_MID/M_BOT 为「M」字填充渐变。
GRAY_PALETTE = dict(
    CARD=(246, 247, 249),           # 近纯白卡片
    CIRCLE_IN=(252, 252, 254),      # 圆心近白
    CIRCLE_OUT=(209, 214, 221),     # 圆缘浅灰
    CIRCLE_RIM=(188, 193, 201),     # 圆描边
    PLUM=(74, 80, 92),              # 石墨灰（描边）
    PLUM_SOFT=(118, 124, 136),      # 软阴影灰
    STAR_PINK_TOP=(206, 210, 217),
    STAR_PINK_BOT=(152, 158, 169),
    BLUE_BALL_BOT=(226, 229, 234),
    PINK_BALL_BOT=(238, 240, 243),
    DOT_PINK=(144, 150, 161),
    M_TOP=(196, 201, 209), M_MID=(230, 233, 238), M_BOT=(255, 255, 255),
)

PINK_PALETTE = dict(
    CARD=(253, 242, 247),           # 浅粉白卡片
    CIRCLE_IN=(255, 246, 250),      # 圆心近白粉
    CIRCLE_OUT=(249, 197, 216),     # 圆缘浅粉
    CIRCLE_RIM=(240, 170, 198),     # 圆描边
    PLUM=(198, 60, 112),            # 深粉描边
    PLUM_SOFT=(232, 128, 170),      # 软阴影粉
    STAR_PINK_TOP=(255, 178, 205),
    STAR_PINK_BOT=(241, 113, 160),
    BLUE_BALL_BOT=(255, 214, 229),
    PINK_BALL_BOT=(250, 196, 216),
    DOT_PINK=(240, 130, 170),
    M_TOP=(255, 168, 199), M_MID=(255, 216, 232), M_BOT=(255, 255, 255),
)


def set_palette(p):
    g = globals()
    for k, v in p.items():
        g[k] = v


set_palette(GRAY_PALETTE)  # 模块级默认（函数默认参数在 def 时求值，必须先存在）

FONT_CANDIDATES = [
    r"C:/Windows/Fonts/comicbd.ttf",
    r"C:/Windows/Fonts/arialroundedmtbold.ttf",
    r"C:/Windows/Fonts/arialbd.ttf",
]


def load_font(size):
    for p in FONT_CANDIDATES:
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, size)
            except OSError:
                continue
    return ImageFont.load_default()


def lerp(c1, c2, t):
    return tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(3))


def radial_gradient(size, inner, outer):
    im = Image.new("RGB", (size, size))
    px = im.load()
    c = size / 2
    for y in range(size):
        for x in range(size):
            t = min(1.0, math.hypot(x - c, y - c) / (size / 2))
            px[x, y] = lerp(inner, outer, t ** 1.25)
    return im.convert("RGBA")


def v_gradient(w, h, top, bot, mid=None):
    im = Image.new("RGB", (w, h))
    px = im.load()
    for y in range(h):
        t = y / max(1, h - 1)
        if mid and t < mid[0]:
            c = lerp(top, mid[1], t / mid[0])
        elif mid:
            c = lerp(mid[1], bot, (t - mid[0]) / (1 - mid[0]))
        else:
            c = lerp(top, bot, t)
        for x in range(w):
            px[x, y] = c
    return im.convert("RGBA")


def concave_star(cx, cy, r, points=4, inner=0.22, rot=-90.0, steps=120):
    """闪光星：外尖内凹的平滑曲线轮廓（rot 处为外尖）。"""
    pts = []
    seg = 360.0 / points
    half = math.radians(seg / 2)
    for i in range(steps):
        ang = math.radians(rot + 360.0 * i / steps)
        ph = math.radians((360.0 * i / steps) % seg)
        if ph <= half:
            t = ph / half
            rr = r * (1 - (1 - inner) * math.sin(t * math.pi / 2) ** 1.6)
        else:
            t = (ph - half) / half
            rr = r * (inner + (1 - inner) * math.sin(t * math.pi / 2) ** 1.6)
        pts.append((cx + rr * math.cos(ang), cy + rr * math.sin(ang)))
    return pts


def star5(cx, cy, r, rot=-90.0, inner=0.45):
    pts = []
    for i in range(10):
        ang = math.radians(rot + i * 36)
        rr = r if i % 2 == 0 else r * inner
        pts.append((cx + rr * math.cos(ang), cy + rr * math.sin(ang)))
    return pts


def outline_layer(shape_alpha, w, color):
    """给 alpha 形状生成圆角描边层（膨胀 - 原形）。"""
    k = max(1, int(w)) * 2 + 1
    dil = shape_alpha.filter(ImageFilter.MaxFilter(k))
    ring = ImageChops.subtract(dil, shape_alpha)
    layer = Image.new("RGBA", shape_alpha.size, color + (0,))
    layer.putalpha(ring)
    return layer


def draw_outline_star(img, cx, cy, r, points, w, color=None, rot=-90.0):
    color = color or PLUM  # 默认参数不能绑调色板（def 时求值），运行时取当前主题色
    pad = int(w) + 6
    size = int(r * 2 + pad * 2)
    a = Image.new("L", (size, size), 0)
    poly = (concave_star(size / 2, size / 2, r, 4, 0.15, rot)
            if points == 4 else star5(size / 2, size / 2, r, rot))
    ImageDraw.Draw(a).polygon(poly, fill=255)
    ring = outline_layer(a, w, color)
    img.alpha_composite(ring, (int(cx - size / 2), int(cy - size / 2)))


def draw_filled_star(img, cx, cy, r, points, w, rot=-90.0,
                     top=None, bot=None):
    top = top or STAR_PINK_TOP
    bot = bot or STAR_PINK_BOT
    """粉色渐变实心星 + 墨紫描边。"""
    pad = int(w) + 6
    size = int(r * 2 + pad * 2)
    a = Image.new("L", (size, size), 0)
    poly = (concave_star(size / 2, size / 2, r, 4, 0.15, rot)
            if points == 4 else star5(size / 2, size / 2, r, rot))
    ImageDraw.Draw(a).polygon(poly, fill=255)
    ox, oy = int(cx - size / 2), int(cy - size / 2)
    ring = outline_layer(a, w, PLUM)
    img.alpha_composite(ring, (ox, oy))
    grad = v_gradient(size, size, top, bot)
    img.paste(grad, (ox, oy), a)
    # 左上小白高光
    hl = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ImageDraw.Draw(hl).ellipse([size * 0.32, size * 0.30, size * 0.48, size * 0.42],
                               fill=(255, 255, 255, 190))
    hl.putalpha(ImageChops.multiply(hl.getchannel("A"), a))
    img.alpha_composite(hl, (ox, oy))


def draw_ball(img, cx, cy, r, bot_color, w):
    """渐变气泡球：浅顶深底 + 墨紫描边 + 左上高光。"""
    pad = int(w) + 4
    size = int(r * 2 + pad * 2)
    a = Image.new("L", (size, size), 0)
    ImageDraw.Draw(a).ellipse([pad, pad, size - pad - 1, size - pad - 1], fill=255)
    ox, oy = int(cx - size / 2), int(cy - size / 2)
    ring = outline_layer(a, w, PLUM)
    img.alpha_composite(ring, (ox, oy))
    grad = radial_gradient(size, (255, 255, 255), bot_color)
    img.paste(grad, (ox, oy), a)
    hl = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ImageDraw.Draw(hl).ellipse([pad + size * 0.18, pad + size * 0.14,
                                pad + size * 0.36, pad + size * 0.30],
                               fill=(255, 255, 255, 220))
    img.alpha_composite(hl, (ox, oy))


def draw_dot(img, cx, cy, r, color=None, outline=None, ow=0):
    color = color or DOT_PINK
    d = ImageDraw.Draw(img)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=color,
              outline=outline, width=ow)


def draw_white_sparkle(img, cx, cy, r, alpha=235):
    d = ImageDraw.Draw(img)
    d.polygon(concave_star(cx, cy, r, 4, 0.16), fill=(255, 255, 255, alpha))


def build_M(size_full, cx, cy, target_w, stroke_w):
    """泡泡字母 M：软阴影 + 墨紫描边 + 粉白渐变 + 中心高光 + 白色小闪光。"""
    probe = Image.new("L", (10, 10))
    b = ImageDraw.Draw(probe).textbbox((0, 0), "M", font=load_font(100))
    fsize = int(100 * target_w / (b[2] - b[0]))
    font = load_font(fsize)
    pad = int(stroke_w * 3 + 40)
    bb = ImageDraw.Draw(probe).textbbox((0, 0), "M", font=font)
    gw, gh = bb[2] - bb[0] + pad * 2, bb[3] - bb[1] + pad * 2
    glyph = Image.new("L", (gw, gh), 0)
    ImageDraw.Draw(glyph).text((pad - bb[0], pad - bb[1]), "M", font=font, fill=255)

    gx, gy = int(cx - gw / 2), int(cy - gh / 2)

    # 软阴影
    shadow_a = glyph.filter(ImageFilter.GaussianBlur(stroke_w * 0.9))
    tint = Image.new("RGBA", (gw, gh), PLUM_SOFT + (0,))
    tint.putalpha(shadow_a.point(lambda v: int(v * 0.55)))
    size_full.alpha_composite(tint, (gx + int(stroke_w * 0.4), gy + int(stroke_w * 1.1)))

    # 描边
    ring = outline_layer(glyph, stroke_w, PLUM)
    size_full.alpha_composite(ring, (gx, gy))

    # 主题色渐变填充 + 中心柔光
    grad = v_gradient(gw, gh, M_TOP, M_BOT,
                      mid=(0.52, M_MID))
    glow = Image.new("L", (gw, gh), 0)
    ImageDraw.Draw(glow).ellipse([gw * 0.18, gh * 0.16, gw * 0.82, gh * 0.92], fill=110)
    glow = glow.filter(ImageFilter.GaussianBlur(stroke_w * 1.4))
    white = Image.new("RGBA", (gw, gh), (255, 255, 255, 255))
    white.putalpha(glow)
    grad = Image.alpha_composite(grad, white)
    size_full.paste(grad, (gx, gy), glyph)

    # 顶部光泽小高光
    gl = Image.new("RGBA", (gw, gh), (0, 0, 0, 0))
    gd = ImageDraw.Draw(gl)
    gd.ellipse([gw * 0.16, gh * 0.10, gw * 0.40, gh * 0.26], fill=(255, 255, 255, 210))
    gd.ellipse([gw * 0.62, gh * 0.06, gw * 0.80, gh * 0.20], fill=(255, 255, 255, 170))
    gl = gl.filter(ImageFilter.GaussianBlur(stroke_w * 0.5))
    gl.putalpha(ImageChops.multiply(gl.getchannel("A"), glyph))
    size_full.alpha_composite(gl, (gx, gy))

    # 字面上的白色闪光星（小而精致）
    sp = Image.new("RGBA", (gw, gh), (0, 0, 0, 0))
    sd = ImageDraw.Draw(sp)
    sd.polygon(concave_star(gw * 0.26, gh * 0.34, gh * 0.085, 4, 0.18),
               fill=(255, 255, 255, 235))
    sd.polygon(concave_star(gw * 0.72, gh * 0.30, gh * 0.065, 4, 0.18),
               fill=(255, 255, 255, 215))
    sd.polygon(concave_star(gw * 0.55, gh * 0.74, gh * 0.055, 4, 0.18),
               fill=(255, 255, 255, 200))
    sd.polygon(concave_star(gw * 0.18, gh * 0.68, gh * 0.05, 4, 0.18),
               fill=(255, 255, 255, 195))
    size_full.alpha_composite(sp, (gx, gy))


def build_circle(img, cx, cy, Rc, s):
    """淡蓝渐变主圆 + 竖条纹 + 月牙高光 + 圆上白色闪光。"""
    size = int(Rc * 2)
    grad = radial_gradient(size, CIRCLE_IN, CIRCLE_OUT)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, size - 1, size - 1], fill=255)
    img.paste(grad, (int(cx - Rc), int(cy - Rc)), mask)

    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ld = ImageDraw.Draw(layer)
    # 竖向高光条纹（裁剪在圆内）
    x = cx - Rc + Rc * 0.12
    i = 0
    while x < cx + Rc - Rc * 0.06:
        w = (12 if i % 3 == 0 else 7) * s
        ld.rounded_rectangle([x, cy - Rc * 0.94, x + w, cy + Rc * 0.94],
                             radius=w / 2, fill=(255, 255, 255, 34))
        x += 27 * s
        i += 1
    # 左上大块柔光
    sheen = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(sheen).ellipse([cx - Rc * 0.85, cy - Rc * 0.9,
                                   cx + Rc * 0.1, cy - Rc * 0.15],
                                  fill=(255, 255, 255, 70))
    sheen = sheen.filter(ImageFilter.GaussianBlur(18 * s))
    layer.alpha_composite(sheen)
    # 月牙边光
    ld.arc([cx - Rc * 0.92, cy - Rc * 0.92, cx + Rc * 0.92, cy + Rc * 0.92],
           start=185, end=245, fill=(255, 255, 255, 110), width=int(7 * s))
    # 圆描边
    ld.ellipse([cx - Rc, cy - Rc, cx + Rc, cy + Rc], outline=CIRCLE_RIM,
               width=int(5 * s))
    # 裁剪到圆内
    clip = Image.new("L", img.size, 0)
    ImageDraw.Draw(clip).ellipse([cx - Rc, cy - Rc, cx + Rc, cy + Rc], fill=255)
    layer.putalpha(ImageChops.multiply(layer.getchannel("A"), clip))
    img.alpha_composite(layer)

    # 圆上白色闪光星
    draw_white_sparkle(img, cx + Rc * 0.38, cy - Rc * 0.70, 10 * s)
    draw_white_sparkle(img, cx + Rc * 0.72, cy - Rc * 0.10, 7 * s, 210)
    draw_white_sparkle(img, cx - Rc * 0.68, cy + Rc * 0.52, 8 * s, 215)
    draw_white_sparkle(img, cx - Rc * 0.10, cy - Rc * 0.84, 6 * s, 190)


def build_scene(s=2.0, with_card=True):
    """整体画面。s=1 对应 432px。"""
    W = int(BASE_SIZE * s)
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    if with_card:
        ImageDraw.Draw(img).rectangle([0, 0, W - 1, W - 1], fill=CARD + (255,))

    cx = cy = W / 2
    Rc = 128 * s

    build_circle(img, cx, cy, Rc, s)
    build_M(img, cx, cy - 4 * s, target_w=136 * s, stroke_w=13 * s)

    # ---- 卡片点缀（四角 + 边缘，参照样图布局）----
    w_out = max(2, int(3.5 * s))
    # 左上：描边闪光星 + 粉色实心星
    draw_outline_star(img, 80 * s, 80 * s, 18 * s, 4, w_out, rot=-100)
    draw_filled_star(img, 82 * s, 112 * s, 16 * s, 5, w_out, rot=-95)
    draw_filled_star(img, 62 * s, 170 * s, 11 * s, 5, w_out, rot=-85)
    # 顶边
    draw_outline_star(img, 224 * s, 36 * s, 11 * s, 4, w_out, rot=-90)
    draw_dot(img, 188 * s, 30 * s, 5 * s)
    draw_dot(img, 262 * s, 30 * s, 4 * s)
    # 右上：蓝气泡球 + 描边星
    draw_ball(img, 312 * s, 90 * s, 24 * s, BLUE_BALL_BOT, w_out)
    draw_outline_star(img, 350 * s, 70 * s, 15 * s, 4, w_out, rot=-70)
    draw_filled_star(img, 400 * s, 122 * s, 13 * s, 4, w_out, rot=-100)
    # 右缘
    draw_ball(img, 398 * s, 192 * s, 13 * s, PINK_BALL_BOT, w_out)
    draw_outline_star(img, 394 * s, 248 * s, 14 * s, 5, w_out, rot=-90)
    draw_ball(img, 380 * s, 296 * s, 15 * s, PINK_BALL_BOT, w_out)
    # 右下
    draw_ball(img, 344 * s, 348 * s, 20 * s, BLUE_BALL_BOT, w_out)
    draw_filled_star(img, 298 * s, 402 * s, 12 * s, 5, w_out, rot=-92)
    draw_dot(img, 342 * s, 416 * s, 5 * s)
    # 底边
    draw_filled_star(img, 168 * s, 388 * s, 13 * s, 5, w_out, rot=-92)
    draw_dot(img, 240 * s, 418 * s, 5 * s)
    # 左下（原吉祥物位置改为星星组合）
    draw_filled_star(img, 74 * s, 352 * s, 18 * s, 5, w_out, rot=-90)
    draw_ball(img, 108 * s, 332 * s, 9 * s, BLUE_BALL_BOT, w_out)
    draw_dot(img, 38 * s, 392 * s, 6 * s)
    draw_outline_star(img, 56 * s, 292 * s, 14 * s, 4, w_out, rot=-95)
    # 左缘
    draw_ball(img, 32 * s, 132 * s, 10 * s, BLUE_BALL_BOT, w_out)
    draw_dot(img, 26 * s, 210 * s, 5 * s)
    draw_dot(img, 30 * s, 252 * s, 6 * s)
    return img


def rounded_mask(size, radius):
    m = Image.new("L", (size, size), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size - 1, size - 1],
                                        radius=radius, fill=255)
    return m


def emit(suffix, preview_name):
    """按当前调色板生成一套图标，文件名带 suffix（如 "_pink"）。"""
    master_full = build_scene(2.0, with_card=True)      # 864px 完整画面
    master_fg = build_scene(2.0, with_card=False)       # 864px 无卡片底

    fg_path = os.path.join(RES, "drawable-xxxhdpi", f"ic_launcher_manga{suffix}_fg.png")
    bg_path = os.path.join(RES, "drawable-xxxhdpi", f"ic_launcher_manga{suffix}_bg.png")
    legacy_path = os.path.join(RES, "mipmap-xxxhdpi", f"ic_launcher_manga{suffix}.png")

    master_fg.resize((432, 432), Image.LANCZOS).save(fg_path)
    Image.new("RGBA", (432, 432), CARD + (255,)).save(bg_path)

    legacy = master_full.resize((192, 192), Image.LANCZOS)
    out = Image.new("RGBA", (192, 192), (0, 0, 0, 0))
    out.paste(legacy, (0, 0), rounded_mask(192, int(192 * 0.2)))
    out.save(legacy_path)

    # ---- 预览：圆形遮罩 / 圆角方遮罩 / legacy ----
    comp = master_full.resize((432, 432), Image.LANCZOS)
    cm = Image.new("L", (432, 432), 0)
    ImageDraw.Draw(cm).ellipse([0, 0, 431, 431], fill=255)
    panel_r = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
    panel_r.paste(comp, (0, 0), cm)
    panel_sq = comp.copy()
    panel_sq.putalpha(rounded_mask(432, int(432 * 0.2)))

    prev = Image.new("RGBA", (432 * 3 + 48 * 2, 432), (244, 244, 246, 255))
    prev.alpha_composite(panel_r, (0, 0))
    prev.alpha_composite(panel_sq, (432 + 48, 0))
    prev.alpha_composite(out.resize((432, 432), Image.LANCZOS), (432 * 2 + 96, 0))
    prev.save(os.path.join(BASE, "tools", preview_name))

    print(f"launcher icons [{suffix or 'gray'}]:")
    for p in (fg_path, bg_path, legacy_path):
        print(" -", os.path.relpath(p, BASE))


def main():
    set_palette(GRAY_PALETTE)
    emit("", "preview_launcher_manga.png")
    set_palette(PINK_PALETTE)
    emit("_pink", "preview_launcher_manga_pink.png")


if __name__ == "__main__":
    main()
