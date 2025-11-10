# Epic Fight: Skill Tree in Minecraft 1.20.1 Changelog
# Changelog on publishing websites and Discord will be parsed between version header ([x.x.x] - yyyy-mm-dd) and (For Devs) section

## [20.2.3] - Unreleased

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