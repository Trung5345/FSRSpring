package com.fsrspring.vocab.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "dotenv";
    static final String DOTENV_PATH_PROPERTY = "app.dotenv.path";
    static final String DOTENV_PATH_ENV = "APP_DOTENV_PATH";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }

        Path dotenvPath = resolveDotenvPath(environment);
        if (!Files.isRegularFile(dotenvPath)) {
            return;
        }

        Map<String, Object> values = loadDotenv(dotenvPath);
        if (values.isEmpty()) {
            return;
        }

        MapPropertySource dotenvSource = new MapPropertySource(PROPERTY_SOURCE_NAME, values);
        MutablePropertySources sources = environment.getPropertySources();
        if (sources.contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
            sources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, dotenvSource);
        } else if (sources.contains(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)) {
            sources.addAfter(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, dotenvSource);
        } else {
            sources.addFirst(dotenvSource);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 9;
    }

    private Path resolveDotenvPath(ConfigurableEnvironment environment) {
        String configuredPath = firstText(
                System.getProperty(DOTENV_PATH_PROPERTY),
                System.getenv(DOTENV_PATH_ENV),
                environment.getProperty(DOTENV_PATH_PROPERTY),
                environment.getProperty(DOTENV_PATH_ENV)
        );
        if (configuredPath != null) {
            return Path.of(configuredPath).toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("user.dir"), ".env").toAbsolutePath().normalize();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private Map<String, Object> loadDotenv(Path dotenvPath) {
        try {
            return parse(Files.readAllLines(dotenvPath, StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            return Map.of();
        }
    }

    static Map<String, Object> parse(List<String> lines) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String line : lines) {
            ParsedLine parsed = parseLine(line);
            if (parsed != null) {
                values.put(parsed.key(), parsed.value());
            }
        }
        return values;
    }

    private static ParsedLine parseLine(String rawLine) {
        if (rawLine == null) {
            return null;
        }

        String line = rawLine.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return null;
        }
        if (line.startsWith("export ")) {
            line = line.substring("export ".length()).trim();
        }

        int separator = line.indexOf('=');
        if (separator <= 0) {
            return null;
        }

        String key = line.substring(0, separator).trim();
        String value = line.substring(separator + 1).trim();
        if (!key.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            return null;
        }

        return new ParsedLine(key, unquote(value));
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                String inner = value.substring(1, value.length() - 1);
                return first == '"' ? unescapeDoubleQuoted(inner) : inner;
            }
        }
        return stripInlineComment(value).trim();
    }

    private static String stripInlineComment(String value) {
        boolean hasWhitespaceBeforeComment = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '#') {
                if (i == 0 || hasWhitespaceBeforeComment) {
                    return value.substring(0, i);
                }
            }
            hasWhitespaceBeforeComment = Character.isWhitespace(c);
        }
        return value;
    }

    private static String unescapeDoubleQuoted(String value) {
        StringBuilder result = new StringBuilder(value.length());
        boolean escaping = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaping) {
                result.append(switch (c) {
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    default -> c;
                });
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else {
                result.append(c);
            }
        }
        if (escaping) {
            result.append('\\');
        }
        return result.toString();
    }

    private record ParsedLine(String key, String value) {
    }
}
