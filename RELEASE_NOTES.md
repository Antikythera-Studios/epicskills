# Epic Fight: Skill Tree in Minecraft 1.21.1 Changelog
# Changelog on publishing websites and Discord will be parsed between version header ([x.x.x] - yyyy-mm-dd) and (For Devs) section

## [21.2.4] - Unreleased

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
