# -*- coding: utf-8 -*-
"""
「柔光漫画 2.0」UI 资源生成器 —— 批量生成 app/src/main/res/drawable 下的
shape / selector / ripple / vector 资源，统一线宽、圆角、描边与配色语言。

用法：python tools/gen_ui_kit.py
可重复执行（幂等覆盖）。只写 res/drawable/*.xml，不改动 res/values 与布局。
"""
import os

BASE = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))
DRAW = os.path.join(BASE, "app", "src", "main", "res", "drawable")
os.makedirs(DRAW, exist_ok=True)

HEAD = '<?xml version="1.0" encoding="utf-8"?>\n'


def put(name, body):
    with open(os.path.join(DRAW, name + ".xml"), "w", encoding="utf-8", newline="\n") as f:
        f.write(HEAD + body.strip() + "\n")


def shape(*, solid=None, gradient=None, corners=0, stroke=None, stroke_w="1dp", shape_type="rectangle", extra=""):
    """生成一个 <shape> 字符串（不含 xml 头，可嵌入 layer-list）"""
    s = ['<shape xmlns:android="http://schemas.android.com/apk/res/android"',
         '    android:shape="%s">' % shape_type]
    if gradient:
        s.append('    <gradient')
        if not gradient.get("type"):
            s.append('        android:angle="%s"' % gradient.get("angle", "315"))
        s.append('        android:startColor="%s"' % gradient["start"])
        if gradient.get("center"):
            s.append('        android:centerColor="%s"' % gradient["center"])
        s.append('        android:endColor="%s"' % gradient["end"])
        if gradient.get("type"):
            s.append('        android:type="%s"' % gradient["type"])
            s.append('        android:gradientRadius="%s"' % gradient.get("radius", "120dp"))
            if gradient.get("cx"):
                s.append('        android:centerX="%s"' % gradient["cx"])
                s.append('        android:centerY="%s"' % gradient["cy"])
            s.append('        />')
        else:
            s.append('        />')
    elif solid is not None:
        s.append('    <solid android:color="%s" />' % solid)
    if corners and shape_type != "oval":
        s.append('    <corners android:radius="%s" />' % corners)
    if stroke:
        s.append('    <stroke android:width="%s" android:color="%s" />' % (stroke_w, stroke))
    if extra:
        s.append(extra)
    s.append('</shape>')
    return "\n".join(s)


def layer(items, comment=""):
    s = ['<layer-list xmlns:android="http://schemas.android.com/apk/res/android">']
    if comment:
        s.append('    <!-- %s -->' % comment)
    for attrs, inner in items:
        s.append('    <item%s>' % (" " + attrs if attrs else ""))
        for line in inner.splitlines():
            s.append('    ' + line)
        s.append('    </item>')
    s.append('</layer-list>')
    return "\n".join(s)


def themed_pill(base_attr, radius="999dp"):
    """主题色实底 + 固定色高光层。
    <solid android:color="?attr/..."> 是本工程 minSdk 23 已验证可用的写法；
    而 <gradient> 内引用 ?attr 在部分低版本设备上解析失败，故高光层只用固定色。
    """
    return layer([
        ('', shape(solid=base_attr, corners=radius)),
        ('', shape(gradient={"start": "#3DFFFFFF", "end": "#00000000", "angle": "270"},
                   corners=radius)),
    ], "主题色胶囊：实底 + 顶部高光（渐变层只用固定色，兼容 minSdk 23）")


# ==========================================================================
# 1. 卡片：柔光白卡（细描边 + 底部柔影），替代原 2dp 纯黑漫画描边
# ==========================================================================
SOFT_CARD = layer([
    ('android:top="3dp"',
     shape(solid="#12233047", corners="22dp")),
    ('android:bottom="3dp"',
     shape(gradient={"start": "#FFFFFFFF", "end": "#FFFBFDFE", "angle": "270"},
           corners="22dp", stroke="@color/line", stroke_w="1dp")),
], "柔光白卡：底部 3dp 柔影 + 1dp 浅描边 + 极淡渐变")

put("bg_manga_card", SOFT_CARD)
put("bg_manga_pill", SOFT_CARD)
put("bg_manga_pill_accent", layer([
    ('android:top="3dp"', shape(solid="#12233047", corners="22dp")),
    ('android:bottom="3dp"', shape(solid="?attr/colorPrimary", corners="22dp")),
    ('android:bottom="3dp"',
     shape(gradient={"start": "#3DFFFFFF", "end": "#00000000", "angle": "270"}, corners="22dp")),
], "强调胶囊：主题色实底 + 高光 + 底部柔影"))

# 通用白卡（旧版 12/16dp 直角感 → 统一 20dp + 细描边 + 柔影）
for n, r in (("bg_white_12", "20dp"), ("bg_white_16", "22dp"),
             ("bg_white_top_round", "22dp"), ("bg_demo", "22dp")):
    put(n, layer([
        ('android:top="2.5dp"', shape(solid="#10233047", corners=r)),
        ('android:bottom="2.5dp"',
         shape(solid="@color/card", corners=r, stroke="@color/line")),
    ], "通用柔光卡"))

put("bg_log", shape(solid="#F7F9FD", corners="18dp", stroke="#E8EDF6"))
put("bg_search", shape(solid="#F3F6FC", corners="18dp", stroke="#E6ECF7"))
put("bg_search_white", shape(solid="@color/card", corners="18dp", stroke="@color/line"))
put("bg_input", shape(solid="#F7F9FD", corners="16dp", stroke="#E3E9F4"))

# 选项 / 判题反馈
put("bg_option", shape(solid="@color/card", corners="18dp", stroke="@color/line"))
put("bg_option_ok", shape(solid="#EAF9F1", corners="18dp", stroke="#57CFA0", stroke_w="1.2dp"))
put("bg_option_err", shape(solid="#FEF0EF", corners="18dp", stroke="#F4948E", stroke_w="1.2dp"))
put("bg_fmt_normal", shape(solid="#00000000", corners="16dp", stroke="#E3E9F4"))
put("bg_fmt_sel", shape(solid="?attr/colorBrandLight", corners="16dp",
                        stroke="?attr/colorPrimary", stroke_w="1.2dp"))
put("bg_range", shape(solid="#1A3B6EF6", corners="12dp",
                      stroke="#3B6EF6", stroke_w="2dp"))

# ==========================================================================
# 2. 胶囊 / 按钮：主题渐变、高光、投影
# ==========================================================================
put("bg_pill_brand_solid", themed_pill("?attr/colorPrimary"))
put("bg_pill_brand", shape(solid="?attr/colorBrandLight", corners="999dp",
                           stroke="@color/line", stroke_w="0.8dp"))
put("bg_pill_gray", shape(solid="#EFF2F8", corners="999dp", stroke="#E4E9F2", stroke_w="0.8dp"))
put("bg_pill_green", shape(solid="#E7F8F0", corners="999dp", stroke="#BCEBD5", stroke_w="0.8dp"))
put("bg_pill_red", shape(solid="#FEECEB", corners="999dp", stroke="#F7C9C6", stroke_w="0.8dp"))
put("bg_pill_white20", shape(solid="#33FFFFFF", corners="999dp"))

put("bg_search_go", layer([
    ('android:top="2.5dp"', shape(solid="#241B2430", corners="999dp")),
    ('android:bottom="2.5dp"', shape(solid="?attr/colorPrimary", corners="999dp")),
    ('android:bottom="2.5dp"',
     shape(gradient={"start": "#3DFFFFFF", "end": "#00000000", "angle": "270"}, corners="999dp")),
], "搜题按钮：主题色实底 + 高光 + 柔影"))

put("bg_ripple", """<ripple xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="?android:attr/colorControlHighlight">
    <item android:id="@android:id/mask">
        <shape android:shape="rectangle">
            <solid android:color="#FFFFFFFF" />
            <corners android:radius="20dp" />
        </shape>
    </item>
</ripple>""")

put("bg_nav_item", """<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_selected="true">
        <shape android:shape="rectangle">
            <solid android:color="?attr/colorBrandLight" />
            <corners android:radius="16dp" />
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="#00000000" />
            <corners android:radius="16dp" />
        </shape>
    </item>
</selector>""")

put("bg_dot", shape(solid="?attr/colorPrimary", corners="999dp", shape_type="oval"))

# 六宫格图标底：固定柔和彩色（低饱和），图标仍染主题色，切换主题保持呼应
CHIPS = {
    "bg_icon_blue":   ("#E4EEFF", "#D3E2FF"),
    "bg_icon_green":  ("#E4F8EE", "#D0F1E1"),
    "bg_icon_purple": ("#EFEAFF", "#E1D8FF"),
    "bg_icon_orange": ("#FFF2DF", "#FFE7C2"),
    "bg_icon_red":    ("#FFEBEA", "#FFDAD8"),
    "bg_icon_gray":   ("#EDF1F7", "#E1E7F0"),
}
for name, (c1, c2) in CHIPS.items():
    put(name, layer([
        ('', shape(gradient={"start": c1, "end": c2, "angle": "315"},
                   corners="18dp", stroke="#1AFFFFFF", stroke_w="1dp")),
    ], "柔彩渐变图标底"))

# 应用伪装图标背景（adaptive-icon 的 background 层）
APP_BG = {
    "bg_app_book":  ("#3366EE", "#7FA6FF"),
    "bg_app_calc":  ("#3B4A63", "#7486A3"),
    "bg_app_card":  ("#E8539B", "#FF96C6"),
    "bg_app_list":  ("#12A9C6", "#69DAEB"),
    "bg_app_note":  ("#F59E22", "#FFCC78"),
    "bg_app_torch": ("#303C4B", "#5A6C80"),
}
for name, (c1, c2) in APP_BG.items():
    put(name, shape(gradient={"start": c1, "end": c2, "angle": "315"}))

# ==========================================================================
# 3. 头部渐变：6 套主题，柔光晕 + 网点纸 + 底部大圆角
# ==========================================================================
HEADERS = {
    # name: (start, center, end, glow, dot_alpha, bottom_radius)
    "bg_header_manga":  ("#FF86B6", "#F9538F", "#B45CE8", "#66FFFFFF"),
    "bg_header_blue":   ("#4E8BFF", "#2F6BFF", "#6C6BEF", "#55FFFFFF"),
    "bg_header_orange": ("#FFB347", "#F79009", "#FF7B54", "#5CFFFFFF"),
    "bg_header_gray":   ("#FFFFFF", "#EDF0F6", "#DCE3EE", "#7AFFFFFF"),
    "bg_header_mint":   ("#4AE0AE", "#10B981", "#0E94A5", "#5AFFFFFF"),
    "bg_header_aurora": ("#A98BFF", "#7A5AF8", "#4F46E5", "#59FFFFFF"),
}
for name, (c1, c2, c3, glow) in HEADERS.items():
    rad = "30dp"
    body = """<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- 柔光漫画头部：三段渐变 + 右上光晕 + 网点纸 + 底部大圆角 -->
    <item>
        <shape android:shape="rectangle">
            <gradient
                android:angle="315"
                android:startColor="%s"
                android:centerColor="%s"
                android:endColor="%s"
                android:type="linear" />
            <corners
                android:bottomLeftRadius="%s"
                android:bottomRightRadius="%s"
                android:topLeftRadius="0dp"
                android:topRightRadius="0dp" />
        </shape>
    </item>
    <item android:top="0dp" android:left="140dp" android:bottom="40dp" android:right="0dp">
        <shape android:shape="oval">
            <gradient
                android:startColor="%s"
                android:endColor="#00FFFFFF"
                android:type="radial"
                android:gradientRadius="220dp"
                android:centerX="0.5"
                android:centerY="0.5" />
        </shape>
    </item>
    <item>
        <bitmap
            android:src="@drawable/halftone_tile"
            android:tileMode="repeat" />
    </item>
</layer-list>""" % (c1, c2, c3, rad, rad, glow)
    put(name, body)

# 通用头部（默认品牌蓝；页面已统一改用 ?attr/headerBackground 指向各主题专有渐变）
put("bg_header", """<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient
        android:angle="315"
        android:startColor="#4E8BFF"
        android:centerColor="#3B6EF6"
        android:endColor="#6C6BEF"
        android:type="linear" />
    <corners
        android:bottomLeftRadius="30dp"
        android:bottomRightRadius="30dp"
        android:topLeftRadius="0dp"
        android:topRightRadius="0dp" />
</shape>""")

# ==========================================================================
# 4. 悬浮窗资源（保持深色/浅色两种语义，只统一圆角与柔和度）
# ==========================================================================
put("bg_float_card", shape(solid="#F21B2230", corners="22dp", stroke="#33FFFFFF", stroke_w="0.8dp"))
put("bg_float_card_light", shape(solid="#FFFFFFFF", corners="22dp", stroke="#1A111827", stroke_w="0.8dp"))
put("bg_float_input", shape(solid="#33FFFFFF", corners="14dp"))
put("bg_float_btn", shape(solid="#26FFFFFF", corners="999dp"))
put("bg_float_btn_light", shape(solid="#14111827", corners="999dp"))
put("bg_float_head", shape(solid="#33FFFFFF", corners="999dp"))
put("bg_float_line", shape(solid="#33FFFFFF", corners="1dp"))
put("bg_dark_ball", shape(solid="#E61B2230", corners="999dp"))
put("bg_float_ball_placeholder", shape(solid="#00000000", corners="999dp"))

# ==========================================================================
# 5. 新增装饰资源（供新版布局使用）
# ==========================================================================
put("bg_glow_orb", """<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <gradient
        android:startColor="#66FFFFFF"
        android:endColor="#00FFFFFF"
        android:type="radial"
        android:gradientRadius="90dp"
        android:centerX="0.5"
        android:centerY="0.5" />
</shape>""")
put("bg_header_badge", shape(solid="#33FFFFFF", corners="999dp",
                             stroke="#2E000000", stroke_w="0.8dp"))
put("bg_stat_chip", shape(solid="?attr/colorBrandLight", corners="16dp"))
put("bg_section_bar", themed_pill("?attr/colorPrimary"))
put("bg_toolbar", """<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- 顶栏：白底 + 底部 1dp 柔线，替代生硬的纯白直切 -->
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/card" />
        </shape>
    </item>
    <item android:gravity="bottom" android:height="1dp">
        <shape android:shape="rectangle">
            <solid android:color="@color/divider_soft" />
        </shape>
    </item>
</layer-list>""")
put("bg_divider", shape(solid="@color/line", corners="999dp"))
put("bg_card_gradient", layer([
    ('', shape(solid="?attr/colorBrandLight", corners="22dp", stroke="@color/line")),
    ('', shape(gradient={"start": "#4DFFFFFF", "end": "#00000000", "angle": "270"}, corners="22dp")),
], "数据卡：主题浅底 + 顶部柔光"))
put("bg_avatar_ring", shape(solid="#00000000", corners="999dp",
                            stroke="#80FFFFFF", stroke_w="2dp", shape_type="oval"))

# ==========================================================================
# 6. 全套矢量图标：24x24 网格、1.8dp 圆头圆角描边、统一墨色
#    布局使用 app:tint 时颜色被覆盖，这里保持中性墨色以便单独引用
# ==========================================================================
INK = "#FF344054"

# (name, [ (pathData, extra_attrs) ... ])
ICONS = {
    "ic_add":       [("M12,5V19 M5,12H19", "")],
    "ic_back":      [("M14.5,5L8,12L14.5,19", ""), ("M8,12H20", "")],
    "ic_chevron":   [("M9.5,5L15.5,12L9.5,19", "")],
    "ic_down":      [("M6,9.5L12,15.5L18,9.5", "")],
    "ic_close":     [("M6,6L18,18 M18,6L6,18", "")],
    "ic_check":     [("M5,12.5L9.8,17.3L19,7", "")],
    "ic_search":    [("M10.8,10.8m-6.3,0a6.3,6.3 0 1,0 12.6,0a6.3,6.3 0 1,0 -12.6,0", ""),
                     ("M15.5,15.5L20.2,20.2", "")],
    "ic_settings":  [("M4,7H20 M4,12H20 M4,17H20", ""),
                     ("M9,7m-2.1,0a2.1,2.1 0 1,0 4.2,0a2.1,2.1 0 1,0 -4.2,0", 'android:fillColor="#00000000"'),
                     ("M15,12m-2.1,0a2.1,2.1 0 1,0 4.2,0a2.1,2.1 0 1,0 -4.2,0", 'android:fillColor="#00000000"'),
                     ("M9,17m-2.1,0a2.1,2.1 0 1,0 4.2,0a2.1,2.1 0 1,0 -4.2,0", 'android:fillColor="#00000000"')],
    "ic_home":      [("M3.8,10.6L12,4L20.2,10.6V20A1,1 0 0,1 19.2,21H4.8A1,1 0 0,1 3.8,20Z", ""),
                     ("M9.4,21V14.2H14.6V21", "")],
    "ic_user":      [("M12,8m-3.6,0a3.6,3.6 0 1,0 7.2,0a3.6,3.6 0 1,0 -7.2,0", ""),
                     ("M4.8,20C4.8,16.7 7.8,15.2 12,15.2C16.2,15.2 19.2,16.7 19.2,20", "")],
    "ic_bank":      [("M12,4C7.6,4 4,5.3 4,7S7.6,10 12,10S20,8.7 20,7S16.4,4 12,4Z", ""),
                     ("M4,7V12C4,13.7 7.6,15 12,15S20,13.7 20,12V7", ""),
                     ("M4,12V17C4,18.7 7.6,20 12,20S20,18.7 20,17V12", "")],
    "ic_practice":  [("M9.5,6.5H20 M9.5,12H20 M9.5,17.5H20", ""),
                     ("M3.8,6.4L5,7.6L7.4,5.2", ""),
                     ("M4.4,12h0.1 M4.4,17.5h0.1", 'android:strokeWidth="2.4"')],
    "ic_text":      [("M5.5,5.5H18.5 M12,5.5V19 M9.2,19H14.8", "")],
    "ic_refresh":   [("M19.6,10A7.9,7.9 0 0,0 6.1,7.2L4,9.3", ""),
                     ("M4,9.3V5.1 M4,9.3H8.2", ""),
                     ("M4.4,14A7.9,7.9 0 0,0 17.9,16.8L20,14.7", ""),
                     ("M20,14.7V18.9 M20,14.7H15.8", "")],
    "ic_wrong":     [("M12,12m-8.3,0a8.3,8.3 0 1,0 16.6,0a8.3,8.3 0 1,0 -16.6,0", ""),
                     ("M9.2,9.2L14.8,14.8 M14.8,9.2L9.2,14.8", "")],
    "ic_close_circle": [("M12,12m-8.3,0a8.3,8.3 0 1,0 16.6,0a8.3,8.3 0 1,0 -16.6,0", ""),
                     ("M9.2,9.2L14.8,14.8 M14.8,9.2L9.2,14.8", "")],
    "ic_star":      [("M12,3.6L14.7,9.2L20.8,10L16.3,14.2L17.4,20.2L12,17.3L6.6,20.2L7.7,14.2L3.2,10L9.3,9.2Z", "")],
    "ic_import":    [("M12,3.8V14.2 M8.4,10.6L12,14.2L15.6,10.6", ""),
                     ("M4.5,16.5V19A1.5,1.5 0 0,0 6,20.5H18A1.5,1.5 0 0,0 19.5,19V16.5", "")],
    "ic_export":    [("M12,15V4.6 M8.4,8.2L12,4.6L15.6,8.2", ""),
                     ("M4.5,16.5V19A1.5,1.5 0 0,0 6,20.5H18A1.5,1.5 0 0,0 19.5,19V16.5", "")],
    "ic_info":      [("M12,12m-8.4,0a8.4,8.4 0 1,0 16.8,0a8.4,8.4 0 1,0 -16.8,0", ""),
                     ("M12,11V16.4", ""),
                     ("M12,7.9h0.01", 'android:strokeWidth="2.6"')],
    "ic_shield":    [("M12,3.2L19.4,6V11.5C19.4,15.7 16.4,19.1 12,20.8C7.6,19.1 4.6,15.7 4.6,11.5V6Z", ""),
                     ("M9.1,12L11.2,14.1L15.1,10.2", "")],
    "ic_history":   [("M12,12m-8.4,0a8.4,8.4 0 1,0 16.8,0a8.4,8.4 0 1,0 -16.8,0", ""),
                     ("M12,7.2V12L15.2,14", "")],
    "ic_clock":     [("M12,12m-8.4,0a8.4,8.4 0 1,0 16.8,0a8.4,8.4 0 1,0 -16.8,0", ""),
                     ("M12,7.4V12.3L15.6,14.2", "")],
    "ic_copy":      [("M9,9H18A2,2 0 0,1 20,11V18A2,2 0 0,1 18,20H9A2,2 0 0,1 7,18V11A2,2 0 0,1 9,9Z", ""),
                     ("M16,7V6A2,2 0 0,0 14,4H6A2,2 0 0,0 4,6V14A2,2 0 0,0 6,16H7", "")],
    "ic_edit":      [("M5,19H19", ""),
                     ("M6.4,15.4L15.4,6.4A2.1,2.1 0 0,1 18.4,9.4L9.4,18.4L5.4,19Z", "")],
    "ic_trash":     [("M5,7H19 M9,7V5A1,1 0 0,1 10,4H14A1,1 0 0,1 15,5V7", ""),
                     ("M7,7L8,20H16L17,7", ""),
                     ("M10.4,10.6V16.4 M13.6,10.6V16.4", "")],
    "ic_file":      [("M7,3.5H13L18,8.5V20A1,1 0 0,1 17,21H7A1,1 0 0,1 6,20V4.5A1,1 0 0,1 7,3.5Z", ""),
                     ("M13,3.5V8.5H18 M9,13H15 M9,16.5H13", "")],
    "ic_float":     [("M5.5,5H18.5A1.5,1.5 0 0,1 20,6.5V17.5A1.5,1.5 0 0,1 18.5,19H5.5A1.5,1.5 0 0,1 4,17.5V6.5A1.5,1.5 0 0,1 5.5,5Z", ""),
                     ("M4,9.5H20", ""),
                     ("M6.8,7.2h0.01 M9.3,7.2h0.01", 'android:strokeWidth="2.4"')],
    "ic_frame":     [("M4,9V5.5A1.5,1.5 0 0,1 5.5,4H9 M15,4H18.5A1.5,1.5 0 0,1 20,5.5V9", ""),
                     ("M20,15V18.5A1.5,1.5 0 0,1 18.5,20H15 M9,20H5.5A1.5,1.5 0 0,1 4,18.5V15", ""),
                     ("M9,12H15", "")],
    "ic_scan":      [("M4,9V5.5A1.5,1.5 0 0,1 5.5,4H9 M15,4H18.5A1.5,1.5 0 0,1 20,5.5V9", ""),
                     ("M20,15V18.5A1.5,1.5 0 0,1 18.5,20H15 M9,20H5.5A1.5,1.5 0 0,1 4,18.5V15", ""),
                     ("M4.5,12H19.5", 'android:strokeWidth="2"')],
    "ic_expand":    [("M14,4H20V10 M20,4L13,11", ""),
                     ("M10,20H4V14 M4,20L11,13", "")],
    "ic_minimize":  [("M10,4V10H4 M10,10L3,3", ""),
                     ("M14,20V14H20 M14,14L21,21", "")],
    "ic_jump":      [("M4.5,5.5L10.5,12L4.5,18.5 M12,5.5L18,12L12,18.5", "")],
    "ic_lock":      [("M6.6,10.5V8A5.4,5.4 0 0,1 17.4,8V10.5", ""),
                     ("M5,10.5H19A1,1 0 0,1 20,11.5V19.5A1,1 0 0,1 19,20.5H5A1,1 0 0,1 4,19.5V11.5A1,1 0 0,1 5,10.5Z", ""),
                     ("M12,15.3h0.01", 'android:strokeWidth="2.6"')],
    "ic_more":      [("M5,12m-1.5,0a1.5,1.5 0 1,0 3,0a1.5,1.5 0 1,0 -3,0", 'android:fillColor="%s" android:strokeWidth="0"' % INK),
                     ("M12,12m-1.5,0a1.5,1.5 0 1,0 3,0a1.5,1.5 0 1,0 -3,0", 'android:fillColor="%s" android:strokeWidth="0"' % INK),
                     ("M19,12m-1.5,0a1.5,1.5 0 1,0 3,0a1.5,1.5 0 1,0 -3,0", 'android:fillColor="%s" android:strokeWidth="0"' % INK)],
    "ic_drag":      [("M9,6m-1.4,0a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0 M15,6m-1.4,0a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0", 'android:fillColor="%s" android:strokeWidth="0"' % INK),
                     ("M9,12m-1.4,0a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0 M15,12m-1.4,0a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0", 'android:fillColor="%s" android:strokeWidth="0"' % INK),
                     ("M9,18m-1.4,0a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0 M15,18m-1.4,0a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0", 'android:fillColor="%s" android:strokeWidth="0"' % INK)],
    "ic_history_dot": [("M12,12m-8.4,0a8.4,8.4 0 1,0 16.8,0a8.4,8.4 0 1,0 -16.8,0", "")],
    "ic_sparkle":   [("M12,2.4C12.8,7.6 15,9.8 20.6,12C15,14.2 12.8,16.4 12,21.6C11.2,16.4 9,14.2 3.4,12C9,9.8 11.2,7.6 12,2.4Z",
                      'android:fillColor="%s" android:strokeWidth="0"' % INK)],
    "ic_star_fill": [("M12,3.4L14.8,9.1L21,10L16.4,14.4L17.5,20.6L12,17.6L6.5,20.6L7.6,14.4L3,10L9.2,9.1Z",
                      'android:fillColor="%s" android:strokeWidth="0"' % INK)],
    "ic_speedlines": [("M2,3.5H15 M7,7.5H22 M2,11.5H13 M9,15.5H22 M2,19.5H17 M14,22H22", "")],
    "ic_flower":    [("M12,6.6m-3.7,0a3.7,3.7 0 1,0 7.4,0a3.7,3.7 0 1,0 -7.4,0 M17.5,10.6m-3.7,0a3.7,3.7 0 1,0 7.4,0a3.7,3.7 0 1,0 -7.4,0",
                      'android:fillColor="%s" android:strokeWidth="0"' % INK),
                     ("M15.5,16.8m-3.7,0a3.7,3.7 0 1,0 7.4,0a3.7,3.7 0 1,0 -7.4,0 M8.5,16.8m-3.7,0a3.7,3.7 0 1,0 7.4,0a3.7,3.7 0 1,0 -7.4,0 M6.5,10.6m-3.7,0a3.7,3.7 0 1,0 7.4,0a3.7,3.7 0 1,0 -7.4,0",
                      'android:fillColor="%s" android:strokeWidth="0"' % INK),
                     ("M12,12m-3.2,0a3.2,3.2 0 1,0 6.4,0a3.2,3.2 0 1,0 -6.4,0", 'android:fillColor="#FFFFFF" android:strokeWidth="0"')],
    "ic_ball_circle": [("M12,12m-8.6,0a8.6,8.6 0 1,0 17.2,0a8.6,8.6 0 1,0 -17.2,0",
                        'android:fillColor="#FFFFFF" android:strokeWidth="0"')],
    "ic_ball_heart": [("M12,20.6C12,20.6 3.4,15 3.4,9.4A4.6,4.6 0 0,1 12,7.1A4.6,4.6 0 0,1 20.6,9.4C20.6,15 12,20.6 12,20.6Z",
                       'android:fillColor="#FFFFFF" android:strokeWidth="0"')],
    "ic_ball_star":  [("M12,3.2L14.7,9L20.9,9.9L16.4,14.2L17.5,20.4L12,17.5L6.5,20.4L7.6,14.2L3.1,9.9L9.3,9Z",
                       'android:fillColor="#FFFFFF" android:strokeWidth="0"')],
}

import re as _re

for name, paths in ICONS.items():
    body = ['<vector xmlns:android="http://schemas.android.com/apk/res/android"',
            '    android:width="24dp"',
            '    android:height="24dp"',
            '    android:viewportWidth="24"',
            '    android:viewportHeight="24">']
    for pd, extra in paths:
        attrs = {
            "android:pathData": pd,
            "android:strokeColor": INK,
            "android:strokeWidth": "1.8",
            "android:strokeLineCap": "round",
            "android:strokeLineJoin": "round",
            "android:fillColor": "#00000000",
        }
        # extra 中出现的属性覆盖默认值，避免同一元素重复属性（aapt 会报错）
        if extra:
            for k, v in _re.findall(r'(android:[A-Za-z]+)="([^"]*)"', extra):
                attrs[k] = v
        body.append('    <path')
        for k, v in attrs.items():
            body.append('        %s="%s"' % (k, v))
        body.append('        />')
    body.append('</vector>')
    put(name, "\n".join(body))

# ==========================================================================
# 7. 品牌 logo / adaptive 前景：108dp 视口，主体落在中央安全区
# ==========================================================================
# ic_logo 供 adaptive-icon 前景使用：主体收进中心 66dp 安全区（21..87）
put("ic_logo", """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <!-- 打开的书 + 书页线条 + 星芒（adaptive 安全区版） -->
    <path android:fillColor="#FFFFFFFF"
        android:pathData="M54,38C47,33 38,32 31,34C29,35 28,37 28,40V72C28,74 29,76 31,77C38,79 47,78 54,73Z" />
    <path android:fillColor="#E3ECFF"
        android:pathData="M54,38C61,33 70,32 77,34C79,35 80,37 80,40V72C80,74 79,76 77,77C70,79 61,78 54,73Z" />
    <path android:strokeColor="#3B6EF6" android:strokeWidth="2.6" android:strokeLineCap="round"
        android:pathData="M34,46H47 M34,55H47 M34,64H43 M61,46H74 M61,55H74 M61,64H71" />
    <path android:fillColor="#FFFFFF"
        android:pathData="M54,26L55.8,31.2L61,33L55.8,34.8L54,40L52.2,34.8L47,33L52.2,31.2Z" />
</vector>""")

# ic_logo_mark 供「我的 / 关于」页内大图标使用：铺满画布
put("ic_logo_mark", """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#FFFFFFFF"
        android:pathData="M54,29C45,23 33,22 24,25C21,26 19,29 19,32V76C19,79 21,82 24,83C33,86 45,85 54,79Z" />
    <path android:fillColor="#E3ECFF"
        android:pathData="M54,29C63,23 75,22 84,25C87,26 89,29 89,32V76C89,79 87,82 84,83C75,86 63,85 54,79Z" />
    <path android:strokeColor="#3B6EF6" android:strokeWidth="3.4" android:strokeLineCap="round"
        android:pathData="M28,40H44 M28,50H44 M28,60H39 M64,40H80 M64,50H80 M64,60H75" />
    <path android:fillColor="#FFFFFF"
        android:pathData="M54,18L56.5,24.5L63,27L56.5,29.5L54,36L51.5,29.5L45,27L51.5,24.5Z" />
    <path android:fillColor="#FFFFFF"
        android:pathData="M84,67L85.6,71.4L90,73L85.6,74.6L84,79L82.4,74.6L78,73L82.4,71.4Z" />
</vector>""")

put("ic_launcher_background", shape(gradient={"start": "#4E8BFF", "end": "#6C6BEF", "angle": "315"}))

# adaptive-icon foreground：六个「伪装应用」符号，白色填充，108 视口居中
APP_FG = {
    "ic_app_book":  'M54,32C46,27 36,26 28,29C25,30 23,33 23,36V76C23,79 25,82 28,83C36,86 46,85 54,80Z M54,32C62,27 72,26 80,29C83,30 85,33 85,36V76C85,79 83,82 80,83C72,86 62,85 54,80Z',
    "ic_app_calc":  'M33,25H75A6,6 0 0,1 81,31V81A6,6 0 0,1 75,87H33A6,6 0 0,1 27,81V31A6,6 0 0,1 33,25Z M36,34H72V47H36Z M36,55H46V65H36Z M53,55H63V65H53Z M70,55H72V65H70Z M36,72H46V80H36Z M53,72H63V80H53Z M70,72H72V80H70Z',
    "ic_app_card":  'M24,32H84A6,6 0 0,1 90,38V76A6,6 0 0,1 84,82H24A6,6 0 0,1 18,76V38A6,6 0 0,1 24,32Z M18,45H90 M30,62H50 M30,70H42',
    "ic_app_list":  'M36,26H78A5,5 0 0,1 83,31V83A5,5 0 0,1 78,88H36A5,5 0 0,1 31,83V31A5,5 0 0,1 36,26Z M42,42L48,48L60,36 M42,62L48,68L60,56 M42,80H70',
    "ic_app_note":  'M30,24H68L84,40V82A5,5 0 0,1 79,87H30A5,5 0 0,1 25,82V29A5,5 0 0,1 30,24Z M68,24V40H84 M38,52H70 M38,62H70 M38,72H58',
    "ic_app_torch": 'M40,26H68A5,5 0 0,1 73,31V45L81,53V60H27V53L35,45V31A5,5 0 0,1 40,26Z M46,66H62 M48,76H60',
}
for name, pd in APP_FG.items():
    put(name, """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#FFFFFFFF" android:pathData="%s" />
    <path android:fillColor="#FFFFFF"
        android:pathData="M84,20L86,25L91,27L86,29L84,34L82,29L77,27L82,25Z" />
</vector>""" % pd)

print("gen_ui_kit: wrote %d drawable xml files" % len(os.listdir(DRAW)))
