package com.helianthus.territorywar.service;

import com.helianthus.territorywar.config.TerritoryConfig;
import com.helianthus.territorywar.core.Region;
import com.helianthus.territorywar.core.RegionId;
import com.helianthus.territorywar.data.RaidCooldownTracker;
import com.helianthus.territorywar.integration.ftb.TeamIntegrationService;
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
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

/**
 * Raids declared on enemy regions. A team leader pays half the region's price in
 * {@link RegionPurchaseService#CURRENCY} to start a warm-up, announced to the whole server, after which
 * the raid becomes {@link State#ACTIVE}. Raids are kept in memory only, so a restart drops them.
 */
public final class RaidService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final RaidService INSTANCE = new RaidService();
    private static final long MILLIS_PER_MINUTE = 60_000L;

    public enum State {
        WARMUP,
        ACTIVE
    }

    /** A raid on a region; {@code startsAt} is the epoch millisecond time the warm-up ends. */
    public record Raid(RegionId regionId, UUID attackerTeamId, UUID defenderTeamId, State state, long startsAt) {
    }

    private final Map<RegionId, Raid> raids = new LinkedHashMap<>();

    RaidService() {
    }

    public static RaidService get() {
        return INSTANCE;
    }

    /** Starts raids whose warm-up has ended each server tick and forgets all raids when the server stops. */
    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(RaidService::onServerTick);
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
        raids.clear();
    }

    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || INSTANCE.raids.isEmpty()) {
            return;
        }
        for (Raid raid : INSTANCE.startDueRaids(System.currentTimeMillis())) {
            LOGGER.info("Raid started: region={} attacker={} defender={}", raid.regionId().value(),
                    raid.attackerTeamId(), raid.defenderTeamId());
        }
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        INSTANCE.clear();
    }
}
