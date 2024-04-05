package net.papierkorb2292.multiscoreboard.mixin;

import net.minecraft.command.EntityDataObject;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityDataObject.class)
public interface EntityDataObjectAccessor {
    @Accessor
    Entity getEntity();
}
