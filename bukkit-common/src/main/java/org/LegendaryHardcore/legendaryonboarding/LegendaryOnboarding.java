package org.LegendaryHardcore.legendaryonboarding;

// Import plugin content
import org.LegendaryHardcore.legendaryonboarding.storage.AcceptedStore;
import org.LegendaryHardcore.legendaryonboarding.storage.DeferredDebugFixStore;
import org.LegendaryHardcore.legendaryonboarding.storage.PendingStore;
import org.LegendaryHardcore.legendaryonboarding.storage.PlayerDataSnapshotStore;
import org.LegendaryHardcore.legendaryonboarding.command.PlayerAccept;
import org.LegendaryHardcore.legendaryonboarding.command.ReloadCommand;
import org.LegendaryHardcore.legendaryonboarding.listener.PlayerCommandBlocker;
import org.LegendaryHardcore.legendaryonboarding.listener.PlayerDamageBlocker;
import org.LegendaryHardcore.legendaryonboarding.listener.PlayerAdvancementBlocker;
import org.LegendaryHardcore.legendaryonboarding.listener.ExternalMessageBlocker;
import org.LegendaryHardcore.legendaryonboarding.listener.StandardAnnouncementBlocker;
import org.LegendaryHardcore.legendaryonboarding.listener.PlayerInteractionBlocker;
import org.LegendaryHardcore.legendaryonboarding.listener.MessageResponsibilityManager;
import org.LegendaryHardcore.legendaryonboarding.platform.ServerScheduler;
import org.LegendaryHardcore.legendaryonboarding.platform.TaskHandle;

import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Location;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;
import java.util.function.Supplier;

/*
 *   Main Onboard plugin class
 */
public abstract class LegendaryOnboarding extends JavaPlugin {
    private static final int RANDOM_FALLBACK_ATTEMPTS = 24;

    // Track whether each player can use /accept
    public final Map<UUID, Boolean> canAcceptRules = new ConcurrentHashMap<>();

    private RulesSequence rulesSequence;
    public RulesSequence getRulesSequence() {
        return this.rulesSequence;
    }

    private JoinSequence joinSequence;
    public JoinSequence getJoinSequence() {
        return this.joinSequence;
    }

    private ConfigData config;
    public ConfigData getConfigData() {
        return this.config;
    }

    public boolean isDebugLoggingEnabled() {
        return config != null && config.isDebugLogging();
    }

    public void debugLog(String message) {
        if (!isDebugLoggingEnabled()) return;
        getLogger().info("[Debug] " + message);
    }

    public void debugLog(Supplier<String> messageSupplier) {
        if (!isDebugLoggingEnabled()) return;
        getLogger().info("[Debug] " + messageSupplier.get());
    }

    public void debugActionBar(Player player, String action, String detail) {
        if (!isDebugLoggingEnabled()) return;
        getLogger().info("[Debug] ActionBar " + action
                + " player=" + player.getName()
                + " detail=" + detail);
    }

    private AcceptedStore acceptedStore;
    public AcceptedStore getAcceptedStore() { return acceptedStore; }

    private PendingStore pendingStore;
    public PendingStore getPendingStore() { return pendingStore; }

    private PlayerDataSnapshotStore playerDataSnapshotStore;
    private DeferredDebugFixStore deferredDebugFixStore;

    public final ConcurrentMap<UUID, TaskHandle> movementLocks = new ConcurrentHashMap<>();
    public final ConcurrentMap<UUID, TaskHandle> countdownTasks = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<UUID, Boolean> acceptInProgress = new ConcurrentHashMap<>();
    public final java.util.Set<UUID> joinSequenceActive = java.util.concurrent.ConcurrentHashMap.newKeySet();
    public final java.util.Set<UUID> debugForcedPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();
    public final java.util.Set<UUID> suppressedJoinMessages =
            java.util.concurrent.ConcurrentHashMap.newKeySet();
    public final java.util.Set<UUID> softProtectedPlayers =
            java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.Set<UUID> forcedCleanupPlayers =
            java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.Set<TabVisibilityPair> hiddenTabPlayers =
            java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.Set<UUID> departureCleanupPlayers =
            java.util.concurrent.ConcurrentHashMap.newKeySet();
    public final ConcurrentMap<UUID, java.util.Set<PotionEffectType>> sequencePotionEffects =
            new ConcurrentHashMap<>();

    private ServerScheduler platformScheduler;
    private MessageResponsibilityManager messageResponsibilityManager;

    protected abstract ServerScheduler createPlatformScheduler();

    public ServerScheduler getPlatformScheduler() {
        return platformScheduler;
    }

    @Override
    public void onEnable() {
        platformScheduler = createPlatformScheduler();
        saveDefaultConfig();
        if (!reloadPluginConfig()) {
            getLogger().severe("Could not load config.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize storage classes
        this.acceptedStore = new AcceptedStore(this);
        this.pendingStore = new PendingStore(this);
        this.playerDataSnapshotStore = new PlayerDataSnapshotStore(this);
        this.deferredDebugFixStore = new DeferredDebugFixStore(this);

        this.acceptedStore.load();
        this.pendingStore.load();
        restoreOfflineRequiredSnapshots();

        // Initialize sequences classes
        this.rulesSequence = new RulesSequence(this);
        this.joinSequence = new JoinSequence(this);
        this.messageResponsibilityManager = new MessageResponsibilityManager(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerCommandBlocker(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDamageBlocker(this), this);
        getServer().getPluginManager().registerEvents(new PlayerAdvancementBlocker(this), this);
        getServer().getPluginManager().registerEvents(new ExternalMessageBlocker(this), this);
        getServer().getPluginManager().registerEvents(new StandardAnnouncementBlocker(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractionBlocker(this), this);
        messageResponsibilityManager.activate(config.getEventPriorities());

        var acceptCmd = getCommand("accept");
        if (acceptCmd == null) {
            getLogger().severe("Command 'accept' is missing from plugin.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        acceptCmd.setExecutor(new PlayerAccept(this));

        var reloadCmd = getCommand("legendaryonboarding");
        if (reloadCmd == null) {
            getLogger().severe("Command 'legendaryonboarding' is missing from plugin.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        var reloadCommand = new ReloadCommand(this);
        reloadCmd.setExecutor(reloadCommand);
        reloadCmd.setTabCompleter(reloadCommand);

        for (Player player : getServer().getOnlinePlayers()) {
            platformScheduler.runEntity(
                    player,
                    () -> reconcilePendingPlayerAfterEnable(player)
            );
        }

        getLogger().info("LegendaryOnboarding enabled.");
    }

    @Override
    public void onDisable() {
        if (messageResponsibilityManager != null) {
            messageResponsibilityManager.deactivate();
        }
        if (pendingStore != null) {
            java.util.Set<UUID> onboardingPlayers = allOnboardingPlayers();
            // A normal shutdown must preserve resumable onboarding sessions.
            // Keep (or persist) a cleanup marker only for cleanup already in
            // progress, including a cleanup whose asynchronous work has not
            // completed yet.
            for (UUID uuid : onboardingPlayers) {
                if (shouldMarkCleanupOnDisable(
                        pendingStore.isCleanupRequired(uuid),
                        forcedCleanupPlayers.contains(uuid)
                )) {
                    pendingStore.markCleanupRequired(uuid);
                }
            }
            for (Player player : getServer().getOnlinePlayers()) {
                if (onboardingPlayers.contains(player.getUniqueId())) {
                    try {
                        playerDataSnapshotStore.markRestoreRequired(
                                player.getUniqueId()
                        );
                        restorePlayerStateForDeparture(player, true);
                    } catch (RuntimeException exception) {
                        getLogger().warning(
                                "Could not fully restore " + player.getName()
                                        + " during shutdown: " + exception.getMessage()
                        );
                    }
                }
            }
            restoreOfflineRequiredSnapshots();
        }
        restoreAllTabPlayersOnDisable();
        canAcceptRules.clear();
        acceptInProgress.clear();
        joinSequenceActive.clear();
        debugForcedPlayers.clear();
        suppressedJoinMessages.clear();
        hiddenTabPlayers.clear();
        departureCleanupPlayers.clear();
        softProtectedPlayers.clear();
        forcedCleanupPlayers.clear();
        sequencePotionEffects.clear();
        countdownTasks.values().forEach(TaskHandle::cancel);
        countdownTasks.clear();
        movementLocks.clear();

        if (pendingStore != null) pendingStore.flushNow();
        if (acceptedStore != null) acceptedStore.flushNow();
    }


    public void startMovementLock(Player player, Location anchor, boolean lockView) {
        final UUID uuid = player.getUniqueId();

        // cancel old lock if any
        TaskHandle old = movementLocks.remove(uuid);
        if (old != null) old.cancel();

        final Location fixed = anchor.clone();
        fixed.setX(fixed.getBlockX() + 0.5);
        fixed.setZ(fixed.getBlockZ() + 0.5);

        TaskHandle task = platformScheduler.runEntityAtFixedRate(player, () -> {
            if (!player.isOnline()) {
                stopMovementLock(uuid);
                return;
            }

            // stop movement
            player.setVelocity(player.getVelocity().zero());
            player.setFallDistance(0f);

            Location cur = player.getLocation();
            if (cur.getWorld() != fixed.getWorld()) {
                player.teleportAsync(fixed);
                return;
            }

            // snap if drifted
            double dx = cur.getX() - fixed.getX();
            double dy = cur.getY() - fixed.getY();
            double dz = cur.getZ() - fixed.getZ();

            if (Math.abs(dx) > 0.05 || Math.abs(dy) > 0.05 || Math.abs(dz) > 0.05) {
                Location snap = fixed.clone();
                if (lockView) {
                    snap.setYaw(fixed.getYaw());
                    snap.setPitch(fixed.getPitch());
                } else {
                    snap.setYaw(cur.getYaw());
                    snap.setPitch(cur.getPitch());
                }
                player.teleportAsync(snap);
                return;
            }

            // lock view even if no drift (prevents looking around)
            if (lockView) {
                float yaw = fixed.getYaw();
                float pitch = fixed.getPitch();

                // only adjust if changed a bit, avoids extra work
                if (Math.abs(cur.getYaw() - yaw) > 2.0f || Math.abs(cur.getPitch() - pitch) > 2.0f) {
                    Location snap = cur.clone();
                    snap.setYaw(yaw);
                    snap.setPitch(pitch);
                    player.teleportAsync(snap);
                }
            }
        }, () -> movementLocks.remove(uuid), 1L, 2L);

        movementLocks.put(uuid, task);
    }

    public void stopMovementLock(UUID uuid) {
        TaskHandle task = movementLocks.remove(uuid);
        if (task != null) task.cancel();
    }

    public void setCountdownTask(UUID uuid, TaskHandle task) {
        TaskHandle old = countdownTasks.put(uuid, task);
        if (old != null) old.cancel();
    }

    public void stopCountdown(UUID uuid) {
        TaskHandle task = countdownTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    public boolean isOnboardingActive(UUID uuid) {
        return canAcceptRules.containsKey(uuid)
                || joinSequenceActive.contains(uuid)
                || (pendingStore != null
                && pendingStore.hasPending(uuid)
                && !pendingStore.isCleanupRequired(uuid));
    }

    public boolean startOnboarding(Player player, boolean announceWhenComplete, boolean debugForced) {
        return startOnboarding(player, announceWhenComplete, debugForced, false);
    }

    public boolean resumeOnboardingFromCurrentLocation(
            Player player,
            boolean announceWhenComplete,
            boolean debugForced
    ) {
        return startOnboarding(player, announceWhenComplete, debugForced, false);
    }

    private boolean startOnboarding(
            Player player,
            boolean announceWhenComplete,
            boolean debugForced,
            boolean refreshReturnLocation
    ) {
        if (!config.isTitleSequenceEnabled()) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        departureCleanupPlayers.remove(uuid);
        forcedCleanupPlayers.remove(uuid);
        stopCountdown(uuid);
        stopMovementLock(uuid);
        acceptInProgress.remove(uuid);
        joinSequenceActive.remove(uuid);
        canAcceptRules.put(uuid, false);
        player.resetTitle();
        player.sendActionBar(net.kyori.adventure.text.Component.empty());
        debugActionBar(player, "clear", "startOnboarding");
        clearSequencePotionEffects(player);

        boolean newReturnLocation = refreshReturnLocation
                || !pendingStore.hasPending(uuid)
                || pendingStore.isCleanupRequired(uuid);
        debugLog(() -> "startOnboarding player=" + player.getName()
                + " refreshReturnLocation=" + refreshReturnLocation
                + " hasPending=" + pendingStore.hasPending(uuid)
                + " cleanupRequired=" + pendingStore.isCleanupRequired(uuid)
                + " announceWhenComplete=" + announceWhenComplete
                + " debugForced=" + debugForced);
        if (!newReturnLocation) {
            pendingStore.configureSession(uuid, debugForced, announceWhenComplete);
            debugLog(() -> "Reusing saved return location for " + player.getName());
            beginOnboardingSequence(player, announceWhenComplete, debugForced);
            return true;
        }

        protectPlayerDuringReturnPreparation(player);
        Location current = player.getLocation().clone();
        if (SafeLocationFinder.isPhysicallySafe(current)) {
            captureReturnLocationAndBegin(
                    player,
                    current,
                    announceWhenComplete,
                    debugForced
            );
            return true;
        }

        resolveSafeReturnLocation(player, current, safe -> {
            if (safe == null) {
                getLogger().warning(
                        "Could not find a safe pre-onboarding return location for "
                                + player.getName() + "; onboarding was not started."
                );
                clearRuntimeState(uuid);
                player.setInvulnerable(false);
                restoreOnboardingPlayerToTab(player);
                return;
            }

            player.teleportAsync(safe).whenComplete((success, error) ->
                    platformScheduler.runEntity(player, () -> {
                        if (error != null || !Boolean.TRUE.equals(success)) {
                            getLogger().warning(
                                    "Could not move " + player.getName()
                                            + " to a safe pre-onboarding return location; "
                                            + "onboarding was not started."
                            );
                            clearRuntimeState(uuid);
                            player.setInvulnerable(false);
                            restoreOnboardingPlayerToTab(player);
                            return;
                        }

                        captureReturnLocationAndBegin(
                                player,
                                safe,
                                announceWhenComplete,
                                debugForced
                        );
                    })
            );
        });
        return true;
    }

    private void captureReturnLocationAndBegin(
            Player player,
            Location returnLocation,
            boolean announceWhenComplete,
            boolean debugForced
    ) {
        if (!player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        playerDataSnapshotStore.captureNewSession(player);
        pendingStore.setPendingAndFlush(
                uuid,
                returnLocation,
                debugForced,
                announceWhenComplete
        );
        debugLog(() -> "Captured return location for " + player.getName()
                + " at " + formatDebugLocation(returnLocation));
        beginOnboardingSequence(player, announceWhenComplete, debugForced);
    }

    private void protectPlayerDuringReturnPreparation(Player player) {
        player.setInvulnerable(true);
        player.setFireTicks(0);
        player.setFallDistance(0f);
    }

    private void beginOnboardingSequence(
            Player player,
            boolean announceWhenComplete,
            boolean debugForced
    ) {
        UUID uuid = player.getUniqueId();
        if (debugForced) {
            debugForcedPlayers.add(uuid);
        } else {
            debugForcedPlayers.remove(uuid);
        }
        if (announceWhenComplete) {
            suppressedJoinMessages.add(uuid);
        } else {
            suppressedJoinMessages.remove(uuid);
        }

        player.setInvulnerable(true);
        player.setFireTicks(0);
        player.setFallDistance(0f);
        hideOnboardingPlayerFromTab(player);
        debugLog(() -> "Beginning onboarding sequence for " + player.getName()
                + " announceWhenComplete=" + announceWhenComplete
                + " debugForced=" + debugForced);
        platformScheduler.runEntity(player, () -> rulesSequence.start(player));
    }

    public void preparePlayerForDeparture(Player player) {
        UUID uuid = player.getUniqueId();
        if (softProtectedPlayers.remove(uuid)) {
            clearSoftProtectionState(player);
            return;
        }
        boolean hasPending = pendingStore != null && pendingStore.hasPending(uuid);
        if (!shouldRestoreDepartingPlayer(isOnboardingActive(uuid), hasPending)) {
            forgetTabVisibility(player);
            return;
        }
        if (!departureCleanupPlayers.add(uuid)) return;

        playerDataSnapshotStore.markRestoreRequired(uuid);
        restorePlayerStateForDeparture(player, false);
        if (!player.isOnline() && playerDataSnapshotStore != null) {
            playerDataSnapshotStore.restoreIfRequired(uuid);
        }
        forgetTabVisibility(player);
    }

    public void deferPlayerDepartureCleanup(Player player) {
        UUID uuid = player.getUniqueId();
        softProtectedPlayers.remove(uuid);
        boolean forcedCleanupPending = forcedCleanupPlayers.contains(uuid);

        boolean hasPending = pendingStore != null && pendingStore.hasPending(uuid);
        if (!shouldRestoreDepartingPlayer(isOnboardingActive(uuid), hasPending)) {
            clearRuntimeState(uuid);
            forgetTabVisibility(player);
            return;
        }
        if (!departureCleanupPlayers.add(uuid)) return;

        if (playerDataSnapshotStore != null) {
            playerDataSnapshotStore.markRestoreRequired(uuid);
        }
        if (hasPending && !shouldPreserveCleanupOnDeferredDeparture(forcedCleanupPending)) {
            pendingStore.configureSession(
                    uuid,
                    debugForcedPlayers.contains(uuid) || pendingStore.isDebugSession(uuid),
                    suppressedJoinMessages.contains(uuid)
                            || pendingStore.shouldAnnounceWhenComplete(uuid)
            );
        }
        // Ordinary disconnects must remain resumable. Reserve cleanupRequired
        // for explicit cleanup flows such as debug end or plugin/server shutdown.
        debugLog(() -> "Deferring departure cleanup for " + player.getName()
                + " hasPending=" + hasPending
                + " onboardingActive=" + isOnboardingActive(uuid)
                + " forcedCleanupPending=" + forcedCleanupPending);
        clearRuntimeState(uuid);
        forgetTabVisibility(player);
    }

    public void beginPlayerSession(UUID uuid) {
        departureCleanupPlayers.remove(uuid);
        forcedCleanupPlayers.remove(uuid);
    }

    public void markForcedCleanup(UUID uuid) {
        forcedCleanupPlayers.add(uuid);
    }

    public void clearForcedCleanup(UUID uuid) {
        forcedCleanupPlayers.remove(uuid);
    }

    public void queueDeferredDebugFix(UUID uuid) {
        deferredDebugFixStore.mark(uuid);
    }

    public boolean consumeDeferredDebugFix(Player player) {
        UUID uuid = player.getUniqueId();
        if (!deferredDebugFixStore.consume(uuid)) {
            return false;
        }
        applyDebugFix(player);
        return true;
    }

    public void scheduleDepartureSnapshotRestore(UUID uuid) {
        if (pendingStore == null
                || playerDataSnapshotStore == null
                || !pendingStore.hasPending(uuid)) {
            return;
        }

        try {
            platformScheduler.runAsyncDelayed(() -> {
                // beginPlayerSession removes this marker before a newly joined
                // player can begin onboarding again.
                if (departureCleanupPlayers.contains(uuid)
                        && pendingStore.hasPending(uuid)) {
                    playerDataSnapshotStore.restoreIfRequired(uuid);
                }
            }, 250L, TimeUnit.MILLISECONDS);
        } catch (RuntimeException ignored) {
            // Schedulers may already reject work during shutdown. onDisable
            // and the next onEnable both retry restoration from the snapshot.
        }
    }

    private void reconcilePendingPlayerAfterEnable(Player player) {
        UUID uuid = player.getUniqueId();
        if (!pendingStore.hasPending(uuid)) return;

        boolean cleanupRequired = pendingStore.isCleanupRequired(uuid);
        boolean sequenceEnabled = config.isTitleSequenceEnabled();
        boolean accepted = acceptedStore.isAccepted(uuid);
        boolean forceOnboarding = config.isDebugForceOnboarding();
        if (!shouldResumePendingPlayer(
                sequenceEnabled,
                cleanupRequired,
                accepted,
                forceOnboarding
        )) {
            releasePendingPlayer(player);
            return;
        }

        boolean debugSession =
                forceOnboarding || pendingStore.isDebugSession(uuid);
        boolean announceWhenComplete =
                !debugSession && pendingStore.shouldAnnounceWhenComplete(uuid);
        resumeOnboardingFromCurrentLocation(
                player,
                announceWhenComplete,
                debugSession
        );
    }

    static boolean shouldResumePendingPlayer(
            boolean sequenceEnabled,
            boolean cleanupRequired,
            boolean accepted,
            boolean forceOnboarding
    ) {
        return sequenceEnabled
                && !cleanupRequired
                && (!accepted || forceOnboarding);
    }

    static boolean shouldRestoreDepartingPlayer(
            boolean onboardingActive,
            boolean hasPending
    ) {
        return onboardingActive || hasPending;
    }

    static boolean shouldPreserveCleanupOnDeferredDeparture(boolean forcedCleanupPending) {
        return forcedCleanupPending;
    }

    static boolean shouldMarkCleanupOnDisable(
            boolean cleanupAlreadyRequired,
            boolean forcedCleanupInProgress
    ) {
        return cleanupAlreadyRequired || forcedCleanupInProgress;
    }

    private CompletableFuture<Boolean> restorePlayerStateForDeparture(
            Player player,
            boolean shuttingDown
    ) {
        UUID uuid = player.getUniqueId();
        stopMovementLock(uuid);
        stopCountdown(uuid);
        clearAllPlayerEffects(player);
        clearRuntimeState(uuid);
        if (!shuttingDown) {
            restoreOnboardingPlayerToTab(player);
        }

        Location saved = pendingStore == null
                ? null
                : pendingStore.getPendingLocation(uuid);
        if (saved == null) return CompletableFuture.completedFuture(false);

        try {
            if (player.teleport(saved)) {
                player.saveData();
                return CompletableFuture.completedFuture(true);
            }
        } catch (RuntimeException synchronousError) {
            if (shuttingDown) {
                getLogger().warning(
                        "Could not synchronously return " + player.getName()
                                + " during shutdown: " + synchronousError.getMessage()
                                + ". Pending recovery data was retained."
                );
                return CompletableFuture.completedFuture(false);
            }
        }

        if (shuttingDown) {
            getLogger().warning(
                    "Synchronous return teleport returned false for "
                            + player.getName()
                            + " during shutdown. Pending recovery data was retained."
            );
            return CompletableFuture.completedFuture(false);
        }

        try {
            return player.teleportAsync(saved).handle((success, error) -> {
                if (error == null && Boolean.TRUE.equals(success)) {
                    try {
                        player.saveData();
                        return true;
                    } catch (RuntimeException saveError) {
                        getLogger().warning(
                                "Returned departing player " + player.getName()
                                        + " but could not save their player data: "
                                        + saveError.getMessage()
                        );
                        return false;
                    }
                }
                String reason = error == null
                        ? "teleport returned false"
                        : error.getMessage();
                getLogger().warning(
                        "Could not return departing player " + player.getName()
                                + " to their saved location: " + reason
                );
                return false;
            });
        } catch (RuntimeException asynchronousError) {
            getLogger().warning(
                    "Could not start return teleport for departing player "
                            + player.getName() + ": " + asynchronousError.getMessage()
            );
            return CompletableFuture.completedFuture(false);
        }
    }

    public void completeOnboarding(Player player) {
        UUID uuid = player.getUniqueId();
        playerDataSnapshotStore.delete(uuid);
        pendingStore.clearPending(uuid);
        acceptedStore.markAccepted(uuid, player.getName());
        canAcceptRules.remove(uuid);
        acceptInProgress.remove(uuid);
        joinSequenceActive.remove(uuid);
        debugForcedPlayers.remove(uuid);
        restoreOnboardingPlayerToTab(player);
        debugLog(() -> "Completed onboarding for " + player.getName());
        if (suppressedJoinMessages.remove(uuid)) {
            platformScheduler.runEntityDelayed(
                    player,
                    () -> broadcastFirstJoinMessage(player),
                    1L
            );
        }
    }

    public boolean endOnboarding(UUID uuid) {
        suppressedJoinMessages.remove(uuid);
        Player player = getServer().getPlayer(uuid);
        if (player != null) {
            releaseActivePlayers(java.util.Set.of(uuid));
            return true;
        }
        if (pendingStore != null) {
            pendingStore.markCleanupRequired(uuid);
        }
        clearRuntimeState(uuid);
        return false;
    }

    private void broadcastFirstJoinMessage(Player player) {
        String configured = config.getFirstJoinMessage();
        if (configured == null || configured.isBlank()) return;

        Component message = TextFormatter.formatChat(
                configured.replace("{player}", player.getName())
        );
        sendToNonOnboardingPlayers(message);
        if (shouldForwardFirstJoinToDiscord(
                config.getDiscordSrvJoinMessages()
        ) && getServer().getPluginManager().isPluginEnabled("DiscordSRV")) {
            DiscordSrvBridge.forwardFirstJoinMessage(this, player, message);
        }
    }

    static boolean shouldForwardFirstJoinToDiscord(
            ConfigData.MessageConsumption discordSrvJoinMessages
    ) {
        return discordSrvJoinMessages != ConfigData.MessageConsumption.NONE;
    }

    public void sendToNonOnboardingPlayers(Component message) {
        getServer().getConsoleSender().sendMessage(message);
        for (Player recipient : getServer().getOnlinePlayers()) {
            if (!isOnboardingActive(recipient.getUniqueId())) {
                recipient.sendMessage(message);
            }
        }
    }

    public void resolveSafeReturnLocation(
            Player player,
            Location preferred,
            Consumer<Location> callback
    ) {
        debugLog(() -> "Resolving return location for " + player.getName()
                + " preferred=" + formatDebugLocation(preferred));
        List<ReturnSearch> searches = new ArrayList<>();
        if (preferred != null && preferred.getWorld() != null) {
            searches.add(new ReturnSearch(preferred, ReturnSearchMode.DESIRED_Y));
            searches.add(new ReturnSearch(preferred, ReturnSearchMode.NEAR_ORIGIN));
        }

        ConfigData.PlayerLocation configuredFallback =
                config.getCleanupFallbackLocation();
        if (configuredFallback != null) {
            Location fallback = configuredFallback.toLocation();
            if (fallback != null) {
                int fallbackRadius = config.getCleanupFallbackRadius();
                if (fallbackRadius == 0) {
                    searches.add(new ReturnSearch(fallback, ReturnSearchMode.NEAR_ORIGIN));
                } else {
                    RandomGenerator random = ThreadLocalRandom.current();
                    for (int attempt = 0; attempt < RANDOM_FALLBACK_ATTEMPTS; attempt++) {
                        Location candidate = fallback.clone();
                        candidate.setX(randomFallbackCoordinate(
                                fallback.getX(),
                                fallbackRadius,
                                random
                        ));
                        candidate.setZ(randomFallbackCoordinate(
                                fallback.getZ(),
                                fallbackRadius,
                                random
                        ));
                        searches.add(new ReturnSearch(
                                candidate,
                                ReturnSearchMode.EXACT_COLUMN_DESIRED_Y
                        ));
                    }
                }
            } else {
                getLogger().warning(
                        "Configured cleanup fallback world is not loaded: "
                                + configuredFallback.worldName()
                );
            }
        }

        int desiredY = config.getReturnDesiredY();
        org.bukkit.World.Environment fallbackEnvironment =
                config.getCleanupFallbackEnvironment();
        for (org.bukkit.World world : getServer().getWorlds()) {
            if (world.getEnvironment() != fallbackEnvironment) continue;
            searches.add(new ReturnSearch(
                    withDesiredY(world.getSpawnLocation(), desiredY),
                    ReturnSearchMode.DESIRED_Y
            ));
        }

        resolveSafeReturnLocation(player, searches, 0, callback);
    }

    private void resolveSafeReturnLocation(
            Player player,
            List<ReturnSearch> searches,
            int index,
            Consumer<Location> callback
    ) {
        if (!player.isOnline() || index >= searches.size()) {
            platformScheduler.runEntity(player, () -> callback.accept(null));
            return;
        }

        ReturnSearch search = searches.get(index);
        platformScheduler.runAtLocation(search.origin(), () -> {
            Location safe;
            try {
                int horizontalRadius = switch (search.mode()) {
                    case DESIRED_Y -> 8;
                    case EXACT_COLUMN_DESIRED_Y -> 0;
                    case NEAR_ORIGIN -> 6;
                };
                safe = SafeLocationFinder.findHighestSurface(
                        search.origin(),
                        horizontalRadius
                );
            } catch (RuntimeException exception) {
                getLogger().warning(
                        "Could not scan a return location for " + player.getName()
                                + ": " + exception.getMessage()
                );
                safe = null;
            }

            Location result = safe;
            platformScheduler.runEntity(player, () -> {
                if (result != null) {
                    debugLog(() -> "Resolved return location for " + player.getName()
                            + " to " + formatDebugLocation(result)
                            + " using " + search.mode());
                    callback.accept(result);
                } else {
                    resolveSafeReturnLocation(player, searches, index + 1, callback);
                }
            });
        });
    }

    private static Location withDesiredY(Location spawn, int desiredY) {
        Location adjusted = spawn.clone();
        adjusted.setY(desiredY);
        return adjusted;
    }

    static double randomFallbackCoordinate(
            double center,
            int radius,
            RandomGenerator random
    ) {
        if (radius <= 0) return center;
        int offset = random.nextInt((radius * 2) + 1) - radius;
        return center + offset;
    }

    public void synchronizeTabVisibility(Player viewer) {
        if (!config.isHideOnboardingPlayersFromTab()) return;
        for (Player onboardingPlayer : getServer().getOnlinePlayers()) {
            if (!viewer.getUniqueId().equals(onboardingPlayer.getUniqueId())
                    && isOnboardingActive(onboardingPlayer.getUniqueId())) {
                unlistForViewer(viewer, onboardingPlayer);
            }
        }
    }

    public void forgetTabVisibility(Player player) {
        UUID uuid = player.getUniqueId();
        hiddenTabPlayers.removeIf(pair ->
                pair.viewer().equals(uuid) || pair.onboardingPlayer().equals(uuid)
        );
    }

    private void hideOnboardingPlayerFromTab(Player onboardingPlayer) {
        if (!config.isHideOnboardingPlayersFromTab()) return;
        for (Player viewer : getServer().getOnlinePlayers()) {
            if (!viewer.getUniqueId().equals(onboardingPlayer.getUniqueId())) {
                unlistForViewer(viewer, onboardingPlayer);
            }
        }
    }

    private void unlistForViewer(Player viewer, Player onboardingPlayer) {
        TabVisibilityPair pair = new TabVisibilityPair(
                viewer.getUniqueId(),
                onboardingPlayer.getUniqueId()
        );
        if (hiddenTabPlayers.contains(pair)) return;

        platformScheduler.runEntity(viewer, () -> {
            if (viewer.isOnline()
                    && onboardingPlayer.isOnline()
                    && isOnboardingActive(onboardingPlayer.getUniqueId())
                    && viewer.isListed(onboardingPlayer)
                    && viewer.unlistPlayer(onboardingPlayer)) {
                hiddenTabPlayers.add(pair);
            }
        });
    }

    private void restoreOnboardingPlayerToTab(Player onboardingPlayer) {
        UUID targetUuid = onboardingPlayer.getUniqueId();
        for (Player viewer : getServer().getOnlinePlayers()) {
            if (viewer.getUniqueId().equals(targetUuid)) continue;
            TabVisibilityPair pair = new TabVisibilityPair(
                    viewer.getUniqueId(),
                    targetUuid
            );
            platformScheduler.runEntity(
                    viewer,
                    () -> restoreTabForViewer(viewer, onboardingPlayer, pair, 3)
            );
        }
        hiddenTabPlayers.removeIf(pair ->
                pair.onboardingPlayer().equals(targetUuid)
                && getServer().getPlayer(pair.viewer()) == null
        );
    }

    private void restoreTabForViewer(
            Player viewer,
            Player onboardingPlayer,
            TabVisibilityPair pair,
            int attemptsRemaining
    ) {
        if (!viewer.isOnline() || !onboardingPlayer.isOnline()) {
            hiddenTabPlayers.remove(pair);
            return;
        }
        if (isOnboardingActive(onboardingPlayer.getUniqueId())) return;

        // Force a false -> true listed-state transition. Calling listPlayer
        // while the server already considers the entry listed may not send a
        // fresh client packet after a teleport or region transition.
        viewer.unlistPlayer(onboardingPlayer);
        platformScheduler.runEntityDelayed(
                viewer,
                () -> finishTabRefresh(
                        viewer,
                        onboardingPlayer,
                        pair,
                        attemptsRemaining
                ),
                2L
        );
    }

    private void finishTabRefresh(
            Player viewer,
            Player onboardingPlayer,
            TabVisibilityPair pair,
            int attemptsRemaining
    ) {
        if (!viewer.isOnline() || !onboardingPlayer.isOnline()) {
            hiddenTabPlayers.remove(pair);
            return;
        }
        if (viewer.listPlayer(onboardingPlayer)) {
            hiddenTabPlayers.remove(pair);
            return;
        }
        if (attemptsRemaining <= 1) {
            getLogger().warning(
                    "Could not restore " + onboardingPlayer.getName()
                            + " to " + viewer.getName() + "'s tab list."
            );
            return;
        }
        platformScheduler.runEntityDelayed(
                viewer,
                () -> restoreTabForViewer(
                        viewer,
                        onboardingPlayer,
                        pair,
                        attemptsRemaining - 1
                ),
                10L
        );
    }

    private void restoreAllTabPlayers() {
        for (TabVisibilityPair pair : java.util.Set.copyOf(hiddenTabPlayers)) {
            Player viewer = getServer().getPlayer(pair.viewer());
            Player onboardingPlayer = getServer().getPlayer(pair.onboardingPlayer());
            if (viewer == null || onboardingPlayer == null) {
                hiddenTabPlayers.remove(pair);
                continue;
            }
            platformScheduler.runEntity(viewer, () -> {
                if (viewer.isOnline() && onboardingPlayer.isOnline()) {
                    viewer.listPlayer(onboardingPlayer);
                }
                hiddenTabPlayers.remove(pair);
            });
        }
    }

    private void restoreAllTabPlayersOnDisable() {
        for (TabVisibilityPair pair : java.util.Set.copyOf(hiddenTabPlayers)) {
            Player viewer = getServer().getPlayer(pair.viewer());
            Player onboardingPlayer = getServer().getPlayer(pair.onboardingPlayer());
            if (viewer == null || onboardingPlayer == null) continue;
            try {
                viewer.listPlayer(onboardingPlayer);
            } catch (RuntimeException exception) {
                getLogger().fine("Could not restore a tab-list entry during shutdown: "
                        + exception.getMessage());
            }
        }
        hiddenTabPlayers.clear();
    }

    public void trackSequencePotionEffect(UUID uuid, PotionEffectType type) {
        sequencePotionEffects
                .computeIfAbsent(uuid, ignored -> ConcurrentHashMap.newKeySet())
                .add(type);
    }

    public void untrackSequencePotionEffect(UUID uuid, PotionEffectType type) {
        var effects = sequencePotionEffects.get(uuid);
        if (effects == null) return;
        effects.remove(type);
        if (effects.isEmpty()) sequencePotionEffects.remove(uuid);
    }

    public void clearSequencePotionEffects(Player player) {
        var effects = sequencePotionEffects.remove(player.getUniqueId());
        if (effects == null) return;
        effects.forEach(player::removePotionEffect);
    }

    public void applyDebugFix(Player player) {
        UUID uuid = player.getUniqueId();
        softProtectedPlayers.remove(uuid);
        clearSequencePotionEffects(player);
        player.resetTitle();
        player.sendActionBar(net.kyori.adventure.text.Component.empty());
        debugActionBar(player, "clear", "applyDebugFix");
        player.stopAllSounds();

        player.setInvisible(false);
        player.setInvulnerable(false);
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setGlowing(false);
        player.setVisualFire(false);
        player.setFireTicks(0);
        player.setFreezeTicks(0);
        player.setNoDamageTicks(0);
        player.setFallDistance(0f);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
    }

    public void clearSoftProtectionState(Player player) {
        UUID uuid = player.getUniqueId();
        softProtectedPlayers.remove(uuid);
        player.setInvulnerable(false);
        player.setFireTicks(0);
        player.setFallDistance(0f);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
    }

    private void clearAllPlayerEffects(Player player) {
        player.clearActivePotionEffects();
        sequencePotionEffects.remove(player.getUniqueId());

        player.resetTitle();
        player.sendActionBar(net.kyori.adventure.text.Component.empty());
        debugActionBar(player, "clear", "clearAllPlayerEffects");
        player.stopAllSounds();

        player.setGameMode(GameMode.SURVIVAL);
        player.setInvisible(false);
        player.setInvulnerable(false);
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setGlowing(false);
        player.setVisualFire(false);
        player.setFireTicks(0);
        player.setFreezeTicks(0);
        player.setNoDamageTicks(0);
        player.setFallDistance(0f);
        player.setVelocity(new org.bukkit.util.Vector());
        player.setGravity(true);
        player.setCollidable(true);
        player.setSilent(false);
        player.setSwimming(false);
        player.setSneaking(false);
        player.setSprinting(false);
        player.closeInventory();
    }

    public boolean reloadPluginConfig() {
        var titleSequence = new TitleSequenceConfig(this).updateAndLoad();
        if (titleSequence == null || !new ConfigUpdater(this).update()) {
            return false;
        }

        reloadConfig();
        ConfigData loaded = new LoadConfig(this, titleSequence).load();
        if (loaded == null) {
            return false;
        }

        boolean wasEnabled = config != null && config.isTitleSequenceEnabled();
        boolean wasDebugEnabled = config != null && config.isDebugForceOnboarding();
        boolean wasHidingFromTab =
                config != null && config.isHideOnboardingPlayersFromTab();
        this.config = loaded;
        if (messageResponsibilityManager != null) {
            messageResponsibilityManager.activate(loaded.getEventPriorities());
        }
        if (wasEnabled && !loaded.isTitleSequenceEnabled()) {
            releaseActivePlayers(allOnboardingPlayers());
        } else if (wasDebugEnabled && !loaded.isDebugForceOnboarding()) {
            releaseActivePlayers(java.util.Set.copyOf(debugForcedPlayers));
        }
        if (wasHidingFromTab && !loaded.isHideOnboardingPlayersFromTab()) {
            restoreAllTabPlayers();
        } else if (!wasHidingFromTab && loaded.isHideOnboardingPlayersFromTab()) {
            for (Player player : getServer().getOnlinePlayers()) {
                if (isOnboardingActive(player.getUniqueId())) {
                    hideOnboardingPlayerFromTab(player);
                }
            }
        }
        return true;
    }

    private java.util.Set<UUID> allOnboardingPlayers() {
        return onboardingPlayersForRelease(
                canAcceptRules.keySet(),
                joinSequenceActive,
                debugForcedPlayers,
                pendingStore == null ? java.util.Set.of() : pendingStore.pendingPlayerIds()
        );
    }

    static java.util.Set<UUID> onboardingPlayersForRelease(
            java.util.Set<UUID> accepting,
            java.util.Set<UUID> joining,
            java.util.Set<UUID> debugForced,
            java.util.Set<UUID> pending
    ) {
        java.util.Set<UUID> players = new java.util.HashSet<>(accepting);
        players.addAll(joining);
        players.addAll(debugForced);
        players.addAll(pending);
        return players;
    }

    public void releasePendingPlayer(Player player) {
        releaseActivePlayers(java.util.Set.of(player.getUniqueId()));
    }

    private void releaseActivePlayers(java.util.Set<UUID> players) {
        for (UUID uuid : players) {
            forcedCleanupPlayers.add(uuid);
            if (pendingStore != null) {
                pendingStore.markCleanupRequired(uuid);
            }
            Player player = getServer().getPlayer(uuid);
            if (player == null) {
                // Keep pending state as a deferred cleanup marker. Bukkit cannot
                // restore an offline player's state until they join again.
                continue;
            }

            platformScheduler.runEntity(player, () -> {
                stopMovementLock(uuid);
                stopCountdown(uuid);
                player.setInvulnerable(true);
                resolveSafeReturnLocation(
                        player,
                        pendingStore.getPendingLocation(uuid),
                        target -> {
                            if (target == null) {
                                getLogger().warning(
                                        "No safe cleanup return location could be found for "
                                                + player.getName()
                                                + "; retaining the cleanup marker instead of "
                                                + "releasing them in the onboarding world."
                                );
                                clearRuntimeState(uuid);
                                canAcceptRules.put(uuid, false);
                                player.setInvulnerable(true);
                                startMovementLock(
                                        player,
                                        player.getLocation(),
                                        true
                                );
                                return;
                            }

                            teleportAndFinishForcedCleanup(
                                    player,
                                    uuid,
                                    target,
                                    true
                            );
                        }
                );
            });
        }
    }

    private void teleportAndFinishForcedCleanup(
            Player player,
            UUID uuid,
            Location target,
            boolean allowFallback
    ) {
        player.teleportAsync(target).whenComplete((success, error) ->
                platformScheduler.runEntity(player, () -> {
                    if (error == null && Boolean.TRUE.equals(success)) {
                        forcedCleanupPlayers.remove(uuid);
                        playerDataSnapshotStore.delete(uuid);
                        pendingStore.clearPending(uuid);
                        clearAllPlayerEffects(player);
                        clearRuntimeState(uuid);
                        restoreOnboardingPlayerToTab(player);
                        return;
                    }

                    if (allowFallback) {
                        resolveSafeReturnLocation(player, null, fallback -> {
                            if (fallback != null) {
                                teleportAndFinishForcedCleanup(
                                        player,
                                        uuid,
                                        fallback,
                                        false
                                );
                            } else {
                                finishFailedForcedCleanup(player, uuid);
                            }
                        });
                        return;
                    }

                    finishFailedForcedCleanup(player, uuid);
                })
        );
    }

    private void finishFailedForcedCleanup(Player player, UUID uuid) {
        forcedCleanupPlayers.remove(uuid);
        getLogger().warning(
                "Deferred cleanup return teleport failed for "
                        + player.getName()
                        + "; the recovery marker will be retried next login."
        );
        clearAllPlayerEffects(player);
        clearRuntimeState(uuid);
        restoreOnboardingPlayerToTab(player);
    }

    private void restoreOfflineRequiredSnapshots() {
        if (pendingStore == null || playerDataSnapshotStore == null) {
            return;
        }

        java.util.Set<UUID> online = getServer().getOnlinePlayers().stream()
                .map(Player::getUniqueId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        playerDataSnapshotStore.restoreOfflineRequired(
                pendingStore.pendingPlayerIds(),
                online
        );
    }

    private void clearRuntimeState(UUID uuid) {
        canAcceptRules.remove(uuid);
        acceptInProgress.remove(uuid);
        joinSequenceActive.remove(uuid);
        debugForcedPlayers.remove(uuid);
        suppressedJoinMessages.remove(uuid);
        sequencePotionEffects.remove(uuid);
        stopCountdown(uuid);
        stopMovementLock(uuid);
    }

    private record TabVisibilityPair(UUID viewer, UUID onboardingPlayer) {
    }

    private record ReturnSearch(Location origin, ReturnSearchMode mode) {
    }

    private static String formatDebugLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return "null";
        }
        return location.getWorld().getName()
                + "("
                + String.format(java.util.Locale.ROOT, "%.2f", location.getX()) + ", "
                + String.format(java.util.Locale.ROOT, "%.2f", location.getY()) + ", "
                + String.format(java.util.Locale.ROOT, "%.2f", location.getZ())
                + ")";
    }

    private enum ReturnSearchMode {
        DESIRED_Y,
        EXACT_COLUMN_DESIRED_Y,
        NEAR_ORIGIN
    }
}
