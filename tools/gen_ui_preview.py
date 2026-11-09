# -*- coding: utf-8 -*-
"""
生成「柔光漫画 2.0」UI 总览预览图：首页视觉稿 + 六套主题色板 + 图标条。
纯 Pillow 绘制静态示意图（不是运行时截图），用于交付说明。
"""
import math
import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

BASE = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))
DESIGN = os.path.join(BASE, "design")
RES = os.path.join(BASE, "app", "src", "main", "res")
os.makedirs(DESIGN, exist_ok=True)

THEMES = [
    ("漫画粉", "manga_pink", (255, 134, 182), (249, 83, 143), (180, 92, 232)),
    ("石墨灰", "manga", (250, 251, 253), (227, 232, 240), (196, 205, 220)),
    ("品牌蓝", "blue", (78, 139, 255), (59, 110, 246), (108, 107, 239)),
    ("活力橙", "torch", (255, 179, 71), (247, 144, 9), (255, 123, 84)),
    ("薄荷青", "mint", (74, 224, 174), (16, 185, 129), (14, 148, 165)),
    ("星野紫", "aurora", (169, 139, 255), (122, 90, 248), (79, 70, 229)),
]


def F(sz, bold=False):
    for p in (r"C:\Windows\Fonts\msyhbd.ttc" if bold else r"C:\Windows\Fonts\msyh.ttc",
              r"C:\Windows\Fonts\arial.ttf"):
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, sz)
            except OSError:
                pass
    return ImageFont.load_default()


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def grad(w, h, c1, c2, c3, radius=0):
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = im.load()
    for y in range(h):
        for x in range(w):
            t = min(1.0, max(0.0, (x + y) / (2.0 * max(1, w + h - 2)) * 2))
            px[x, y] = lerp(c1, c2, t * 2) + (255,) if t < 0.5 else lerp(c2, c3, (t - 0.5) * 2) + (255,)
    if radius:
        m = Image.new("L", (w, h), 0)
        ImageDraw.Draw(m).rounded_rectangle((0, 0, w - 1, h - 1), radius=radius, fill=255)
        im.putalpha(m)
    return im


def rr(d, box, r, fill=None, outline=None, width=1, shadow=None):
    if shadow:
        sb = (box[0] + 2, box[1] + 4, box[2] + 2, box[3] + 4)
        d.rounded_rectangle(sb, radius=r, fill=(28, 42, 64, 26))
    d.rounded_rectangle(box, radius=r, fill=fill, outline=outline, width=width)


def star(d, cx, cy, r, fill, inner=0.26, points=4):
    pts = []
    for i in range(points * 2):
        ang = -math.pi / 2 + i * math.pi / points
        rr_ = r if i % 2 == 0 else r * inner
        pts.append((cx + rr_ * math.cos(ang), cy + rr_ * math.sin(ang)))
    d.polygon(pts, fill=fill)


def draw_phone(theme):
    """绘制一张 360x760 的首页视觉稿"""
    name, key, c1, c2, c3 = theme
    W, H = 360, 760
    im = Image.new("RGBA", (W, H), (244, 247, 252, 255))
    d = ImageDraw.Draw(im)

    # 头部渐变
    head = grad(W, 250, c1, c2, c3)
    head = head.crop((0, 0, W, 250))
    # 只圆下方两角
    m = Image.new("L", (W, 250), 0)
    md = ImageDraw.Draw(m)
    md.rounded_rectangle((0, -40, W, 250), radius=30, fill=255)
    head.putalpha(m)
    im.alpha_composite(head)
    # 光晕
    glow = Image.new("RGBA", (W, 250), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse((W - 130, -70, W + 90, 150), fill=(255, 255, 255, 70))
    im.alpha_composite(glow.filter(ImageFilter.GaussianBlur(42)))
    # 网点
    nl = Image.new("RGBA", (W, 250), (0, 0, 0, 0))
    nd = ImageDraw.Draw(nl)
    for y in range(0, 250, 20):
        for x in range(0, W, 20):
            nd.ellipse((x + (10 if y // 20 % 2 else 0) - 2, y - 2,
                        x + (10 if y // 20 % 2 else 0) + 2, y + 2), fill=(255, 255, 255, 34))
    im.alpha_composite(nl)

    dark_text = sum(c2) / 3 > 205
    hc = (38, 48, 63) if dark_text else (255, 255, 255)
    sub = (90, 100, 115) if dark_text else (255, 255, 255, 220)

    d = ImageDraw.Draw(im)
    d.text((22, 40), "自用宝典", font=F(30, True), fill=hc)
    d.text((22, 80), "屏上搜题 · 一秒出答案", font=F(12), fill=hc)
    rr(d, (W - 62, 36, W - 22, 76), 20, fill=(255, 255, 255, 60), outline=(255, 255, 255, 110))
    # 齿轮：三条滑杆 + 圆形滑块（不依赖字体字符）
    gx, gy = W - 52, 48
    for k, yy in enumerate((gy, gy + 7, gy + 14)):
        d.line((gx, yy, gx + 18, yy), fill=hc, width=2)
        ox = 5 if k == 0 else (12 if k == 1 else 7)
        d.ellipse((gx + ox - 3, yy - 3, gx + ox + 3, yy + 3), outline=hc, width=2)

    # 搜索框
    rr(d, (20, 118, W - 20, 172), 22, fill=(255, 255, 255), outline=(230, 236, 246),
       width=1, shadow=True)
    d.ellipse((38, 137, 51, 150), outline=c2, width=2)
    d.line((50, 149, 57, 156), fill=c2, width=2)
    d.text((66, 136), "粘贴题目文字，立即搜题", font=F(13), fill=(152, 162, 179))
    rr(d, (W - 96, 126, W - 28, 164), 19, fill=c2)
    d.text((W - 79, 134), "搜题", font=F(13, True), fill=(255, 255, 255))
    d.text((112, 184), "长按搜索框可开启屏上搜题", font=F(11), fill=hc)

    # 分区标题
    rr(d, (20, 272, 24, 290), 2, fill=c2)
    d.text((32, 270), "开始练习", font=F(15, True), fill=(27, 36, 52))
    d.text((W - 108, 274), "选择一种刷题方式", font=F(11), fill=(152, 162, 179))

    CHIPS = [((228, 238, 255), (211, 226, 255)), ((228, 248, 238), (208, 241, 225)),
             ((239, 234, 255), (225, 216, 255)), ((228, 238, 255), (211, 226, 255)),
             ((255, 235, 234), (255, 218, 216)), ((255, 242, 223), (255, 231, 194))]
    LABELS = [("顺序练习", "从头刷到尾"), ("题型练习", "单选/多选/判断"),
              ("题库练习", "选一个题库开刷"), ("随机自测", "打乱抽题"),
              ("错题重练", "自动收错题本"), ("收藏复习", "重点反复看")]
    for i, (label, sublabel) in enumerate(LABELS):
        r, c = divmod(i, 2)
        x0 = 20 + c * 164
        y0 = 304 + r * 118
        rr(d, (x0, y0, x0 + 152, y0 + 106), 22, fill=(255, 255, 255),
           outline=(230, 236, 246), width=1, shadow=True)
        cc1, cc2 = CHIPS[i]
        rr(d, (x0 + 55, y0 + 12, x0 + 97, y0 + 54), 18, fill=cc1, outline=cc2)
        # 功能图标：书页 + 横线（不依赖字体字符）
        d.rounded_rectangle((x0 + 66, y0 + 23, x0 + 86, y0 + 43), radius=3,
                            outline=c2, width=2)
        d.line((x0 + 76, y0 + 23, x0 + 76, y0 + 43), fill=c2, width=2)
        for k in range(2):
            d.line((x0 + 69, y0 + 29 + k * 7, x0 + 73, y0 + 29 + k * 7), fill=c2, width=1)
            d.line((x0 + 79, y0 + 29 + k * 7, x0 + 83, y0 + 29 + k * 7), fill=c2, width=1)
        tw = d.textbbox((0, 0), label, font=F(13, True))[2]
        d.text((x0 + (152 - tw) / 2, y0 + 62), label, font=F(13, True), fill=(27, 36, 52))
        sw = d.textbbox((0, 0), sublabel, font=F(10))[2]
        d.text((x0 + (152 - sw) / 2, y0 + 82), sublabel, font=F(10), fill=(152, 162, 179))

    # 学习概况
    rr(d, (20, 662, W - 20, 738), 22, fill=(255, 255, 255), outline=(230, 236, 246),
       width=1, shadow=True)
    rr(d, (34, 676, 38, 692), 2, fill=c2)
    d.text((46, 674), "学习概况", font=F(13, True), fill=(27, 36, 52))
    d.text((W - 82, 676), "实时统计", font=F(10), fill=(152, 162, 179))
    for j, (num, lab) in enumerate([("1", "题库"), ("1708", "总题量"), ("0", "错题"), ("0", "收藏")]):
        cx = 56 + j * 82
        nw = d.textbbox((0, 0), num, font=F(19, True))[2]
        d.text((cx - nw / 2, 698), num, font=F(19, True), fill=c2)
        lw = d.textbbox((0, 0), lab, font=F(10))[2]
        d.text((cx - lw / 2, 720), lab, font=F(10), fill=(102, 112, 133))
        if j < 3:
            d.line((cx + 41, 702, cx + 41, 728), fill=(230, 236, 246), width=1)

    return im


def main():
    phones = [draw_phone(t) for t in THEMES]
    PH_W, PH_H = 360, 760
    gap = 24
    pad = 40
    cols = 3
    rows = 2
    W = pad * 2 + cols * PH_W + (cols - 1) * gap
    H = 130 + rows * (PH_H + 52) + 120
    canvas = Image.new("RGB", (W, H), (238, 242, 249))
    d = ImageDraw.Draw(canvas)
    d.text((pad, 26), "柔光漫画 2.0 · UI 美化总览", font=F(34, True), fill=(27, 36, 52))
    d.text((pad, 72), "六套主题 · 统一图标系统 · 柔光卡片 / 渐变头部 / 主题色按钮   （静态视觉稿，非运行截图）",
           font=F(15), fill=(102, 112, 133))

    for i, (theme, ph) in enumerate(zip(THEMES, phones)):
        r, c = divmod(i, cols)
        x = pad + c * (PH_W + gap)
        y = 120 + r * (PH_H + 52)
        # 手机外框
        d.rounded_rectangle((x - 6, y - 6, x + PH_W + 6, y + PH_H + 6),
                            radius=32, fill=(255, 255, 255), outline=(216, 224, 236), width=2)
        canvas.paste(ph.convert("RGB"), (x, y))
        d.rounded_rectangle((x, y, x + PH_W, y + PH_H), radius=26, outline=(226, 232, 242), width=1)
        # 主题标签
        lx, ly = x, y + PH_H + 12
        d.rounded_rectangle((lx, ly, lx + 18, ly + 18), radius=9, fill=theme[3])
        d.text((lx + 28, ly - 1), theme[0], font=F(16, True), fill=(27, 36, 52))
        d.text((lx + 108, ly + 2), "#%02X%02X%02X" % theme[3], font=F(12), fill=(120, 130, 148))

    # 底部图标条
    iy = 120 + rows * (PH_H + 52) + 8
    d.text((pad, iy), "桌面图标（自适应 · 统一书本 + 星芒构图）", font=F(17, True), fill=(27, 36, 52))
    for i, theme in enumerate(THEMES):
        p = os.path.join(RES, "mipmap-xxxhdpi", "ic_launcher_%s.png" % theme[1])
        if os.path.exists(p):
            ic = Image.open(p).convert("RGBA").resize((76, 76), Image.LANCZOS)
            xx = pad + i * 128
            canvas.paste(ic.convert("RGB"), (xx, iy + 34), ic.getchannel("A"))

    out = os.path.join(DESIGN, "UI美化_总览.png")
    canvas.save(out)
    print("preview ->", out, canvas.size)


if __name__ == "__main__":
    main()
