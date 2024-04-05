package net.papierkorb2292.multiscoreboard.mixin;

import net.minecraft.command.DataCommandStorage;
import net.minecraft.command.StorageDataObject;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(StorageDataObject.class)
public interface StorageDataObjectAccessor {
    @Accessor
    Identifier getId();

    @Invoker("<init>")
    static StorageDataObject callInit(DataCommandStorage dataCommandStorage, Identifier id) {
        throw new AssertionError();
    }
}
