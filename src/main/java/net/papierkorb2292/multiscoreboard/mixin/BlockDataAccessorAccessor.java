
package net.papierkorb2292.multiscoreboard.mixin;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.commands.data.BlockDataAccessor;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockDataAccessor.class)
public interface BlockDataAccessorAccessor {
    @Accessor
    BlockPos getPos();
    @Accessor
    BlockEntity getEntity();
}
