package com.helianthus.territorywar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.core.Region;
import com.helianthus.territorywar.core.RegionId;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PurchaseResultTest {
    private static final Set<ChunkKey> CHUNKS = Set.of(new ChunkKey(0, 0, "minecraft:overworld"));

    private static Optional<Region> region(UUID owner, int price, boolean forSale) {
        return Optional.of(new Region(RegionId.random(), "spawn", CHUNKS, owner, price, forSale));
    }

    @Test
    void unknownRegionIsNotFound() {
        assertEquals(PurchaseResult.REGION_NOT_FOUND, PurchaseResult.check(Optional.empty(), true, 100));
    }

    @Test
    void ownedRegionIsAlreadyOwned() {
        assertEquals(PurchaseResult.ALREADY_OWNED,
                PurchaseResult.check(region(UUID.randomUUID(), 10, true), true, 100));
    }

    @Test
    void regionOffMarketIsNotForSale() {
        assertEquals(PurchaseResult.NOT_FOR_SALE,
                PurchaseResult.check(region(null, 10, false), true, 100));
    }

    @Test
    void nonLeaderIsRejected() {
        assertEquals(PurchaseResult.NOT_TEAM_LEADER,
                PurchaseResult.check(region(null, 10, true), false, 100));
    }

    @Test
    void shortOfCurrencyIsInsufficientFunds() {
        assertEquals(PurchaseResult.INSUFFICIENT_FUNDS,
                PurchaseResult.check(region(null, 10, true), true, 9));
    }

    @Test
    void exactPriceSucceeds() {
        assertEquals(PurchaseResult.SUCCESS, PurchaseResult.check(region(null, 10, true), true, 10));
    }

    @Test
    void negativePriceIsFree() {
        assertEquals(PurchaseResult.SUCCESS, PurchaseResult.check(region(null, -5, true), true, 0));
    }

    @Test
    void availabilityIsCheckedBeforeLeadershipAndFunds() {
        assertEquals(PurchaseResult.NOT_FOR_SALE,
                PurchaseResult.check(region(null, 10, false), false, 0));
    }
}
