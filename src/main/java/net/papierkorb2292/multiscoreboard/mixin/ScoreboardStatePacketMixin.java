package net.papierkorb2292.multiscoreboard.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.scoreboard.ScoreboardState;
import net.papierkorb2292.multiscoreboard.CustomSidebarPacked;
import net.papierkorb2292.multiscoreboard.MultiScoreboardRecordRecorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ScoreboardState.Packed.class)
public class ScoreboardStatePacketMixin {

    @ModifyReturnValue(
            method = "method_67463",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/datafixers/Products$P4;apply(Lcom/mojang/datafixers/kinds/Applicative;Lcom/mojang/datafixers/util/Function4;)Lcom/mojang/datafixers/kinds/App;"
            )
    )
    private static App<RecordCodecBuilder.Mu<ScoreboardState.Packed>, ScoreboardState.Packed> multiScoreboard$addCustomSidebarToCodec(App<RecordCodecBuilder.Mu<ScoreboardState.Packed>, ScoreboardState.Packed> app, RecordCodecBuilder.Instance<ScoreboardState.Packed> instance) {
        return instance.group(
                app,
                CustomSidebarPacked.MAP_CODEC.forGetter(packed ->
                    MultiScoreboardRecordRecorder.CUSTOM_SIDEBAR_KEY.getOptional(packed).orElse(CustomSidebarPacked.EMPTY)
                )
        ).apply(instance, MultiScoreboardRecordRecorder::copyScoreboardStateWithCustomState);
    }
}
