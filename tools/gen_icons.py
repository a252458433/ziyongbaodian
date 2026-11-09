# -*- coding: utf-8 -*-
"""批量生成线性图标 vector drawable"""
import os, textwrap

OUT = r"C:/Users/Administrator/WorkBuddy/2026-10-28-15-17-43/ziyongbaodian/app/src/main/res/drawable"
os.makedirs(OUT, exist_ok=True)

TMPL = '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:pathData="{path}"
        android:strokeWidth="{w}"
        android:strokeColor="{color}"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"{extra} />
</vector>
'''

ICONS = {
    "ic_search": "M11,4a7,7 0 1,0 0,14a7,7 0 1,0 0,-14M16.2,16.2L21,21",
    "ic_scan": "M4,8.5V5.5A1.5,1.5 0 0,1 5.5,4H8.5M15.5,4H18.5A1.5,1.5 0 0,1 20,5.5V8.5M20,15.5V18.5A1.5,1.5 0 0,1 18.5,20H15.5M8.5,20H5.5A1.5,1.5 0 0,1 4,18.5V15.5M7.5,11.5H16.5M7.5,15H12.5",
    "ic_edit": "M4.5,19.5L5.2,16.2L15.6,5.8A2.1,2.1 0 0,1 18.6,8.8L8.2,19.2L4.5,19.5Z",
    "ic_import": "M12,3.5V14.5M8,10.5L12,14.5L16,10.5M4,16.5V18.5A2,2 0 0,0 6,20.5H18A2,2 0 0,0 20,18.5V16.5",
    "ic_bank": "M5,4.5H15.5A2,2 0 0,1 17.5,6.5V20H7A2,2 0 0,1 5,18V4.5ZM5,17.2H17.5M8.5,8H14.5M8.5,11.5H14.5",
    "ic_practice": "M8.5,7.5H20M8.5,12H20M8.5,16.5H15M3.5,7L5,8.5L7.5,5.5M3.5,15.5L5,17L7.5,14",
    "ic_wrong": "M12,3.8a8.2,8.2 0 1,0 0,16.4a8.2,8.2 0 1,0 0,-16.4M9,9L15,15M15,9L9,15",
    "ic_star": "M12,3.8L14.6,9.2L20.4,10L16.2,14.1L17.2,19.9L12,17.1L6.8,19.9L7.8,14.1L3.6,10L9.4,9.2Z",
    "ic_settings": "M12,8.6a3.4,3.4 0 1,0 0,6.8a3.4,3.4 0 1,0 0,-6.8M12,3.6V6M12,18V20.4M20.4,12H18M6,12H3.6M17.9,6.1L16.2,7.8M7.8,16.2L6.1,17.9M17.9,17.9L16.2,16.2M7.8,7.8L6.1,6.1",
    "ic_chevron": "M9.5,5.5L16,12L9.5,18.5",
    "ic_back": "M14.5,5.5L8,12L14.5,18.5",
    "ic_close": "M6.5,6.5L17.5,17.5M17.5,6.5L6.5,17.5",
    "ic_minimize": "M12,4.5V13M8.2,10L12,13.8L15.8,10M5,19.5H19",
    "ic_expand": "M14,4.5H19.5V10M10,19.5H4.5V14M19.5,4.5L13.5,10.5M4.5,19.5L10.5,13.5",
    "ic_add": "M12,5V19M5,12H19",
    "ic_trash": "M4.5,7H19.5M9.5,7V5.2H14.5V7M6.5,7L7.4,19.5H16.6L17.5,7M10.2,10.5V16M13.8,10.5V16",
    "ic_history": "M12,3.8a8.2,8.2 0 1,0 0,16.4a8.2,8.2 0 1,0 0,-16.4M12,7.8V12L15,14",
    "ic_info": "M12,3.8a8.2,8.2 0 1,0 0,16.4a8.2,8.2 0 1,0 0,-16.4M12,11V16.5M12,7.9V8.6",
    "ic_shield": "M12,3.5L19,6.3V12C19,16.1 16,19.2 12,20.6C8,19.2 5,16.1 5,12V6.3Z M9.2,11.8L11.3,13.9L15,10.2",
    "ic_file": "M6,3.5H13L18.5,9V20.5H6ZM13,3.5V9H18.5M9,13H15M9,16.5H13",
    "ic_home": "M4,11.2L12,4.2L20,11.2M6.2,9.8V19.5H17.8V9.8M10,19.5V14.2H14V19.5",
    "ic_user": "M12,4.2a3.8,3.8 0 1,0 0,7.6a3.8,3.8 0 1,0 0,-7.6M5,20C5,16.4 8.1,13.8 12,13.8C15.9,13.8 19,16.4 19,20",
    "ic_refresh": "M19.6,12.6A7.8,7.8 0 1,1 17.4,6.3M19.8,4V9.4H14.4",
    "ic_drag": "M12,3.5V20.5M8.2,7.3L12,3.5L15.8,7.3M8.2,16.7L12,20.5L15.8,16.7",
    "ic_more": "M6,12H6.1M12,12H12.1M18,12H18.1",
    "ic_check": "M5,12.5L9.5,17L19,7",
    "ic_float": "M4.5,5.5H19.5V18.5H4.5ZM12,9H16V13H12Z",
    "ic_clock": "M12,4a8,8 0 1,0 0,16a8,8 0 1,0 0,-16M12,8V12.2L14.8,14",
    "ic_lock": "M6.5,10.5H17.5V20H6.5ZM8.8,10.5V7.8A3.2,3.2 0 0,1 15.2,7.8V10.5",
    "ic_copy": "M8.5,8.5H19V19H8.5ZM5,15.5H4.2V4.2H15.5V5",
    "ic_text": "M5,5.5H19M12,5.5V18.5M9,18.5H15",
}

for name, path in ICONS.items():
    open(os.path.join(OUT, name + ".xml"), "w", encoding="utf-8").write(
        TMPL.format(path=path, w="1.7", color="#5B6675", extra="")
    )

# 实心星（收藏态）
STAR_FILL = '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path
        android:fillColor="#F79009"
        android:pathData="M12,3.4L14.7,9L20.9,9.9L16.4,14.2L17.5,20.3L12,17.4L6.5,20.3L7.6,14.2L3.1,9.9L9.3,9Z" />
</vector>
'''
open(os.path.join(OUT, "ic_star_fill.xml"), "w", encoding="utf-8").write(STAR_FILL)

# 应用图标前景（书本 + 放大镜）
LOGO = '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#FFFFFF"
        android:pathData="M30,32h26c5,0 8,3 8,8v38H38c-5,0 -8,-3 -8,-8z" />
    <path android:fillColor="#DCE7FF"
        android:pathData="M64,32h14c5,0 8,3 8,8v38H64z" />
    <path android:strokeColor="#2F6BFF" android:strokeWidth="4" android:strokeLineCap="round"
        android:pathData="M38,46h18M38,56h18M38,66h12" />
    <path android:fillColor="#2F6BFF"
        android:pathData="M72,60a10,10 0 1,0 0,20a10,10 0 1,0 0,-20" />
    <path android:fillColor="#FFFFFF"
        android:pathData="M72,64a6,6 0 1,0 0,12a6,6 0 1,0 0,-12" />
    <path android:strokeColor="#2F6BFF" android:strokeWidth="4" android:strokeLineCap="round"
        android:pathData="M79,79L86,86" />
</vector>
'''
open(os.path.join(OUT, "ic_logo.xml"), "w", encoding="utf-8").write(LOGO)
print("icons:", len(ICONS) + 2)
