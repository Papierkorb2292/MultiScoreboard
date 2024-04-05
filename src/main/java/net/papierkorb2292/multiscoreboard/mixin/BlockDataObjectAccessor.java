package net.papierkorb2292.multiscoreboard.mixin;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.BlockDataObject;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockDataObject.class)
public interface BlockDataObjectAccessor {
    @Accessor
    BlockPos getPos();
    @Accessor
    BlockEntity getBlockEntity();
}
