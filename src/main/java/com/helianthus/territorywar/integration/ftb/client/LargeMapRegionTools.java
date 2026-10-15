package com.helianthus.territorywar.integration.ftb.client;

import com.helianthus.territorywar.client.ClientRegionCache;
import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.core.Region;
import dev.ftb.mods.ftbchunks.client.gui.LargeMapScreen;
import dev.ftb.mods.ftbchunks.client.gui.RegionMapPanel;
import dev.ftb.mods.ftbchunks.client.map.MapRegion;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Territory tools on the FTB Chunks large map, driven by the {@code mixin} hooks. Fills region chunks
 * with their admin colour while unowned and their owner's FTB team colour once bought. In admin mode,
 * dragging on the map selects a rectangle of chunks and opens {@link CreateRegionScreen}; otherwise
 * clicking an unowned region that is for sale opens {@link PurchaseRegionScreen}.
 */
public final class LargeMapRegionTools {
    private static final int REGION_ALPHA = 0x66;
    private static final Color4I OWNED = Color4I.rgba(0x66FF0000);
    private static final Color4I SELECTED = Color4I.rgba(0x8000FFFF);
    private static final Color4I SELECTED_TOO_LARGE = Color4I.rgba(0x80FF0000);
    private static final int CHUNKS_PER_TILE = 32;
    private static final int ADMIN_BUTTON_WIDTH = 56;
    private static final int BUTTON_HEIGHT = 16;
    private static final int CLICK_TOLERANCE = 2;

    private static boolean adminMode;
    private static ChunkKey selectionStart;
    private static ChunkKey selectionEnd;
    private static ChunkKey pressedChunk;
    private static int pressedMouseX;
    private static int pressedMouseY;
    private static List<Region> indexedRegions;
    private static Map<String, Map<Long, List<RegionChunk>>> chunksByTile = Map.of();

    private LargeMapRegionTools() {
    }

    private record RegionChunk(ChunkKey chunk, Region region, Color4I unownedFill) {
    }

    /** Admin mode toggle, only drawn for players with admin permission. */
    public static SimpleTextButton createAdminButton(Panel screen) {
        selectionStart = null;
        pressedChunk = null;
        return new SimpleTextButton(screen, adminModeTitle(), Icon.empty()) {
            @Override
            public boolean shouldDraw() {
                return ChunkGridPanel.isAdmin();
            }

            @Override
            public void onClicked(MouseButton button) {
                playClickSound();
                adminMode = !adminMode && ChunkGridPanel.isAdmin();
                selectionStart = null;
                setTitle(adminModeTitle());
            }

            @Override
            public void addMouseOverText(TooltipList list) {
                list.add(Component.translatable("territorywar.map.admin.hint"));
            }
        };
    }

    /** Places the admin toggle in the top right corner of the large map. */
    public static void alignAdminButton(SimpleTextButton button, int screenWidth) {
        button.setTitle(adminModeTitle());
        button.setPosAndSize(screenWidth - ADMIN_BUTTON_WIDTH - 1, 1, ADMIN_BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    /** Returns true if the press starts an admin selection and must not pan the map. */
    public static boolean onMapPressed(RegionMapPanel panel, MouseButton button) {
        if (!button.isLeft() || !panel.isMouseOver()) {
            return false;
        }
        ChunkKey chunk = chunkUnderMouse(panel);
        if (isAdminMode()) {
            selectionStart = chunk;
            selectionEnd = chunk;
            return true;
        }
        pressedChunk = chunk;
        pressedMouseX = panel.getMouseX();
        pressedMouseY = panel.getMouseY();
        return false;
    }

    /** Follows the mouse with the selection corner; the panel updates its hovered block while drawing. */
    public static void onMapDrawn(RegionMapPanel panel) {
        if (selectionStart != null) {
            selectionEnd = chunkUnderMouse(panel);
        }
    }

    /** Finishes an admin selection, or opens the purchase dialog if a press was a click on a region. */
    public static void onMapReleased(RegionMapPanel panel, MouseButton button) {
        if (selectionStart != null) {
            ChunkKey start = selectionStart;
            ChunkKey end = selectionEnd;
            selectionStart = null;
            finishSelection(start, end);
            return;
        }
        ChunkKey chunk = pressedChunk;
        pressedChunk = null;
        if (chunk == null || !button.isLeft()
                || Math.abs(panel.getMouseX() - pressedMouseX) > CLICK_TOLERANCE
                || Math.abs(panel.getMouseY() - pressedMouseY) > CLICK_TOLERANCE) {
            return;
        }
        regionAt(chunk).filter(region -> !region.isOwned() && region.isForSale())
                .ifPresent(region -> new PurchaseRegionScreen(region).openGui());
    }

    /** Fills the region and selected chunks inside one 512x512 block map tile drawn at x, y, w, h. */
    public static void drawTileOverlay(GuiGraphics graphics, MapRegion tile, int x, int y, int w, int h) {
        String dimension = tile.dimension.dimension.location().toString();
        int tileX = tile.pos.x();
        int tileZ = tile.pos.z();
        List<RegionChunk> chunks = index().getOrDefault(dimension, Map.of()).get(tileKey(tileX, tileZ));
        if (chunks != null) {
            for (RegionChunk regionChunk : chunks) {
                fillChunk(graphics, fill(regionChunk), regionChunk.chunk().chunkX() - tileX * CHUNKS_PER_TILE,
                        regionChunk.chunk().chunkZ() - tileZ * CHUNKS_PER_TILE, x, y, w, h);
            }
        }
        if (selectionStart == null || !selectionStart.dimension().equals(dimension)) {
            return;
        }
        Color4I color = MapSelection.area(selectionStart, selectionEnd) > MapSelection.maxChunks(dimension)
                ? SELECTED_TOO_LARGE : SELECTED;
        int minX = Math.max(Math.min(selectionStart.chunkX(), selectionEnd.chunkX()), tileX * CHUNKS_PER_TILE);
        int maxX = Math.min(Math.max(selectionStart.chunkX(), selectionEnd.chunkX()), tileX * CHUNKS_PER_TILE + 31);
        int minZ = Math.max(Math.min(selectionStart.chunkZ(), selectionEnd.chunkZ()), tileZ * CHUNKS_PER_TILE);
        int maxZ = Math.min(Math.max(selectionStart.chunkZ(), selectionEnd.chunkZ()), tileZ * CHUNKS_PER_TILE + 31);
        for (int chunkX = minX; chunkX <= maxX; chunkX++) {
            for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
                fillChunk(graphics, color, chunkX - tileX * CHUNKS_PER_TILE, chunkZ - tileZ * CHUNKS_PER_TILE,
                        x, y, w, h);
            }
        }
    }

    private static boolean isAdminMode() {
        return adminMode && ChunkGridPanel.isAdmin();
    }

    private static Component adminModeTitle() {
        return Component.translatable(isAdminMode() ? "territorywar.screen.admin.on" : "territorywar.screen.admin.off");
    }

    private static ChunkKey chunkUnderMouse(RegionMapPanel panel) {
        BlockPos pos = panel.blockPos();
        String dimension = ((LargeMapScreen) panel.getGui()).currentDimension().location().toString();
        return new ChunkKey(pos.getX() >> 4, pos.getZ() >> 4, dimension);
    }

    private static void finishSelection(ChunkKey start, ChunkKey end) {
        if (!isAdminMode()) {
            return;
        }
        int max = MapSelection.maxChunks(start.dimension());
        long area = MapSelection.area(start, end);
        if (area > max) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(Component.translatable("territorywar.map.selection.too_large", area, max)
                        .withStyle(ChatFormatting.RED), false);
            }
            return;
        }
        Set<ChunkKey> chunks = MapSelection.rectangle(start, end);
        chunks.removeIf(chunk -> regionAt(chunk).isPresent());
        if (!chunks.isEmpty()) {
            new CreateRegionScreen(chunks, () -> { }).openGui();
        }
    }

    private static Optional<Region> regionAt(ChunkKey chunk) {
        for (Region region : ClientRegionCache.getRegions()) {
            if (region.containsChunk(chunk)) {
                return Optional.of(region);
            }
        }
        return Optional.empty();
    }

    private static Color4I fill(RegionChunk regionChunk) {
        if (!regionChunk.region().isOwned()) {
            return regionChunk.unownedFill();
        }
        return ChunkGridPanel.ownerTeam(regionChunk.region())
                .map(team -> team.getProperty(TeamProperties.COLOR).withAlpha(REGION_ALPHA))
                .orElse(OWNED);
    }

    private static void fillChunk(GuiGraphics graphics, Color4I color, int localX, int localZ,
            int x, int y, int w, int h) {
        int left = x + localX * w / CHUNKS_PER_TILE;
        int top = y + localZ * h / CHUNKS_PER_TILE;
        int right = x + (localX + 1) * w / CHUNKS_PER_TILE;
        int bottom = y + (localZ + 1) * h / CHUNKS_PER_TILE;
        if (right > left && bottom > top) {
            color.draw(graphics, left, top, right - left, bottom - top);
        }
    }

    /** Region chunks grouped by dimension and map tile, rebuilt whenever the synced regions change. */
    private static Map<String, Map<Long, List<RegionChunk>>> index() {
        List<Region> regions = ClientRegionCache.getRegions();
        if (regions != indexedRegions) {
            Map<String, Map<Long, List<RegionChunk>>> built = new HashMap<>();
            for (Region region : regions) {
                Color4I unownedFill = Color4I.rgb(region.getColorRgb()).withAlpha(REGION_ALPHA);
                for (ChunkKey chunk : region.getChunks()) {
                    built.computeIfAbsent(chunk.dimension(), dimension -> new HashMap<>())
                            .computeIfAbsent(tileKey(chunk.chunkX() >> 5, chunk.chunkZ() >> 5), key -> new ArrayList<>())
                            .add(new RegionChunk(chunk, region, unownedFill));
                }
            }
            chunksByTile = built;
            indexedRegions = regions;
        }
        return chunksByTile;
    }

    private static long tileKey(int tileX, int tileZ) {
        return (long) tileX << 32 | tileZ & 0xFFFFFFFFL;
    }
}
