package net.papierkorb2292.multiscoreboard.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.Commands;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.papierkorb2292.multiscoreboard.MultiScoreboard;
import net.papierkorb2292.multiscoreboard.ServerNbtSidebarManager;
import net.papierkorb2292.multiscoreboard.ServerNbtSidebarManagerContainer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(DataCommands.class)
public abstract class DataCommandsMixin {

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyVariable(
            method = "register",
            at = @At("STORE")
    )
    private static DataCommands.DataProvider multiScoreboard$registerNbtSidebarCommand(DataCommands.DataProvider type, @Local LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder) {
        literalArgumentBuilder.then(
                Commands.literal("multiscoreboard")
                        .then(type.wrap(
                                Commands.literal("toggle"), builder ->
                                        builder.executes(context -> multiScoreboard$executeDisplaySidebar(
                                                context.getSource(),
                                                type.access(context),
                                                ServerNbtSidebarManager.ROOT_PATH,
                                                null
                                        ))
                                                .then(Commands.argument("path", NbtPathArgument.nbtPath())
                                                        .executes(context -> multiScoreboard$executeDisplaySidebar(
                                                                context.getSource(),
                                                                type.access(context),
                                                                NbtPathArgument.getPath(context, "path"),
                                                                null
                                                        ))
                                                        .then(
                                                                Commands.argument("name", StringArgumentType.string())
                                                                        .executes(context -> multiScoreboard$executeDisplaySidebar(
                                                                                context.getSource(),
                                                                                type.access(context),
                                                                                NbtPathArgument.getPath(context, "path"),
                                                                                StringArgumentType.getString(context, "name")
                                                                        ))))))
                        .then(type.wrap(
                                Commands.literal("removeAll"), builder ->
                                        builder.executes(context -> multiScoreboard$removeDataObjectSidebar(
                                                context.getSource(),
                                                type.access(context)
                                        )))
                                .executes(context ->
                                        multiScoreboard$removeAllNbtSidebars(context.getSource())
                                )
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests((CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) -> {
                                            var nbtSidebarManager = ((ServerNbtSidebarManagerContainer)context.getSource().getServer()).multiScoreboard$getNbtSidebarManager();
                                            return SharedSuggestionProvider.suggest(nbtSidebarManager.getNames().stream().map(StringArgumentType::escapeIfRequired), builder);
                                        })
                                        .executes(context -> multiScoreboard$removeNbtSidebarByName(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "name")
                                        )))));
        return type;
    }

    @Unique
    private static int multiScoreboard$executeDisplaySidebar(CommandSourceStack source, DataAccessor dataObject, NbtPathArgument.NbtPath path, @Nullable String name) throws CommandSyntaxException {
        var nbtSidebarManager = ((ServerNbtSidebarManagerContainer)source.getServer()).multiScoreboard$getNbtSidebarManager();
        var existingName = nbtSidebarManager.getEntryNameIfMatches(name, dataObject, path);
        if(existingName != null) {
            nbtSidebarManager.removeEntry(existingName);
            source.sendSuccess(() -> Component.translatable("multiscoreboard.commands.data.multiscoreboard.remove", existingName), false);
            return -1;
        }
        var newName = nbtSidebarManager.addEntry(name, dataObject, path);
        source.sendSuccess(() -> Component.translatable("multiscoreboard.commands.data.multiscoreboard.add", newName), false);
        return 1;
    }

    @Unique
    private static int multiScoreboard$removeDataObjectSidebar(CommandSourceStack source, DataAccessor dataObject) {
        var nbtSidebarManager = ((ServerNbtSidebarManagerContainer)source.getServer()).multiScoreboard$getNbtSidebarManager();
        var removedCount = nbtSidebarManager.removeEntriesOfDataObject(dataObject);
        source.sendSuccess(() -> {
            Component dataObjectText = switch(dataObject) {
                case BlockDataAccessorAccessor block ->
                        Component.translatable("multiscoreboard.commands.data.object.block", block.getPos().getX(), block.getPos().getY(), block.getPos().getZ());
                case EntityDataAccessorAccessor entity ->
                        Component.translatable("multiscoreboard.commands.data.object.entity", entity.getEntity().getDisplayName());
                case StorageDataAccessorAccessor storage ->
                        Component.translatable("multiscoreboard.commands.data.object.storage", storage.getId());
                default -> Component.translatable("multiscoreboard.commands.data.object.unknown");
            };
            return MultiScoreboard.getTranslatableTextWithCount("multiscoreboard.commands.data.multiscoreboard.removedAll.dataObject", removedCount, dataObjectText);
        }, false);
        return removedCount;
    }

    @Unique
    private static int multiScoreboard$removeNbtSidebarByName(CommandSourceStack source, String name) {
        var nbtSidebarManager = ((ServerNbtSidebarManagerContainer)source.getServer()).multiScoreboard$getNbtSidebarManager();
        var success = nbtSidebarManager.removeEntry(name);
        if(!success) {
            source.sendSuccess(() -> Component.translatable("multiscoreboard.commands.data.multiscoreboard.name_not_found", name), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("multiscoreboard.commands.data.multiscoreboard.remove", name), false);
        return 1;
    }

    @Unique
    private static int multiScoreboard$removeAllNbtSidebars(CommandSourceStack source) {
        var nbtSidebarManager = ((ServerNbtSidebarManagerContainer)source.getServer()).multiScoreboard$getNbtSidebarManager();
        var removedCount = nbtSidebarManager.removeAllEntries();
        source.sendSuccess(() -> MultiScoreboard.getTranslatableTextWithCount("multiscoreboard.commands.data.multiscoreboard.removedAll", removedCount), false);
        return removedCount;
    }
}
