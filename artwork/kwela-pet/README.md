# Kwela pet

Three 400 × 400 SVG animations based on `../woman-chat-avatar.svg`:

- `idle.svg`: gentle breathing and sway.
- `working.svg`: reading an open document.
- `finished.svg`: presenting a completed document.

Open `preview.html` in a browser to compare the states. Idle and working loop independently. Finished plays once and holds the completed document; reload the asset to replay it. The matching `-still.svg` files provide static alternatives for reduced motion. Choose the still asset when the user's motion settings request reduced motion.

These are source assets, not an Android component. Android's built-in VectorDrawable does not play SVG animation. An Android integration needs a compatible renderer or conversion to Android-native animation, and should respect the system animator-duration setting. The files do not change the app UI.
