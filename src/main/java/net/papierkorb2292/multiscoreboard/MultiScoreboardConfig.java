package net.papierkorb2292.multiscoreboard;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.Properties;

public class MultiScoreboardConfig {
    public static final Path DEFAULT_CONFIG_PATH = Path.of("config/multiscoreboard.properties");

    public static final String SCOREBOARD_POSITION_NAME = "scoreboard-position";
    private static final Properties configDefaults = new Properties();
    static {
        configDefaults.setProperty(SCOREBOARD_POSITION_NAME, ScoreboardPosition.CENTER_RIGHT.name);
    }

    private ScoreboardPosition scoreboardPosition;
    private final String modVersion;

    private final Path configPath;

    private MultiScoreboardConfig(Properties properties, Path configPath, @Nullable String configVersion, String modVersion) {
        scoreboardPosition = ScoreboardPosition.fromName(properties.getProperty(SCOREBOARD_POSITION_NAME));
        this.configPath = configPath;
        this.modVersion = modVersion;
        if (configVersion == null)
            return;
        if (!configVersion.equals(modVersion))
            saveToFile(configPath);
    }

    public void saveToFile(Path path) {
        final var properties = new Properties();
        properties.setProperty(SCOREBOARD_POSITION_NAME, scoreboardPosition.name);
        try (var writer = new java.io.FileWriter(path.toFile())) {
            writer.append('v').append(modVersion).append('\n');
            properties.store(writer, "MultiScoreboard Config");
        } catch (java.io.IOException e) {
            MultiScoreboard.LOGGER.error("Failed to save config file", e);
        }
    }

    public ScoreboardPosition getScoreboardPosition() {
        return scoreboardPosition;
    }

    public void setScoreboardPosition(ScoreboardPosition scoreboardPosition) {
        this.scoreboardPosition = scoreboardPosition;
        saveToFile(configPath);
    }

    public static MultiScoreboardConfig loadFromFile(Path path, String modVersion) {
        var file = path.toFile();
        if (!file.exists()) {
            final var config = new MultiScoreboardConfig(configDefaults, path, null, modVersion);
            config.saveToFile(path);
            return config;
        }
        try (var reader = new BufferedReader(new FileReader(file))) {
            reader.mark(16);
            final var firstChar = reader.read();
            final String version;
            if (firstChar == 'v') {
                version = reader.readLine();
            } else {
                version = MultiScoreboard.MOD_VERSION;
                reader.reset();
            }
            final var properties = new Properties(configDefaults);
            properties.load(reader);
            return new MultiScoreboardConfig(properties, path, version, modVersion);
        } catch (java.io.IOException e) {
            MultiScoreboard.LOGGER.error("Failed to load config file", e);
            return new MultiScoreboardConfig(configDefaults, path, null, modVersion);
        }
    }

    public enum ScoreboardPosition {
        BOTTOM_RIGHT("Bottom-Right"),
        CENTER_RIGHT("Center-Right"),
        TOP_RIGHT("Top-Right"),
        TOP_LEFT("Top-Left"),
        CENTER_LEFT("Center-Left"),
        BOTTOM_LEFT("Bottom-Left");

        private final String name;

        ScoreboardPosition(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static ScoreboardPosition fromName(String name) {
            for (var position : values()) {
                if (position.name.equals(name)) {
                    return position;
                }
            }
            return CENTER_RIGHT;
        }
    }
}
