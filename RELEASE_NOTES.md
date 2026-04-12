# Epic Fight: Skill Tree in Minecraft 1.21.1 Changelog
# Changelog on publishing websites and Discord will be parsed between version header ([x.x.x] - yyyy-mm-dd) and (For Devs) section

## [21.3.3] - 2026-04-13

### Fixed

- Fixed the skill node and ability point not returned for custom lock skills when reset

## [21.3.2] - 2026-03-10

- Added an option to skill tree reset command where players will be returned the ability points they've spent so far
    - Usage: /skilltree reset [targetplayers] [return_points]
    - [return_points]: true or false value

## [21.3.1] - 2026-01-31

### Changed

- All changes according to dependent epic fight version 21.15.3

## [21.2.6] - 2025-12-17

### Changed

- Internal changes for better code quality

### For Devs

- Deprecated `EpicFightMod.rl` and added `EpicFightMod.identifier` since
  [Mojang renamed `ResourceLocation` to
  `Identifier` in 1.21.11](https://neoforged.net/news/21.11release/#renaming-of-resourcelocation-to-identifier).

## [21.2.5] - 2025-12-07

### Changed

- **[Controlify]** Skill tree navigation updated:
    - Automatically skips locked pages when navigating
    - Focus now moves correctly to the next available page

## [21.2.4] - 2025-11-12

### Fixed
- Fixed the skills with a custom unlock condition is not applied in dedicated servers
  [#13](https://github.com/Epic-Fight/epicskills/issues/13)

### For Devs
- Adopted KeyConflictContext for each keybind as documented by
[Neoforge](https://docs.neoforged.net/docs/misc/keymappings/#ikeyconflictcontext) to avoid potential problem from
inconsistency
- Avoid shadowing the private `ControlEngine#playerpatch` property from Epic Fight; instead, depend on the public `getPlayerPatch()` instead to avoid future breakage.
- Avoid depending on the deprecated `ControlEngine#isKeyDown` to avoid future breakage.

## [21.2.3] - 2025-11-04

### Changed
- Improved [Controlify mod](https://modrinth.com/mod/controlify) compatibility by allowing:
    - Scaling and moving the skill tree viewport using the right thumbstick.
    - Navigating between skill tree pages using the left and right shoulder buttons.
    - Opening the skill editor using the north button and converting XP to an ability point using the west button.
    - Playing click sound when pressing the equip/unequip button via controller

## [21.2.2] - 2025-11-01

### Added
- Added Controlify compatibility to allow opening the skill tree via the radial menu with navigation support.

## [21.2.1] - 2025-10-20

### Changed
- Ported from Epic Fight: Skill Tree 20.2.1
