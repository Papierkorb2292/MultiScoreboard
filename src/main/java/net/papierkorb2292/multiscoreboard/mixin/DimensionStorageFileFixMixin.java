package net.papierkorb2292.multiscoreboard.mixin;

import net.minecraft.util.filefix.fixes.DimensionStorageFileFix;
import net.minecraft.util.filefix.operations.FileFixOperation;
import net.minecraft.util.filefix.operations.FileFixOperations;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(DimensionStorageFileFix.class)
abstract class DimensionStorageFileFixMixin {
    @ModifyArg(
            method = "makeFixer",
            at = @At(
                    value = "INVOKE:FIRST",
                    target = "Lnet/minecraft/util/filefix/operations/FileFixOperations;applyInFolders(Lnet/minecraft/util/filefix/access/FileRelation;Ljava/util/List;)Lnet/minecraft/util/filefix/operations/ApplyInFolders;"
            ),
            slice = @Slice(
                    from = @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/util/filefix/access/FileRelation;DATA:Lnet/minecraft/util/filefix/access/FileRelation;",
                            opcode = Opcodes.GETSTATIC
                    )
            )
    )
    private List<FileFixOperation> addFabricAttachmentsMigration(List<FileFixOperation> original) {
        List<FileFixOperation> operations = new ArrayList<>(original);
        operations.add(FileFixOperations.move("multiscoreboard_nbt.dat", "multiscoreboard/nbt.dat"));
        return Collections.unmodifiableList(operations);
    }
}
