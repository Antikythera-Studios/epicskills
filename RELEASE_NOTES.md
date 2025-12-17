# Epic Fight: Skill Tree in Minecraft 1.20.1 Changelog
# Changelog on publishing websites and Discord will be parsed between version header ([x.x.x] - yyyy-mm-dd) and (For Devs) section

## [20.2.5] - 2025-12-17

### Changed

- Internal changes for better code quality

### For Devs

- Deprecated `EpicFightMod.rl` and added `EpicFightMod.identifier` since
  [Mojang renamed `ResourceLocation` to
  `Identifier` in 1.21.11](https://neoforged.net/news/21.11release/#renaming-of-resourcelocation-to-identifier).
- Removed `MixinControlEngine` to avoid future breaking changes

## [20.2.4] - 2025-12-07

### Changed

- **[Controlify]** Skill tree navigation updated:
    - Automatically skips locked pages when navigating
    - Focus now moves correctly to the next available page

## [20.2.3] - 2025-11-11

### Added

- Built-in Controlify integration for controller support.
  No need to install
  [Epic Fight: Controlify](https://www.curseforge.com/minecraft/mc-mods/epic-fight-controlify) anymore.
  Install only
  [Controlify: Forgified](https://www.curseforge.com/minecraft/mc-mods/controlify-forgified-unofficial) on 1.20.1

### Fixed
- Fixed the skills with a custom unlock condition is not applied in dedicated servers [#13](https://github.com/Epic-Fight/epicskills/issues/13)

### For Devs
- Adopted KeyConflictContext for each keybind as documented by [Neoforge](https://docs.neoforged.net/docs/misc/keymappings/#ikeyconflictcontext) to avoid potential problem from inconsistency

## [20.2.2] - 2025-11-04

### Added
- Added Controlify support for unofficial Forge port

### For Devs
- Fix minor mixin issues in `com.yesman.epicskills.mixin.MixinControlEngine`

## [20.2.1] - 2025-10-20

### Added
- Added skill editor open button in skill tree screen

### Fixed
- Fixed the Husk not dropping Ability stone

### Changed
- Replace the default skill tree open key to 'N', and now you can open the original skill edit screen in Epic Fight
- Players now get restricted by a cooldown to replace skills, which added in Epic Fight 20.13.1