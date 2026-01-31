package net.papierkorb2292.multiscoreboard.client;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.nbt.Tag;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.papierkorb2292.multiscoreboard.*;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Stream;

public class MultiScoreboardClient implements ClientModInitializer {
    private static boolean useMultiScoreboard = false;
    private static final Map<String, List<Tag>> nbtSidebars = new HashMap<>();
    private static int sidebarScrollTranslation = 0;
    private static final int scrollAmount = 10;
    private static final int maxTranslationBoundary = 10;
    private static KeyMapping scrollUpKeyBinding;
    private static KeyMapping scrollDownKeyBinding;

    public static final int sidebarGap = 11;

    public static final Component NO_DATA_TEXT = Component.translatable("multiScoreboard.sidebarNbt.noData").withStyle(style -> style.withColor(CommonColors.RED));

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SetUseMultiScoreboardS2CPacket.ID, (packet, context) -> {
            useMultiScoreboard = packet.useMultiScoreboard();
        });
        ClientPlayNetworking.registerGlobalReceiver(SetNbtSidebarS2CPacket.ID, (packet, context) -> {
            nbtSidebars.put(packet.nbtSidebarName(), packet.nbt());
        });
        ClientPlayNetworking.registerGlobalReceiver(RemoveNbtSidebarS2CPacket.ID, (packet, context) -> {
            nbtSidebars.remove(packet.nbtSidebarName());
            clampScrollTranslation();
        });
        ClientPlayNetworking.registerGlobalReceiver(ToggleSingleScoreSidebarS2CPacket.ID, (packet, context) -> {
            var scoreboard = context.player().level().getScoreboard();
            var objective = scoreboard.getObjective(packet.objective());
            if(objective == null) return;
            ((MultiScoreboardSidebarInterface)scoreboard).multiScoreboard$toggleSingleScoreSidebar(objective, packet.score());
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            useMultiScoreboard = false;
            nbtSidebars.clear();
        });

        final KeyMapping.Category keybindCategory = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("multiscoreboard", "generic"));
        scrollUpKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.multiscoreboard.scroll_up",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UP,
                keybindCategory
        ));
        scrollDownKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.multiscoreboard.scroll_down",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_DOWN,
                keybindCategory
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var wasUpPressed = scrollUpKeyBinding.consumeClick();
            var wasDownPressed = scrollDownKeyBinding.consumeClick();
            var isUpPressed = scrollUpKeyBinding.isDown();
            var isDownPressed = scrollDownKeyBinding.isDown();
            if(wasUpPressed == wasDownPressed && isUpPressed == isDownPressed) return;

            var scoreboard = Objects.requireNonNull(client.level).getScoreboard();
            PlayerTeam team = scoreboard.getPlayersTeam(Objects.requireNonNull(Minecraft.getInstance().player).getScoreboardName());
            DisplaySlot scoreboardDisplaySlot;
            Objective teamObjective = null;
            if (team != null && (scoreboardDisplaySlot = DisplaySlot.teamColorToSlot(team.getColor())) != null) {
                teamObjective = scoreboard.getDisplayObjective(scoreboardDisplaySlot);
            }
            var calculatedHeights = calculateSidebarHeights(scoreboard, teamObjective);
            var totalHeight = calculatedHeights.getFirst();
            var scoreboardHeights = calculatedHeights.getSecond();
            var sortedObjectives = scoreboardHeights.entrySet().stream()
                    .sorted(Comparator.comparing(renderable -> renderable.getKey().getSortName()))
                    .toList();
            var maxTranslation = (totalHeight - client.getWindow().getGuiScaledHeight())/2 + maxTranslationBoundary;
            if(maxTranslation < 0) {
                sidebarScrollTranslation = 0;
                return;
            }
            var shouldJumpScoreboard = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL);
            if(shouldJumpScoreboard) {
                if(wasUpPressed) {
                    do {
                        var accumulatedLength = sidebarScrollTranslation;
                        for (var entry : sortedObjectives) {
                            var newLength = entry.getValue() + sidebarGap;
                            accumulatedLength += newLength;
                            if (accumulatedLength >= maxTranslation) {
                                sidebarScrollTranslation += newLength - (accumulatedLength - maxTranslation);
                                break;
                            }
                        }
                    } while(scrollUpKeyBinding.consumeClick());
                    return;
                }
                if(wasDownPressed) {
                    do {
                        var accumulatedLength = -sidebarScrollTranslation;
                        for (var i = sortedObjectives.size() - 1; i >= 0; i--) {
                            var entry = sortedObjectives.get(i);
                            var newLength = entry.getValue() + sidebarGap;
                            accumulatedLength += newLength;
                            if (accumulatedLength >= maxTranslation) {
                                sidebarScrollTranslation -= newLength - (accumulatedLength - maxTranslation);
                                break;
                            }
                        }
                    } while(scrollDownKeyBinding.consumeClick());
                    return;
                }
                return;
            }
            if(isUpPressed) {
                sidebarScrollTranslation = Math.min(sidebarScrollTranslation + scrollAmount, maxTranslation);
                return;
            }
            if(isDownPressed) {
                sidebarScrollTranslation = Math.max(sidebarScrollTranslation - scrollAmount, -maxTranslation);
            }
        });
    }

    public static void clampScrollTranslation() {
        var player = Objects.requireNonNull(Minecraft.getInstance().player);
        var scoreboard = player.level().getScoreboard();
        PlayerTeam team = scoreboard.getPlayersTeam(player.getScoreboardName());
        DisplaySlot scoreboardDisplaySlot;
        Objective teamObjective = null;
        if (team != null && (scoreboardDisplaySlot = DisplaySlot.teamColorToSlot(team.getColor())) != null) {
            teamObjective = scoreboard.getDisplayObjective(scoreboardDisplaySlot);
        }
        var calculatedHeights = calculateSidebarHeights(scoreboard, teamObjective);
        var totalHeight = calculatedHeights.getFirst();
        var maxTranslation = (totalHeight - Minecraft.getInstance().getWindow().getGuiScaledHeight())/2 + maxTranslationBoundary;
        if(maxTranslation < 0) {
            sidebarScrollTranslation = 0;
            return;
        }
        sidebarScrollTranslation = Mth.clamp(sidebarScrollTranslation, -maxTranslation, maxTranslation);
    }

    public static boolean useMultiScoreboard() {
        return useMultiScoreboard;
    }

    public static int getSidebarScrollTranslation() {
        return sidebarScrollTranslation;
    }

    public static Pair<Integer, Map<SidebarRenderable, Integer>> calculateSidebarHeights(Scoreboard scoreboard, Objective teamObjective) {
        var scoreboardHeights = new HashMap<SidebarRenderable, Integer>();

        var sidebarObjectives = ((MultiScoreboardSidebarInterface)scoreboard).multiScoreboard$getSidebarObjectives();
        var singleScoreSidebars = ((MultiScoreboardSidebarInterface)scoreboard).multiScoreboard$getSingleScoreSidebars();
        var sidebarNbt = MultiScoreboardClient.getNbtSidebars();

        Stream<SidebarRenderable> renderablesStream = sidebarObjectives.stream()
                .map(SidebarObjectiveRenderable::new);
        renderablesStream = Stream.concat(
                renderablesStream,
                sidebarNbt.entrySet().stream()
                        .map(entry -> new SidebarNbtRenderable(entry.getKey(), entry.getValue()))
        );
        renderablesStream = Stream.concat(
                renderablesStream,
                singleScoreSidebars.keySet().stream()
                        .filter(key -> !sidebarObjectives.contains(key))
                        .map(SidebarSingleScoresRenderable::new)
        );

        var totalHeight = renderablesStream.mapToInt(renderable -> {
            var height = renderable.calculateHeight();
            scoreboardHeights.put(renderable, height);
            return height;
        }).sum() + (scoreboardHeights.size() - 1) * sidebarGap;

        if(teamObjective != null) {
            var teamObjectiveRenderable = new SidebarObjectiveRenderable(teamObjective, false);
            var teamObjectiveHeight = teamObjectiveRenderable.calculateHeight();
            scoreboardHeights.put(teamObjectiveRenderable, teamObjectiveHeight);
            totalHeight += teamObjectiveHeight + sidebarGap;
        }

        return Pair.of(totalHeight, scoreboardHeights);
    }

    public static Map<String, List<Tag>> getNbtSidebars() {
        return nbtSidebars;
    }
}
