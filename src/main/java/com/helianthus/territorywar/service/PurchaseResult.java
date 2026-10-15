package com.helianthus.territorywar.service;

import com.helianthus.territorywar.core.Region;
import java.util.Optional;

/** Outcome of a region purchase. Every value other than {@link #SUCCESS} leaves state unchanged. */
public enum PurchaseResult {
    SUCCESS,
    REGION_NOT_FOUND,
    ALREADY_OWNED,
    NOT_FOR_SALE,
    NOT_TEAM_LEADER,
    INSUFFICIENT_FUNDS;

    /** Price of the region in purchase currency; negative prices count as free. */
    public static int price(Region region) {
        return Math.max(0, region.getPurchasePrice());
    }

    /** Returns {@link #SUCCESS} if the region exists, is unowned and is for sale. */
    static PurchaseResult availability(Optional<Region> region) {
        if (region.isEmpty()) {
            return REGION_NOT_FOUND;
        }
        if (region.get().isOwned()) {
            return ALREADY_OWNED;
        }
        if (!region.get().isForSale()) {
            return NOT_FOR_SALE;
        }
        return SUCCESS;
    }

    /**
     * Checks a purchase in order: region availability, team leadership, then whether
     * {@code currencyHeld} covers the price.
     */
    static PurchaseResult check(Optional<Region> region, boolean teamLeader, int currencyHeld) {
        PurchaseResult availability = availability(region);
        if (availability != SUCCESS) {
            return availability;
        }
        if (!teamLeader) {
            return NOT_TEAM_LEADER;
        }
        if (currencyHeld < price(region.get())) {
            return INSUFFICIENT_FUNDS;
        }
        return SUCCESS;
    }
}
