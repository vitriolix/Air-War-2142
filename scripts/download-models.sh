#!/usr/bin/env bash
# Download 3D models from Sketchfab.
# Usage: download-models.sh [number|short-name|full-name|sketchfab-id|all]
# With no argument: prompts for a number when run with a real terminal attached
# (i.e. run this DIRECTLY); `./gradlew downloadModels` has no controlling terminal
# (Gradle daemon), so it just lists models instead — same split as pruneBranches.
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"
source "$(dirname "${BASH_SOURCE[0]}")/_sketchfab_auth.sh"

MODELS_DIR="design/aircraft-reference/models"

# sketchfab-id:Full Name:short-name
MODELS=(
  # Fighters
  "ce01462b17244ed490d97124599d6e84:Douglas A-26 Invader:A-26"
  "bdf92bf676b84b56bb50efc6f589e349:Fokker Dr.I 1917:Dr.I"
  "71667f7f8f2b4991880cdbb21378f46b:P-51D Mustang:P-51D"
  "109ef10964dc404089507fc33aad2982:Messerschmitt Bf-109:Bf-109"
  "9fd7912fde0941dda60a584ae3813d3c:Supermarine Spitfire Mk XIV:Spitfire"
  "7eab500310604fd996b116f9cd7520a7:Lockheed P-38 Lightning:P-38"
  "30002b04d490465b90c7ecbfe01e0f52:Republic P-47 Thunderbolt:P-47"
  "8bb0a00e65b741fe83aefe12cfe733f3:Messerschmitt Me-262:Me-262"
  # Bombers
  "927f07f6ddcf470ab0387ce5829024d5:Boeing B-17 Flying Fortress:B-17"
  "fb5cdf7c77124dcc9f06f29ed833fa5c:North American B-25 Mitchell:B-25"
  "a173584ff087441098322ce242b7e9a1:Heinkel He-111:He-111"
  "9d7456f25298471aa7b736f2e1c3ffd9:Junkers Ju-88:Ju-88"
  # Modern/Novelty
  "1850c7bb7ac54902bd9868381ff8b652:Lockheed SR-71 Blackbird:SR-71"
  "757581776f56449c939cb0a72683dce2:Lockheed F-117 Nighthawk:F-117"
)

download_model() {
  local model_id="$1" model_name="$2"
  local save_dir="$REPO_ROOT/$MODELS_DIR/$(echo "$model_name" | tr ' ' '_')"

  echo "📥 Downloading: $model_name"
  echo "   Model ID: $model_id"

  if sketchfab_download_model "$model_id" "$save_dir"; then
    echo ""
    return 0
  fi

  local manual_url="https://sketchfab.com/3d-models/$model_id"
  echo "   Visit: $manual_url to download manually"
  echo ""
  echo "   Note: automated download isn't set up (or failed) for this model."
  echo "   Steps:"
  echo "   1. Visit the URL above (opening it now if possible)"
  echo "   2. Click 'Download'"
  echo "   3. Choose format (GLB recommended for game engines)"
  echo "   4. Save to: $save_dir"
  echo ""
  open_url "$manual_url" || true
}

list_models() {
  echo "Available models:"
  echo ""
  local i=1
  for model in "${MODELS[@]}"; do
    IFS=':' read -r id name short <<< "$model"
    echo "$i. $name"
    echo "   ./scripts/download-models.sh $i        (or: --model=\"$short\" or --model=\"$name\")"
    ((i++))
  done
  echo ""
  echo "Download all: ./scripts/download-models.sh all"
}

# Resolve $1 against MODELS by index, sketchfab id, short name (exact, case-insensitive), or
# full-name substring (case-insensitive). Echoes "id:name" on match.
#
# Checked as PRIORITY TIERS across the whole list, not per-entry-in-array-order — checking
# all 4 match types for entry 1, then all 4 for entry 2, etc. lets a low-priority match on an
# EARLIER entry beat a high-priority match on a LATER one. Real case this hit: querying "5"
# (meaning index 5, Spitfire) matched entry 3's full-name substring check instead ("P-51D
# Mustang" contains "5", from "51") because entry 3 comes before entry 5 in the array — the
# loop returned that wrong match before ever reaching Spitfire's exact index. A numeric query
# is unambiguous (the user typed a number, they mean an index) so it's its own tier and never
# falls through to name matching at all.
resolve_model() {
  local query="$1" i model id name short
  # An empty query must never match — `*""*` is a no-op glob that matches every string,
  # so without this guard an empty $query would silently "resolve" to model #1.
  [[ -z "$query" ]] && return 1

  if [[ "$query" =~ ^[0-9]+$ ]]; then
    i=1
    for model in "${MODELS[@]}"; do
      if [[ "$i" == "$query" ]]; then
        IFS=':' read -r id name short <<< "$model"
        echo "$id:$name"
        return 0
      fi
      ((i++))
    done
    return 1
  fi

  for model in "${MODELS[@]}"; do
    IFS=':' read -r id name short <<< "$model"
    [[ "$query" == "$id" ]] && { echo "$id:$name"; return 0; }
  done
  for model in "${MODELS[@]}"; do
    IFS=':' read -r id name short <<< "$model"
    [[ "${query,,}" == "${short,,}" ]] && { echo "$id:$name"; return 0; }
  done
  for model in "${MODELS[@]}"; do
    IFS=':' read -r id name short <<< "$model"
    [[ "${name,,}" == *"${query,,}"* ]] && { echo "$id:$name"; return 0; }
  done
  return 1
}

echo "🛩️  Aircraft 3D Model Downloader"
echo "================================"
echo ""

selection="${1:-}"

if [[ -z "$selection" ]]; then
  # can_prompt (see _common.sh) covers both: run directly (real /dev/tty) or run via
  # `./gradlew downloadModels --no-daemon --console=plain --force-prompt`
  # (scripts/download-models-interactive.sh / `npm run models:pick`), where
  # AIR_WAR_2142_FORCE_PROMPT=1 signals that DownloadModels.kt forwarded this terminal's real
  # input through Gradle's client<->daemon protocol (the Gradle build daemon itself has no
  # OS-level controlling terminal, --no-daemon or not, so /dev/tty alone would never
  # detect that path).
  if can_prompt; then
    list_models
    echo ""
    selection="$(prompt_line "Enter a number (or 'all'):")"
    echo ""
  else
    list_models
    exit 0
  fi
  # Prompted but got nothing back (e.g. --force-prompt was set but stdin wasn't actually a
  # real terminal after all — closed/EOF). Already showed the list; don't fall through to
  # resolve_model with an empty selection.
  [[ -z "$selection" ]] && exit 0
fi

if [[ "$selection" == "all" ]]; then
  for model in "${MODELS[@]}"; do
    IFS=':' read -r id name short <<< "$model"
    download_model "$id" "$name"
  done
elif resolved="$(resolve_model "$selection")"; then
  IFS=':' read -r id name <<< "$resolved"
  download_model "$id" "$name"
else
  fail "Model not found: $selection"
fi
