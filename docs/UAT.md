# LegendaryOnboarding UAT

Use this checklist before publishing the current platform jars. Run destructive
data-file cases only on a disposable server or after backing up
`plugins/LegendaryOnboarding/`.

## Sign-Off

| Field | Value |
| --- | --- |
| Build or commit | |
| Tester | |
| Date | |
| Paper/Purpur version | |
| Folia version | |
| Canvas version | |
| Result | PASS / FAIL |
| Blocking issues | |

For each test, record `PASS`, `FAIL`, or `N/A` in the Result column and add the
relevant console excerpt or issue number in Notes.

## Test Accounts

- `Admin`: operator with all `legendaryonboarding.*` permissions.
- `Tester`: normal player with only `legendaryonboarding.accept`.
- `Observer`: normal player used to verify tab visibility, announcements, and
  damage behavior.

Before starting, back up and then remove the Tester's entries from
`accepted.yml` and `pending.yml`. Restart the server after editing data files.

## Baseline Configuration

Set the following values, then run `/lo reload`:

```yaml
# config.yml
SERVER_NAME: "UAT Server"
ONBOARD_GAMEMODE: "adventure"
ONBOARD_TELEPORT: true
DEBUG_FORCE_ONBOARDING: false
DEBUG_LOGGING: false
ONBOARD_UNACCEPTED_RETURNING_PLAYERS: false
BLOCK_ADVANCEMENTS: true
BLOCK_EXTERNAL_MESSAGES_DURING_ONBOARDING: true
MESSAGE_CONSUMPTION:
  IN_GAME:
    JOIN: "SOME"
    QUIT: "SOME"
  DISCORDSRV:
    JOIN: "SOME"
    QUIT: "SOME"
HIDE_ONBOARDING_PLAYERS_FROM_TAB: true
ONBOARDING_DAMAGE_MESSAGE: "{yellow}{player} is currently onboarding."
FIRST_JOIN_MESSAGE: "{yellow}{player} joined the server for the first time"
CLEANUP_FALLBACK_ENABLED: true
CLEANUP_FALLBACK_WORLD: "world"
CLEANUP_FALLBACK_WORLD_TYPE: "NORMAL"
CLEANUP_FALLBACK_X: 0
CLEANUP_FALLBACK_Y: 64
CLEANUP_FALLBACK_Z: 0
CLEANUP_FALLBACK_RADIUS: 5000
RETURN_DESIRED_Y: 64
POS_WORLD: "world"
POS_WORLD_TYPE: "NORMAL"
POS_X: 0
POS_Y: 120
POS_Z: 0
```

```yaml
# titlesequence.yml
ENABLED: true
ACTIONBAR_COUNTDOWN: "{yellow}/accept unlocks in {gold}{seconds}{yellow}s"
ACTIONBAR_REFRESH_TICKS: 20
```

Use a short sequence during repeated testing, but keep at least one welcome
step, two rules steps, one prompt step, and one post-acceptance step. Configure
one sound and one potion effect that can be observed easily.

Create:

- A safe two-block-high landing area near `world, 0, 64, 0`.
- An unsafe column at the same X/Z so at least one test can verify vertical
  searching.
- A safe platform near maximum build height for the max-Y return test.
- A disposable onboarding location where void damage can be tested safely.

## Automated Gate

| ID | Test | Expected | Result | Notes |
| --- | --- | --- | --- | --- |
| AUT-01 | Run `.\gradlew.bat clean test collectPlatformJars`. | All tests pass and Paper, Folia, and Canvas jars are present in `dist/`. | | |
| AUT-02 | Inspect each generated `plugin.yml` or run `/version LegendaryOnboarding` on each platform. | Plugin version matches the root build configuration; `/lo` is registered as an alias. | | |

## Configuration And Reload

| ID | Test | Expected | Result | Notes |
| --- | --- | --- | --- | --- |
| CFG-01 | Set `titlesequence.yml` `ENABLED: false`, set `POS_WORLD` to a missing world, and restart. | Plugin remains enabled because the onboarding world is not required while the sequence is disabled. | | |
| CFG-02 | Restore the baseline, run `/lo reload`, and inspect the console. | Both config files reload without an exception. | | |
| CFG-03 | As Tester, run `/lo reload`. | Command is denied and configuration is unchanged. | | |
| CFG-04 | Type `/lo `, `/lo re`, `/lo debug `, and `/lo debug isAccepted Tester `. | Tab completion offers the appropriate subcommands, players, and `true`, `false`, `remove` values. | | |
| CFG-05 | Start onboarding, set `ENABLED: false`, and run `/lo reload`. | The active player is fully released and their pending cleanup completes. | | |
| CFG-06 | Run `/lo` and `/lo help` as Tester, then as Admin. | Both forms show help. Tester sees only `/accept` and `/lo help`; Admin sees reload, status, and every debug command. | | |
| CFG-07 | Set `POS_WORLD` to a missing name and `POS_WORLD_TYPE: NORMAL`, then start onboarding while a normal world is loaded. | The player is teleported to the configured coordinates in a loaded normal world, not left in their current world. | | |
| CFG-08 | Set `CLEANUP_FALLBACK_WORLD` to a missing name and `CLEANUP_FALLBACK_WORLD_TYPE: NORMAL`; complete onboarding from an End onboarding world with no valid saved return. | Cleanup uses a loaded normal world and never releases the player into the End onboarding world. | | |

## Entry And Sequence

| ID | Test | Expected | Result | Notes |
| --- | --- | --- | --- | --- |
| ENT-01 | Join as a brand-new Tester. | Normal join message is suppressed; Tester enters onboarding at the configured location. | | |
| ENT-02 | Observe the welcome and rules sequence. | Configured titles, subtitles, chat, sound, potion effects, and timings appear in order. `{server_name}` and `{player_name}` resolve correctly. | | |
| ENT-03 | Watch the action bar through the rules sequence. | Countdown decreases once per second and `/accept` is unavailable until it completes. | | |
| ENT-04 | Run `/accept` before the countdown completes. | Player receives the not-ready response; no exception occurs and onboarding remains active. | | |
| ENT-05 | Put `{newline}` in configured chat and in a subtitle. | Chat renders a line break. The subtitle renders a space and no LF control icon. | | |
| ENT-06 | Set `DEBUG_FORCE_ONBOARDING: true`, reload, and reconnect an accepted Tester. | Tester enters onboarding without deleting the existing accepted record. | | |
| ENT-07 | Disable force mode, enable `ONBOARD_UNACCEPTED_RETURNING_PLAYERS`, set Tester to unaccepted, and reconnect. | Returning unaccepted Tester enters onboarding. | | |
| ENT-08 | Disable both testing toggles and reconnect a returning unaccepted Tester with no pending entry. | Tester is not automatically onboarded. | | |

## Isolation And Protection

| ID | Test | Expected | Result | Notes |
| --- | --- | --- | --- | --- |
| ISO-01 | Damage onboarding Tester with melee and a projectile from Observer. | Tester takes no damage. Observer receives the configured message naming Tester, with cooldown preventing spam. | | |
| ISO-02 | Place onboarding Tester in the void for at least five seconds. | Void damage is cancelled and Tester remains alive. | | |
| ISO-03 | Join and quit while onboarding with the baseline message-consumption settings. | No damage is taken during either transition and no standard join/quit message is shown in game or forwarded through DiscordSRV. | | |
| ISO-04 | Try moving, turning, attacking, breaking/placing blocks, opening inventories, using/dropping/picking up items, and interacting with entities. | Movement and view remain locked; every listed world interaction is blocked. | | |
| ISO-05 | Run a non-whitelisted command and then `/accept`. | Non-whitelisted command is blocked; `/accept` reaches the plugin. | | |
| ISO-06 | Send chat, a server broadcast, and a standard advancement announcement while Tester is onboarding. | Tester does not see the external messages; plugin sequence messages remain visible. | | |
| ISO-07 | With DiscordSRV enabled, cause a player death while another player is onboarding. | The death message remains visible in game and is delivered to the configured Discord channel. | | |
| ISO-08 | Mark Tester accepted, then run `/lo debug start Tester` and try sending and receiving chat. | Tester cannot send or receive chat while actively onboarding, regardless of prior acceptance. Normal chat resumes after debug end. | | |
| ISO-09 | Join and quit as a completed, non-onboarding Tester with the baseline message-consumption settings. | The native join and quit messages remain visible in game and are forwarded by DiscordSRV. | | |
| ISO-10 | Trigger an advancement criterion while onboarding. | Advancement is not granted. | | |
| ISO-11 | Compare the player list from Observer and Tester. | Observer cannot see Tester in the tab list. Tester can still see appropriate non-onboarding players. | | |

## Completion And Return

| ID | Test | Expected | Result | Notes |
| --- | --- | --- | --- | --- |
| END-01 | After the countdown, run `/accept` once. | Post-acceptance sequence runs once, acceptance is saved, and the first-join message is broadcast after completion. | | |
| END-02 | Spam `/accept` during completion. | Completion does not duplicate, no exception is logged, and only one announcement is sent. | | |
| END-03 | After completion, move and turn, take damage after the temporary protection expires, inspect tab visibility, and check flight/invisibility/invulnerability. | Player is fully released; normal movement, visibility, damage, and flight rules are restored. | | |
| END-04 | Inspect potion effects after normal completion. | Sequence-managed effects are removed. Unrelated pre-existing effects are not removed by normal completion. | | |
| END-05 | Reconnect the accepted Tester with testing toggles disabled. | Tester bypasses onboarding and any stale pending entry is cleared. | | |
| END-06 | Complete onboarding with DiscordSRV enabled and `MESSAGE_CONSUMPTION.DISCORDSRV.JOIN: SOME`. | The configured first-join announcement is visible in game and sent through DiscordSRV using its configured first-join format and destination. | | |

## Recovery And Cleanup

| ID | Test | Expected | Result | Notes |
| --- | --- | --- | --- | --- |
| REC-01 | Disconnect during onboarding and reconnect with the sequence enabled. | Tester resumes onboarding using the existing pending return location. | | |
| REC-02 | Disconnect during onboarding, stop the server, disable the sequence, restart, and reconnect. | Full cleanup runs on login; Tester is returned safely and is not locked, hidden, invulnerable, or left flying. | | |
| REC-03 | Run `/lo debug end Tester` while Tester is online and has several potion effects. | All potion effects and all transient onboarding state are cleared; no first-join message is sent. | | |
| REC-04 | Start Tester onboarding, disconnect, then run `/lo debug end Tester`. Reconnect Tester. | `pending.yml` records cleanup while offline. Cleanup and return run on login, then the pending entry is removed. | | |
| REC-05 | Repeat REC-04, but make the saved world unavailable before reconnecting. | Configured cleanup fallback is used. Player is not released at the onboarding location. | | |
| REC-06 | Make the saved destination and configured fallback unsafe or unavailable. | A safe current-world or primary-world spawn is used. If no safe destination exists, pending cleanup is retained and a clear error is logged. | | |
| REC-07 | Reload or disable the plugin while multiple players are onboarding or accepting. | Players already in forced cleanup retain their cleanup marker. Ordinary unfinished onboarding sessions remain resumable; no movement locks or protection state remain while the plugin is disabled. | | |
| REC-08 | Restart a Folia server while at least one player is actively onboarding. | Plugin disable completes without `IllegalPluginAccessException`; unfinished pending sessions remain resumable after restart. | | |
| REC-09 | Disconnect normally during onboarding, disable the plugin before reconnecting, then reconnect. | Player data was saved at the exact original location with onboarding effects cleared; the player does not appear at the onboarding anchor or an unsafe fallback. | | |
| REC-10 | Kick a player during onboarding, disable the plugin, then reconnect. | The exact original location and cleared player state were durably saved before removal; the pending record remains resumable. | | |
| REC-11 | Stop the server with an online onboarding player, disable the plugin before the next login, then start the server. | Shutdown durably saves the exact original location and cleared state, preserves the resumable pending session, and logs no task-registration exception. | | |
| REC-12 | Begin onboarding, disconnect, disable the plugin at the server level, join and leave while it is disabled, re-enable it, then join again. | The player resumes onboarding using the original pending return location. They are not released, marked accepted, or safety-teleported to a new location. | | |
| REC-13 | Begin onboarding, disconnect, restart the server, disable the plugin before login, then reconnect while it remains disabled. | The player appears at the exact saved pre-onboarding location, not the onboarding location. | | |
| REC-14 | Begin onboarding, disable the plugin while the player is online, then reconnect while the plugin remains disabled. | The player appears at the exact saved pre-onboarding location with normal game mode, visibility, flight, invulnerability, and potion state. | | |

## Return Height Safety

| ID | Test | Expected | Result | Notes |
| --- | --- | --- | --- | --- |
| RET-01 | Start Tester at maximum build height above a safe area near Y=64, then complete onboarding. | Return search uses the saved X/Z near `RETURN_DESIRED_Y`; Tester is not returned to build height merely because that was the original Y. | | |
| RET-02 | Block the exact desired Y while leaving a nearby lower or higher safe level. | The nearest safe level is selected, preferring the lower level when distances are equal. | | |
| RET-03 | Make all levels near desired Y unsafe but leave the original saved Y safe. | Original saved Y is used after the desired-Y search fails. | | |
| RET-04 | Remove the saved world and verify the configured fallback. | Safe configured fallback is used before world-spawn fallbacks. | | |
| RET-05 | Set fallback X/Z to `0/0`, radius to `5000`, invalidate the saved destination, and repeat cleanup several times. | Every fallback return is on safe ground with X and Z each between `-5000` and `+5000`; results vary across runs. | | |
| RET-06 | Set `CLEANUP_FALLBACK_RADIUS: 0` and repeat RET-05. | The fixed fallback area is used without wide randomization. | | |
| RET-07 | Use a test column with a safe stone cave floor below a safe grass, dirt-family, or leaves surface. Complete onboarding or trigger fallback at that X/Z. | Tester is placed above the highest preferred surface and is not placed on the cave floor. | | |
| RET-08 | Test a snowy mountain with a cave near `RETURN_DESIRED_Y`, where the exposed surface is substantially higher. | Tester is placed on the safe exposed snow/stone/ice surface rather than the cave floor. | | |
| RET-09 | Use a return or random fallback coordinate in an ocean, including a kelp or bubble-column area. | The submerged column is rejected and Tester is returned to a dry exposed surface elsewhere. | | |

## Admin Commands And Persistence

| ID | Test | Expected | Result | Notes |
| --- | --- | --- | --- | --- |
| ADM-01 | Run `/lo status Tester` and `/lo status <Tester UUID>` while inactive, active, pending offline cleanup, and accepted. | Output accurately reports acceptance, pending, cleanup, debug, and active state for both lookup forms. | | |
| ADM-02 | Run `/lo debug start Tester`. | Online Tester enters onboarding without a first-join announcement. Offline targets are rejected clearly. | | |
| ADM-03 | Run `/lo debug isAccepted Tester true`, `false`, then `remove`, checking `/lo status` after each. | Stored acceptance changes exactly as requested; `remove` deletes the entry rather than storing false. | | |
| ADM-04 | Restart after changing accepted and pending state. | `accepted.yml` and `pending.yml` preserve the intended UUID state and reload without data loss. | | |
| ADM-05 | Run all management commands as Tester. | Commands are denied without the matching admin permission. | | |
| ADM-06 | Run `/lo debug start Tester` while Tester is already onboarding. | Command is rejected and no second sequence or exception occurs. | | |
| ADM-07 | Run `/lo debug end Tester` while Tester is not onboarding. | Command is rejected and no player state is changed. | | |
| ADM-08 | On Folia, run `/lo debug start Tester` from console or an admin in a different region. | Onboarding starts on Tester's entity scheduler without a thread-check exception. | | |

## Platform Smoke Test

Run these cases on Paper/Purpur, Folia, and Canvas:

- `CFG-02`: reload.
- `ENT-01`: onboarding entry.
- `ISO-01`: damage cancellation.
- `END-01`: acceptance and return.
- `REC-04`: offline cleanup.
- `RET-01`: desired-Y return.

There must be no scheduler/thread-access warnings, command exceptions, or task
exceptions in the console.

## Release Criteria

The release passes when:

- `AUT-01` and `AUT-02` pass.
- Every non-platform-specific UAT case passes on Paper or Purpur.
- The platform smoke test passes on Folia and Canvas.
- No player remains locked, invisible, invulnerable, flying, hidden from tab, or
  affected by onboarding potion effects after completion or forced cleanup.
- Saved return locations are never discarded before a successful return.
- No uncaught exception appears during join, quit, reload, accept, debug cleanup,
  or failed-destination recovery.
