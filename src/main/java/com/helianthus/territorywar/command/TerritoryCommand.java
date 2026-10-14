package com.helianthus.territorywar.command;

import com.helianthus.territorywar.core.ChunkKey;
import com.helianthus.territorywar.core.RegionId;
import com.helianthus.territorywar.integration.ftb.ChunkClaimIntegrationService;
import com.helianthus.territorywar.integration.ftb.TeamIntegrationService;
import com.helianthus.territorywar.service.RegionPurchaseService;
import com.helianthus.territorywar.service.TerritoryManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.RegisterCommandsEvent;

/**
 * {@code /territory} command tree: {@code admin create|delete} for operators and {@code buy} for
 * team leaders.
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
                                .executes(TerritoryCommand::buy))));
    }

    /** Creates a for-sale region from the chunk the player is standing in. */
    private static int create(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        String name = StringArgumentType.getString(context, "name");
        int price = IntegerArgumentType.getInteger(context, "price");
        ChunkPos pos = player.chunkPosition();
        ChunkKey chunk = new ChunkKey(pos.x, pos.z, player.level().dimension().location().toString());

        RegionId id;
        try {
            id = TerritoryManager.get(source.getServer()).createRegion(name, Set.of(chunk), price, true);
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.translatable("territorywar.command.create.overlap"));
            return 0;
        }
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
        source.sendSuccess(() -> Component.translatable("territorywar.command.delete.success", id.value().toString()), true);
        return 1;
    }

    private static int buy(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        RegionId id = new RegionId(UuidArgument.getUuid(context, "regionId"));
        RegionPurchaseService purchases = new RegionPurchaseService(TerritoryManager.get(source.getServer()),
                new TeamIntegrationService(), new ChunkClaimIntegrationService());
        if (!purchases.purchaseRegion(player, id)) {
            source.sendFailure(Component.translatable("territorywar.command.buy.failed", id.value().toString()));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("territorywar.command.buy.success", id.value().toString()), false);
        return 1;
    }
}
