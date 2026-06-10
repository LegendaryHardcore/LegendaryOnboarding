# LegendaryOnboarding

LegendaryOnboarding is a Paper, Purpur, Folia, and Canvas plugin that presents a configurable onboarding sequence to brand-new players and requires them to accept the server rules before continuing.

## Requirements

- Java 21
- Paper, Purpur, Folia, or Canvas 1.21.x
- Built against the Paper API for Minecraft 1.21.11

Use the jar matching the server platform:

- `LegendaryOnboarding-Paper`: Paper and Purpur
- `LegendaryOnboarding-Folia`: Folia
- `LegendaryOnboarding-Canvas`: Canvas

Shared behavior lives in `bukkit-common`; each platform module supplies its scheduler and task exception handling adapter.

## What It Does

### First join

When a player joins for the first time, the plugin:

1. Records the player's UUID, current name, and original location.
2. Optionally teleports the player to a configured onboarding location.
3. Applies the configured onboarding game mode.
4. Freezes the player's position and view.
5. Makes the player invisible and invulnerable, enables flight, and applies configured sequence effects.
6. Displays a welcome title followed by the configured rules title sequence.
7. Displays a configurable `/accept` prompt and chat message.

Players who have played on the server before are not automatically onboarded unless they have an unfinished onboarding session recorded in `pending.yml`.

### Restrictions during onboarding

Until onboarding is complete:

- All player damage, including void damage, is cancelled.
- Advancement criteria are blocked by default.
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
6. Removes configured sequence effects and the movement lock.
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
| `/legendaryonboarding reload` (`/lo reload`) | Reloads both configuration files | `legendaryonboarding.reload` |

The accept permission defaults to all players. The reload permission defaults to server operators.

## Configuration

Configuration is split by purpose:

- `config.yml` controls the onboarding location, game mode, server name, and command whitelist.
- `titlesequence.yml` contains the rules, acceptance prompt, post-acceptance sequence, chat messages, formatting, and timing.

On startup, both files are updated to their bundled layouts while preserving supported values, adding new defaults, and removing obsolete settings. When upgrading from the older single-file layout, sequence settings are migrated from `config.yml` into `titlesequence.yml` before obsolete keys are removed.

| Setting | Purpose |
| --- | --- |
| `SERVER_NAME` | Value inserted by the `{server_name}` sequence placeholder |
| `ONBOARD_GAMEMODE` | Onboarding game mode: `survival`, `creative`, `spectator`, or `adventure` |
| `ONBOARD_TELEPORT` | Whether to move the player to the configured onboarding location |
| `DEBUG_FORCE_ONBOARDING` | Testing mode that enables onboarding and forces every joining player through it without deleting acceptance records |
| `BLOCK_ADVANCEMENTS` | Prevents advancement criteria from being granted during onboarding; defaults to `true` |
| `POS_WORLD` | Onboarding world; required only when onboarding and teleporting are enabled |
| `POS_X`, `POS_Y`, `POS_Z` | Onboarding coordinates |
| `POS_YAW`, `POS_PITCH` | Onboarding view direction |
| `COMMAND_WHITELIST` | Commands allowed during onboarding, lowercase and without `/` |

Minecraft formatting codes can be used in configured messages. Namespaced commands are normalized before whitelist checks, so `minecraft:help` is checked as `help`.

The bundled sequence applies blindness during onboarding. Minecraft does not
display its fog while the player is in spectator mode, so use `adventure`,
`survival`, or `creative` when blindness should be visible.

For testing on an established server, set `DEBUG_FORCE_ONBOARDING: true`, run
`/lo reload`, then reconnect. Debug mode temporarily activates the title sequence
even when `ENABLED` is `false` in `titlesequence.yml`.
Set the debug option back to `false` and reload when testing is complete.

### Sequence Steps

Inside `titlesequence.yml`, every welcome, rules, prompt, and post-acceptance step supports these optional fields:

- `TITLE`, `SUBTITLE`, and `CHAT`
- `STOP_ALL_SOUNDS`: stops all sounds currently playing for that player
- `SOUND`: sound name plus optional category, volume, and pitch
- `POTION_EFFECTS`: effects to apply when the step begins
- `REMOVE_POTION_EFFECTS`: effect names to remove when the step begins
- `FADE_IN`, `DURATION`, and `FADE_OUT`, in seconds

Potion durations are measured in seconds. Use `DURATION: -1` for an effect that
lasts until a later step removes it or onboarding ends. Amplifiers are zero-based,
so `AMPLIFIER: 0` is level I.

```yaml
- TITLE: "{aqua}Welcome"
  STOP_ALL_SOUNDS: true
  SOUND:
    NAME: "minecraft:block.note_block.pling"
    CATEGORY: "master"
    VOLUME: 1.0
    PITCH: 1.0
  POTION_EFFECTS:
    - TYPE: "minecraft:blindness"
      DURATION: -1
      AMPLIFIER: 0
      PARTICLES: false
      ICON: false
```

Use the `master` sound category for ambience that should not depend on an
individual Minecraft sound-category slider. `STOP_ALL_SOUNDS` clears sounds
already playing when that step begins; it cannot prevent other plugins or the
world from starting new sounds afterward. Add another step with
`STOP_ALL_SOUNDS: true` to end a long or looping ambience sound.

`WELCOME_SEQUENCE` runs first and also supports `{server_name}` and
`{player_name}` placeholders:

```yaml
WELCOME_SEQUENCE:
  - TITLE: "Welcome to {gold}{server_name}{reset}, {player_name}!"
    SUBTITLE: "Please review our rules"
    DURATION: 3
```

Rules and post-acceptance steps inherit their sequence defaults when timing fields are omitted. This keeps ordinary entries short while allowing individual pacing:

```yaml
RULES_SEQUENCE_CONTENT:
  - {TITLE: "{gold}{bold}Rules", SUBTITLE: "Be excellent to each other"}
  - TITLE: "{aqua}Full Rulebook"
    SUBTITLE: "Open the link posted in chat"
    CHAT: "{yellow}Follow this link for all rules: {aqua}https://example.com/rules"
    DURATION: 8
  - {CHAT: "Discord: https://discord.gg/example | Map: https://map.example.com"}
```

Web addresses beginning with `http://` or `https://` inside `CHAT` are automatically clickable. They can be embedded naturally in a sentence, multiple links are supported, and trailing punctuation is kept outside the clickable address. The URL itself is the clickable text; surrounding labels such as `Rules:` remain ordinary chat text.

The sequence file uses these top-level settings:

| Setting | Purpose |
| --- | --- |
| `ENABLED` | Enables onboarding; defaults to `false` so the plugin can be configured before use |
| `ACTIONBAR_COUNTDOWN` | Action-bar countdown shown until `/accept` unlocks; supports `{seconds}` or an empty value to disable |
| `WELCOME_SEQUENCE` | Configurable welcome steps shown before the rules; supports server and player placeholders |
| `RULES_SEQUENCE_CONTENT` | Rules steps shown before the acceptance prompt |
| `RULES_SEQUENCE_FADE_IN`, `RULES_SEQUENCE_DURATION`, `RULES_SEQUENCE_FADE_OUT` | Default rules-step timing |
| `PROMPT_ACCEPT` | Steps shown while waiting for `/accept` |
| `JOIN_SEQUENCE_CONTENT` | Steps shown after the player accepts |
| `JOIN_SEQUENCE_FADE_IN`, `JOIN_SEQUENCE_DURATION`, `JOIN_SEQUENCE_FADE_OUT` | Default post-acceptance timing |

### Text Formatting

Configured titles, subtitles, and sequence chat messages support named formatting variables:

- Colors: `{black}`, `{dark_blue}`, `{dark_green}`, `{dark_aqua}`, `{dark_red}`, `{dark_purple}`, `{gold}`, `{gray}`, `{dark_gray}`, `{blue}`, `{green}`, `{aqua}`, `{red}`, `{light_purple}`, `{yellow}`, and `{white}`.
- RGB colors: `{#55FFAA}`.
- Styles: `{bold}`, `{italic}`, `{underlined}`, `{strikethrough}`, and `{obfuscated}`.
- `{standard}` or `{plain}` clears active styles while retaining the current color.
- `{reset}` clears active styles and resets the color to white.
- `{newline}` inserts a line break in `CHAT`. Minecraft does not support
  multiline titles, subtitles, or action bars, so it renders as a space there.

Legacy Minecraft formatting remains supported using either `&` or `§`, including colors, `&k` through `&o`, `&r`, `&#RRGGBB`, and the full `&x&R&R&G&G&B&B` RGB form. Unknown brace variables are displayed unchanged.

## Data Files

The plugin creates these files in `plugins/LegendaryOnboarding/`:

- `config.yml`: server name, onboarding state/location, and command settings.
- `titlesequence.yml`: the complete player-facing onboarding sequence and its timing defaults.
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

The top-level build compiles and tests the shared code, builds all three platform jars, and copies them into `dist/`:

```text
dist/
  LegendaryOnboarding-Paper-1.1.1.jar
  LegendaryOnboarding-Folia-1.1.1.jar
  LegendaryOnboarding-Canvas-1.1.1.jar
```

The project version is defined in the root `build.gradle` and expanded into each platform's `plugin.yml` during resource processing.
