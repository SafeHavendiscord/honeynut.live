MUSIC FOLDER
============
Drop songs in here with normal names, like:
    Artist - Song Title.mp3     -> shows "Song Title" by "Artist"
    Song Title.mp3              -> shows "Song Title"

The site is hosted on Cloudflare, which can't list folders, so this folder
also needs a files.json listing the songs. To make it:
  1. Sign in to the site as HoneyNUT and open "List Maker".
  2. Choose this music folder on your computer, click "Download files.json".
  3. Put files.json in here (replace the old one), then upload/deploy.
(Or ask Claude Code to regenerate it: tools/make-lists.sh does the same thing.)

.m4a, .ogg and .wav work too.
