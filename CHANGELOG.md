# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
