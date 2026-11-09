# -*- coding: utf-8 -*-
import os
BASE = r"C:/Users/Administrator/WorkBuddy/2026-10-28-15-17-43/ziyongbaodian/app/src/main/res"
for d in ["mipmap", "mipmap-anydpi-v26"]:
    os.makedirs(os.path.join(BASE, d), exist_ok=True)

adaptive = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_logo" />
</adaptive-icon>
'''
for name in ["ic_launcher", "ic_launcher_round"]:
    open(os.path.join(BASE, "mipmap-anydpi-v26", name + ".xml"), "w", encoding="utf-8").write(adaptive)

legacy = '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp" android:height="48dp"
    android:viewportWidth="48" android:viewportHeight="48">
    <path android:fillColor="#2F6BFF"
        android:pathData="M10,0h28a10,10 0 0,1 10,10v28a10,10 0 0,1 -10,10H10A10,10 0 0,1 0,38V10A10,10 0 0,1 10,0Z" />
    <path android:fillColor="#FFFFFF"
        android:pathData="M13,14h12c2.2,0 3.6,1.4 3.6,3.6V35H16.6c-2.2,0 -3.6,-1.4 -3.6,-3.6Z" />
    <path android:fillColor="#DCE7FF"
        android:pathData="M28.6,14h4.4c2.2,0 3.6,1.4 3.6,3.6V35h-8Z" />
    <path android:strokeColor="#2F6BFF" android:strokeWidth="1.8" android:strokeLineCap="round"
        android:pathData="M16.6,19h8M16.6,23.5h8M16.6,28h5.4" />
    <path android:fillColor="#2F6BFF"
        android:pathData="M32,29.5a4.4,4.4 0 1,0 0,8.8a4.4,4.4 0 1,0 0,-8.8" />
    <path android:fillColor="#FFFFFF"
        android:pathData="M32,31.3a2.6,2.6 0 1,0 0,5.2a2.6,2.6 0 1,0 0,-5.2" />
    <path android:strokeColor="#2F6BFF" android:strokeWidth="1.8" android:strokeLineCap="round"
        android:pathData="M35.1,37.1L37.6,39.6" />
</vector>
'''
for name in ["ic_launcher", "ic_launcher_round"]:
    open(os.path.join(BASE, "mipmap", name + ".xml"), "w", encoding="utf-8").write(legacy)
print("launcher ok")
