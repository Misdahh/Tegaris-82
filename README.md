# TEGARIS82 FIGHT v2.1 – GAME UI / AUDIO / OUTFIT

Update from v2.0:
- Modern fighting-game main menu background and layout.
- Settings menu: Music, Sound Effects, Vibration.
- Original generated menu music loop and punch/hit/skill sound effects.
- Outfit Garage with 5 outfit color sets: Default, Gold Fist, Mystic, Cyber, Crimson.
- Outfit selection changes fighter appearance in-game.
- Keeps 17 original fighters, human motion, virtual PS-style controls, mystic effects, power clash, and all arenas including MMC PONSEL Service.
- Vibration feedback on attacks when enabled.

Build: `gradle :app:assembleDebug`

## v2.2 – HUMAN-LIKE ANIMATION PASS
- Improved procedural fighter anatomy with articulated elbows and knees, separate limb segments, joint pads, glove/boot silhouettes, facial landmarks, and subtle breathing/guard motion.
- Attacks continue to pose the body and limbs through the existing fighting animation system.
- Important limitation: this is still a stylized 2D Canvas fighter, not photorealistic 3D or motion-captured animation. For true lifelike characters, the project would need rigged 3D models/animations and a 3D renderer/engine.

## v2.3 – 3D STYLE + APP ICON + PROFILE
- Added 3D-style depth shading/highlights to procedural human fighters and avatar.
- Added launcher application icon so Android does not show a blank/default icon.
- Added Player Profile screen with avatar, fighter, level, fights, wins, and rank.
- Profile stats update as matches are started and won.
- Note: this remains a Canvas-based 2D renderer with 3D-style shading; it is not a true polygonal 3D engine/model.
