package com.helianthus.territorywar.command;

import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.core.Region;
import com.helianthus.territorywar.core.RegionId;
import com.helianthus.territorywar.integration.ftb.ChunkClaimIntegrationService;
import com.helianthus.territorywar.integration.ftb.TeamIntegrationService;
import com.helianthus.territorywar.network.TerritoryNetwork;
import com.helianthus.territorywar.service.PurchaseResult;
import com.helianthus.territorywar.service.RegionPurchaseService;
import com.helianthus.territorywar.service.TerritoryManager;
import com.helianthus.territorywar.service.TerritoryService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Optional;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.RegisterCommandsEvent;

/**
 * {@code /territory} command tree: {@code admin create|delete} for operators, {@code buy} for
 * team leaders and {@code menu} to open the territory screen.
 */
public final class TerritoryCommand {
    private static final int ADMIN_PERMISSION_LEVEL = 2;

    private TerritoryCommand() {
    }

    /** Listener for {@link RegisterCommandsEvent} on the Forge event bus. */
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("territory")
                .then(Commands.literal("admin")
                        .requires(source -> source.hasPermission(ADMIN_PERMISSION_LEVEL))
                        .then(Commands.literal("create")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .then(Commands.argument("price", IntegerArgumentType.integer(0))
                                                .executes(TerritoryCommand::create))))
                        .then(Commands.literal("delete")
                                .then(Commands.argument("regionId", UuidArgument.uuid())
                                        .executes(TerritoryCommand::delete))))
                .then(Commands.literal("buy")
                        .then(Commands.argument("regionId", UuidArgument.uuid())
                                .executes(TerritoryCommand::buy)))
                .then(Commands.literal("menu")
                        .executes(TerritoryCommand::menu)));
    }

    /** Creates a for-sale region from the chunk the player is standing in. */
    private static int create(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        String name = StringArgumentType.getString(context, "name");
        int price = IntegerArgumentType.getInteger(context, "price");
        ChunkPos pos = player.chunkPosition();
        ChunkKey chunk = new ChunkKey(pos.x, pos.z, player.level().dimension().location().toString());

        TerritoryService territory = TerritoryManager.get(source.getServer());
        Optional<RegionId> existing = territory.getRegionAt(chunk);
        if (existing.isPresent()) {
            source.sendFailure(Component.translatable("territorywar.command.create.overlap",
                    pos.x, pos.z, existing.get().value().toString()));
            return 0;
        }
        RegionId id = territory.createRegion(name, Set.of(chunk), price, true);
        TerritoryNetwork.syncRegionsToAll(source.getServer());
        String idText = id.value().toString();
        source.sendSuccess(() -> Component.translatable("territorywar.command.create.success", name, idText), true);
        return 1;
    }

    private static int delete(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        RegionId id = new RegionId(UuidArgument.getUuid(context, "regionId"));
        if (!TerritoryManager.get(source.getServer()).deleteRegion(id)) {
            source.sendFailure(Component.translatable("territorywar.command.unknown_region", id.value().toString()));
            return 0;
        }
        TerritoryNetwork.syncRegionsToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("territorywar.command.delete.success", id.value().toString()), true);
        return 1;
    }

    private static int buy(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        RegionId id = new RegionId(UuidArgument.getUuid(context, "regionId"));
        TerritoryService territory = TerritoryManager.get(source.getServer());
        RegionPurchaseService purchases = new RegionPurchaseService(territory,
                new TeamIntegrationService(), new ChunkClaimIntegrationService());
        PurchaseResult result = purchases.purchaseRegion(player, id);
        if (result != PurchaseResult.SUCCESS) {
            source.sendFailure(buyFailure(result, id, territory.getRegion(id), player));
            return 0;
        }
        TerritoryNetwork.syncRegionsToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("territorywar.command.buy.success", id.value().toString()), false);
        return 1;
    }

    /** Opens the territory screen on the player's client. */
    private static int menu(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        TerritoryNetwork.openScreen(context.getSource().getPlayerOrException());
        return 1;
    }

    /** Chat message explaining why a purchase failed. */
    public static Component buyFailure(PurchaseResult result, RegionId id, Optional<Region> region,
            ServerPlayer player) {
        String idText = id.value().toString();
        String name = region.map(Region::getName).orElse(idText);
        return switch (result) {
            case REGION_NOT_FOUND -> Component.translatable("territorywar.command.unknown_region", idText);
            case ALREADY_OWNED -> Component.translatable("territorywar.command.buy.already_owned", name);
            case NOT_FOR_SALE -> Component.translatable("territorywar.command.buy.not_for_sale", name);
            case NOT_TEAM_LEADER -> Component.translatable("territorywar.command.buy.not_team_leader", name);
            case INSUFFICIENT_FUNDS -> Component.translatable("territorywar.command.buy.insufficient_funds",
                    name, region.map(PurchaseResult::price).orElse(0),
                    RegionPurchaseService.CURRENCY.getDescription(),
                    player.getInventory().countItem(RegionPurchaseService.CURRENCY));
            case SUCCESS -> throw new IllegalArgumentException("not a failure: " + result);
        };
    }
}
