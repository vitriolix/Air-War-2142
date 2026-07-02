#!/usr/bin/env bash
# Launch Blender without needing to remember its install path or CLI flags.
# Usage: blender.sh [--headless] [model-name]
#   --headless    Run without GUI (blender -b) — for batch/scripted work.
#   model-name    Fuzzy-matched (case-insensitive substring) against downloaded model
#                 folder names in design/aircraft-reference/models/. If found and it has a
#                 .blend file, opens that. If it only has a scene.gltf (the common case for
#                 a freshly-downloaded, never-yet-saved-in-Blender model), auto-imports it
#                 and switches every 3D viewport to Material Preview shading — Blender's
#                 default "Solid" mode never shows textures, regardless of import
#                 correctness (see the models/README.md texture-viewing note).
#                 Omit to just launch Blender with an empty scene.
#
# Edit BLENDER_APP below if Blender is installed somewhere else on your machine.
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

BLENDER_APP="/Applications/Blender.app/Contents/MacOS/Blender"
MODELS_DIR="$REPO_ROOT/design/aircraft-reference/models"

[ -x "$BLENDER_APP" ] || fail "Blender not found at $BLENDER_APP — edit BLENDER_APP in scripts/blender.sh if it's installed elsewhere."

headless=0
model_query=""
for arg in "$@"; do
  case "$arg" in
    --headless) headless=1 ;;
    *) model_query="$arg" ;;
  esac
done

cmd=("$BLENDER_APP")
[ "$headless" -eq 1 ] && cmd+=("-b")

if [ -n "$model_query" ]; then
  match_dir="$(find "$MODELS_DIR" -maxdepth 1 -type d -iname "*${model_query}*" 2>/dev/null | head -1)"
  [ -n "$match_dir" ] || fail "No downloaded model folder matches '$model_query' in $MODELS_DIR. Run 'npm run models:pick' to download it first."

  blend_file="$(find "$match_dir" -maxdepth 1 -iname "*.blend" 2>/dev/null | head -1)"
  if [ -n "$blend_file" ]; then
    info "Opening $blend_file"
    cmd+=("$blend_file")
  else
    gltf_file="$(find "$match_dir" -maxdepth 1 -iname "scene.gltf" 2>/dev/null | head -1)"
    [ -n "$gltf_file" ] || fail "No .blend or scene.gltf found in $match_dir."
    info "No .blend yet for '$model_query' — auto-importing $gltf_file and switching to Material Preview shading"
    py_script="$(mktemp -t blender-import).py"
    cat > "$py_script" <<PYEOF
import bpy
bpy.ops.import_scene.gltf(filepath="${gltf_file}")
for window in bpy.context.window_manager.windows:
    for area in window.screen.areas:
        if area.type == 'VIEW_3D':
            for space in area.spaces:
                if space.type == 'VIEW_3D':
                    space.shading.type = 'MATERIAL'
PYEOF
    cmd+=(--python "$py_script")
  fi
fi

info "Launching: ${cmd[*]}"
exec "${cmd[@]}"
