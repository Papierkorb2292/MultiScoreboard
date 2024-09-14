package net.papierkorb2292.multiscoreboard.client;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.nbt.NbtElement;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.math.MathHelper;
import net.papierkorb2292.multiscoreboard.*;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Stream;

public class MultiScoreboardClient implements ClientModInitializer {
    private static boolean useMultiScoreboard = false;
    private static final Map<String, List<NbtElement>> nbtSidebars = new HashMap<>();
    private static int sidebarScrollTranslation = 0;
    private static final int scrollAmount = 10;
    private static final int maxTranslationBoundary = 10;
    private static KeyBinding scrollUpKeyBinding;
    private static KeyBinding scrollDownKeyBinding;

    public static final int sidebarGap = 11;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SetUseMultiScoreboardS2CPacket.PACKET_TYPE, (packet, player, sender) -> {
            useMultiScoreboard = packet.useMultiScoreboard();
        });
        ClientPlayNetworking.registerGlobalReceiver(SetNbtSidebarS2CPacket.PACKET_TYPE, (packet, player, sender) -> {
            nbtSidebars.put(packet.nbtSidebarName(), packet.nbt());
        });
        ClientPlayNetworking.registerGlobalReceiver(RemoveNbtSidebarS2CPacket.PACKET_TYPE, (packet, player, sender) -> {
            nbtSidebars.remove(packet.nbtSidebarName());
            clampScrollTranslation();
        });
        ClientPlayNetworking.registerGlobalReceiver(ToggleSingleScoreSidebarS2CPacket.PACKET_TYPE, (packet, player, sender) -> {
            var scoreboard = player.getScoreboard();
            var objective = scoreboard.getNullableObjective(packet.objective());
            if(objective == null) return;
            ((MultiScoreboardSidebarInterface)scoreboard).multiScoreboard$toggleSingleScoreSidebar(objective, packet.score());
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            useMultiScoreboard = false;
            nbtSidebars.clear();
        });

        scrollUpKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.multiscoreboard.scroll_up",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UP,
                "category.multiscoreboard"
        ));
        scrollDownKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.multiscoreboard.scroll_down",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_DOWN,
                "category.multiscoreboard"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var wasUpPressed = scrollUpKeyBinding.wasPressed();
            var wasDownPressed = scrollDownKeyBinding.wasPressed();
            var isUpPressed = scrollUpKeyBinding.isPressed();
            var isDownPressed = scrollDownKeyBinding.isPressed();
            if(wasUpPressed == wasDownPressed && isUpPressed == isDownPressed) return;

            var scoreboard = Objects.requireNonNull(client.world).getScoreboard();
            Team team = scoreboard.getScoreHolderTeam(Objects.requireNonNull(MinecraftClient.getInstance().player).getNameForScoreboard());
            ScoreboardDisplaySlot scoreboardDisplaySlot;
            ScoreboardObjective teamObjective = null;
            if (team != null && (scoreboardDisplaySlot = ScoreboardDisplaySlot.fromFormatting(team.getColor())) != null) {
                teamObjective = scoreboard.getObjectiveForSlot(scoreboardDisplaySlot);
            }
            var calculatedHeights = calculateSidebarHeights(scoreboard, teamObjective);
            var totalHeight = calculatedHeights.getFirst();
            var scoreboardHeights = calculatedHeights.getSecond();
            var sortedObjectives = scoreboardHeights.entrySet().stream()
                    .sorted(Comparator.comparing(renderable -> renderable.getKey().getSortName()))
                    .toList();
            var maxTranslation = (totalHeight - client.getWindow().getScaledHeight())/2 + maxTranslationBoundary;
            if(maxTranslation < 0) {
                sidebarScrollTranslation = 0;
                return;
            }
            var shouldJumpScoreboard = InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL);
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
                    } while(scrollUpKeyBinding.wasPressed());
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
                    } while(scrollDownKeyBinding.wasPressed());
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
        var player = Objects.requireNonNull(MinecraftClient.getInstance().player);
        var scoreboard = player.getWorld().getScoreboard();
        Team team = scoreboard.getScoreHolderTeam(player.getNameForScoreboard());
        ScoreboardDisplaySlot scoreboardDisplaySlot;
        ScoreboardObjective teamObjective = null;
        if (team != null && (scoreboardDisplaySlot = ScoreboardDisplaySlot.fromFormatting(team.getColor())) != null) {
            teamObjective = scoreboard.getObjectiveForSlot(scoreboardDisplaySlot);
        }
        var calculatedHeights = calculateSidebarHeights(scoreboard, teamObjective);
        var totalHeight = calculatedHeights.getFirst();
        var maxTranslation = (totalHeight - MinecraftClient.getInstance().getWindow().getScaledHeight())/2 + maxTranslationBoundary;
        if(maxTranslation < 0) {
            sidebarScrollTranslation = 0;
            return;
        }
        sidebarScrollTranslation = MathHelper.clamp(sidebarScrollTranslation, -maxTranslation, maxTranslation);
    }

    public static boolean useMultiScoreboard() {
        return useMultiScoreboard;
    }

    public static int getSidebarScrollTranslation() {
        return sidebarScrollTranslation;
    }

    public static Pair<Integer, Map<SidebarRenderable, Integer>> calculateSidebarHeights(Scoreboard scoreboard, ScoreboardObjective teamObjective) {
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

    public static Map<String, List<NbtElement>> getNbtSidebars() {
        return nbtSidebars;
    }
}
