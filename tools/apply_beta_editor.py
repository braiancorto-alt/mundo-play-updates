#!/usr/bin/env python3
"""Apply the beta-only catalog editor hooks to the decoded 6.32 MainActivity."""
from pathlib import Path
import sys

root = Path(sys.argv[1])
target = root / "smali/com/mundoplay/app/MainActivity.smali"
source = target.read_text()

def method_replace(signature: str, old: str, new: str) -> None:
    global source
    start = source.find(signature)
    if start < 0:
        raise SystemExit(f"Missing expected method: {signature}")
    end = source.find(".end method", start)
    body = source[start:end]
    if body.count(old) != 1:
        raise SystemExit(f"Expected one patch point in {signature}; found {body.count(old)}")
    source = source[:start] + body.replace(old, new, 1) + source[end:]

method_replace(
    ".method private startCatalog(Ljava/util/ArrayList;)V",
    "    iput-object v0, p0, Lcom/mundoplay/app/MainActivity;->liveCategories:Ljava/util/ArrayList;\n",
    "    invoke-static {p0, v0}, Lcom/mundoplay/app/BetaEditor;->applyCategoryNames(Landroid/app/Activity;Ljava/util/List;)V\n"
    "    iput-object v0, p0, Lcom/mundoplay/app/MainActivity;->liveCategories:Ljava/util/ArrayList;\n",
)
method_replace(
    ".method private startCatalog(Ljava/util/ArrayList;)V",
    "    invoke-direct {p0, p1, v0}, Lcom/mundoplay/app/MainActivity;->buildHome(Ljava/util/List;Ljava/lang/String;)V\n",
    "    invoke-static {p0, p1}, Lcom/mundoplay/app/BetaEditor;->filterCategories(Landroid/app/Activity;Ljava/util/List;)Ljava/util/ArrayList;\n"
    "    move-result-object v2\n"
    "    invoke-direct {p0, v2, v0}, Lcom/mundoplay/app/MainActivity;->buildHome(Ljava/util/List;Ljava/lang/String;)V\n",
)
method_replace(
    ".method synthetic lambda$startCatalog$18$com-mundoplay-app-MainActivity(Ljava/util/ArrayList;I)V",
    "    move-result-object v4\n\n    .line 621",
    "    move-result-object v4\n"
    "    invoke-static {p0, v4}, Lcom/mundoplay/app/BetaEditor;->applyChannelEdits(Landroid/app/Activity;Ljava/util/Map;)V\n\n"
    "    .line 621",
)
method_replace(
    ".method synthetic lambda$startCatalog$15$com-mundoplay-app-MainActivity(ILjava/util/ArrayList;Ljava/util/Map;I)V",
    "    invoke-virtual {p2}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;\n",
    "    invoke-static {p0, p2}, Lcom/mundoplay/app/BetaEditor;->filterCategories(Landroid/app/Activity;Ljava/util/List;)Ljava/util/ArrayList;\n"
    "    move-result-object p2\n"
    "    invoke-virtual {p2}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;\n",
)
method_replace(
    ".method synthetic lambda$startCatalog$15$com-mundoplay-app-MainActivity(ILjava/util/ArrayList;Ljava/util/Map;I)V",
    "    check-cast p2, Ljava/util/ArrayList;\n\n    const/4 v1, 0x0\n",
    "    check-cast p2, Ljava/util/ArrayList;\n"
    "    invoke-static {p0, v0, p2}, Lcom/mundoplay/app/BetaEditor;->filterChannels(Landroid/app/Activity;Ljava/lang/String;Ljava/util/List;)Ljava/util/ArrayList;\n"
    "    move-result-object p2\n\n    const/4 v1, 0x0\n",
)
method_replace(
    ".method private showSettings()V",
    "    .line 1687\n    invoke-direct {p0, v11}, Lcom/mundoplay/app/MainActivity;->addSettingDivider(Landroid/widget/LinearLayout;)V\n",
    "    invoke-static {p0, v11}, Lcom/mundoplay/app/BetaEditor;->addEntry(Landroid/app/Activity;Landroid/widget/LinearLayout;)V\n"
    "    .line 1687\n    invoke-direct {p0, v11}, Lcom/mundoplay/app/MainActivity;->addSettingDivider(Landroid/widget/LinearLayout;)V\n",
)

target.write_text(source)

for relative in (
    "smali/com/mundoplay/app/MainActivity$MundoItem.smali",
    "smali/com/mundoplay/app/MainActivity$Category.smali",
):
    field_file = root / relative
    field_source = field_file.read_text()
    old = ".field final name:Ljava/lang/String;"
    if field_source.count(old) != 1:
        raise SystemExit(f"Expected one editable name field in {field_file}")
    field_file.write_text(field_source.replace(old, ".field name:Ljava/lang/String;", 1))


method_replace(
    ".method private playCandidate(Lcom/mundoplay/app/MainActivity$MundoItem;Landroid/view/SurfaceView;Landroid/widget/TextView;I)V",
    "    const/16 v1, 0x2710\n\n    invoke-virtual {v0, v1}, Lcom/google/android/exoplayer2/upstream/DefaultHttpDataSource$Factory;->setConnectTimeoutMs(I)Lcom/google/android/exoplayer2/upstream/DefaultHttpDataSource$Factory;\n",
    "    const/16 v1, 0x1388\n\n    invoke-virtual {v0, v1}, Lcom/google/android/exoplayer2/upstream/DefaultHttpDataSource$Factory;->setConnectTimeoutMs(I)Lcom/google/android/exoplayer2/upstream/DefaultHttpDataSource$Factory;\n",
)
method_replace(
    ".method private playCandidate(Lcom/mundoplay/app/MainActivity$MundoItem;Landroid/view/SurfaceView;Landroid/widget/TextView;I)V",
    "    const/16 v1, 0x4e20\n\n    invoke-virtual {v0, v1}, Lcom/google/android/exoplayer2/upstream/DefaultHttpDataSource$Factory;->setReadTimeoutMs(I)Lcom/google/android/exoplayer2/upstream/DefaultHttpDataSource$Factory;\n",
    "    const/16 v1, 0x1f40\n\n    invoke-virtual {v0, v1}, Lcom/google/android/exoplayer2/upstream/DefaultHttpDataSource$Factory;->setReadTimeoutMs(I)Lcom/google/android/exoplayer2/upstream/DefaultHttpDataSource$Factory;\n",
)
method_replace(
    ".method private playCandidate(Lcom/mundoplay/app/MainActivity$MundoItem;Landroid/view/SurfaceView;Landroid/widget/TextView;I)V",
    "    new-instance v0, Lcom/google/android/exoplayer2/SimpleExoPlayer$Builder;\n\n    invoke-direct {v0, p0}, Lcom/google/android/exoplayer2/SimpleExoPlayer$Builder;-><init>(Landroid/content/Context;)V\n\n    invoke-virtual {v0}, Lcom/google/android/exoplayer2/SimpleExoPlayer$Builder;->build()Lcom/google/android/exoplayer2/SimpleExoPlayer;\n",
    "    invoke-static {p0}, Lcom/mundoplay/app/FastPlayer;->create(Landroid/content/Context;)Lcom/google/android/exoplayer2/SimpleExoPlayer;\n",
)

print(f"Applied editor hooks, fast player, and shorter stream timeouts to {root}")
