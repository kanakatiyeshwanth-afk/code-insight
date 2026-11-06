package com.codeinsight.config;

import com.codeinsight.model.ThresholdConfig;

import java.util.prefs.Preferences;

/**
 * Persists user settings and preferences across application sessions
 * using standard Java Preferences API.
 */
public final class UserPreferences {
    private static final String KEY_MAX_COMPLEXITY = "max_complexity";
    private static final String KEY_MAX_NESTING = "max_nesting";
    private static final String KEY_MAX_LINES = "max_lines";
    private static final String KEY_LAST_PATH = "last_analyzed_path";

    private static final Preferences PREFS = Preferences.userNodeForPackage(UserPreferences.class);

    private UserPreferences() {}

    public static ThresholdConfig loadThresholds() {
        int complexity = PREFS.getInt(KEY_MAX_COMPLEXITY, ThresholdConfig.DEFAULT_MAX_COMPLEXITY);
        int nesting = PREFS.getInt(KEY_MAX_NESTING, ThresholdConfig.DEFAULT_MAX_NESTING);
        int lines = PREFS.getInt(KEY_MAX_LINES, ThresholdConfig.DEFAULT_MAX_LINES);
        return new ThresholdConfig(complexity, nesting, lines);
    }

    public static void saveThresholds(ThresholdConfig config) {
        if (config == null) return;
        PREFS.putInt(KEY_MAX_COMPLEXITY, config.maxComplexity());
        PREFS.putInt(KEY_MAX_NESTING, config.maxNesting());
        PREFS.putInt(KEY_MAX_LINES, config.maxLines());
    }

    public static String loadLastPath() {
        return PREFS.get(KEY_LAST_PATH, "samples");
    }

    public static void saveLastPath(String path) {
        if (path != null && !path.isBlank()) {
            PREFS.put(KEY_LAST_PATH, path);
        }
    }

    public static void resetToDefaults() {
        saveThresholds(ThresholdConfig.defaultConfig());
    }
}

