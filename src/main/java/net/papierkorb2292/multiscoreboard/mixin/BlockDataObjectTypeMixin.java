package net.papierkorb2292.multiscoreboard.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net/minecraft/command/BlockDataObject$1")
public class BlockDataObjectTypeMixin {

    @WrapOperation(
            method = "getObject",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ServerWorld;getBlockEntity(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/entity/BlockEntity;"
            )
    )
    private BlockEntity multiScoreboard$allowBlockEntityFromOtherThread(ServerWorld world, BlockPos pos, Operation<BlockEntity> op) {
        var original = op.call(world, pos);
        if(original != null) return original;
        return world.getWorldChunk(pos).getBlockEntity(pos);
    }
}
