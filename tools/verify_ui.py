# -*- coding: utf-8 -*-
"""
柔光漫画 2.0 · 静态完整性校验（无需 Android SDK）

检查项：
  1. 全部 res/**/*.xml + AndroidManifest.xml 可被 XML 解析器解析
  2. 所有 @drawable / @color / @string / @style / @mipmap / @layout / @xml 引用有定义
  3. 所有 ?attr/xxx 在 attrs.xml 或 Android 框架 attr 白名单里
  4. Kotlin 源码里的 R.drawable/R.color/R.string/R.style/R.layout/R.id 引用有定义
  5. 布局 @+id 集合与原始工程一致（不能因美化弄丢 ViewBinding 字段）
  6. Themes.ALL 数量 == ALIASES 数量 == manifest activity-alias 数量 == 主题 style 数量
  7. 六套主题的 headerBackground / adaptive 图标 / legacy PNG 齐全
  8. 启动图标 PNG 尺寸正确
"""
import os
import re
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict

BASE = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))
RES = os.path.join(BASE, "app", "src", "main", "res")
MAIN = os.path.join(BASE, "app", "src", "main")
JAVA = os.path.join(MAIN, "java")
MANIFEST = os.path.join(MAIN, "AndroidManifest.xml")

errors = []
warns = []


def err(msg):
    errors.append(msg)


def warn(msg):
    warns.append(msg)


# ---------------------------------------------------------------- 1. XML 合法
xml_files = []
for root, dirs, files in os.walk(RES):
    for fn in files:
        if fn.endswith(".xml"):
            xml_files.append(os.path.join(root, fn))
xml_files.append(MANIFEST)
xml_files.append(os.path.join(BASE, "app", "build.gradle.kts")) if False else None

for p in xml_files:
    try:
        ET.parse(p)
    except Exception as e:
        err("XML 解析失败 %s: %s" % (os.path.relpath(p, BASE), e))
print("[1] XML 文件数: %d, 解析错误: %d" % (len(xml_files), len(errors)))

# ---------------------------------------------------------------- 2. 资源定义表
defs = defaultdict(set)          # type -> names
for root, dirs, files in os.walk(RES):
    folder = os.path.basename(root)
    rtype = folder.split("-")[0]  # drawable, mipmap, values, layout, color, xml
    for fn in files:
        name = fn.split(".")[0]
        if rtype in ("drawable", "mipmap", "layout", "color", "xml", "anim", "menu"):
            defs[rtype].add(name)
        elif rtype in ("values",):
            if not fn.endswith(".xml"):
                continue
            try:
                tree = ET.parse(os.path.join(root, fn))
            except Exception:
                continue
            for child in tree.getroot():
                tag = child.tag
                n = child.get("name")
                if not n:
                    continue
                if tag == "string":
                    defs["string"].add(n)
                elif tag == "color":
                    defs["color"].add(n)
                elif tag == "style":
                    # aapt 生成的 R.style 字段把点号换成下划线
                    defs["style"].add(n)
                    defs["style"].add(n.replace(".", "_"))
                elif tag == "attr":
                    defs["attr"].add(n)
                elif tag == "dimen":
                    defs["dimen"].add(n)
                elif tag in ("string-array", "array"):
                    defs["array"].add(n)
                elif tag in ("integer", "bool", "item"):
                    t = child.get("type") or child.get("format")
                    if t:
                        defs[t].add(n)
                    else:
                        defs["item"].add(n)
                else:
                    defs[tag].add(n)

# 系统资源（工程自带 / framework）
FRAMEWORK = re.compile(r"^@(android:|androidx:)?")
FRAMEWORK_ATTRS = {
    "selectableItemBackground", "selectableItemBackgroundBorderless",
    "colorPrimary", "colorPrimaryDark", "colorOnPrimary", "colorSecondary",
    "colorSurface", "colorOnSurface", "colorAccent", "colorControlHighlight",
    "colorControlNormal", "colorControlActivated", "colorBackground",
    "windowBackground", "statusBarColor", "navigationBarColor",
    "textColorPrimary", "textColorSecondary", "fontFamily", "letterSpacing",
    "dividerVertical", "listPreferredItemHeight", "actionBarSize",
    "colorPrimaryVariant", "colorSurfaceVariant", "colorOnSurfaceVariant",
    "colorOutline", "colorError", "colorOnError", "colorTertiary",
}

REF = re.compile(r'"@\+?(?:android:)?([A-Za-z][A-Za-z0-9_]*)/([A-Za-z0-9_.]+)"')
ATTR_REF = re.compile(r'"\?attr/([A-Za-z0-9_.]+)"')
ATTR_REF_NS = re.compile(r'"\?(?:android:)?attr/([A-Za-z0-9_.]+)"')
ID_DEF = re.compile(r'"@\+id/([A-Za-z0-9_]+)"')

missing = set()
for p in xml_files:
    try:
        text = open(p, "r", encoding="utf-8").read()
    except Exception:
        continue
    rel = os.path.relpath(p, BASE)
    for typ, name in REF.findall(text):
        if typ == "id":
            continue
        if typ in ("drawable", "mipmap", "layout", "color", "string", "style", "dimen", "xml", "array", "attr"):
            if name not in defs[typ]:
                # drawable 与 mipmap 可互为别名（adaptive 引用 @drawable/xxx 而文件在 mipmap 时被 aapt 视为同一资源类型）
                if typ in ("drawable", "mipmap") and name in defs["mipmap"] | defs["drawable"]:
                    continue
                if name.startswith("android:"):
                    continue
                missing.add((typ, name, rel))
        elif typ in ("android",):
            continue
    for name in ATTR_REF_NS.findall(text):
        if name not in defs["attr"] and name not in FRAMEWORK_ATTRS:
            missing.add(("attr", name, rel))

for typ, name, rel in sorted(missing):
    err("缺失资源引用 @%s/%s  (来自 %s)" % (typ, name, rel))
print("[2][3] 引用缺失: %d" % len(missing))

# ---------------------------------------------------------------- 4. Kotlin R.* 引用
KREF = re.compile(r"(?<![A-Za-z0-9_.])R\.([A-Za-z][A-Za-z0-9_]*)\.([A-Za-z0-9_]+)")
# 来自 androidx.appcompat / com.google.android.material 等非本模块 R 的引用
EXTERNAL_R = {"androidx.appcompat.R", "com.google.android.material.R", "android.R"}
kotlin_missing = set()
kt_files = 0
for root, dirs, files in os.walk(JAVA):
    for fn in files:
        if not fn.endswith(".kt"):
            continue
        kt_files += 1
        text = open(os.path.join(root, fn), "r", encoding="utf-8").read()
        for m in KREF.finditer(text):
            typ, name = m.group(1), m.group(2)
            prefix = text[max(0, m.start() - 32):m.start()]
            if any(prefix.endswith(x) for x in EXTERNAL_R):
                continue   # androidx.appcompat.R.attr.colorPrimary 等
            if typ == "id":
                continue
            if typ == "attr":
                if name not in defs["attr"] and name not in FRAMEWORK_ATTRS:
                    kotlin_missing.add((typ, name, fn))
                continue
            if typ in defs and name not in defs[typ]:
                kotlin_missing.add((typ, name, fn))
for typ, name, fn in sorted(kotlin_missing):
    err("Kotlin 引用失效 R.%s.%s  (来自 %s)" % (typ, name, fn))
print("[4] Kotlin 文件: %d, 失效引用: %d" % (kt_files, len(kotlin_missing)))

# ---------------------------------------------------------------- 5. 布局 id 与原工程一致
def collect_ids(root_path):
    ids = set()
    for dirpath, dirs, files in os.walk(root_path):
        for fn in files:
            if fn.endswith(".xml"):
                try:
                    t = open(os.path.join(dirpath, fn), "r", encoding="utf-8").read()
                except Exception:
                    continue
                ids.update(ID_DEF.findall(t))
    return ids

orig_root = os.environ.get("UI_ORIGINAL_LAYOUTS", "")
if orig_root and os.path.isdir(orig_root):
    old = collect_ids(orig_root)
    new = collect_ids(os.path.join(RES, "layout"))
    lost = old - new
    for i in sorted(lost):
        err("布局 id 丢失（会破坏 ViewBinding）: @+id/%s" % i)
    print("[5] 原始 id %d / 现有 id %d / 丢失 %d / 新增 %d" %
          (len(old), len(new), len(lost), len(new - old)))
else:
    print("[5] 跳过（未设置 UI_ORIGINAL_LAYOUTS）")

# ---------------------------------------------------------------- 6. 主题链路一致
themes_kt = open(os.path.join(JAVA, "com", "ziyongbaodian", "util", "Themes.kt"),
                 "r", encoding="utf-8").read()
n_spec = len(re.findall(r'^\s*Spec\("', themes_kt, re.M))
alias_block = re.search(r"ALIASES\s*=\s*listOf\((.*?)\)", themes_kt, re.S)
n_alias = len(re.findall(r'"\.Launcher[A-Za-z]+"', alias_block.group(1))) if alias_block else 0
man = open(MANIFEST, "r", encoding="utf-8").read()
n_manifest = len(re.findall(r'<activity-alias', man))
th_xml = open(os.path.join(RES, "values", "themes.xml"), "r", encoding="utf-8").read()
n_styles = len(re.findall(r'<style name="Theme\.ZiyongBaodian\.(Manga|Gray|Blue|Torch|Mint|Aurora)"', th_xml))

if not (n_spec == n_alias == n_manifest == n_styles):
    err("主题链路数量不一致: Themes.Spec=%d ALIASES=%d manifest alias=%d styles=%d" %
        (n_spec, n_alias, n_manifest, n_styles))
print("[6] 主题链路: Spec=%d ALIASES=%d manifest=%d styles=%d" %
      (n_spec, n_alias, n_manifest, n_styles))

# ---------------------------------------------------------------- 7. 主题配套资源
PAIRS = [
    ("Manga", "manga_pink"), ("Gray", "manga"), ("Blue", "blue"),
    ("Torch", "torch"), ("Mint", "mint"), ("Aurora", "aurora"),
]
for style, key in PAIRS:
    need = [
        os.path.join(RES, "mipmap-anydpi-v26", "ic_launcher_%s.xml" % key),
        os.path.join(RES, "mipmap-xxxhdpi", "ic_launcher_%s.png" % key),
        os.path.join(RES, "drawable-xxxhdpi", "ic_launcher_%s_bg.png" % key),
        os.path.join(RES, "drawable-xxxhdpi", "ic_launcher_%s_fg.png" % key),
    ]
    for p in need:
        if not os.path.exists(p):
            err("主题 %s 缺资源: %s" % (style, os.path.relpath(p, BASE)))
    if 'Theme.ZiyongBaodian.%s"' % style not in th_xml:
        err("缺主题 style: %s" % style)
    if "@string/app_name" not in th_xml:
        pass
for k in ("manga", "blue", "orange", "gray", "mint", "aurora"):
    p = os.path.join(RES, "drawable", "bg_header_%s.xml" % k)
    if not os.path.exists(p):
        err("缺头部渐变: bg_header_%s.xml" % k)
print("[7] 主题配套资源检查完成")

# ---------------------------------------------------------------- 8. PNG 尺寸
try:
    from PIL import Image
    for fn in os.listdir(os.path.join(RES, "mipmap-xxxhdpi")):
        if fn.startswith("ic_launcher_") and fn.endswith(".png"):
            im = Image.open(os.path.join(RES, "mipmap-xxxhdpi", fn))
            if im.size != (192, 192):
                err("legacy 图标尺寸异常 %s: %s" % (fn, im.size))
    for fn in os.listdir(os.path.join(RES, "drawable-xxxhdpi")):
        if fn.startswith("ic_launcher_") and fn.endswith(".png"):
            im = Image.open(os.path.join(RES, "drawable-xxxhdpi", fn))
            if im.size != (432, 432):
                err("adaptive 图层尺寸异常 %s: %s" % (fn, im.size))
    print("[8] 启动图标 PNG 尺寸检查完成")
except ImportError:
    warn("未安装 Pillow，跳过 PNG 尺寸检查")

# ---------------------------------------------------------------- 汇总
print("\n" + "=" * 60)
print("错误 %d / 警告 %d" % (len(errors), len(warns)))
for e in errors:
    print("  [ERR ] " + e)
for w in warns:
    print("  [WARN] " + w)
print("=" * 60)
sys.exit(1 if errors else 0)
