# -*- coding: utf-8 -*-
"""批量生成 shape / selector 资源"""
import os

OUT = r"C:/Users/Administrator/WorkBuddy/2026-10-28-15-17-43/ziyongbaodian/app/src/main/res/drawable"
os.makedirs(OUT, exist_ok=True)

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

solid("bg_search", "#F1F4FA", "14dp")
solid("bg_search_white", "#FFFFFF", "14dp")
solid("bg_pill_brand", "#EAF1FF", "999dp")
solid("bg_pill_green", "#E7F8F0", "999dp")
solid("bg_pill_red", "#FEECEB", "999dp")
solid("bg_pill_gray", "#F1F4FA", "999dp")
solid("bg_input", "#F6F8FC", "12dp", stroke="#E6EBF5")
solid("bg_option", "#FFFFFF", "12dp", stroke="#E6EBF5")
solid("bg_option_ok", "#E7F8F0", "12dp", stroke="#12B76A")
solid("bg_option_err", "#FEECEB", "12dp", stroke="#F04438")
solid("bg_float_card", "#F51B2230", "18dp")
solid("bg_float_head", "#33FFFFFF", "999dp")
solid("bg_float_line", "#33FFFFFF", "1dp")
solid("bg_float_input", "#33FFFFFF", "10dp")
solid("bg_range", "#332F6BFF", "6dp", stroke="#2F6BFF", sw="2dp")
solid("bg_dot", "#2F6BFF", "999dp")
solid("bg_white_16", "#FFFFFF", "16dp")
solid("bg_white_12", "#FFFFFF", "12dp")

# 渐变头部
open(os.path.join(OUT, "bg_header.xml"), "w", encoding="utf-8").write('''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient
        android:angle="315"
        android:startColor="#3D7BFF"
        android:centerColor="#4F8BFF"
        android:endColor="#6FA4FF"
        android:type="linear" />
    <corners
        android:bottomLeftRadius="26dp"
        android:bottomRightRadius="26dp"
        android:topLeftRadius="0dp"
        android:topRightRadius="0dp" />
</shape>
''')

# 底部导航选中背景
open(os.path.join(OUT, "bg_nav_item.xml"), "w", encoding="utf-8").write('''<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_selected="true">
        <shape android:shape="rectangle">
            <solid android:color="#EAF1FF" />
            <corners android:radius="12dp" />
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="#00000000" />
            <corners android:radius="12dp" />
        </shape>
    </item>
</selector>
''')

# 点击态
open(os.path.join(OUT, "bg_ripple.xml"), "w", encoding="utf-8").write('''<?xml version="1.0" encoding="utf-8"?>
<ripple xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="#1A2F6BFF">
    <item android:id="@android:id/mask">
        <shape android:shape="rectangle">
            <solid android:color="#FFFFFF" />
            <corners android:radius="16dp" />
        </shape>
    </item>
</ripple>
''')

# 应用图标背景
open(os.path.join(OUT, "ic_launcher_background.xml"), "w", encoding="utf-8").write('''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient android:angle="315" android:startColor="#2F6BFF" android:endColor="#6FA4FF" />
</shape>
''')
print("shapes ok")
