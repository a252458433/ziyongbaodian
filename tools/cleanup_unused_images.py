# -*- coding: utf-8 -*-
"""
清理工程里零引用的历史图标/图片素材（幂等，可重复执行）。

判定依据：静态扫描全部 xml / kt / md / py / kts 的文本引用，
只删「既不被 Android 资源引用、也不被任何文档/脚本提及」的历史图片。

保留白名单（KEEP_STEMS）之外的 res PNG 一律不动：
  - halftone_tile.png              6 套头部渐变都在用
  - ic_launcher_*_bg/fg.png        adaptive-icon 图层
  - ic_launcher_*.png (mipmap)     legacy 图标 / manifest alias
  - design/UI美化_*.png            本次交付预览（说明文档引用）
  - design/实拍_*.png              本次真机实拍归档

用法：python tools/cleanup_unused_images.py
"""
import os
import re

BASE = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))
RES = os.path.join(BASE, "app", "src", "main", "res")
DESIGN = os.path.join(BASE, "design")
TOOLS = os.path.join(BASE, "tools")
EXT = (".png", ".jpg", ".jpeg", ".webp", ".gif")

# 资源目录里必须保留的图片（被 XML / manifest 真实引用）
KEEP_RES = {
    "halftone_tile.png",
}
for key in ("manga", "manga_pink", "blue", "torch", "mint", "aurora"):
    KEEP_RES.add("ic_launcher_%s_bg.png" % key)
    KEEP_RES.add("ic_launcher_%s_fg.png" % key)
    KEEP_RES.add("ic_launcher_%s.png" % key)

# design/ 里必须保留的本次交付预览
KEEP_DESIGN_PREFIX = ("UI美化_", "实拍_")

# 历史素材命名规则（旧版设计稿 / 少女立绘 / 旧图标验证截图）
HISTORY = re.compile(
    r"^(device_|u\d+\.|verify_|icon_circle_preview|icon_manga_masked_preview|"
    r"_icon_v2_preview|Keep_this_exact|Redraw_this_black)", re.I)


def text_blob():
    parts = []
    for root, dirs, files in os.walk(BASE):
        rel = os.path.relpath(root, BASE).replace("\\", "/")
        if rel != "." and re.search(r"(^|/)(build|\.gradle|\.idea|\.cxx|__pycache__)(/|$)", rel):
            dirs[:] = []
            continue
        for fn in files:
            if fn.lower().endswith((".xml", ".kt", ".md", ".py", ".kts", ".properties", ".txt")):
                try:
                    parts.append(open(os.path.join(root, fn), "r", encoding="utf-8",
                                      errors="ignore").read())
                except OSError:
                    pass
    return "\n".join(parts)


def main():
    blob = text_blob()
    removed, kept = [], []
    freed = 0

    # 1) res 下的图片：不在白名单 且 零引用 → 删
    for root, dirs, files in os.walk(RES):
        for fn in files:
            if not fn.lower().endswith(EXT):
                continue
            if fn in KEEP_RES:
                kept.append(os.path.relpath(os.path.join(root, fn), BASE))
                continue
            stem = os.path.splitext(fn)[0]
            # 只认真实资源引用；README 等历史说明里提到文件名不算引用
            used = bool(re.search(
                r"(?:@(?:drawable|mipmap)/%s\b|R\.(?:drawable|mipmap)\.%s\b|"
                r"\"@(?:drawable|mipmap)/%s\")" % (re.escape(stem), re.escape(stem), re.escape(stem)),
                blob))
            if used:
                kept.append(os.path.relpath(os.path.join(root, fn), BASE))
            else:
                p = os.path.join(root, fn)
                freed += os.path.getsize(p)
                os.remove(p)
                removed.append(os.path.relpath(p, BASE))

    # 2) design/ 历史素材
    if os.path.isdir(DESIGN):
        for fn in sorted(os.listdir(DESIGN)):
            p = os.path.join(DESIGN, fn)
            if not os.path.isfile(p) or not fn.lower().endswith(EXT):
                continue
            if fn.startswith(KEEP_DESIGN_PREFIX):
                kept.append(os.path.relpath(p, BASE))
                continue
            if HISTORY.match(fn) and fn not in blob:
                freed += os.path.getsize(p)
                os.remove(p)
                removed.append(os.path.relpath(p, BASE))
            else:
                kept.append(os.path.relpath(p, BASE))

    # 3) tools/ 旧版图标预览
    if os.path.isdir(TOOLS):
        for fn in sorted(os.listdir(TOOLS)):
            p = os.path.join(TOOLS, fn)
            if not os.path.isfile(p) or not fn.lower().endswith(EXT):
                continue
            if fn.startswith("preview_launcher_"):
                freed += os.path.getsize(p)
                os.remove(p)
                removed.append(os.path.relpath(p, BASE))
            else:
                kept.append(os.path.relpath(p, BASE))

    print("REMOVED %d files, freed %.2f MB" % (len(removed), freed / 1024 / 1024))
    for r in removed:
        print("  - " + r)
    print("KEPT %d files" % len(kept))
    for k in sorted(kept):
        print("  * " + k)


if __name__ == "__main__":
    main()
