#!/bin/bash
# Download 3D models from Sketchfab
# Usage: ./download-models.sh [model-id]
# or: ./download-models.sh all

set -e

MODELS=(
  # Fighters
  "ce01462b17244ed490d97124599d6e84:Douglas A-26 Invader"
  "bdf92bf676b84b56bb50efc6f589e349:Fokker Dr.I 1917"
  "71667f7f8f2b4991880cdbb21378f46b:P-51D Mustang"
  "109ef10964dc404089507fc33aad2982:Messerschmitt Bf-109"
  "9fd7912fde0941dda60a584ae3813d3c:Supermarine Spitfire Mk XIV"
  # Bombers
  "927f07f6ddcf470ab0387ce5829024d5:Boeing B-17 Flying Fortress"
  "fb5cdf7c77124dcc9f06f29ed833fa5c:North American B-25 Mitchell"
  "a173584ff087441098322ce242b7e9a1:Heinkel He-111"
  "9d7456f25298471aa7b736f2e1c3ffd9:Junkers Ju-88"
)

download_model() {
  local model_id="$1"
  local model_name="$2"

  echo "📥 Downloading: $model_name"
  echo "   Model ID: $model_id"
  echo "   Visit: https://sketchfab.com/3d-models/$model_id to download manually"
  echo ""
  echo "   Note: Sketchfab requires manual download for free models."
  echo "   Steps:"
  echo "   1. Visit the URL above"
  echo "   2. Click 'Download'"
  echo "   3. Choose format (GLB recommended for game engines)"
  echo "   4. Save to: $(pwd)/$(echo $model_name | tr ' ' '_')"
  echo ""
}

echo "🛩️  Aircraft 3D Model Downloader"
echo "================================"
echo ""

if [[ "$1" == "all" ]]; then
  for model in "${MODELS[@]}"; do
    IFS=':' read -r id name <<< "$model"
    download_model "$id" "$name"
  done
elif [[ -n "$1" ]]; then
  # Search for specific model
  found=false
  for model in "${MODELS[@]}"; do
    IFS=':' read -r id name <<< "$model"
    if [[ "$id" == "$1" ]] || [[ "$name" == *"$1"* ]]; then
      download_model "$id" "$name"
      found=true
      break
    fi
  done
  if [[ "$found" == false ]]; then
    echo "❌ Model not found: $1"
    exit 1
  fi
else
  echo "Available models:"
  echo ""
  i=1
  for model in "${MODELS[@]}"; do
    IFS=':' read -r id name <<< "$model"
    echo "$i. $name"
    echo "   ./download-models.sh '$name'"
    ((i++))
  done
  echo ""
  echo "Download all: ./download-models.sh all"
fi
