# -*- coding: utf-8 -*-
import os
OUT = r"C:/Users/Administrator/WorkBuddy/2026-10-28-15-17-43/ziyongbaodian/app/src/main/res/drawable"

def solid(name, color, radius, stroke=None, sw="1dp"):
    s = f'''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="{color}" />
    <corners android:radius="{radius}" />
'''
    if stroke:
        s += f'    <stroke android:width="{sw}" android:color="{stroke}" />\n'
    s += '</shape>\n'
    open(os.path.join(OUT, name + ".xml"), "w", encoding="utf-8").write(s)

solid("bg_float_card_light", "#F7FFFFFF", "18dp", stroke="#1A111827", sw="0.8dp")
solid("bg_float_btn_light", "#14111827", "999dp")
print("ok")
