# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Combined natural regeneration (`combineNaturalRegeneration`, enabled by default, applies while
  hunger is not shared): the shared health heals at the rate of a single player, only while every
  player meets the vanilla regeneration conditions (fast regeneration needs everyone at full hunger
  with saturation), and each heal drains hunger from all players. Previously, each fed player's own
  regeneration was added to the shared health separately, so players in safety could out-heal the
  danger the rest of the group was in.
- A concise chat summary of how much damage each player took since the shared health was last full,
  shown when the shared life ends. Disable with `announceDeathSummary`.

### Changed

- `shareHunger` now defaults to disabled, making everyone keeping their own hunger topped up (see
  `combineNaturalRegeneration`) the default experience. Existing config files keep their saved
  value.

## [v1.21.1-0.1.7] - 2026-08-26

### Fixed

- [NeoForge] Fixed armor, enchantments and absorption not reducing the damage taken by the shared health bar

## [v1.21.1-0.1.6] - 2026-06-20

### Fixed

- [NeoForge] Fixed totems of undying still letting the shared health bar die when their holder takes a fatal hit

## [v1.21.1-0.1.5] - 2026-06-14

### Added

- Added Fabric support
- Added config option to allow sharing death only (not health)

### Changed

- [Fabric] Now requires the Forge Config API Port mod

### Fixed

- Fixed spectators sometimes spawning with health
- Fixed player-hurt sound inconsistency
- Fixed crash on recreating singleplayer world
- [Fabric] Fixed death event not working properly
- Fixed duplicate event handling after loading multiple worlds in a single game session
- Fixed totems of undying not reviving the shared health bar when their holder takes a fatal hit

## [v1.21.1-0.1.1] - 2026-01-12

### Added

- Add option to disable damage announcements in chat

## [v1.21.1-0.1.0] - 2026-01-07

### Added

- Initial release
