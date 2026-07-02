#!/usr/bin/env bash
# Run the downloadModels Gradle task with an actual interactive picker.
#
# The Gradle build daemon has no OS-level controlling terminal (true even with
# --no-daemon, which only controls whether the daemon is kept alive for reuse — a daemon
# process is still forked to execute the task). So the picker can't detect a real tty
# there. Instead, DownloadModels.kt explicitly forwards this terminal's stdin through
# Gradle's client<->daemon protocol (`standardInput = System.\`in\`` on the Exec spec,
# gated on --force-prompt so the plain task never risks blocking in CI). --console=plain
# avoids Gradle's rich-UI redraws fighting with the prompt for the terminal.
#
# Equivalent to running scripts/download-models.sh directly (which is faster, since it
# skips Gradle/JVM startup entirely) — this exists so the interactive path is reachable
# via the Gradle task surface (`./gradlew tasks --group game`) without knowing the script
# exists underneath it.
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

exec $GRADLE downloadModels --no-daemon --console=plain --force-prompt "$@"
