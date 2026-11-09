# -*- coding: utf-8 -*-
"""
重绘 res/mipmap/*.xml（API<26 的矢量 fallback 图标），与新启动图标统一视觉语言：
柔光渐变圆角方底 + 白色书本符号 + 星芒。
同时补齐 mipmap-anydpi-v26 中缺失的三套 adaptive 声明。
"""
import os

BASE = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))
MIP = os.path.join(BASE, "app", "src", "main", "res", "mipmap")
ADP = os.path.join(BASE, "app", "src", "main", "res", "mipmap-anydpi-v26")
os.makedirs(MIP, exist_ok=True)
os.makedirs(ADP, exist_ok=True)

PALETTES = {
    "ic_launcher":       ("#4E8BFF", "#6C6BEF"),
    "ic_launcher_round": ("#4E8BFF", "#6C6BEF"),
    "ic_launcher_book":  ("#3366EE", "#7FA6FF"),
    "ic_launcher_calc":  ("#3B4A63", "#7486A3"),
    "ic_launcher_card":  ("#E8539B", "#FF96C6"),
    "ic_launcher_list":  ("#12A9C6", "#69DAEB"),
    "ic_launcher_note":  ("#F59E22", "#FFCC78"),
    "ic_launcher_torch": ("#FFB347", "#FF7B54"),
    "ic_launcher_manga": ("#FF86B6", "#B45CE8"),
    "ic_launcher_manga_pink": ("#FF86B6", "#B45CE8"),
    "ic_launcher_blue":  ("#4E8BFF", "#6C6BEF"),
    "ic_launcher_mint":  ("#4AE0AE", "#0E94A5"),
    "ic_launcher_aurora": ("#A98BFF", "#4F46E5"),
}

TPL = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp" android:height="48dp"
    android:viewportWidth="48" android:viewportHeight="48">
    <path android:fillColor="%s"
        android:pathData="M10,0h28a10,10 0 0,1 10,10v28a10,10 0 0,1 -10,10H10A10,10 0 0,1 0,38V10A10,10 0 0,1 10,0Z" />
    <path android:fillColor="#33FFFFFF"
        android:pathData="M36,-8a20,20 0 1,0 0,40a20,20 0 1,0 0,-40" />
    <path android:fillColor="#FFFFFF"
        android:pathData="M24,16C20,13 14,12.5 10,14C9,14.4 8.5,15.4 8.5,16.5V33C8.5,34.1 9,35 10,35.4C14,37 20,36.5 24,33.5Z" />
    <path android:fillColor="#DCE8FF"
        android:pathData="M24,16C28,13 34,12.5 38,14C39,14.4 39.5,15.4 39.5,16.5V33C39.5,34.1 39,35 38,35.4C34,37 28,36.5 24,33.5Z" />
    <path android:strokeColor="%s" android:strokeWidth="1.6" android:strokeLineCap="round"
        android:pathData="M12,20H20 M12,24H20 M12,28H18 M28,20H36 M28,24H36 M28,28H34" />
    <path android:fillColor="#FFFFFF"
        android:pathData="M24,9L25.3,12.7L29,14L25.3,15.3L24,19L22.7,15.3L19,14L22.7,12.7Z" />
</vector>
"""

for name, (c1, c2) in PALETTES.items():
    with open(os.path.join(MIP, name + ".xml"), "w", encoding="utf-8", newline="\n") as f:
        f.write(TPL % (c1, c2))

# 六套主题都补上 adaptive 声明（原工程只有 manga 一套，其余在 8.0+ 上退化成 PNG）
ADP_KEYS = ["manga", "manga_pink", "blue", "torch", "mint", "aurora"]
for key in ADP_KEYS:
    path = os.path.join(ADP, "ic_launcher_%s.xml" % key)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n')
        f.write('<!-- 柔光漫画 2.0 · %s -->\n' % key)
        f.write('<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n')
        f.write('    <background android:drawable="@drawable/ic_launcher_%s_bg" />\n' % key)
        f.write('    <foreground android:drawable="@drawable/ic_launcher_%s_fg" />\n' % key)
        f.write('</adaptive-icon>\n')

print("gen_mipmap_vectors: %d mipmap vectors + %d adaptive declarations" %
      (len(PALETTES), len(ADP_KEYS)))
