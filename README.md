# LegendaryOnboarding

LegendaryOnboarding is a Paper, Purpur, Folia, and Canvas plugin that presents a configurable onboarding sequence to brand-new players and requires them to accept the server rules before continuing.

## Requirements

- Java 25 to build; the generated plugin bytecode remains Java 21-compatible
- Paper, Purpur, Folia, or Canvas
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
- Players who attempt to damage an onboarding player receive a configurable notice.
- Advancement criteria are blocked by default.
- Chat messages are blocked.
- Native chat, broadcasts, and standard join, quit, and advancement
  announcements can be hidden from onboarding players.
- Onboarding players can be hidden from other players' tab lists.
- World interaction is blocked, including outgoing damage, block changes,
  inventories, item use/drop/pickup, entity interaction, and mob targeting.
- Commands are blocked unless they are listed in `ONBOARDSETTINGS.COMMAND_WHITELIST`.
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

The safe-location check requires dry, passable feet and head blocks, solid ground, and avoids common hazards such as water, bubble columns, lava, fire, magma blocks, campfires, cactus, and powder snow. For each return or randomized fallback area, the plugin selects an exposed world surface and never descends through terrain to an otherwise-safe cave or ocean floor. Grass, dirt-family, moss, mud, and leaves are preferred, while safe biome surfaces such as snow, stone, sand, gravel, and ice remain valid when no preferred surface is nearby. Ground within eight blocks of the world build ceiling is rejected. Return resolution then tries the saved location, the optional cleanup fallback, the current world spawn, and finally the primary world spawn.

### Disconnect and restart recovery

- A player who disconnects or is kicked during onboarding has transient onboarding state cleared immediately. The plugin synchronously returns them to their exact saved location and immediately saves their player data so that location remains safe even while the plugin is disabled. If the platform rejects the synchronous return during a disconnect, an asynchronous attempt is made and the pending record remains available for recovery.
- During plugin or server shutdown, online onboarding players receive the same immediate state cleanup, synchronous exact return, and player-data save. Their unfinished pending session remains resumable, so re-enabling the plugin places them back into onboarding rather than treating shutdown as an intentional end request.
- Accepted players bypass onboarding on future joins.
- Stale pending data is cleared when an accepted player joins.
- Acceptance and pending-location data are saved asynchronously and flushed when the plugin disables.

## Commands and Permissions

| Command | Description | Permission |
| --- | --- | --- |
| `/accept` | Accepts the server rules after the rules sequence finishes | `legendaryonboarding.accept` |
| `/legendaryonboarding help` (`/lo help`) | Lists the commands available to the sender | None |
| `/legendaryonboarding reload` (`/lo reload`) | Reloads all configuration files | `legendaryonboarding.reload` |
| `/lo status <playername \| UUID>` | Shows acceptance, pending, and active onboarding state | `legendaryonboarding.status` |
| `/lo debug start <playername \| UUID>` | Forces an online player into onboarding without a first-join announcement | `legendaryonboarding.debug` |
| `/lo debug end <playername \| UUID>` | Releases a player and clears all potion effects plus transient onboarding state | `legendaryonboarding.debug` |
| `/lo debug isAccepted <playername \| UUID> <true \| false \| remove>` | Changes or removes a stored acceptance entry | `legendaryonboarding.debug` |

The accept permission defaults to all players. All management permissions default to server operators.
Running `/lo` without arguments also opens the permission-filtered help list.

## Configuration

Configuration is split by purpose:

- `config.yml` controls the onboarding location, game mode, server name, and command whitelist.
- `ONBOARDSETTINGS.EVENT_PRIORITIES` controls when chat cancellation and onboarding join/quit
  message suppression run relative to other plugins. Changing these values with
  `/lo reload` unregisters and rebuilds the affected listeners.
- `ONBOARDSETTINGS.MESSAGE_CONSUMPTION` controls how join and quit messages are consumed for
  in-game delivery and DiscordSRV-facing native events.
- `discordSRVmessages.yml` contains LegendaryOnboarding's direct DiscordSRV
  templates for first joins, joins, and quits.
- `titlesequence.yml` contains the rules, acceptance prompt, post-acceptance sequence, chat messages, formatting, and timing.

On startup, all configuration files are updated to their bundled layouts while preserving supported values, adding new defaults, and removing obsolete settings. When upgrading from the older single-file layout, sequence settings are migrated from `config.yml` into `titlesequence.yml` before obsolete keys are removed.

| Setting | Purpose |
| --- | --- |
| `SERVER_NAME` | Value inserted by the `{server_name}` sequence placeholder |
| `ONBOARDSETTINGS.GAMEMODE` | Onboarding game mode: `survival`, `creative`, `spectator`, or `adventure` |
| `ONBOARDSETTINGS.TELEPORT` | Whether to move the player to the configured onboarding location |
| `DEBUG` | A list of named diagnostics; use `FORCE_ONBOARDING` to test every join, and add the documented `LOG_*` entries for individual lifecycle logs |
| `ONBOARDSETTINGS.FORCE_RETURNING_PLAYERS` | Onboards returning players whose acceptance state is false or missing; defaults to `false` |
| `ONBOARDSETTINGS.BLOCK_ADVANCEMENTS` | Prevents advancement criteria from being granted during onboarding; defaults to `true` |
| `ONBOARDSETTINGS.BLOCK_EXTERNAL_MESSAGES_DURING_ONBOARDING` | Hides native chat, broadcasts, and standard server announcements from onboarding players |
| `ONBOARDSETTINGS.MESSAGE_CONSUMPTION.IN_GAME.JOIN` / `QUIT` | `NONE`, `SOME`, or `ALL` handling for native join/quit messages shown in game |
| `ONBOARDSETTINGS.MESSAGE_CONSUMPTION.DISCORDSRV.JOIN` / `QUIT` | `NONE`, `SOME`, or `ALL` handling for native join/quit events before DiscordSRV can consume them |
| `ONBOARDSETTINGS.HIDE_ONBOARDING_PLAYERS_FROM_TAB` | Removes onboarding players from other players' tab lists without hiding their in-world entity |
| `ONBOARDSETTINGS.DAMAGE_MESSAGE` | Message sent to someone who attacks an onboarding player; supports `{player}` |
| `ONBOARDSETTINGS.INGAME_FIRST_JOIN_MESSAGE` | Announcement sent after normal onboarding completes; supports `{player}` |
| `ONBOARDSETTINGS.CLEANUP_FALLBACK_ENABLED` | Enables the configured recovery location when a saved destination is missing or unsafe |
| `ONBOARDSETTINGS.CLEANUP_FALLBACK_WORLD`, `ONBOARDSETTINGS.CLEANUP_FALLBACK_X/Y/Z`, `ONBOARDSETTINGS.CLEANUP_FALLBACK_YAW/PITCH` | Optional recovery destination |
| `ONBOARDSETTINGS.CLEANUP_FALLBACK_WORLD_TYPE` | Loaded world environment used when the named cleanup world is unavailable: `NORMAL`, `NETHER`, or `THE_END` |
| `ONBOARDSETTINGS.CLEANUP_FALLBACK_RADIUS` | Randomizes fallback X and Z from `-radius` through `+radius` around the configured coordinates; defaults to `5000`, or use `0` for a fixed fallback |
| `ONBOARDSETTINGS.RETURN_DESIRED_Y` | Preferred height used near the saved X/Z and world-spawn fallbacks; defaults to `64` |
| `ONBOARDSETTINGS.POS_WORLD` | Preferred onboarding world name |
| `ONBOARDSETTINGS.POS_WORLD_TYPE` | Loaded world environment used when `ONBOARDSETTINGS.POS_WORLD` is unavailable: `NORMAL`, `NETHER`, or `THE_END` |
| `ONBOARDSETTINGS.POS_X`, `ONBOARDSETTINGS.POS_Y`, `ONBOARDSETTINGS.POS_Z` | Onboarding coordinates |
| `ONBOARDSETTINGS.POS_YAW`, `ONBOARDSETTINGS.POS_PITCH` | Onboarding view direction |
| `ONBOARDSETTINGS.COMMAND_WHITELIST` | Commands allowed during onboarding, lowercase and without `/` |

Minecraft formatting codes can be used in configured messages. Namespaced commands are normalized before whitelist checks, so `minecraft:help` is checked as `help`.

The bundled sequence applies blindness during onboarding. Minecraft does not
display its fog while the player is in spectator mode, so use `adventure`,
`survival`, or `creative` when blindness should be visible.

For testing on an established server, add `FORCE_ONBOARDING` to `DEBUG`, run
`/lo reload`, then reconnect. Debug mode temporarily activates the title sequence
even when `ENABLED` is `false` in `titlesequence.yml`.
Remove the option and reload when testing is complete.

For narrower testing, omit `FORCE_ONBOARDING` and use
`/lo debug start <player>`. Use `/lo debug end <player>` to restore the player
without accepting the rules or sending the first-join announcement.

Forced cleanup through `debug end`, disabling the sequence, or reconnecting with
the sequence disabled removes every active potion effect. It also clears titles,
action bars, sounds, invisibility, invulnerability, flight, glowing, fire,
freezing, velocity, movement locks, and tab-list hiding. Normal successful
completion removes only potion effects tracked as part of the sequence.

When `debug end` targets an offline player, `pending.yml` stores a
`cleanupRequired` marker and retains any saved return location. The full cleanup
runs on their next login, and the pending entry is removed only after the return
teleport succeeds. Ordinary plugin/server shutdown does not set this marker:
unfinished sessions remain resumable. Pending records also persist whether a
session was debug-forced and whether completion should emit the first-join
announcement.

Return resolution first checks the player's saved X/Z around `ONBOARDSETTINGS.RETURN_DESIRED_Y`.
This avoids returning someone to build height merely because they originally
joined there. The original saved Y remains a later fallback. World-spawn searches
use the same preferred height and only select safe ground with enough room.
When the configured cleanup fallback is reached, the plugin samples safe columns
inside `ONBOARDSETTINGS.CLEANUP_FALLBACK_RADIUS` around its configured X/Z coordinates. X and Z
are randomized independently, giving a square range such as `-5000` through
`+5000` on each axis with the default value. The configured fallback world is
preferred, its configured world type is used when that name is unavailable,
and unsafe sampled columns are skipped.

World destinations resolve the configured world name first. If that world is
not loaded, the plugin selects the first loaded world matching the configured
world type. Cleanup spawn fallbacks are restricted to
`ONBOARDSETTINGS.CLEANUP_FALLBACK_WORLD_TYPE`, so an invalid overworld fallback cannot silently
release a player into the End onboarding world.

Message filtering uses the events exposed by Bukkit and Paper. It covers native
chat, broadcasts, and standard join, quit, and advancement announcements. Death
events are left unchanged so integrations such as DiscordSRV can consume and
forward their messages. Messages sent directly to a player by another plugin
cannot be intercepted without a packet-level dependency.

`ONBOARDSETTINGS.MESSAGE_CONSUMPTION` controls whether the native join or quit event is cleared
before in-game delivery and DiscordSRV can process it. `NONE` leaves the event
untouched, `SOME` consumes only onboarding players' own join/quit messages, and
`ALL` consumes every native join/quit message. Bukkit exposes one shared native
event, so consuming it for either destination prevents the other destination
from receiving that event. When LegendaryOnboarding consumes a DiscordSRV-facing
event, it sends any replacement through its own `discordSRVmessages.yml` template
to DiscordSRV's main mapped channel. The first-join template is sent after normal
onboarding completes; the join and quit templates replace non-onboarding native
events only when the relevant DiscordSRV mode is `ALL`. For in-game delivery,
`ALL` replays a copy only to console and players who are not onboarding.

### DiscordSRV announcement templates

`discordSRVmessages.yml` provides `FIRST_JOIN`, `JOIN`, and `QUIT` templates.
Each can be disabled independently with `ENABLED: false` and supports a DiscordSRV-style
`CONTENT` value plus an optional `EMBED` with color, author, thumbnail, title,
description, image, footer, timestamp, and `FIELDS`. Fields use
`name;value;inline`; use `blank` for an empty field.

Available placeholders are `{player}`, `{display_name}`, `{message}`, `{uuid}`,
`{avatar_url}`, and `{server_name}`. `{avatar_url}` uses DiscordSRV's public
player-avatar resolver, which derives a player-face URL using its configured
avatar provider. LegendaryOnboarding sends these templates
using DiscordSRV's bot and main configured Discord channel. To prevent the
premature native notices, disable the matching `MinecraftPlayerFirstJoinMessage`,
`MinecraftPlayerJoinMessage`, and/or `MinecraftPlayerLeaveMessage` templates in
DiscordSRV's own `messages.yml` when LegendaryOnboarding owns the relevant event.

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
| `ACTIONBAR_REFRESH_TICKS` | How often the countdown action bar is resent, in ticks; defaults to `20` |
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
- `discordSRVmessages.yml`: direct DiscordSRV first-join, join, and quit templates.
- `titlesequence.yml`: the complete player-facing onboarding sequence and its timing defaults.
- `accepted.yml`: accepted UUIDs, up to ten recently seen player names, and the first acceptance timestamp.
- `pending.yml`: saved return locations for players with unfinished onboarding.
- `recovery/playerdata/`: durable pre-onboarding player data used to recover interrupted sessions.

## Building

Use the included Gradle wrapper:

```shell
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

The top-level build compiles and tests the shared code, builds all three
platform jars, and copies the generated artifacts into `dist/`.

The release version is defined in the root `build.gradle`. Packaging with
`build`, `collectPlatformJars`, or a platform `shadowJar` expands that version
into each platform's `plugin.yml`, server plugin/version output, and jar name.

## Testing

Run the automated suite and build all platform artifacts with:

```powershell
.\gradlew.bat clean test collectPlatformJars
```

The manual release checklist is in
[`docs/UAT.md`](docs/UAT.md). It covers first join, debug modes,
message and interaction isolation, damage protection, reload/disable cleanup,
offline recovery, admin commands, persistence, desired-Y return behavior, and
Paper/Folia/Canvas smoke testing.
