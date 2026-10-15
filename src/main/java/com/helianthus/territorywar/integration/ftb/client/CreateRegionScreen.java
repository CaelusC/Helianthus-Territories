package com.helianthus.territorywar.integration.ftb.client;

import com.helianthus.territorywar.client.TerritoryClient;
import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.network.CreateRegionCodec;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.TextBox;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import java.util.Set;
import net.minecraft.network.chat.Component;

/**
 * Dialog over the territory screen asking an admin for the name, price and for-sale state of a
 * region made from the selected chunks. Closing it returns to the territory screen.
 */
final class CreateRegionScreen extends BaseScreen {
    private static final int MARGIN = 8;
    private static final int FIELD_HEIGHT = 14;
    private static final int BUTTON_HEIGHT = 16;
    private static final String PRICE_PATTERN = "\\d{0,9}";

    private final Set<ChunkKey> chunks;
    private final Runnable onCreated;
    private final TextField title;
    private final TextField nameLabel;
    private final TextBox nameBox;
    private final TextField priceLabel;
    private final TextBox priceBox;
    private final SimpleTextButton forSaleButton;
    private final SimpleTextButton createButton;
    private final SimpleTextButton cancelButton;
    private boolean forSale = true;

    CreateRegionScreen(Set<ChunkKey> chunks, Runnable onCreated) {
        setSize(200, 132);
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
        createButton.setPosAndSize(MARGIN, height - MARGIN - BUTTON_HEIGHT, halfWidth, BUTTON_HEIGHT);
        cancelButton.setPosAndSize(width - MARGIN - halfWidth, height - MARGIN - BUTTON_HEIGHT, halfWidth, BUTTON_HEIGHT);
    }

    private Component forSaleTitle() {
        return Component.translatable(forSale
                ? "territorywar.screen.create.for_sale.yes" : "territorywar.screen.create.for_sale.no");
    }

    private boolean canCreate() {
        return !nameBox.getText().isBlank() && !priceBox.getText().isEmpty();
    }

    private void submit() {
        if (!canCreate()) {
            return;
        }
        TerritoryClient.requestCreateRegion(new CreateRegionCodec.Request(nameBox.getText().trim(), chunks,
                Integer.parseInt(priceBox.getText()), forSale));
        onCreated.run();
        closeGui(true);
    }
}
