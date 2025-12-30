package net.papierkorb2292.multiscoreboard.mixin;

import net.minecraft.world.level.storage.CommandStorage;
import net.minecraft.server.commands.data.StorageDataAccessor;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(StorageDataAccessor.class)
public interface StorageDataAccessorAccessor {
    @Accessor
    Identifier getId();

    @Invoker("<init>")
    static StorageDataAccessor callInit(CommandStorage dataCommandStorage, Identifier id) {
        throw new AssertionError();
    }
}
