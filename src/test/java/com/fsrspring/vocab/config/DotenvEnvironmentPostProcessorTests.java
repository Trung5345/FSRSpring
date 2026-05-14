package com.fsrspring.vocab.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.StandardEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DotenvEnvironmentPostProcessorTests {

    @TempDir
    Path tempDir;

    @Test
    void parsesCommonDotenvSyntax() {
        assertThat(DotenvEnvironmentPostProcessor.parse(List.of(
                "# comment",
                "APP_API_PIXABAY_KEY=plain-value",
                "export APP_API_PEXELS_KEY=\"quoted value\"",
                "APP_API_WIKTAPI_BASE_URL='https://api.wiktapi.dev/path#fragment'",
                "APP_ENRICHMENT_ENABLED=true # local comment",
                "INVALID LINE",
                "1INVALID=value"
        ))).containsEntry("APP_API_PIXABAY_KEY", "plain-value")
                .containsEntry("APP_API_PEXELS_KEY", "quoted value")
                .containsEntry("APP_API_WIKTAPI_BASE_URL", "https://api.wiktapi.dev/path#fragment")
                .containsEntry("APP_ENRICHMENT_ENABLED", "true")
                .doesNotContainKeys("INVALID LINE", "1INVALID");
    }

    @Test
    void loadsConfiguredDotenvFileIntoEnvironment() throws Exception {
        Path dotenv = tempDir.resolve(".env");
        Files.writeString(dotenv, "APP_API_PIXABAY_KEY=from-dotenv\n");

        withSystemProperty(DotenvEnvironmentPostProcessor.DOTENV_PATH_PROPERTY, dotenv.toString(), () -> {
            StandardEnvironment environment = new StandardEnvironment();

            new DotenvEnvironmentPostProcessor().postProcessEnvironment(environment, null);

            assertThat(environment.getPropertySources().contains(DotenvEnvironmentPostProcessor.PROPERTY_SOURCE_NAME))
                    .isTrue();
            assertThat(environment.getProperty("APP_API_PIXABAY_KEY")).isEqualTo("from-dotenv");
        });
    }

    @Test
    void keepsSystemPropertiesHigherPriorityThanDotenvFile() throws Exception {
        Path dotenv = tempDir.resolve(".env");
        Files.writeString(dotenv, "APP_API_PIXABAY_KEY=from-dotenv\n");

        withSystemProperty(DotenvEnvironmentPostProcessor.DOTENV_PATH_PROPERTY, dotenv.toString(), () ->
                withSystemProperty("APP_API_PIXABAY_KEY", "from-system-property", () -> {
                    StandardEnvironment environment = new StandardEnvironment();

                    new DotenvEnvironmentPostProcessor().postProcessEnvironment(environment, null);

                    assertThat(environment.getProperty("APP_API_PIXABAY_KEY")).isEqualTo("from-system-property");
                }));
    }

    private void withSystemProperty(String key, String value, ThrowingRunnable runnable) throws Exception {
        String previous = System.getProperty(key);
        try {
            if (value == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, value);
            }
            runnable.run();
        } finally {
            if (previous == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, previous);
            }
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
