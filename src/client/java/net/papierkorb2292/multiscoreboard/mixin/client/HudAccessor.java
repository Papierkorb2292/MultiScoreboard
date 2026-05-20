package net.papierkorb2292.multiscoreboard.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Hud.class)
public interface HudAccessor {

    @Invoker
    void callDisplayScoreboardSidebar(GuiGraphicsExtractor context, Objective objective);
}
