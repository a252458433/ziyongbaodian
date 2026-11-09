# -*- coding: utf-8 -*-
"""把 adb 抓到的真机截图整理成一张交付拼图 design/UI美化_真机实拍.png"""
import os
import shutil

from PIL import Image, ImageDraw, ImageFont

BASE = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))
DESIGN = os.path.join(BASE, "design")
# 截图目录可通过环境变量指定；默认读工程内 design/screens（本机专属路径不写死）
SHOTS = os.environ.get("UI_SHOTS_DIR", os.path.join(BASE, "design", "screens"))
os.makedirs(DESIGN, exist_ok=True)

ITEMS = [
    ("漫画粉 · 首页", r"home\screen.png"),
    ("薄荷青 · 首页（新增）", r"mint\screen.png"),
    ("星野紫 · 首页（新增）", r"aurora\screen.png"),
    ("设置页 · 柔光卡片", r"after_install\screen.png"),
    ("主题弹窗 · 六套齐全", r"theme_dialog\screen.png"),
    ("桌面 · 图标/名称联动", r"desktop\screen.png"),
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


def main():
    PW, PH = 330, 696
    gap, pad = 22, 36
    cols = 3
    rows = 2
    W = pad * 2 + cols * PW + (cols - 1) * gap
    H = 108 + rows * (PH + 44) + 30
    canvas = Image.new("RGB", (W, H), (240, 243, 249))
    d = ImageDraw.Draw(canvas)
    d.text((pad, 22), "柔光漫画 2.0 · 真机实拍", font=F(30, True), fill=(27, 36, 52))
    d.text((pad, 64), "小米 8 Lite / Android 10 · adb 实机截图（非效果图）",
           font=F(14), fill=(102, 112, 133))

    for i, (label, rel) in enumerate(ITEMS):
        src = os.path.join(SHOTS, rel)
        if not os.path.exists(src):
            print("missing", src)
            continue
        r, c = divmod(i, cols)
        x = pad + c * (PW + gap)
        y = 100 + r * (PH + 44)
        im = Image.open(src).convert("RGB").resize((PW, PH), Image.LANCZOS)
        d.rounded_rectangle((x - 4, y - 4, x + PW + 4, y + PH + 4),
                            radius=22, fill=(255, 255, 255), outline=(216, 224, 236), width=2)
        canvas.paste(im, (x, y))
        d.rounded_rectangle((x, y, x + PW, y + PH), radius=18, outline=(226, 232, 242), width=1)
        d.text((x + 2, y + PH + 10), label, font=F(15, True), fill=(27, 36, 52))
        # 归档到 design，随源码包一起交付
        dst = os.path.join(DESIGN, "实拍_%s.png" % label.split(" · ")[0])
        shutil.copyfile(src, dst)

    out = os.path.join(DESIGN, "UI美化_真机实拍.png")
    canvas.save(out)
    print("saved", out, canvas.size)


if __name__ == "__main__":
    main()
