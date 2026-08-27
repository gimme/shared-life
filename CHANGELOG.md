# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- `shareDeath` now defaults to disabled. Death sharing still implicitly applies while `shareHealth`
  is enabled, so the default experience is unchanged; existing config files keep their saved value.

### Fixed

- Fixed `shareDeath` doing nothing while `shareHealth` was disabled: one player's death now kills
  everyone even when only death sharing is enabled
- Fixed the death summary never being announced when a death ended the shared life. It arrives right
  after the death messages.
- Fixed switching out of creative or spectator mode not joining the player into the shared life:
  they now sync onto the live shared health — or re-seed it from their own state after a total
  death — the same way joining the server does.

## [v26.2-0.2.1] - 2026-08-26

### Fixed

- [NeoForge] Fixed armor, enchantments and absorption not reducing the damage taken by the shared
  health bar

## [v26.2-0.2.0] - 2026-07-06

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

### Fixed

- [Fabric] Fixed the mod not loading in LAN-opened worlds

## [v26.2-0.1.6] - 2026-06-20

### Fixed

- [NeoForge] Fixed totems of undying still letting the shared health bar die when their holder takes a fatal hit

## [v26.1-0.1.5] - 2026-06-14

### Changed

- [Fabric] Now requires the Forge Config API Port mod

### Fixed

- Fixed duplicate event handling after loading multiple worlds in a single game session
- Fixed totems of undying not reviving the shared health bar when their holder takes a fatal hit

## [v26.1-0.1.4] - 2026-03-30

### Added

- Version 26.1

## [v1.21.11-0.1.4] - 2026-02-14

### Added

- Added config option to allow sharing death only (not health)

### Fixed

- [Fabric] Fixed death event not working properly

## [v1.21.11-0.1.3] - 2026-02-08

### Fixed

- Fixed crash on recreating singleplayer world

## [v1.21.11-0.1.2] - 2026-02-07

### Added

- Added Fabric support

### Fixed

- Fixed spectators sometimes spawning with health
- Fixed player-hurt sound inconsistency

## [v1.21.11-0.1.1] - 2026-01-17

### Added

- Initial release
