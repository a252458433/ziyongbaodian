# -*- coding: utf-8 -*-
import os
OUT = r"C:/Users/Administrator/WorkBuddy/2026-10-28-15-17-43/ziyongbaodian/app/src/main/res/drawable"

def solid(name, color, radius, alpha=None, stroke=None, sw="1dp"):
    a = f'    <solid android:color="{color}" />\n'
    s = f'''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
{a}    <corners android:radius="{radius}" />
'''
    if stroke:
        s += f'    <stroke android:width="{sw}" android:color="{stroke}" />\n'
    s += '</shape>\n'
    open(os.path.join(OUT, name + ".xml"), "w", encoding="utf-8").write(s)

solid("bg_pill_white20", "#33FFFFFF", "999dp")
solid("bg_pill_brand_solid", "#2F6BFF", "999dp")
solid("bg_icon_blue", "#EAF1FF", "14dp")
solid("bg_icon_green", "#E7F8F0", "14dp")
solid("bg_icon_orange", "#FFF6E6", "14dp")
solid("bg_icon_purple", "#F1EDFF", "14dp")
solid("bg_icon_red", "#FEECEB", "14dp")
solid("bg_icon_gray", "#F1F4FA", "14dp")
solid("bg_white_top_round", "#FFFFFF", "18dp")
solid("bg_dark_ball", "#E61B2230", "999dp")
solid("bg_float_btn", "#26FFFFFF", "999dp")
print("ok")
