package net.papierkorb2292.multiscoreboard.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Gui.class)
public interface GuiAccessor {

    @Invoker
    void callDisplayScoreboardSidebar(GuiGraphics context, Objective objective);
}
