#!/usr/bin/env bash
# Run the downloadModels Gradle task with an actual interactive picker.
#
# The Gradle *daemon* has no controlling terminal, so a normal `./gradlew downloadModels`
# can only list models (see DownloadModels.kt). `--no-daemon` gives the forked script a
# real controlling terminal to probe via /dev/tty (download-models.sh reads the answer
# straight from /dev/tty, sidestepping Gradle's stdio plumbing), so the "enter a number"
# prompt works. Cost: no daemon reuse, so this invocation is slower to start than a normal
# Gradle command — that's the deliberate tradeoff for interactivity.
#
# Equivalent to running scripts/download-models.sh directly (which is faster, since it
# skips Gradle/JVM startup entirely) — this exists so the interactive path is reachable
# via the Gradle task surface (`./gradlew tasks --group game`) without knowing the script
# exists underneath it.
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

exec $GRADLE downloadModels --no-daemon "$@"
