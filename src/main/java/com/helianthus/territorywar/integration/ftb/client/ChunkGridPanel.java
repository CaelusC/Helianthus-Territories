package com.helianthus.territorywar.integration.ftb.client;

import com.helianthus.territorywar.client.ClientRegionCache;
import com.helianthus.territorywar.client.TerritoryClient;
import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.core.Region;
import com.helianthus.territorywar.service.PurchaseResult;
import com.helianthus.territorywar.service.RegionPurchaseService;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;

/** Grid of chunks centred on the player, coloured by the region each chunk belongs to. */
final class ChunkGridPanel extends Panel {
    static final int RADIUS = 6;
    static final int TILE_SIZE = 11;
    static final int SIZE = (RADIUS * 2 + 1) * TILE_SIZE;

    private static final Color4I FOR_SALE = Color4I.rgba(0x33FFD700);
    private static final Color4I OWNED = Color4I.rgba(0x33FF0000);
    private static final Color4I UNCLAIMED = Color4I.rgba(0x33808080);
    private static final Color4I HOVER = Color4I.rgba(0x40FFFFFF);
    private static final int TEAM_ALPHA = 0x33;

    ChunkGridPanel(Panel parent) {
        super(parent);
    }

    @Override
    public void addWidgets() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        String dimension = player.level().dimension().location().toString();
        Map<ChunkKey, Region> regionsByChunk = new HashMap<>();
        for (Region region : ClientRegionCache.getRegions()) {
            for (ChunkKey chunk : region.getChunks()) {
                regionsByChunk.put(chunk, region);
            }
        }
        ChunkPos center = player.chunkPosition();
        for (int dz = -RADIUS; dz <= RADIUS; dz++) {
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                ChunkKey chunk = new ChunkKey(center.x + dx, center.z + dz, dimension);
                add(new ChunkTile(this, chunk, regionsByChunk.get(chunk), dx + RADIUS, dz + RADIUS));
            }
        }
    }

    @Override
    public void alignWidgets() {
        for (Widget widget : getWidgets()) {
            if (widget instanceof ChunkTile tile) {
                tile.setPosAndSize(tile.column * TILE_SIZE, tile.row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }
    }

    private static Optional<Team> ownerTeam(Region region) {
        if (!region.isOwned() || !FTBTeamsAPI.api().isClientManagerLoaded()) {
            return Optional.empty();
        }
        return FTBTeamsAPI.api().getClientManager().getTeamByID(region.getOwnerTeamId());
    }

    private static final class ChunkTile extends Widget {
        private final ChunkKey chunk;
        private final Region region;
        private final int column;
        private final int row;
        private final Color4I color;

        ChunkTile(Panel parent, ChunkKey chunk, Region region, int column, int row) {
            super(parent);
            this.chunk = chunk;
            this.region = region;
            this.column = column;
            this.row = row;
            this.color = color(region);
        }

        private static Color4I color(Region region) {
            if (region == null) {
                return UNCLAIMED;
            }
            if (region.isOwned()) {
                return ownerTeam(region)
                        .map(team -> team.getProperty(TeamProperties.COLOR).withAlpha(TEAM_ALPHA))
                        .orElse(OWNED);
            }
            return region.isForSale() ? FOR_SALE : UNCLAIMED;
        }

        private boolean isBuyable() {
            return region != null && !region.isOwned() && region.isForSale();
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            color.draw(graphics, x, y, w - 1, h - 1);
            if (isMouseOver()) {
                HOVER.draw(graphics, x, y, w - 1, h - 1);
            }
        }

        @Override
        public boolean mousePressed(MouseButton button) {
            if (!isMouseOver() || !button.isLeft() || !isBuyable()) {
                return false;
            }
            playClickSound();
            getGui().openYesNo(Component.literal(region.getName()), Component.translatable("territorywar.screen.buy.confirm",
                    region.getName(), PurchaseResult.price(region), RegionPurchaseService.CURRENCY.getDescription()),
                    () -> TerritoryClient.requestPurchase(region.getId()));
            return true;
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            if (region == null) {
                list.add(Component.translatable("territorywar.screen.no_region"));
            } else {
                list.add(Component.literal(region.getName()));
                list.add(Component.translatable("territorywar.screen.price",
                        PurchaseResult.price(region), RegionPurchaseService.CURRENCY.getDescription()));
                list.add(owner(region));
                list.add(Component.translatable("territorywar.screen.chunks", region.getChunks().size()));
            }
            list.add(Component.translatable("territorywar.screen.chunk_pos", chunk.chunkX(), chunk.chunkZ()));
            if (isBuyable()) {
                list.add(Component.translatable("territorywar.screen.buy.hint"));
            }
        }

        private static Component owner(Region region) {
            if (region.isOwned()) {
                UUID teamId = region.getOwnerTeamId();
                Component name = ownerTeam(region).map(Team::getName)
                        .orElseGet(() -> Component.literal(teamId.toString()));
                return Component.translatable("territorywar.screen.owner", name);
            }
            return Component.translatable(region.isForSale()
                    ? "territorywar.screen.status.for_sale" : "territorywar.screen.status.not_for_sale");
        }
    }
}
