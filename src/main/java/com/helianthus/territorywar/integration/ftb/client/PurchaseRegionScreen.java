package com.helianthus.territorywar.integration.ftb.client;

import com.helianthus.territorywar.client.TerritoryClient;
import com.helianthus.territorywar.core.Region;
import com.helianthus.territorywar.service.PurchaseResult;
import com.helianthus.territorywar.service.RegionPurchaseService;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Helianthus-styled confirmation for buying an unowned region clicked on the FTB Chunks large map.
 * The server checks team leadership and payment; closing returns to the map.
 */
final class PurchaseRegionScreen extends BaseScreen {
    /** Helianthus warm yellow. */
    static final int ACCENT_RGB = 0xFFC300;

    private static final Color4I ACCENT = Color4I.rgb(ACCENT_RGB);
    private static final int MARGIN = 8;
    private static final int LINE_GAP = 4;
    private static final int ACCENT_BAR = 3;
    private static final int BUTTON_HEIGHT = 16;

    private final TextField title;
    private final TextField name;
    private final TextField price;
    private final TextField chunks;
    private final SimpleTextButton buyButton;
    private final SimpleTextButton cancelButton;

    PurchaseRegionScreen(Region region) {
        setSize(200, 104);
        int textWidth = width - MARGIN * 2;
        title = new TextField(this).setMaxWidth(textWidth).setColor(ACCENT)
                .setText(Component.translatable("territorywar.map.buy.title"));
        name = new TextField(this).setMaxWidth(textWidth).setColor(ACCENT).setText(Component.literal(region.getName()));
        price = new TextField(this).setMaxWidth(textWidth).setText(Component.translatable("territorywar.screen.price",
                PurchaseResult.price(region), RegionPurchaseService.CURRENCY.getDescription()));
        chunks = new TextField(this).setMaxWidth(textWidth)
                .setText(Component.translatable("territorywar.screen.chunks", region.getChunks().size()));
        buyButton = new SimpleTextButton(this, Component.translatable("territorywar.map.buy.confirm"), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) {
                playClickSound();
                TerritoryClient.requestPurchase(region.getId());
                closeGui(true);
            }
        };
        cancelButton = new SimpleTextButton(this, Component.translatable("gui.cancel"), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) {
                playClickSound();
                closeGui(true);
            }
        };
    }

    @Override
    public void addWidgets() {
        add(title);
        add(name);
        add(price);
        add(chunks);
        add(buyButton);
        add(cancelButton);
    }

    @Override
    public void alignWidgets() {
        int y = MARGIN + ACCENT_BAR;
        for (TextField field : new TextField[] {title, name, price, chunks}) {
            field.setPos(MARGIN, y);
            y += field.height + LINE_GAP;
        }
        int halfWidth = (width - MARGIN * 3) / 2;
        int buttonY = height - MARGIN - BUTTON_HEIGHT;
        buyButton.setPosAndSize(MARGIN, buttonY, halfWidth, BUTTON_HEIGHT);
        cancelButton.setPosAndSize(width - MARGIN - halfWidth, buttonY, halfWidth, BUTTON_HEIGHT);
    }

    @Override
    public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
        super.drawBackground(graphics, theme, x, y, w, h);
        ACCENT.draw(graphics, x, y, w, ACCENT_BAR);
        ACCENT.draw(graphics, x, y + h - 1, w, 1);
        ACCENT.draw(graphics, x, y, 1, h);
        ACCENT.draw(graphics, x + w - 1, y, 1, h);
    }
}
