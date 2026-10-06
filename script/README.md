# TEGARIS82 FIGHT — True 3D GitHub Android Build

Godot 4 project for an original 3D fighting prototype. It is designed to build an Android debug APK automatically on GitHub Actions.

## GitHub build
1. Create a repository.
2. Upload all files from this folder to the repository root.
3. Push to `main` or run **Actions → Build TEGARIS82 Android → Run workflow**.
4. Download artifact **TEGARIS82-FIGHT-Android-debug**.

The workflow uses the `barichello/godot-ci:4.3` image and the committed `export_presets.cfg`.

## Controls
Desktop: A/D move, J punch, K kick, L block, Space jump.

Android: the current prototype is playable with touch through Godot's virtual/input mapping layer; desktop keyboard remains available for development. Future iterations can add full on-screen fighting buttons.

## Important
The fighters are original procedural 3D geometry, not copied Tekken assets. For a production-quality fighting game, replace the procedural bodies with licensed/original GLB characters and authored skeletal animations.
