package net.papierkorb2292.multiscoreboard.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.server.commands.data.BlockDataAccessor$1")
public class BlockDataObjectTypeMixin {

    @WrapOperation(
            method = "access",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"
            )
    )
    private BlockEntity multiScoreboard$allowBlockEntityFromOtherThread(ServerLevel world, BlockPos pos, Operation<BlockEntity> op) {
        var original = op.call(world, pos);
        if(original != null) return original;
        return world.getChunkAt(pos).getBlockEntity(pos);
    }
}
