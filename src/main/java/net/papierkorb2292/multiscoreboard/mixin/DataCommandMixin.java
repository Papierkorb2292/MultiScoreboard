package net.papierkorb2292.multiscoreboard.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.DataCommandObject;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.DataCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.papierkorb2292.multiscoreboard.ServerNbtSidebarManager;
import net.papierkorb2292.multiscoreboard.ServerNbtSidebarManagerContainer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(DataCommand.class)
public abstract class DataCommandMixin {

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyVariable(
            method = "register",
            at = @At("STORE")
    )
    private static DataCommand.ObjectType multiScoreboard$registerNbtSidebarCommand(DataCommand.ObjectType type, @Local LiteralArgumentBuilder<ServerCommandSource> literalArgumentBuilder) {
        literalArgumentBuilder.then(
                CommandManager.literal("multiscoreboard")
                        .then(type.addArgumentsToBuilder(
                                CommandManager.literal("toggle"), builder ->
                                        builder.executes(context -> multiScoreboard$executeDisplaySidebar(
                                                context.getSource(),
                                                type.getObject(context),
                                                ServerNbtSidebarManager.ROOT_PATH,
                                                null
                                        ))
                                                .then(CommandManager.argument("path", NbtPathArgumentType.nbtPath())
                                                        .executes(context -> multiScoreboard$executeDisplaySidebar(
                                                                context.getSource(),
                                                                type.getObject(context),
                                                                NbtPathArgumentType.getNbtPath(context, "path"),
                                                                null
                                                        ))
                                                        .then(
                                                                CommandManager.argument("name", StringArgumentType.string())
                                                                        .executes(context -> multiScoreboard$executeDisplaySidebar(
                                                                                context.getSource(),
                                                                                type.getObject(context),
                                                                                NbtPathArgumentType.getNbtPath(context, "path"),
                                                                                StringArgumentType.getString(context, "name")
                                                                        ))))))
                        .then(type.addArgumentsToBuilder(
                                CommandManager.literal("removeAll"), builder ->
                                        builder.executes(context -> multiScoreboard$removeDataObjectSidebar(
                                                context.getSource(),
                                                type.getObject(context)
                                        ))))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .suggests((CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) -> {
                                            var nbtSidebarManager = ((ServerNbtSidebarManagerContainer)context.getSource().getServer()).multiScoreboard$getNbtSidebarManager();
                                            return CommandSource.suggestMatching(nbtSidebarManager.getNames().stream().map(StringArgumentType::escapeIfRequired), builder);
                                        })
                                        .executes(context -> multiScoreboard$removeNbtSidebarByName(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "name")
                                        )))));
        return type;
    }

    @Unique
    private static int multiScoreboard$executeDisplaySidebar(ServerCommandSource source, DataCommandObject dataObject, NbtPathArgumentType.NbtPath path, @Nullable String name) throws CommandSyntaxException {
        var nbtSidebarManager = ((ServerNbtSidebarManagerContainer)source.getServer()).multiScoreboard$getNbtSidebarManager();
        var existingName = nbtSidebarManager.getEntryNameIfMatches(name, dataObject, path);
        if(existingName != null) {
            nbtSidebarManager.removeEntry(existingName);
            source.sendFeedback(() -> Text.translatable("multiscoreboard.commands.data.multiscoreboard.remove", existingName), false);
            return -1;
        }
        var newName = nbtSidebarManager.addEntry(name, dataObject, path);
        source.sendFeedback(() -> Text.translatable("multiscoreboard.commands.data.multiscoreboard.add", newName), false);
        return 1;
    }

    @Unique
    private static int multiScoreboard$removeDataObjectSidebar(ServerCommandSource source, DataCommandObject dataObject) {
        var nbtSidebarManager = ((ServerNbtSidebarManagerContainer)source.getServer()).multiScoreboard$getNbtSidebarManager();
        var removedCount = nbtSidebarManager.removeEntriesOfDataObject(dataObject);
        source.sendFeedback(() -> Text.translatable("multiscoreboard.commands.data.multiscoreboard.remove_data_object", removedCount), false);
        return removedCount;
    }

    @Unique
    private static int multiScoreboard$removeNbtSidebarByName(ServerCommandSource source, String name) {
        var nbtSidebarManager = ((ServerNbtSidebarManagerContainer)source.getServer()).multiScoreboard$getNbtSidebarManager();
        var success = nbtSidebarManager.removeEntry(name);
        if(!success) {
            source.sendFeedback(() -> Text.translatable("multiscoreboard.commands.data.multiscoreboard.name_not_found", name), false);
            return 0;
        }
        source.sendFeedback(() -> Text.translatable("multiscoreboard.commands.data.multiscoreboard.remove", name), false);
        return 1;
    }
}
