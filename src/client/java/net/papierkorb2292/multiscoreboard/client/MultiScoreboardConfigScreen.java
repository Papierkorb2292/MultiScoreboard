package net.papierkorb2292.multiscoreboard.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.papierkorb2292.multiscoreboard.MultiScoreboard;
import net.papierkorb2292.multiscoreboard.MultiScoreboardConfig;

import java.util.Arrays;

public class MultiScoreboardConfigScreen extends OptionsSubScreen {
    public MultiScoreboardConfigScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.translatable("multiscoreboard.config.title"));
    }

    @Override
    protected void addOptions() {
        if (list != null) {
            final var config = MultiScoreboard.config;
            list.addBig(new OptionInstance<>(
                    convertNameToTranslationKey(MultiScoreboardConfig.SCOREBOARD_POSITION_NAME),
                    _ -> Tooltip.create(Component.translatable(
                            convertNameToTranslationKey(MultiScoreboardConfig.SCOREBOARD_POSITION_NAME) + ".description"
                    )),
                    (_, value) -> Component.literal(value.getValue()),
                    new OptionInstance.Enum<>(
                            Arrays.asList(MultiScoreboardConfig.ScoreboardPosition.values()),
                            com.mojang.serialization.Codec.STRING.xmap(
                                    MultiScoreboardConfig.ScoreboardPosition::fromValue,
                                    MultiScoreboardConfig.ScoreboardPosition::getValue
                            )
                    ),
                    config.getScoreboardPosition(),
                    config::setScoreboardPosition
            ));
        }
    }

    private static String convertNameToTranslationKey(String name) {
        return "multiscoreboard.config." + name.replace("-", "_");
    }
}
