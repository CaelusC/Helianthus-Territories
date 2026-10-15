package com.helianthus.territorywar.service;

import com.helianthus.territorywar.TerritoryWar;
import com.helianthus.territorywar.config.TerritoryConfig;
import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.core.Region;
import com.helianthus.territorywar.core.RegionId;
import com.helianthus.territorywar.data.RaidCooldownTracker;
import com.helianthus.territorywar.data.TerritoryDataService;
import com.helianthus.territorywar.integration.ftb.TeamIntegrationService;
import com.helianthus.territorywar.network.TerritoryNetwork;
import com.mojang.logging.LogUtils;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/**
 * Raids declared on enemy regions. A team leader pays half the region's price in
 * {@link RegionPurchaseService#CURRENCY} to start a warm-up, announced to the whole server, after which
 * the raid becomes {@link State#ACTIVE}. While active, a global boss bar counts each team's members within
 * {@link #CONTROL_RADIUS} blocks of the region centre; raiders win once they have outnumbered the victims there
 * for the hold time, otherwise the victims win when the raid's maximum duration ends. Storage blocks in the
 * region cannot be opened by anyone outside the defending team. Raids are kept in memory only, so a restart
 * drops them.
 */
public final class RaidService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final RaidService INSTANCE = new RaidService();
    private static final long MILLIS_PER_MINUTE = 60_000L;
    private static final long MILLIS_PER_DAY = 86_400_000L;
    private static final int TICKS_PER_SECOND = 20;
    /** Horizontal distance in blocks from the region centre within which players count towards control. */
    static final double CONTROL_RADIUS = 40.0;

    public enum State {
        WARMUP,
        ACTIVE
    }

    /** A raid on a region; {@code startsAt} is the epoch millisecond time the warm-up ends. */
    public record Raid(RegionId regionId, UUID attackerTeamId, UUID defenderTeamId, State state, long startsAt) {
    }

    /** Horizontal block position of a region's centre. */
    record Center(double x, double z) {
    }

    /** Live state of an active raid: its boss bar and the ticks raiders have held the centre so far. */
    private static final class Control {
        private final CustomBossEvent bossBar;
        private final Center center;
        private final long holdTicks;
        private final long endsAt;
        private long heldTicks;

        private Control(CustomBossEvent bossBar, Center center, long holdTicks, long endsAt) {
            this.bossBar = bossBar;
            this.center = center;
            this.holdTicks = holdTicks;
            this.endsAt = endsAt;
        }
    }

    private final Map<RegionId, Raid> raids = new LinkedHashMap<>();
    private final Map<RegionId, Control> controls = new LinkedHashMap<>();

    RaidService() {
    }

    public static RaidService get() {
        return INSTANCE;
    }

    /**
     * Starts and runs raids each server tick, keeps raided storage blocks closed to outsiders and forgets all
     * raids when the server stops.
     */
    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(RaidService::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(RaidService::onRightClickBlock);
        MinecraftForge.EVENT_BUS.addListener(RaidService::onServerStopped);
    }

    public Optional<Raid> getRaid(RegionId regionId) {
        return Optional.ofNullable(raids.get(Objects.requireNonNull(regionId, "regionId")));
    }

    /**
     * Declares a raid by the player's team on the region. Returns why it failed, taking no payment, if
     * the region is unknown, unowned or already raided, the player does not lead another team, either
     * team has too few members online, either team is on cooldown, or the player cannot pay.
     */
    public RaidResult declareRaid(ServerPlayer player, RegionId regionId, TerritoryService territory,
            TeamIntegrationService teams, RaidCooldownTracker cooldowns) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(regionId, "regionId");
        String raider = player.getGameProfile().getName();

        Optional<Region> found = territory.getRegion(regionId);
        RaidResult result = RaidResult.target(found, raids.containsKey(regionId));
        Optional<UUID> attackerTeamId = teams.getPlayerTeamId(player);
        if (result == RaidResult.SUCCESS) {
            result = RaidResult.attacker(found.get(), attackerTeamId,
                    attackerTeamId.isPresent() && teams.isTeamLeader(player, attackerTeamId.get()));
        }
        if (result != RaidResult.SUCCESS) {
            LOGGER.debug("Raid on region {} by {} rejected: {}", regionId.value(), raider, result);
            return result;
        }
        Region region = found.get();
        UUID attacker = attackerTeamId.get();
        UUID defender = region.getOwnerTeamId();
        int price = RaidResult.price(region);

        result = RaidResult.online(teams.getOnlineMemberCount(attacker), TerritoryConfig.MIN_ATTACKER_ONLINE.get(),
                teams.getOnlineMemberCount(defender), TerritoryConfig.MIN_DEFENDER_ONLINE.get());
        long now = System.currentTimeMillis();
        if (result == RaidResult.SUCCESS) {
            result = RaidResult.cooldowns(cooldowns.getVictimCooldownUntil(defender),
                    cooldowns.getRaiderCooldownUntil(attacker), now);
        }
        Inventory inventory = player.getInventory();
        if (result == RaidResult.SUCCESS && inventory.countItem(RegionPurchaseService.CURRENCY) < price) {
            result = RaidResult.INSUFFICIENT_FUNDS;
        }
        if (result != RaidResult.SUCCESS) {
            LOGGER.debug("Raid on region {} by {} rejected: {}", regionId.value(), raider, result);
            return result;
        }

        ContainerHelper.clearOrCountMatchingItems(inventory, stack -> stack.is(RegionPurchaseService.CURRENCY),
                price, false);
        player.containerMenu.broadcastChanges();
        int warmupMinutes = TerritoryConfig.RAID_WARMUP_MINUTES.get();
        startWarmup(regionId, attacker, defender, now + warmupMinutes * MILLIS_PER_MINUTE);

        Component teamName = teams.getTeamName(attacker).orElse(Component.literal(attacker.toString()));
        player.server.getPlayerList().broadcastSystemMessage(Component.translatable("territorywar.raid.announce",
                teamName, region.getName(), formatHours(warmupMinutes)), false);
        LOGGER.info("Raid declared: region={} name={} attacker={} defender={} raider={} paid={} warmupMinutes={}",
                regionId.value(), region.getName(), attacker, defender, raider, price, warmupMinutes);
        return RaidResult.SUCCESS;
    }

    /** Minutes as hours with at most one decimal, e.g. 90 as {@code 1.5} and 60 as {@code 1}. */
    public static String formatHours(int minutes) {
        return new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(minutes / 60.0);
    }

    void startWarmup(RegionId regionId, UUID attackerTeamId, UUID defenderTeamId, long startsAt) {
        raids.put(regionId, new Raid(regionId, attackerTeamId, defenderTeamId, State.WARMUP, startsAt));
    }

    /** Moves every raid whose warm-up has ended by {@code now} to {@link State#ACTIVE} and returns them. */
    List<Raid> startDueRaids(long now) {
        List<Raid> started = new ArrayList<>();
        for (Map.Entry<RegionId, Raid> entry : raids.entrySet()) {
            Raid raid = entry.getValue();
            if (raid.state() == State.WARMUP && raid.startsAt() <= now) {
                Raid active = new Raid(raid.regionId(), raid.attackerTeamId(), raid.defenderTeamId(), State.ACTIVE,
                        raid.startsAt());
                entry.setValue(active);
                started.add(active);
            }
        }
        return started;
    }

    void clear() {
        for (Control control : controls.values()) {
            control.bossBar.removeAllPlayers();
        }
        controls.clear();
        raids.clear();
    }

    /** Centre of the bounding box of the chunks, in block coordinates. */
    static Center regionCenter(Set<ChunkKey> chunks) {
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("region has no chunks");
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (ChunkKey chunk : chunks) {
            minX = Math.min(minX, chunk.chunkX());
            maxX = Math.max(maxX, chunk.chunkX());
            minZ = Math.min(minZ, chunk.chunkZ());
            maxZ = Math.max(maxZ, chunk.chunkZ());
        }
        return new Center((minX * 16.0 + (maxX + 1) * 16.0) / 2.0, (minZ * 16.0 + (maxZ + 1) * 16.0) / 2.0);
    }

    /** Returns true if the block position is within {@link #CONTROL_RADIUS} of the centre, ignoring height. */
    static boolean inControlZone(Center center, double x, double z) {
        double dx = x - center.x();
        double dz = z - center.z();
        return dx * dx + dz * dz <= CONTROL_RADIUS * CONTROL_RADIUS;
    }

    /**
     * Seconds raiders must outnumber the victims to win: {@code chunkCount * secondsPerChunk}, capped at the
     * maximum raid duration.
     */
    static long holdSeconds(int chunkCount, int secondsPerChunk, int maxRaidDurationMinutes) {
        return Math.min((long) maxRaidDurationMinutes * 60, (long) chunkCount * secondsPerChunk);
    }

    /**
     * Returns true if opening a storage block must be refused: it is in a region with an active raid and the
     * player is not in the defending team.
     */
    static boolean blocksContainerUse(Optional<Raid> raid, boolean storageBlock, Optional<UUID> playerTeamId) {
        return storageBlock && raid.isPresent() && raid.get().state() == State.ACTIVE
                && !playerTeamId.map(raid.get().defenderTeamId()::equals).orElse(false);
    }

    private void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        for (Raid raid : startDueRaids(now)) {
            start(server, raid);
        }
        if (controls.isEmpty()) {
            return;
        }
        TerritoryService territory = TerritoryManager.get(server);
        TeamIntegrationService teams = new TeamIntegrationService();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (RegionId regionId : List.copyOf(controls.keySet())) {
            Raid raid = raids.get(regionId);
            Control control = controls.get(regionId);
            Optional<Region> region = territory.getRegion(regionId);
            if (region.isEmpty()) {
                LOGGER.warn("Raid on region {} abandoned: region no longer exists", regionId.value());
                end(regionId);
                continue;
            }

            int victims = 0;
            int raiders = 0;
            for (ServerPlayer player : players) {
                if (player.level().dimension() != Level.OVERWORLD
                        || !inControlZone(control.center, player.getX(), player.getZ())) {
                    continue;
                }
                Optional<UUID> team = teams.getPlayerTeamId(player);
                if (team.isEmpty()) {
                    continue;
                }
                if (team.get().equals(raid.attackerTeamId())) {
                    raiders++;
                } else if (team.get().equals(raid.defenderTeamId())) {
                    victims++;
                }
            }
            if (raiders > victims) {
                control.heldTicks++;
            }

            control.bossBar.setName(Component.translatable("territorywar.raid.bossbar",
                    region.get().getName(), victims, raiders));
            control.bossBar.setValue((int) Math.min(Integer.MAX_VALUE, control.heldTicks / TICKS_PER_SECOND));
            control.bossBar.setPlayers(players);

            if (control.heldTicks >= control.holdTicks) {
                raiderVictory(server, territory, raid, region.get(), now);
            } else if (now >= control.endsAt) {
                victimVictory(server, teams, raid, region.get(), now);
            }
        }
    }

    private void start(MinecraftServer server, Raid raid) {
        Optional<Region> region = TerritoryManager.get(server).getRegion(raid.regionId());
        if (region.isEmpty() || region.get().getChunks().isEmpty()) {
            LOGGER.warn("Raid on region {} abandoned at start: region no longer exists", raid.regionId().value());
            raids.remove(raid.regionId());
            return;
        }
        int maxMinutes = TerritoryConfig.MAX_RAID_DURATION_MINUTES.get();
        long holdSeconds = holdSeconds(region.get().getChunks().size(), TerritoryConfig.SECONDS_PER_CHUNK.get(),
                maxMinutes);
        CustomBossEvent bossBar = new CustomBossEvent(
                new ResourceLocation(TerritoryWar.MODID, "raid/" + raid.regionId().value()), Component.empty());
        bossBar.setColor(BossEvent.BossBarColor.RED);
        bossBar.setMax((int) Math.min(Integer.MAX_VALUE, holdSeconds));
        controls.put(raid.regionId(), new Control(bossBar, regionCenter(region.get().getChunks()),
                holdSeconds * TICKS_PER_SECOND, raid.startsAt() + maxMinutes * MILLIS_PER_MINUTE));

        playGlobalSound(server, SoundEvent.createVariableRangeEvent(
                new ResourceLocation(TerritoryConfig.GLOBAL_RAID_SOUND.get())));
        LOGGER.info("Raid started: region={} attacker={} defender={} holdSeconds={}", raid.regionId().value(),
                raid.attackerTeamId(), raid.defenderTeamId(), holdSeconds);
    }

    private void raiderVictory(MinecraftServer server, TerritoryService territory, Raid raid, Region region,
            long now) {
        end(raid.regionId());
        territory.transferRegion(raid.regionId(), raid.attackerTeamId());
        RaidCooldownTracker cooldowns = TerritoryDataService.get(server).getRaidCooldowns();
        cooldowns.setVictimCooldownUntil(raid.defenderTeamId(),
                now + TerritoryConfig.VICTIM_COOLDOWN_DAYS.get() * MILLIS_PER_DAY);
        cooldowns.setRaiderCooldownUntil(raid.attackerTeamId(),
                now + TerritoryConfig.RAIDER_COOLDOWN_DAYS.get() * MILLIS_PER_DAY);
        if (ModList.get().isLoaded("architectury")) {
            TerritoryNetwork.syncRegionsToAll(server);
        }
        playGlobalSound(server, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE);
        LOGGER.info("Raid won by raiders: region={} name={} attacker={} defender={}", raid.regionId().value(),
                region.getName(), raid.attackerTeamId(), raid.defenderTeamId());
    }

    private void victimVictory(MinecraftServer server, TeamIntegrationService teams, Raid raid, Region region,
            long now) {
        end(raid.regionId());
        int payout = RaidResult.price(region);
        Optional<ServerPlayer> payee = teams.getOnlineOwnerOrMember(raid.defenderTeamId());
        if (payee.isPresent()) {
            give(payee.get(), payout);
        } else {
            LOGGER.warn("Raid on region {} defended but no member of team {} is online to receive {} payout",
                    raid.regionId().value(), raid.defenderTeamId(), payout);
        }
        TerritoryDataService.get(server).getRaidCooldowns().setRaiderCooldownUntil(raid.attackerTeamId(),
                now + TerritoryConfig.RAIDER_COOLDOWN_DAYS.get() * MILLIS_PER_DAY);
        playGlobalSound(server, SoundEvents.BEACON_DEACTIVATE);
        LOGGER.info("Raid won by victims: region={} name={} attacker={} defender={} payout={} payee={}",
                raid.regionId().value(), region.getName(), raid.attackerTeamId(), raid.defenderTeamId(), payout,
                payee.map(player -> player.getGameProfile().getName()).orElse(null));
    }

    private void end(RegionId regionId) {
        Control control = controls.remove(regionId);
        if (control != null) {
            control.bossBar.removeAllPlayers();
        }
        raids.remove(regionId);
    }

    private static void give(ServerPlayer player, int amount) {
        int stackSize = new ItemStack(RegionPurchaseService.CURRENCY).getMaxStackSize();
        for (int remaining = amount; remaining > 0; remaining -= stackSize) {
            player.getInventory().placeItemBackInInventory(
                    new ItemStack(RegionPurchaseService.CURRENCY, Math.min(remaining, stackSize)));
        }
    }

    private static void playGlobalSound(MinecraftServer server, SoundEvent sound) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.playNotifySound(sound, SoundSource.MASTER, 1.0F, 1.0F);
        }
    }

    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || INSTANCE.raids.isEmpty()) {
            return;
        }
        INSTANCE.tick(event.getServer());
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (INSTANCE.controls.isEmpty() || !(event.getEntity() instanceof ServerPlayer player)
                || player.level().dimension() != Level.OVERWORLD) {
            return;
        }
        boolean storageBlock = player.level().getBlockEntity(event.getPos()) instanceof Container;
        if (!storageBlock) {
            return;
        }
        ChunkKey chunk = new ChunkKey(event.getPos().getX() >> 4, event.getPos().getZ() >> 4,
                Level.OVERWORLD.location().toString());
        Optional<Raid> raid = TerritoryManager.get(player.server).getRegionAt(chunk).flatMap(INSTANCE::getRaid);
        if (blocksContainerUse(raid, storageBlock, new TeamIntegrationService().getPlayerTeamId(player))) {
            event.setUseBlock(Event.Result.DENY);
            LOGGER.debug("Blocked {} opening storage at {} during raid on region {}",
                    player.getGameProfile().getName(), event.getPos(), raid.get().regionId().value());
        }
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        INSTANCE.clear();
    }
}
