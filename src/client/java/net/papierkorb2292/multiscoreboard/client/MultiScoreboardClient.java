package net.papierkorb2292.multiscoreboard.client;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Hud;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ARGB;
import net.minecraft.world.scores.*;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.papierkorb2292.multiscoreboard.*;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

public class MultiScoreboardClient implements ClientModInitializer {
    private static boolean useMultiScoreboard = false;
    private static final Map<String, List<Tag>> nbtSidebars = new HashMap<>();
    private static final Object2FloatMap<SidebarRenderableKey> sidebarAddedTime = new Object2FloatOpenHashMap<>();
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
        ClientPlayNetworking.registerGlobalReceiver(SyncFinishedS2CPacket.ID, (packet, context) -> {
            // The sidebar entries that were already there when the player joined shouldn't be animated
            var scoreboard = context.player().level().getScoreboard();
            final var existingEntries = calculateSidebarHeights(scoreboard, null).getSecond();
            for(final var sidebar : existingEntries.keySet()) {
                markSidebarAddedTimeDone(sidebar);
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            useMultiScoreboard = false;
            nbtSidebars.clear();
            sidebarAddedTime.clear();
        });

        final KeyMapping.Category keybindCategory = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("multiscoreboard", "generic"));
        scrollUpKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.multiscoreboard.scroll_up",
                InputConstants.KEY_UP,
                keybindCategory
        ));
        scrollDownKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.multiscoreboard.scroll_down",
                InputConstants.KEY_DOWN,
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
            Optional<TeamColor> teamColor;
            Objective teamObjective = null;
            if (team != null && (teamColor = team.getColor()).isPresent()) {
                teamObjective = scoreboard.getDisplayObjective(teamColor.get().displaySlot());
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
            var positionOffset = getScoreboardPositionYOffset(totalHeight); // Position offset is handled in a similar way to clampScrollTranslation
            var shouldJumpScoreboard = InputConstants.isKeyDown(InputConstants.KEY_LCONTROL);
            if(shouldJumpScoreboard) {
                if(wasUpPressed) {
                    do {
                        var accumulatedLength = sidebarScrollTranslation + positionOffset;
                        for (var entry : sortedObjectives) {
                            var newLength = entry.getValue() + sidebarGap;
                            accumulatedLength += newLength;
                            // Explanation: Think of maxTranslation as the space that the scoreboards take up above the screen when scrollTranslation=0,positionOffset=0.
                            // We are going through the scoreboards, starting at the top, until they span the whole space above the screen, which gives
                            // us the next scoreboard that is supposed to come into view.
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
                        var accumulatedLength = -sidebarScrollTranslation - positionOffset;
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
                sidebarScrollTranslation = Math.min(sidebarScrollTranslation + scrollAmount, maxTranslation - positionOffset);
                return;
            }
            if(isDownPressed) {
                sidebarScrollTranslation = Math.max(sidebarScrollTranslation - scrollAmount, -maxTranslation - positionOffset);
            }
        });
    }

    public static void clampScrollTranslation() {
        var player = Objects.requireNonNull(Minecraft.getInstance().player);
        var scoreboard = player.level().getScoreboard();
        PlayerTeam team = scoreboard.getPlayersTeam(player.getScoreboardName());
        Optional<TeamColor> teamColor;
        Objective teamObjective = null;
        if (team != null && (teamColor = team.getColor()).isPresent()) {
            teamObjective = scoreboard.getDisplayObjective(teamColor.get().displaySlot());
        }
        var calculatedHeights = calculateSidebarHeights(scoreboard, teamObjective);
        var totalHeight = calculatedHeights.getFirst();
        var maxTranslation = (totalHeight - Minecraft.getInstance().getWindow().getGuiScaledHeight())/2 + maxTranslationBoundary;
        if(maxTranslation < 0) {
            sidebarScrollTranslation = 0;
            return;
        }
        var positionOffset = getScoreboardPositionYOffset(totalHeight);
        // Subtract position offset from both sides of the clamp. For example:
        // When displaying a scoreboard at the top of the screen, the position offset is the same as max translation,
        // so they cancel out for 'max' and the scroll translation can never be above 0 (which would leave a gap at the top)
        sidebarScrollTranslation = Math.clamp(sidebarScrollTranslation, -maxTranslation - positionOffset, maxTranslation - positionOffset);
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

        clearRemovedSidebarsAddedTime(scoreboardHeights.keySet());

        return Pair.of(totalHeight, scoreboardHeights);
    }

    private static final float sidebarAddedAnimationLengthTicks = 5;

    private static void clearRemovedSidebarsAddedTime(Collection<SidebarRenderable> sidebars) {
        sidebarAddedTime.object2FloatEntrySet().removeIf(entry -> {
            final var clazz = entry.getKey().clazz();
            final var name = entry.getKey().name();
            final var entryId = entry.getKey().entryId();
            return sidebars.stream().noneMatch(sidebar -> {
                if(sidebar.getClass() == clazz && sidebar.getSortName().equals(name) && (entryId == null || sidebar.getActiveEntryIds().contains(entryId)))
                    return true;
                if(sidebar instanceof SidebarObjectiveRenderable objective) {
                    var child = objective.singleScoresRenderable;
                    if(child.getClass() == clazz && child.getSortName().equals(name) && (entryId == null || child.getActiveEntryIds().contains(entryId)))
                        return true;
                }
                return false;
            });
        });
    }

    private static void markSidebarAddedTimeDone(SidebarRenderable sidebar) {
        final var finishedTime = Minecraft.getInstance().gui.hud.getGuiTicks() - sidebarAddedAnimationLengthTicks;
        for(var entryId : sidebar.getActiveEntryIds()) {
            final var pair = new SidebarRenderableKey(sidebar.getClass(), sidebar.getSortName(), entryId);
            sidebarAddedTime.put(pair, finishedTime);
        }
        sidebarAddedTime.put(new SidebarRenderableKey(sidebar.getClass(), sidebar.getSortName(), null), finishedTime);
        if(sidebar instanceof SidebarObjectiveRenderable objective) {
            markSidebarAddedTimeDone(objective.singleScoresRenderable);
        }
    }

    public static float getSidebarAddedAnimationProgress(SidebarRenderable sidebar, String entryId, Hud hud, DeltaTracker deltaTracker) {
        final var pair = new SidebarRenderableKey(sidebar.getClass(), sidebar.getSortName(), entryId);
        final var currentTime = hud.getGuiTicks() + deltaTracker.getGameTimeDeltaPartialTick(false);
        if(!sidebarAddedTime.containsKey(pair)) {
            if(sidebarAddedTime.isEmpty()) {
                // If this is the first sidebar entry, don't play an animation, since it's already obvious where the new entry is
                sidebarAddedTime.put(pair, currentTime - sidebarAddedAnimationLengthTicks);
            } else {
                sidebarAddedTime.put(pair, currentTime);
            }
        }
        return Math.min(1, (currentTime - sidebarAddedTime.getFloat(pair)) / sidebarAddedAnimationLengthTicks);
    }

    public static int getSidebarAddedAnimatedBackground(SidebarRenderable sidebar, String entryId, Hud hud, DeltaTracker deltaTracker, float defaultOpacity) {
        final var t = getSidebarAddedAnimationProgress(sidebar, entryId, hud, deltaTracker);
        final var options = Minecraft.getInstance().options;
        final var actualOpacity = options.getBackgroundOpacity(defaultOpacity);
        return ARGB.colorFromFloat(
                actualOpacity,
                (1-t) * 0,
                (1-t) * 1,
                (1-t) * 1
        );
    }
    public static int fadeOutSidebarAddedAnimatedBackground(SidebarRenderable sidebar, String entryId, Hud hud, DeltaTracker deltaTracker, float initialOpacity) {
        final var t = getSidebarAddedAnimationProgress(sidebar, entryId, hud, deltaTracker);
        return ARGB.colorFromFloat(
                initialOpacity * (1-t),
                0,
                1,
                1
        );
    }

    public static int getScoreboardPositionYOffset(int scoreboardHeight) {
        return switch (MultiScoreboard.config.getScoreboardPosition()) {
            // The same as max translation. Uses floorDiv and ceilDiv to avoid rounding errors when the scoreboard height is odd, which would lead to the scoreboard shifting by 1 pixel when a line is added/removed
            case TOP_RIGHT, TOP_LEFT -> Math.floorDiv(scoreboardHeight - Minecraft.getInstance().getWindow().getGuiScaledHeight(),2) + maxTranslationBoundary;
            case CENTER_RIGHT, CENTER_LEFT -> 0;
            // Negative of max translation
            case BOTTOM_RIGHT, BOTTOM_LEFT -> -Math.ceilDiv((scoreboardHeight - Minecraft.getInstance().getWindow().getGuiScaledHeight()),2) - maxTranslationBoundary;
        };
    }

    public static int getScoreboardXPosition(int maxWidth, int borderEdge, int borderCenter) {
        return switch (MultiScoreboard.config.getScoreboardPosition()) {
            case BOTTOM_RIGHT, CENTER_RIGHT, TOP_RIGHT -> Minecraft.getInstance().getWindow().getGuiScaledWidth() - maxWidth - borderEdge - borderCenter;
            case BOTTOM_LEFT, CENTER_LEFT, TOP_LEFT -> borderEdge;
        };
    }

    public static Map<String, List<Tag>> getNbtSidebars() {
        return nbtSidebars;
    }

    public record SidebarRenderableKey(Class<? extends SidebarRenderable> clazz, String name, @Nullable String entryId) { }
}
