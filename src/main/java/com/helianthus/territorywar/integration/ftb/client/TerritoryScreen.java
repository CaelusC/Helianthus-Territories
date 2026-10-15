package com.helianthus.territorywar.integration.ftb.client;

import com.helianthus.territorywar.client.ClientRegionCache;
import com.helianthus.territorywar.client.TerritoryClient;
import com.helianthus.territorywar.core.Region;
import com.helianthus.territorywar.service.PurchaseResult;
import com.helianthus.territorywar.service.RegionPurchaseService;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.PanelScrollBar;
import dev.ftb.mods.ftblibrary.ui.ScreenWrapper;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetLayout;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Lists synced regions with their price and status next to a chunk grid around the player;
 * clicking a region or chunk that is for sale buys it.
 */
public final class TerritoryScreen extends BaseScreen {
    private static final int MARGIN = 8;
    private static final int TITLE_HEIGHT = 14;
    private static final int SCROLL_BAR_WIDTH = 8;
    private static final int ROW_HEIGHT = 20;

    private final TextField title;
    private final Panel regionList;
    private final PanelScrollBar scrollBar;
    private final Panel chunkGrid;

    private TerritoryScreen() {
        setSize(400, 180);
        title = new TextField(this).setText(Component.translatable("territorywar.screen.title"));
        regionList = new RegionListPanel(this);
        scrollBar = new PanelScrollBar(this, regionList);
        chunkGrid = new ChunkGridPanel(this);
    }

    public static void open() {
        new TerritoryScreen().openGui();
    }

    /** Rebuilds the open territory screen from {@link ClientRegionCache}, if it is showing. */
    public static void refreshIfOpen() {
        if (Minecraft.getInstance().screen instanceof ScreenWrapper wrapper
                && wrapper.getGui() instanceof TerritoryScreen screen) {
            screen.refreshWidgets();
        }
    }

    @Override
    public void addWidgets() {
        add(title);
        add(regionList);
        add(scrollBar);
        add(chunkGrid);
    }

    @Override
    public void alignWidgets() {
        title.setPos(MARGIN, MARGIN);
        int listTop = MARGIN + TITLE_HEIGHT;
        int listHeight = height - listTop - MARGIN;
        int gridX = width - MARGIN - ChunkGridPanel.SIZE;
        regionList.setPosAndSize(MARGIN, listTop, gridX - MARGIN * 3 - SCROLL_BAR_WIDTH, listHeight);
        regionList.alignWidgets();
        scrollBar.setPosAndSize(gridX - MARGIN - SCROLL_BAR_WIDTH, listTop, SCROLL_BAR_WIDTH, listHeight);
        chunkGrid.setPosAndSize(gridX, listTop, ChunkGridPanel.SIZE, ChunkGridPanel.SIZE);
        chunkGrid.alignWidgets();
    }

    private static final class RegionListPanel extends Panel {
        RegionListPanel(Panel parent) {
            super(parent);
        }

        @Override
        public void addWidgets() {
            if (ClientRegionCache.getRegions().isEmpty()) {
                add(new TextField(this).setText(Component.translatable("territorywar.screen.empty")));
                return;
            }
            for (Region region : ClientRegionCache.getRegions()) {
                add(new RegionButton(this, region));
            }
        }

        @Override
        public void alignWidgets() {
            for (Widget widget : getWidgets()) {
                if (widget instanceof RegionButton) {
                    widget.setSize(width, ROW_HEIGHT);
                }
            }
            align(WidgetLayout.VERTICAL);
        }
    }

    private static final class RegionButton extends SimpleTextButton {
        private final Region region;

        RegionButton(Panel parent, Region region) {
            super(parent, Component.literal(region.getName()).append(" - ").append(status(region)), Icon.empty());
            this.region = region;
        }

        private boolean isBuyable() {
            return !region.isOwned() && region.isForSale();
        }

        @Override
        public void onClicked(MouseButton button) {
            if (!button.isLeft() || !isBuyable()) {
                return;
            }
            playClickSound();
            getGui().openYesNo(Component.literal(region.getName()), Component.translatable("territorywar.screen.buy.confirm",
                    region.getName(), PurchaseResult.price(region), RegionPurchaseService.CURRENCY.getDescription()),
                    () -> TerritoryClient.requestPurchase(region.getId()));
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            list.add(Component.literal(region.getName()));
            list.add(Component.translatable("territorywar.screen.price",
                    PurchaseResult.price(region), RegionPurchaseService.CURRENCY.getDescription()));
            list.add(Component.translatable("territorywar.screen.chunks", region.getChunks().size()));
            list.add(status(region));
            if (isBuyable()) {
                list.add(Component.translatable("territorywar.screen.buy.hint"));
            }
        }

        private static Component status(Region region) {
            if (region.isOwned()) {
                return Component.translatable("territorywar.screen.status.owned");
            }
            return Component.translatable(region.isForSale()
                    ? "territorywar.screen.status.for_sale" : "territorywar.screen.status.not_for_sale");
        }
    }
}
