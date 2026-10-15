package com.helianthus.territorywar.integration.ftb.client;

import com.helianthus.territorywar.client.ClientRegionCache;
import com.helianthus.territorywar.client.TerritoryClient;
import com.helianthus.territorywar.core.Region;
import com.helianthus.territorywar.network.RaidInfoCodec;
import com.helianthus.territorywar.service.RaidService;
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
 * Helianthus-styled prompt for declaring a raid on an enemy region clicked on the FTB Chunks large map,
 * showing the half price, both teams' online counts and the warm-up. The server re-checks everything.
 */
public final class RaidRegionScreen extends BaseScreen {
    private static final Color4I ACCENT = Color4I.rgb(PurchaseRegionScreen.ACCENT_RGB);
    private static final int MARGIN = 8;
    private static final int LINE_GAP = 4;
    private static final int ACCENT_BAR = 3;
    private static final int BUTTON_HEIGHT = 16;

    private final TextField[] lines;
    private final SimpleTextButton raidButton;
    private final SimpleTextButton cancelButton;

    private RaidRegionScreen(Region region, RaidInfoCodec.Info info) {
        setSize(200, 130);
        int textWidth = width - MARGIN * 2;
        lines = new TextField[] {
                new TextField(this).setMaxWidth(textWidth).setColor(ACCENT)
                        .setText(Component.translatable("territorywar.map.raid.title")),
                new TextField(this).setMaxWidth(textWidth).setColor(ACCENT).setText(Component.literal(region.getName())),
                new TextField(this).setMaxWidth(textWidth).setText(Component.translatable("territorywar.screen.price",
                        info.price(), RegionPurchaseService.CURRENCY.getDescription())),
                new TextField(this).setMaxWidth(textWidth).setText(Component.translatable(
                        "territorywar.map.raid.attackers", info.attackersOnline(), info.minAttackers())),
                new TextField(this).setMaxWidth(textWidth).setText(Component.translatable(
                        "territorywar.map.raid.defenders", info.defendersOnline(), info.minDefenders())),
                new TextField(this).setMaxWidth(textWidth).setText(Component.translatable(
                        "territorywar.map.raid.warmup", RaidService.formatHours(info.warmupMinutes())))
        };
        raidButton = new SimpleTextButton(this, Component.translatable("territorywar.map.raid.confirm"), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) {
                playClickSound();
                TerritoryClient.requestDeclareRaid(region.getId());
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

    /** Opens the prompt if the region is still among the synced regions. */
    public static void open(RaidInfoCodec.Info info) {
        for (Region region : ClientRegionCache.getRegions()) {
            if (region.getId().equals(info.regionId())) {
                new RaidRegionScreen(region, info).openGui();
                return;
            }
        }
    }

    @Override
    public void addWidgets() {
        for (TextField line : lines) {
            add(line);
        }
        add(raidButton);
        add(cancelButton);
    }

    @Override
    public void alignWidgets() {
        int y = MARGIN + ACCENT_BAR;
        for (TextField line : lines) {
            line.setPos(MARGIN, y);
            y += line.height + LINE_GAP;
        }
        int halfWidth = (width - MARGIN * 3) / 2;
        int buttonY = height - MARGIN - BUTTON_HEIGHT;
        raidButton.setPosAndSize(MARGIN, buttonY, halfWidth, BUTTON_HEIGHT);
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
