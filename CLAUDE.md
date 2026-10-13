# HoneyNUT's World — project memory for Claude Code

A personal website at **https://www.honeynut.live** that pretends to be a retro computer.
It boots to a boot menu with two "operating systems":

- **HoneyNUT XP**: Windows XP–style desktop (Bliss wallpaper, blue taskbar, green Start button).
- **HoneyNUT 7**: Windows 7 / Frutiger Aero–style desktop (glass windows, water-orb Start button, underwater look, bubbles, light-up aquarium).

Both share the same apps: Welcome, a Windows Media Player–style music player with a visualizer, Car Stuff, Coding Projects, Photography (with picture viewer), Games (Solitaire, FreeCell, Minesweeper, "3D Pinball for HoneyNUT – Space Cougar", DOOM shareware), a Discord link, and a HoneyNUT-only "List Maker" tool.

The owner's handle is **HoneyNUT**. Their car, a dark green **2002 Mercury Cougar**, is the site's mascot, and they're from the Indianapolis area (the Monument Circle photo).

---

## How the owner likes to work (please follow these)

- **Go step by step.** Make one change at a time, show it, and check in before moving on. They explicitly asked not to go "guns blazing" with many changes at once. For bigger features, give a short plan first, then do step 1.
- **Plain language.** Explain what changed and how to use it in a few short bullets. They are not comfortable with terminals (they couldn't find how to open one), so **run commands yourself** and never ask them to type commands.
- **Test before handing anything over.** Serve the site locally and click through it in a real browser (see "Testing" below). Say what you tested, and be upfront about anything you couldn't test.
- **Their browser is Firefox on Linux.** Test in Firefox when you can, not just Chromium.
- **Don't break the other OS.** When changing HoneyNUT 7, make sure XP looks exactly the same, and the other way around.
- **Fewest manual steps possible.** Prefer things that just work without extra uploads or setup. Small images are embedded straight into index.html as data URIs for this reason.
- **When they send a reference image, use it directly.** Don't try to recreate or cut it out yourself. An auto background-removal once deleted the car's tires and later its roof. The clean car image is `assets-src/cougar-cutout.png`.
- **Original art only.** Use no Microsoft logos, Windows flags or copied artwork. Parody names are fine (HoneyNUT XP, HoneyNUT 7, Space Cougar Pinball). DOOM is limited to the freely shareable shareware episode.
- They enjoy fun touches and easter eggs, so feel free to suggest some, but ask before building them.

---

## Folder layout

```
honeynut-world/
├── CLAUDE.md            ← this file
├── START-HERE.md        ← how to start + the first message to send
├── site/                ← EXACTLY what gets deployed to the website
│   ├── index.html       ← the whole site: all HTML, CSS and JavaScript (~4,700 lines)
│   ├── honeynut-logo.jpg        XP boot logo + HoneyNUT user picture
│   ├── honeynut-wallpaper.jpg   HoneyNUT's easter-egg wallpaper (Monument Circle)
│   ├── doom.jsdos               DOOM shareware packaged for the js-dos emulator
│   ├── music/           ← songs + files.json (the list of songs)
│   └── photos/          ← pictures + files.json (the list of photos)
├── assets-src/          ← originals, NOT deployed
│   ├── cougar-cutout.png              clean transparent car (use this for any car art)
│   ├── honeynut-logo-original.png     full-size logo
│   ├── monument-circle-photo.jpg      original of the easter-egg wallpaper
│   ├── cougar-photo-market-st.jpg     photo of the real car
│   └── doom-shareware-1.9-original.zip
└── tools/
    └── make-lists.sh    ← rebuilds site/music/files.json and site/photos/files.json
```

**Their real songs and photos live on their computer and on the live site, not in this bundle.** Before deploying, make sure their songs are in `site/music/` and their photos in `site/photos/`, then run `sh tools/make-lists.sh`.

---

## How index.html is organized

Everything lives in one file, in three parts. Each part has labeled section comments; search for them.

1. **CSS** (`<style>` at the top), with sections like `/* ---------- Taskbar ---------- */`.
   - XP styles are the defaults and are **not** prefixed.
   - **Every HoneyNUT 7 style is prefixed with `body.os-aero`**. They start at the big `HoneyNUT 7 (Aero) skin` banner comment, followed by sections `HoneyNUT 7: boot animation / sign-in screen / shut down / popping bubbles / Light-up Aquarium`.
   - `Phones / small screens` comes last (media query at 760px).
2. **HTML**, with sections like `<!-- Boot menu -->`, `<!-- Welcome window -->`, `<!-- Pinball -->`. Each app is a `<section class="window" id="win-...">`.
3. **JavaScript** (`<script>` at the bottom), with sections like `/* ---------- Window manager ---------- */`, `/* ---------- Media player ---------- */`.

### State lives in classes on `<body>`
- `os-xp` or `os-aero`: which OS was picked on the boot menu.
- `user-honeynut` or `user-guest`: who is signed in.
- `wall-aquarium`: the live aquarium wallpaper is on.
- `.honey-only` elements are hidden unless `user-honeynut`. `.aero-only` / `.xp-only` show for one OS only.

### Important rules (each one bit us before)
- **All JavaScript sits inside one private wrapper**, `(function(){ ... })();`. Keep new code inside it. Without the wrapper, a top-level `let z` collided with the DOOM emulator's own `z` and Firefox refused to load DOOM. For debugging, a few things are exposed on `window.honeynut` (openWin, closeWin, minWin, sdDo, showLogin, tracks, user).
- **Order matters for top-level code.** A section that runs at load can't use a `const` declared further down (one crashed using the `$` helper that's defined later in the Media player section). In new sections use `document.getElementById(...)`.
- **Wallpapers are saved by their position in the `wallpapers` list** in localStorage, so **only add new wallpapers at the end** of the list. Each entry can have `os: "xp" | "aero"` (none means both), `honey: true` (HoneyNUT only), and `live: "aquarium"`.
- **Windows:** use `openWin(id)`, `closeWin(el)`, `minWin(el)`, `toggleMax(el)`. They handle the taskbar and the animations.
- **Sign-in:** the HoneyNUT password is stored only as a **SHA-256 hash** in `USERS` (Sign-in screen section). The owner knows the password. Never write it into the code in plain text. To change it, hash the new password and replace the hash. It's a fun lock, not real security.
- **Performance:** Firefox on their machine once stopped drawing the pinball table under heavy canvas effects (a restart fixed it, but we lightened the drawing anyway). Avoid per-frame `shadowBlur`, huge animated `filter`s, and full-screen `backdrop-filter` blur. Animate with `transform` and `opacity`.
- **Browsers block sound until a click**, so music starts on the boot-screen or sign-in click.

### Where specific things are
| Thing | Where |
|---|---|
| Boot menu (OS choice, 10s countdown, remembers last choice in `hn-os`) | JS `Boot menu: choose an OS`, HTML `#bootMenu` |
| XP boot screen | `#splash` (uses honeynut-logo.jpg) |
| HoneyNUT 7 boot animation (car in glass orb) | HTML `.aero-boot`; the car is a data URI on `img#abCar` |
| Sign-in screen (both OSes) | HTML `#login`, JS `Sign-in screen` |
| Shut down: XP dialog / Windows 7 power menu | JS `Turn Off / Log Off` (`sdOpen`, `sdDo`, `pwOpen`) |
| Music player | JS `Media player`. Reads `music/files.json`; "Artist - Title.mp3" names become title/artist |
| Photos | JS `Photography`. Reads `photos/files.json` |
| Folder lists | JS `Folder reading`: tries files.php → files.json → host folder listing → numbered 1.mp3, 2.mp3… |
| Pinball | JS `Pinball: Space Cougar` (two canvases: `#pbArt` static art, `#pbCanvas` moving parts). Car = data URI in `carImg.src` |
| DOOM | JS `DOOM`. Loads js-dos v8 from `https://v8.js-dos.com/latest/` when the window opens, runs `doom.jsdos` |
| Aquarium wallpaper (fish, coral, gravel) | HTML `.aquarium`, CSS `Light-up Aquarium` |
| Bubble popping / fish darting | JS `HoneyNUT 7: popping bubbles + darting fish` |
| Wallpapers + Display Properties | JS `Wallpapers` |

---

## Hosting and deploying

- Hosted on **Cloudflare Workers** (static assets). There's also a `*.workers.dev` address.
- The custom domains **honeynut.live** and **www.honeynut.live** must be attached to the Worker as **Custom Domains**. A route that only covers the front page caused **HTTP 522** errors for every other file (logo, music, DOOM).
- Cloudflare **does not run PHP and does not list folders**. That's why `music/` and `photos/` need a **files.json**, made with List Maker (inside the site, HoneyNUT only) or `tools/make-lists.sh`. (`files.php` support is still in the code for other hosts; it does nothing on Cloudflare.)
- So far the owner has deployed by uploading files in the Cloudflare dashboard. A possible future upgrade is deploying straight from Claude Code with Cloudflare's `wrangler` tool. Only set that up if they want it, walk them through any login step, and explain in plain words.
- After deploying, a hard refresh (Ctrl+Shift+R) avoids old cached files.
- One wallpaper (Bliss) loads from an outside site (wallpaperflare). It could be self-hosted later.

## Testing (do this before every hand-off)
1. Serve the site: `cd site && python3 -m http.server 8000`, then open `http://localhost:8000`. DOOM's emulator won't run from a `file://` page.
2. Click through: boot menu → each OS → sign in as Guest and as HoneyNUT → open the windows you touched → check the browser console for errors.
3. Check both OSes and a phone-width window (about 390px wide).
4. Playwright works well for automated clicks and screenshots. Look at the screenshots yourself before saying something works.

---

## Ideas the owner mentioned but we haven't built yet
- Separate music (and maybe sounds) for HoneyNUT 7, e.g. its own music folder.
- A startup sound of the Cougar starting (needs a recording from the owner).
- More games: Spider Solitaire, Hearts, Checkers/Reversi.
- Real content for the Car Stuff and Coding Projects windows.
- Photo-real aquarium fish if the owner sends fish images with transparent backgrounds. The current fish are drawn in SVG.
- Hosting the DOOM emulator on the site itself instead of loading it from js-dos.com, and/or pinning a fixed js-dos version instead of `latest`.
- More HoneyNUT-only easter eggs. The wallpaper is the first one; `onLogin()` in the Sign-in section is the hook for more.
