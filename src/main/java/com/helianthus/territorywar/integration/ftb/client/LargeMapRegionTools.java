package com.helianthus.territorywar.integration.ftb.client;

import com.helianthus.territorywar.client.ClientRegionCache;
import com.helianthus.territorywar.client.TerritoryClient;
import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.core.Region;
import com.helianthus.territorywar.core.RegionId;
import com.helianthus.territorywar.network.RegionEditCodec;
import dev.ftb.mods.ftbchunks.client.gui.LargeMapScreen;
import dev.ftb.mods.ftbchunks.client.gui.RegionMapPanel;
import dev.ftb.mods.ftbchunks.client.map.MapRegion;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Territory tools on the FTB Chunks large map, driven by the {@code mixin} hooks. Fills region chunks
 * with their admin colour while unowned and their owner's FTB team colour once bought, outlines each
 * region and labels it with its name and owner. In admin mode, dragging paints free chunks for a new
 * region or for the focused region, clicking a region focuses it, and the erase tool marks chunks of the
 * focused region for removal; otherwise clicking an unowned region that is for sale opens
 * {@link PurchaseRegionScreen}.
 */
public final class LargeMapRegionTools {
    private static final int REGION_ALPHA = 0x66;
    private static final Color4I OWNED = Color4I.rgba(0x66FF0000);
    private static final Color4I PAINTED = Color4I.rgba(0x8000FFFF);
    private static final Color4I ERASED = Color4I.rgba(0x80FF0000);
    private static final Color4I FOCUSED = Color4I.rgba(0x40FFFFFF);
    private static final int LABEL_COLOR = 0xFFFFFFFF;
    private static final int CHUNKS_PER_TILE = 32;
    private static final int BLOCKS_PER_TILE = 512;
    private static final int BUTTON_WIDTH = 90;
    private static final int BUTTON_HEIGHT = 16;
    private static final int BUTTON_GAP = 2;
    private static final int CLICK_TOLERANCE = 2;

    private enum Tool {
        PAINT,
        ERASE
    }

    private static final Set<ChunkKey> painted = new HashSet<>();
    private static final Set<ChunkKey> erased = new HashSet<>();
    private static boolean adminMode;
    private static Tool tool = Tool.PAINT;
    private static boolean painting;
    private static RegionId focusedRegion;
    private static ChunkKey pressedChunk;
    private static int pressedMouseX;
    private static int pressedMouseY;
    private static boolean tileDrawn;
    private static int originX;
    private static int originY;
    private static double pixelsPerBlock;
    private static List<Region> indexedRegions;
    private static Map<String, Map<Long, List<RegionChunk>>> chunksByTile = Map.of();
    private static Map<ChunkKey, Region> regionsByChunk = Map.of();

    private LargeMapRegionTools() {
    }

    private record RegionChunk(ChunkKey chunk, Region region, Color4I unownedFill) {
    }

    /** Admin toggle and admin tool buttons, each only drawn when it applies. */
    public static List<SimpleTextButton> createButtons(Panel screen) {
        painting = false;
        pressedChunk = null;
        List<SimpleTextButton> buttons = new ArrayList<>();
        buttons.add(button(screen, LargeMapRegionTools::adminModeTitle, ChunkGridPanel::isAdmin, () -> {
            adminMode = !adminMode && ChunkGridPanel.isAdmin();
            painting = false;
        }, "territorywar.map.admin.hint"));
        buttons.add(button(screen, LargeMapRegionTools::toolTitle, LargeMapRegionTools::isAdminMode, () -> {
            tool = tool == Tool.PAINT ? Tool.ERASE : Tool.PAINT;
            painting = false;
        }, "territorywar.map.tool.hint"));
        buttons.add(button(screen, () -> Component.translatable("territorywar.screen.create"),
                () -> isAdminMode() && !painted.isEmpty(),
                () -> new CreateRegionScreen(painted, painted::clear).openGui(), null));
        buttons.add(button(screen, () -> Component.translatable("territorywar.map.add"),
                () -> isAdminMode() && focused().isPresent() && !painted.isEmpty(),
                LargeMapRegionTools::addPaintedToFocused, null));
        buttons.add(button(screen, () -> Component.translatable("territorywar.map.remove"),
                () -> isAdminMode() && focused().isPresent() && !erased.isEmpty(),
                LargeMapRegionTools::removeErasedFromFocused, null));
        buttons.add(button(screen, () -> Component.translatable("territorywar.map.delete"),
                () -> isAdminMode() && focused().isPresent(),
                () -> focused().ifPresent(region -> screen.getGui().openYesNo(
                        Component.translatable("territorywar.map.delete.confirm", region.getName()),
                        Component.empty(), () -> deleteRegion(region.getId()))), null));
        buttons.add(button(screen, () -> Component.translatable("territorywar.map.clear"),
                () -> isAdminMode() && (!painted.isEmpty() || !erased.isEmpty() || focused().isPresent()),
                LargeMapRegionTools::clearSelection, null));
        return buttons;
    }

    /** Stacks the territory buttons down the top right corner of the large map. */
    public static void alignButtons(List<SimpleTextButton> buttons, int screenWidth) {
        for (int i = 0; i < buttons.size(); i++) {
            SimpleTextButton button = buttons.get(i);
            button.setPosAndSize(screenWidth - BUTTON_WIDTH - 1, 1 + i * (BUTTON_HEIGHT + BUTTON_GAP),
                    BUTTON_WIDTH, BUTTON_HEIGHT);
        }
        buttons.get(0).setTitle(adminModeTitle());
        buttons.get(1).setTitle(toolTitle());
    }

    /** Returns true if the press is handled by an admin tool and must not pan the map. */
    public static boolean onMapPressed(RegionMapPanel panel, MouseButton button) {
        if (!button.isLeft() || !panel.isMouseOver()) {
            return false;
        }
        ChunkKey chunk = chunkUnderMouse(panel);
        if (isAdminMode()) {
            Optional<Region> region = regionAt(chunk);
            if (region.isPresent() && !region.get().getId().equals(focusedRegion)) {
                focusedRegion = region.get().getId();
                erased.clear();
                return true;
            }
            painting = true;
            paint(chunk);
            return true;
        }
        pressedChunk = chunk;
        pressedMouseX = panel.getMouseX();
        pressedMouseY = panel.getMouseY();
        return false;
    }

    /** Paints under the mouse while held, then draws region labels over the map and its icons. */
    public static void onMapDrawn(RegionMapPanel panel, GuiGraphics graphics) {
        if (painting) {
            if (isAdminMode()) {
                paint(chunkUnderMouse(panel));
            } else {
                painting = false;
            }
        }
        if (tileDrawn) {
            tileDrawn = false;
            drawLabels(graphics, ((LargeMapScreen) panel.getGui()).currentDimension().location().toString());
        }
    }

    /** Ends painting, or opens the purchase dialog if a press was a click on a region. */
    public static void onMapReleased(RegionMapPanel panel, MouseButton button) {
        if (painting) {
            painting = false;
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

    /** Fills and outlines the region and selected chunks inside one 512x512 block map tile drawn at x, y, w, h. */
    public static void drawTileOverlay(GuiGraphics graphics, MapRegion tile, int x, int y, int w, int h) {
        String dimension = tile.dimension.dimension.location().toString();
        int tileX = tile.pos.x();
        int tileZ = tile.pos.z();
        recordTransform(tileX, tileZ, x, y, w, h);
        List<RegionChunk> chunks = index().getOrDefault(dimension, Map.of()).get(tileKey(tileX, tileZ));
        if (chunks != null) {
            Optional<Region> focused = isAdminMode() ? focused() : Optional.empty();
            for (RegionChunk regionChunk : chunks) {
                int localX = regionChunk.chunk().chunkX() - tileX * CHUNKS_PER_TILE;
                int localZ = regionChunk.chunk().chunkZ() - tileZ * CHUNKS_PER_TILE;
                Color4I fill = fill(regionChunk);
                fillChunk(graphics, fill, localX, localZ, x, y, w, h);
                if (focused.isPresent() && focused.get().getId().equals(regionChunk.region().getId())) {
                    fillChunk(graphics, FOCUSED, localX, localZ, x, y, w, h);
                }
                drawBorder(graphics, fill.withAlpha(0xFF), regionChunk, localX, localZ, x, y, w, h);
            }
        }
        drawSelection(graphics, painted, PAINTED, dimension, tileX, tileZ, x, y, w, h);
        drawSelection(graphics, erased, ERASED, dimension, tileX, tileZ, x, y, w, h);
    }

    private static SimpleTextButton button(Panel screen, Supplier<Component> titleSupplier, BooleanSupplier visible,
            Runnable action, String hintKey) {
        return new SimpleTextButton(screen, titleSupplier.get(), Icon.empty()) {
            @Override
            public boolean shouldDraw() {
                return visible.getAsBoolean();
            }

            @Override
            public void onClicked(MouseButton button) {
                playClickSound();
                action.run();
                setTitle(titleSupplier.get());
            }

            @Override
            public void addMouseOverText(TooltipList list) {
                if (hintKey != null) {
                    list.add(Component.translatable(hintKey));
                }
            }
        };
    }

    private static boolean isAdminMode() {
        return adminMode && ChunkGridPanel.isAdmin();
    }

    private static Component adminModeTitle() {
        return Component.translatable(isAdminMode() ? "territorywar.screen.admin.on" : "territorywar.screen.admin.off");
    }

    private static Component toolTitle() {
        return Component.translatable(tool == Tool.PAINT ? "territorywar.map.tool.paint" : "territorywar.map.tool.erase");
    }

    private static ChunkKey chunkUnderMouse(RegionMapPanel panel) {
        BlockPos pos = panel.blockPos();
        String dimension = ((LargeMapScreen) panel.getGui()).currentDimension().location().toString();
        return new ChunkKey(pos.getX() >> 4, pos.getZ() >> 4, dimension);
    }

    /** Paint adds free chunks to the selection; erase removes painted chunks or marks focused region chunks. */
    private static void paint(ChunkKey chunk) {
        Optional<Region> region = regionAt(chunk);
        if (tool == Tool.ERASE) {
            if (!painted.remove(chunk) && region.isPresent() && region.get().getId().equals(focusedRegion)) {
                addWithinLimit(erased, chunk);
            }
            return;
        }
        if (region.isEmpty()) {
            addWithinLimit(painted, chunk);
        }
    }

    /** Adds the chunk unless the set would no longer fit in one packet; a new dimension starts a new set. */
    private static void addWithinLimit(Set<ChunkKey> chunks, ChunkKey chunk) {
        if (chunks.contains(chunk)) {
            return;
        }
        if (!chunks.isEmpty() && !chunks.iterator().next().dimension().equals(chunk.dimension())) {
            chunks.clear();
        }
        int max = MapSelection.maxChunks(chunk.dimension());
        if (chunks.size() >= max) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(Component.translatable("territorywar.map.selection.too_large",
                        chunks.size() + 1, max).withStyle(ChatFormatting.RED), true);
            }
            return;
        }
        chunks.add(chunk);
    }

    private static void addPaintedToFocused() {
        focused().ifPresent(region -> {
            TerritoryClient.requestEditRegionChunks(new RegionEditCodec.ChunkEdit(region.getId(), painted, Set.of()));
            painted.clear();
        });
    }

    private static void removeErasedFromFocused() {
        focused().ifPresent(region -> {
            TerritoryClient.requestEditRegionChunks(new RegionEditCodec.ChunkEdit(region.getId(), Set.of(), erased));
            erased.clear();
        });
    }

    private static void deleteRegion(RegionId id) {
        TerritoryClient.requestDeleteRegion(id);
        if (id.equals(focusedRegion)) {
            focusedRegion = null;
            erased.clear();
        }
    }

    private static void clearSelection() {
        painted.clear();
        erased.clear();
        focusedRegion = null;
    }

    /** The focused region, while it still exists in the synced regions. */
    private static Optional<Region> focused() {
        if (focusedRegion == null) {
            return Optional.empty();
        }
        for (Region region : ClientRegionCache.getRegions()) {
            if (region.getId().equals(focusedRegion)) {
                return Optional.of(region);
            }
        }
        return Optional.empty();
    }

    private static Optional<Region> regionAt(ChunkKey chunk) {
        index();
        return Optional.ofNullable(regionsByChunk.get(chunk));
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

    /** Draws a one pixel line on each side of the chunk whose neighbour is not in the same region. */
    private static void drawBorder(GuiGraphics graphics, Color4I color, RegionChunk regionChunk, int localX, int localZ,
            int x, int y, int w, int h) {
        int left = x + localX * w / CHUNKS_PER_TILE;
        int top = y + localZ * h / CHUNKS_PER_TILE;
        int right = x + (localX + 1) * w / CHUNKS_PER_TILE;
        int bottom = y + (localZ + 1) * h / CHUNKS_PER_TILE;
        if (right <= left || bottom <= top) {
            return;
        }
        ChunkKey chunk = regionChunk.chunk();
        Region region = regionChunk.region();
        if (!sameRegion(region, chunk.chunkX() - 1, chunk.chunkZ(), chunk.dimension())) {
            color.draw(graphics, left, top, 1, bottom - top);
        }
        if (!sameRegion(region, chunk.chunkX() + 1, chunk.chunkZ(), chunk.dimension())) {
            color.draw(graphics, right - 1, top, 1, bottom - top);
        }
        if (!sameRegion(region, chunk.chunkX(), chunk.chunkZ() - 1, chunk.dimension())) {
            color.draw(graphics, left, top, right - left, 1);
        }
        if (!sameRegion(region, chunk.chunkX(), chunk.chunkZ() + 1, chunk.dimension())) {
            color.draw(graphics, left, bottom - 1, right - left, 1);
        }
    }

    private static boolean sameRegion(Region region, int chunkX, int chunkZ, String dimension) {
        Region neighbour = regionsByChunk.get(new ChunkKey(chunkX, chunkZ, dimension));
        return neighbour != null && neighbour.getId().equals(region.getId());
    }

    private static void drawSelection(GuiGraphics graphics, Set<ChunkKey> chunks, Color4I color, String dimension,
            int tileX, int tileZ, int x, int y, int w, int h) {
        for (ChunkKey chunk : chunks) {
            if (chunk.dimension().equals(dimension) && chunk.chunkX() >> 5 == tileX && chunk.chunkZ() >> 5 == tileZ) {
                fillChunk(graphics, color, chunk.chunkX() - tileX * CHUNKS_PER_TILE,
                        chunk.chunkZ() - tileZ * CHUNKS_PER_TILE, x, y, w, h);
            }
        }
    }

    /** Remembers where block 0, 0 is on screen this frame, from any drawn tile, for placing labels. */
    private static void recordTransform(int tileX, int tileZ, int x, int y, int w, int h) {
        tileDrawn = true;
        originX = x - tileX * w;
        originY = y - tileZ * h;
        pixelsPerBlock = (double) w / BLOCKS_PER_TILE;
    }

    /** Draws each region's name, and its owner team below it, at the centre of its chunks in the dimension. */
    private static void drawLabels(GuiGraphics graphics, String dimension) {
        Font font = Minecraft.getInstance().font;
        for (Region region : ClientRegionCache.getRegions()) {
            long sumX = 0;
            long sumZ = 0;
            int count = 0;
            for (ChunkKey chunk : region.getChunks()) {
                if (chunk.dimension().equals(dimension)) {
                    sumX += chunk.chunkX();
                    sumZ += chunk.chunkZ();
                    count++;
                }
            }
            if (count == 0) {
                continue;
            }
            double centerX = ((double) sumX / count + 0.5) * 16;
            double centerZ = ((double) sumZ / count + 0.5) * 16;
            int screenX = originX + (int) Math.round(centerX * pixelsPerBlock);
            int screenY = originY + (int) Math.round(centerZ * pixelsPerBlock);
            Optional<Component> owner = ChunkGridPanel.ownerTeam(region).map(Team::getName);
            int top = screenY - (owner.isPresent() ? font.lineHeight : font.lineHeight / 2);
            graphics.drawCenteredString(font, region.getName(), screenX, top, LABEL_COLOR);
            owner.ifPresent(name -> graphics.drawCenteredString(font,
                    Component.translatable("territorywar.screen.owner", name), screenX, top + font.lineHeight,
                    LABEL_COLOR));
        }
    }

    /** Region chunks grouped by dimension and map tile, rebuilt whenever the synced regions change. */
    private static Map<String, Map<Long, List<RegionChunk>>> index() {
        List<Region> regions = ClientRegionCache.getRegions();
        if (regions != indexedRegions) {
            Map<String, Map<Long, List<RegionChunk>>> built = new HashMap<>();
            Map<ChunkKey, Region> byChunk = new HashMap<>();
            for (Region region : regions) {
                Color4I unownedFill = Color4I.rgb(region.getColorRgb()).withAlpha(REGION_ALPHA);
                for (ChunkKey chunk : region.getChunks()) {
                    built.computeIfAbsent(chunk.dimension(), dimension -> new HashMap<>())
                            .computeIfAbsent(tileKey(chunk.chunkX() >> 5, chunk.chunkZ() >> 5), key -> new ArrayList<>())
                            .add(new RegionChunk(chunk, region, unownedFill));
                    byChunk.put(chunk, region);
                }
            }
            chunksByTile = built;
            regionsByChunk = byChunk;
            indexedRegions = regions;
        }
        return chunksByTile;
    }

    private static long tileKey(int tileX, int tileZ) {
        return (long) tileX << 32 | tileZ & 0xFFFFFFFFL;
    }
}
