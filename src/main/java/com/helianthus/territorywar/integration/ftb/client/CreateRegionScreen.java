package com.helianthus.territorywar.integration.ftb.client;

import com.helianthus.territorywar.client.TerritoryClient;
import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.network.CreateRegionCodec;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.TextBox;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Dialog asking an admin for the name, price, for-sale state and map colour of a region made from
 * the selected chunks. Closing it returns to the screen it was opened from.
 */
final class CreateRegionScreen extends BaseScreen {
    private static final int MARGIN = 8;
    private static final int FIELD_HEIGHT = 14;
    private static final int BUTTON_HEIGHT = 16;
    private static final String PRICE_PATTERN = "\\d{0,9}";
    private static final int SWATCH_SIZE = 14;
    private static final int SWATCH_GAP = 4;
    private static final Color4I SWATCH_BORDER = Color4I.rgba(0xFFFFFFFF);

    private final Set<ChunkKey> chunks;
    private final Runnable onCreated;
    private final TextField title;
    private final TextField nameLabel;
    private final TextBox nameBox;
    private final TextField priceLabel;
    private final TextBox priceBox;
    private final SimpleTextButton forSaleButton;
    private final TextField colorLabel;
    private final TextBox colorBox;
    private final ColorSwatch colorPreview;
    private final List<ColorSwatch> swatches = new ArrayList<>();
    private final SimpleTextButton createButton;
    private final SimpleTextButton cancelButton;
    private boolean forSale = true;

    CreateRegionScreen(Set<ChunkKey> chunks, Runnable onCreated) {
        setSize(200, 176);
        this.chunks = Set.copyOf(chunks);
        this.onCreated = onCreated;
        title = new TextField(this).setText(Component.translatable("territorywar.screen.create.title", this.chunks.size()));
        nameLabel = new TextField(this).setText(Component.translatable("territorywar.screen.create.name"));
        nameBox = new TextBox(this);
        nameBox.setMaxLength(CreateRegionCodec.MAX_NAME_LENGTH);
        priceLabel = new TextField(this).setText(Component.translatable("territorywar.screen.create.price"));
        priceBox = new TextBox(this);
        priceBox.setFilter(text -> text.matches(PRICE_PATTERN));
        forSaleButton = new SimpleTextButton(this, forSaleTitle(), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) {
                playClickSound();
                forSale = !forSale;
                setTitle(forSaleTitle());
            }
        };
        colorLabel = new TextField(this).setText(Component.translatable("territorywar.screen.create.color"));
        colorBox = new TextBox(this);
        colorBox.setFilter(text -> text.matches(RegionColors.PARTIAL_HEX_PATTERN));
        colorBox.setText(RegionColors.toHex(RegionColors.SWATCHES[0]));
        colorPreview = new ColorSwatch(this, -1);
        for (int swatch : RegionColors.SWATCHES) {
            swatches.add(new ColorSwatch(this, swatch));
        }
        createButton = new SimpleTextButton(this, Component.translatable("territorywar.screen.create.confirm"), Icon.empty()) {
            @Override
            public boolean isEnabled() {
                return canCreate();
            }

            @Override
            public void onClicked(MouseButton button) {
                playClickSound();
                submit();
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
        add(nameLabel);
        add(nameBox);
        add(priceLabel);
        add(priceBox);
        add(forSaleButton);
        add(colorLabel);
        add(colorBox);
        add(colorPreview);
        swatches.forEach(this::add);
        add(createButton);
        add(cancelButton);
    }

    @Override
    public void alignWidgets() {
        int fieldWidth = width - MARGIN * 2;
        int halfWidth = (fieldWidth - MARGIN) / 2;
        title.setPos(MARGIN, MARGIN);
        nameLabel.setPos(MARGIN, 24);
        nameBox.setPosAndSize(MARGIN, 34, fieldWidth, FIELD_HEIGHT);
        priceLabel.setPos(MARGIN, 52);
        priceBox.setPosAndSize(MARGIN, 62, fieldWidth, FIELD_HEIGHT);
        forSaleButton.setPosAndSize(MARGIN, 82, fieldWidth, BUTTON_HEIGHT);
        colorLabel.setPos(MARGIN, 104);
        colorBox.setPosAndSize(MARGIN, 114, fieldWidth - SWATCH_SIZE - SWATCH_GAP, FIELD_HEIGHT);
        colorPreview.setPosAndSize(width - MARGIN - SWATCH_SIZE, 114, SWATCH_SIZE, SWATCH_SIZE);
        for (int i = 0; i < swatches.size(); i++) {
            swatches.get(i).setPosAndSize(MARGIN + i * (SWATCH_SIZE + SWATCH_GAP), 132, SWATCH_SIZE, SWATCH_SIZE);
        }
        createButton.setPosAndSize(MARGIN, height - MARGIN - BUTTON_HEIGHT, halfWidth, BUTTON_HEIGHT);
        cancelButton.setPosAndSize(width - MARGIN - halfWidth, height - MARGIN - BUTTON_HEIGHT, halfWidth, BUTTON_HEIGHT);
    }

    private Component forSaleTitle() {
        return Component.translatable(forSale
                ? "territorywar.screen.create.for_sale.yes" : "territorywar.screen.create.for_sale.no");
    }

    private boolean canCreate() {
        return !nameBox.getText().isBlank() && !priceBox.getText().isEmpty() && selectedColor().isPresent();
    }

    private OptionalInt selectedColor() {
        return RegionColors.parseHex(colorBox.getText());
    }

    private void submit() {
        if (!canCreate()) {
            return;
        }
        TerritoryClient.requestCreateRegion(new CreateRegionCodec.Request(nameBox.getText().trim(), chunks,
                Integer.parseInt(priceBox.getText()), forSale, selectedColor().getAsInt()));
        onCreated.run();
        closeGui(true);
    }

    /** Colour square; a preset swatch when {@code rgb >= 0}, otherwise a preview of the typed hex. */
    private final class ColorSwatch extends Widget {
        private final int rgb;

        ColorSwatch(Panel parent, int rgb) {
            super(parent);
            this.rgb = rgb;
        }

        private OptionalInt color() {
            return rgb >= 0 ? OptionalInt.of(rgb) : selectedColor();
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            OptionalInt color = color();
            if (rgb < 0 || color.equals(selectedColor())) {
                SWATCH_BORDER.draw(graphics, x, y, w, h);
            }
            if (color.isPresent()) {
                Color4I.rgba(0xFF000000 | color.getAsInt()).draw(graphics, x + 1, y + 1, w - 2, h - 2);
            }
        }

        @Override
        public boolean mousePressed(MouseButton button) {
            if (rgb < 0 || !isMouseOver() || !button.isLeft()) {
                return false;
            }
            playClickSound();
            colorBox.setText(RegionColors.toHex(rgb));
            return true;
        }
    }
}
