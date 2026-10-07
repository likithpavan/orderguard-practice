package com.orderguard;

import java.io.IOException;
import java.util.Properties;

public record BuildInfo(String version, String commit) {
    public static BuildInfo load() {
        Properties properties = new Properties();
        try (var stream = BuildInfo.class.getResourceAsStream("/build-info.properties")) {
            if (stream != null) properties.load(stream);
        } catch (IOException exception) { throw new IllegalStateException("Cannot read build identity.", exception); }
        return new BuildInfo(properties.getProperty("version", "development"), properties.getProperty("commit", "untracked"));
    }
}
