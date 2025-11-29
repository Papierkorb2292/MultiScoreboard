package net.papierkorb2292.multiscoreboard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.Codecs;

import java.util.*;

public record CustomSidebarPacked(
        List<String> sidebarObjectives,
        Map<String, List<String>> singleScoreSidebarObjectives
) {
    public static CustomSidebarPacked EMPTY = new CustomSidebarPacked(Collections.emptyList(), Collections.emptyMap());

    public static Codec<CustomSidebarPacked> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.STRING.listOf().optionalFieldOf("SidebarSlotObjectives", List.of()).forGetter(CustomSidebarPacked::sidebarObjectives),
                    Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()).optionalFieldOf("SingleScoreSidebars", Map.of()).forGetter(CustomSidebarPacked::singleScoreSidebarObjectives)
            ).apply(instance, CustomSidebarPacked::new));

    public static Codec<Optional<CustomSidebarPacked>> OPTIONAL_CODEC = Codecs.optional(CODEC);

    public interface Container {
        CustomSidebarPacked multiScoreboard$getCustomSidebarPacked();
        void multiScoreboard$setCustomSidebarPacked(CustomSidebarPacked packed);
    }
}
