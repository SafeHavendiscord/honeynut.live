# Starting HoneyNUT's World in Claude Code

## 1. Unzip
Unzip **honeynut-world.zip** somewhere easy to find, like your Documents folder.
You'll get a folder called **honeynut-world**.

## 2. Add your real songs and photos
Copy your songs into `honeynut-world/site/music/` and your photos into `honeynut-world/site/photos/`
(the same files you already have on the live site). Claude Code can rebuild the `files.json` lists for you.

## 3. Open the folder in Claude Code
Open Claude Code with **honeynut-world** as the project folder (the folder that contains `CLAUDE.md`).
Claude Code reads `CLAUDE.md` automatically at the start of every session, so it'll already know the project and how you like to work.

## 4. Paste this as your first message

> Hi! I'm moving my website project, HoneyNUT's World, here from Claude chat. Please read CLAUDE.md first. It explains the site, how I like to work, and how the code is laid out.
>
> To get started:
> 1. Set up git in this folder so every change we make can be undone.
> 2. Run tools/make-lists.sh so the music and photos lists match what's in site/music and site/photos.
> 3. Start a local server for the site folder and quickly check that both operating systems (HoneyNUT XP and HoneyNUT 7) boot, sign in, and have no errors.
> 4. Give me a short, plain-language summary of what you found.
>
> Please don't change anything else yet. We'll go step by step from there.

## Tips
- Ask it to explain anything in the code: "what does the Media player section do?"
- When you're happy with a change, ask: "Which files do I need to upload to Cloudflare?"
- If something goes wrong: "Undo that last change." (That's why step 1 sets up git.)
