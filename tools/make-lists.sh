#!/bin/sh
# HoneyNUT's World: writes music/files.json and photos/files.json
# so the site knows what songs and photos you have.
#
# Run from anywhere:  sh tools/make-lists.sh   (it updates site/music and site/photos)
# (Claude Code can run this for you.)
cd "$(dirname "$0")/../site" || exit 1
for dir in music photos; do
    [ -d "$dir" ] || continue
    out="$dir/files.json"
    first=1
    printf '[' > "$out"
    for f in "$dir"/*; do
        [ -f "$f" ] || continue
        name=$(basename "$f")
        lower=$(printf '%s' "$name" | tr 'A-Z' 'a-z')
        case "$lower" in
            *.mp3|*.m4a|*.ogg|*.wav|*.jpg|*.jpeg|*.png|*.webp|*.gif) ;;
            *) continue ;;
        esac
        esc=$(printf '%s' "$name" | sed 's/\\/\\\\/g; s/"/\\"/g')
        if [ $first -eq 1 ]; then first=0; else printf ',' >> "$out"; fi
        printf '"%s"' "$esc" >> "$out"
    done
    printf ']\n' >> "$out"
    echo "Wrote $out"
done
