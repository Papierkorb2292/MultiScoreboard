package net.papierkorb2292.multiscoreboard.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.SuggestionContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.CompletableFuture;

@Mixin(CommandDispatcher.class)
public class CommandDispatcherMixin {

    @WrapOperation(
            method = "getCompletionSuggestions(Lcom/mojang/brigadier/ParseResults;I)Ljava/util/concurrent/CompletableFuture;",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/tree/CommandNode;listSuggestions(Lcom/mojang/brigadier/context/CommandContext;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)Ljava/util/concurrent/CompletableFuture;"
            )
    )
    private <S> CompletableFuture<Suggestions> multiScoreboard$getSuggestionsFromNbtAutocomplete(CommandNode<S> node, CommandContext<S> context, SuggestionsBuilder suggestionsBuilder, Operation<CompletableFuture<Suggestions>> op) {
        final var nodes = context.getNodes();
        if(node.getName().equals("path") && nodes.size() > 2
                && nodes.get(0).getNode() instanceof LiteralCommandNode<S> literal1 && literal1.getLiteral().equals("data")
                && nodes.get(1).getNode() instanceof LiteralCommandNode<S> literal2 && literal2.getLiteral().equals("multiscoreboard"))
        {
            // Replace second literal with 'get' so nbt autocomplete will generate suggestions
            nodes.set(1, new ParsedCommandNode<S>(literal1.getChild("get"), nodes.get(1).getRange()));
            // Remove third literal ('toggle'), so nbt autocomplete can find 'entity' or 'block'
            nodes.remove(2);
        }
        return op.call(node, context, suggestionsBuilder);
    }
}
