# LegendaryOnboarding

LegendaryOnboarding is a Paper/Folia plugin that presents a configurable onboarding sequence to brand-new players and requires them to accept the server rules before continuing.

## Requirements

- Java 21
- Paper or Folia 1.21.x
- Built against the Paper API for Minecraft 1.21.11

The plugin declares `folia-supported: true` and uses Paper/Folia player and async schedulers.

## What It Does

### First join

When a player joins for the first time, the plugin:

1. Records the player's UUID, current name, and original location.
2. Optionally teleports the player to a configured onboarding location.
3. Applies the configured onboarding game mode.
4. Freezes the player's position and view.
5. Makes the player invisible and invulnerable, enables flight, and applies blindness.
6. Displays a welcome title followed by the configured rules title sequence.
7. Displays a configurable `/accept` prompt and chat message.

Players who have played on the server before are not automatically onboarded unless they have an unfinished onboarding session recorded in `pending.yml`.

### Restrictions during onboarding

Until onboarding is complete:

- Chat messages are blocked.
- Commands are blocked unless they are listed in `COMMAND_WHITELIST`.
- `/accept` is always permitted by the command blocker, but it only succeeds after the rules sequence has finished.
- Repeated `/accept` attempts are guarded so the completion sequence cannot run more than once at a time.

### Accepting the rules

After the player runs `/accept`, the plugin:

1. Clears the acceptance prompt.
2. Displays the configured post-acceptance title sequence.
3. Restores survival mode, visibility, flight settings, and fall distance.
4. Finds a physically safe location near the player's saved original position.
5. Teleports the player back asynchronously.
6. Removes blindness and the movement lock.
7. Grants five seconds of temporary invulnerability and resistance.
8. Permanently records the player's acceptance.

The safe-location check requires passable feet and head blocks, solid ground, and avoids common hazards such as lava, fire, magma blocks, campfires, cactus, and powder snow. If the saved location or world is unavailable, the world spawn is used as a fallback.

### Disconnect and restart recovery

- A player who disconnects before completing onboarding retains their saved return location and resumes onboarding when they reconnect.
- Accepted players bypass onboarding on future joins.
- Stale pending data is cleared when an accepted player joins.
- Acceptance and pending-location data are saved asynchronously and flushed when the plugin disables.

## Commands and Permissions

| Command | Description | Permission |
| --- | --- | --- |
| `/accept` | Accepts the server rules after the rules sequence finishes | `legendaryonboarding.accept` |

The permission defaults to all players.

## Configuration

The default configuration is stored in `src/main/resources/config.yml` and copied to the plugin data directory on first startup.

| Setting | Purpose |
| --- | --- |
| `SERVER_NAME` | Server name used in the welcome title |
| `ONBOARD_GAMEMODE` | Onboarding game mode: `survival`, `creative`, `spectator`, or `adventure` |
| `ONBOARD_TELEPORT` | Whether to move the player to the configured onboarding location |
| `POS_WORLD` | Onboarding world; this world must be loaded when the plugin starts |
| `POS_X`, `POS_Y`, `POS_Z` | Onboarding coordinates |
| `POS_YAW`, `POS_PITCH` | Onboarding view direction |
| `RULES_SEQUENCE_CONTENT` | Ordered rules titles and subtitles |
| `RULES_SEQUENCE_FADE_IN` | Rules title fade-in time in seconds |
| `RULES_SEQUENCE_DURATION` | Rules title display time in seconds |
| `RULES_SEQUENCE_FADE_OUT` | Rules title fade-out time in seconds |
| `PROMPT_ACCEPT` | Long-running title/subtitle prompting the player to accept |
| `PROMPT_CHAT` | Chat message displayed with the acceptance prompt |
| `JOIN_SEQUENCE_CONTENT` | Ordered titles shown after acceptance |
| `JOIN_SEQUENCE_FADE_IN` | Post-acceptance title fade-in time in seconds |
| `JOIN_SEQUENCE_DURATION` | Post-acceptance title display time in seconds |
| `JOIN_SEQUENCE_FADE_OUT` | Post-acceptance title fade-out time in seconds |
| `COMMAND_WHITELIST` | Commands allowed during onboarding, lowercase and without `/` |

Minecraft formatting codes can be used in configured messages. Namespaced commands are normalized before whitelist checks, so `minecraft:help` is checked as `help`.

## Data Files

The plugin creates these files in `plugins/LegendaryOnboarding/`:

- `config.yml`: onboarding messages, timing, location, and command settings.
- `accepted.yml`: accepted UUIDs, up to ten recently seen player names, and the first acceptance timestamp.
- `pending.yml`: saved return locations for players with unfinished onboarding.

## Building

Use the included Gradle wrapper:

```shell
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

The project version is defined in `build.gradle` and expanded into `plugin.yml` during resource processing.
