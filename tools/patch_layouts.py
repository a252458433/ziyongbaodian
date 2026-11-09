# -*- coding: utf-8 -*-
"""
柔光漫画 2.0 · 布局批量精修（幂等，字符串精确替换，不改任何 id）。

- 所有二级页面顶部工具栏：白底 → 细描边柔光条 + 标题字号/字距统一
- 「我的」页：统计卡圆角、菜单行图标加柔彩底、底部说明改渐变卡、版本胶囊
- 设置页：卡片圆角与描边统一、分组标题加色条
- 分隔线统一更柔和的 divider_soft
"""
import os

BASE = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))
LAY = os.path.join(BASE, "app", "src", "main", "res", "layout")


def patch(name, pairs, required=True):
    path = os.path.join(LAY, name)
    if not os.path.exists(path):
        if required:
            raise SystemExit("missing layout: " + name)
        return 0
    with open(path, "r", encoding="utf-8") as f:
        s = f.read()
    n = 0
    for old, new in pairs:
        if old in s:
            c = s.count(old)
            s = s.replace(old, new)
            n += c
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(s)
    return n


# ---------- 1. 二级页面工具栏 ----------
# 白色 toolbar → 柔光细描边；标题 17sp/16sp → 18sp + 字距
TOOLBAR_FILES = [
    "activity_settings.xml", "activity_practice.xml", "activity_search.xml",
    "activity_import.xml", "activity_tiku.xml", "activity_about.xml",
    "activity_guide.xml", "activity_question_list.xml",
]
total = 0
for name in TOOLBAR_FILES:
    total += patch(name, [
        ('android:background="@color/white"\n        android:gravity="center_vertical"',
         'android:background="@drawable/bg_toolbar"\n        android:gravity="center_vertical"'),
        ('android:textSize="17sp"\n            android:textStyle="bold"',
         'android:textSize="18sp"\n            android:letterSpacing="0.02"\n            android:textStyle="bold"'),
        ('android:textSize="16sp"\n            android:textStyle="bold"',
         'android:textSize="18sp"\n            android:letterSpacing="0.02"\n            android:textStyle="bold"'),
        ('app:cardCornerRadius="16dp"', 'app:cardCornerRadius="22dp"'),
        ('app:cardCornerRadius="18dp"', 'app:cardCornerRadius="22dp"'),
        ('app:strokeColor="@color/line"', 'app:strokeColor="@color/outline"'),
        ('android:background="@color/line"', 'android:background="@color/divider_soft"'),
    ], required=False)
print("toolbars/cards patched:", total)

# ---------- 2. 「我的」页 ----------
mine = [
    ('app:cardCornerRadius="18dp"', 'app:cardCornerRadius="22dp"'),
    ('app:strokeColor="@color/line"', 'app:strokeColor="@color/outline"'),
    ('android:background="@color/line"', 'android:background="@color/divider_soft"'),
    ('android:textColor="@color/text"\n                        android:textSize="20sp"',
     'android:textColor="?attr/colorPrimary"\n                        android:textSize="22sp"'),
    # 底部说明卡：淡蓝实底 → 主题渐变柔光卡
    ('app:cardBackgroundColor="#F2F6FF"', 'app:cardBackgroundColor="@color/white"'),
    ('android:textColor="#41506B"', 'android:textColor="@color/text_sub"'),
    # 版本号胶囊
    ('android:id="@+id/tv_version"\n            android:layout_width="match_parent"\n            android:layout_height="wrap_content"\n            android:layout_marginTop="20dp"\n            android:layout_marginBottom="20dp"\n            android:gravity="center"\n            android:text="自用宝典 1.0.0"\n            android:textColor="@color/text_hint"\n            android:textSize="11sp"',
     'android:id="@+id/tv_version"\n            android:layout_width="wrap_content"\n            android:layout_height="wrap_content"\n            android:layout_gravity="center_horizontal"\n            android:layout_marginTop="22dp"\n            android:layout_marginBottom="24dp"\n            android:background="@drawable/bg_pill_gray"\n            android:gravity="center"\n            android:paddingStart="16dp"\n            android:paddingTop="8dp"\n            android:paddingEnd="16dp"\n            android:paddingBottom="8dp"\n            android:text="自用宝典 1.0.0"\n            android:textColor="@color/text_sub"\n            android:textSize="11sp"'),
]
print("mine patched:", patch("fragment_mine.xml", mine))

# 菜单行图标：20dp 裸图标 → 38dp 柔彩圆角底 + 内边距（保持视觉尺寸 20dp）
with open(os.path.join(LAY, "fragment_mine.xml"), "r", encoding="utf-8") as f:
    s = f.read()
repl = [
    ('android:src="@drawable/ic_import"', 'android:src="@drawable/ic_import"'),
]
chips = [
    ("ic_import", "bg_icon_blue"),
    ("ic_bank", "bg_icon_purple"),
    ("ic_info", "bg_icon_green"),
    ("ic_settings", "bg_icon_gray"),
    ("ic_shield", "bg_icon_orange"),
]
for icon, chip in chips:
    old = ('<ImageView\n                        android:layout_width="20dp"\n'
           '                        android:layout_height="20dp"\n'
           '                        android:src="@drawable/%s"' % icon)
    new = ('<ImageView\n                        android:layout_width="38dp"\n'
           '                        android:layout_height="38dp"\n'
           '                        android:background="@drawable/%s"\n'
           '                        android:padding="9dp"\n'
           '                        android:src="@drawable/%s"' % (chip, icon))
    s = s.replace(old, new)
# 底部说明卡那枚图标仍为 18dp 裸图标，保持不动
with open(os.path.join(LAY, "fragment_mine.xml"), "w", encoding="utf-8", newline="\n") as f:
    f.write(s)

# ---------- 3. 设置行 & 选项 ----------
print("setting rows patched:", patch("view_setting_row.xml", [
    ('android:paddingStart="14dp"\n    android:paddingTop="13dp"',
     'android:paddingStart="16dp"\n    android:paddingTop="14dp"'),
    ('android:paddingEnd="14dp"\n    android:paddingBottom="13dp"',
     'android:paddingEnd="16dp"\n    android:paddingBottom="14dp"'),
    ('android:textSize="14.5sp"', 'android:textSize="15sp"'),
    ('android:textColor="@color/text_hint"\n        android:textSize="11sp"',
     'android:textColor="@color/text_sub"\n        android:textSize="11.5sp"'),
], required=False))

print("options patched:", patch("item_option.xml", [
    ('android:textColor="?attr/colorPrimary"', 'android:textColor="?attr/colorPrimary"'),
], required=False))

# ---------- 4. 悬浮窗（圆角与统一描边由 gen_ui_kit 的 drawable 生效） ----------
print("done")
