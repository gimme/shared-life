# Shared Life

Makes all players share one health bar. When one player takes damage, it affects all players in the world. The intention is to create a challenging multiplayer experience where players cooperate to survive (or blame each other for their deaths). Ideally paired with hardcore mode.

Natural regeneration is a group effort: the shared health only regenerates while *every* player is fed. This means a single player can't sit in safety and eat the group back to health while others are in danger.

### Features
- Shared health bar.
- Natural regeneration requires the whole group to stay fed.
- Shared hunger bar (off by default): a single hunger bar for everyone instead of the group-fed requirement.
- Shared experience (off by default).
- Announcement in chat when someone takes damage (including amount and source).
- Damage summary in chat when the shared life ends, showing how much each player took since it was last at full health.

**Note:** After a server restart, the first player to join will use their current health/hunger to initialize the shared values.


![Logo](images/logo.png)


## Credits

Project template used: https://github.com/jaredlll08/MultiLoader-Template
