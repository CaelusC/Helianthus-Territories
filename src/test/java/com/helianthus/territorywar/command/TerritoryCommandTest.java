package com.helianthus.territorywar.command;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;

class TerritoryCommandTest {
    @Test
    void registersAdminBuyAndMenuBranches() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        TerritoryCommand.register(dispatcher);

        CommandNode<CommandSourceStack> root = dispatcher.getRoot().getChild("territory");
        assertNotNull(root);
        CommandNode<CommandSourceStack> admin = root.getChild("admin");
        assertNotNull(admin);
        assertNotNull(admin.getChild("create").getChild("name").getChild("price").getCommand());
        assertNotNull(admin.getChild("delete").getChild("regionId").getCommand());
        assertNotNull(root.getChild("buy").getChild("regionId").getCommand());
        assertNotNull(root.getChild("menu").getCommand());
    }
}
