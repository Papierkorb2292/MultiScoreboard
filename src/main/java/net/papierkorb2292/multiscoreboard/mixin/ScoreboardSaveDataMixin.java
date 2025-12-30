package net.papierkorb2292.multiscoreboard.mixin;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.world.scores.ScoreboardSaveData;
import net.minecraft.world.level.saveddata.SavedData;
import net.papierkorb2292.multiscoreboard.CustomSidebarPacked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Optional;

@Mixin(ScoreboardSaveData.class)
public class ScoreboardSaveDataMixin extends SavedData implements CustomSidebarPacked.Container {

    private CustomSidebarPacked multiScoreboard$customSidebarPacked = null;

    @ModifyArg(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/saveddata/SavedDataType;<init>(Ljava/lang/String;Ljava/util/function/Supplier;Lcom/mojang/serialization/Codec;Lnet/minecraft/util/datafix/DataFixTypes;)V"
            )
    )
    private static Codec<ScoreboardSaveData> multiScoreboard$addCustomSidebarToCodec(Codec<ScoreboardSaveData> codec) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<ScoreboardSaveData, T>> decode(DynamicOps<T> ops, T input) {
                return codec.decode(ops, input).flatMap(state ->
                        CustomSidebarPacked.OPTIONAL_CODEC.decode(ops, input).map(packed -> {
                            if(packed.getFirst().isPresent()) {
                                ((CustomSidebarPacked.Container) state.getFirst()).multiScoreboard$setCustomSidebarPacked(packed.getFirst().get());
                            }
                            return state;
                    })
                );
            }

            @Override
            public <T> DataResult<T> encode(ScoreboardSaveData input, DynamicOps<T> ops, T prefix) {
                return codec.encode(input, ops, prefix).flatMap(data -> {
                    final var packed = Optional.ofNullable(((CustomSidebarPacked.Container)input).multiScoreboard$getCustomSidebarPacked());
                    return CustomSidebarPacked.OPTIONAL_CODEC.encode(packed, ops, data);
                });
            }
        };
    }

    @Override
    public CustomSidebarPacked multiScoreboard$getCustomSidebarPacked() {
        return multiScoreboard$customSidebarPacked;
    }

    @Override
    public void multiScoreboard$setCustomSidebarPacked(CustomSidebarPacked packed) {
        if(!packed.equals(multiScoreboard$customSidebarPacked)) {
            multiScoreboard$customSidebarPacked = packed;
            setDirty();
        }
    }
}
