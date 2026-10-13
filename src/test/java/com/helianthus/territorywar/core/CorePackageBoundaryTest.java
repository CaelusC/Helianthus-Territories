package com.helianthus.territorywar.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CorePackageBoundaryTest {
    private static final Path CORE_SOURCES = Paths.get("src/main/java/com/helianthus/territorywar/core");
    private static final List<String> FORBIDDEN = List.of("net.minecraft", "net.minecraftforge", "dev.ftb");

    @Test
    void coreSourcesDoNotReferenceMinecraftForgeOrFtb() throws IOException {
        assertTrue(Files.isDirectory(CORE_SOURCES), "core source directory missing: " + CORE_SOURCES.toAbsolutePath());

        List<Path> sources;
        try (Stream<Path> walk = Files.walk(CORE_SOURCES)) {
            sources = walk.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
        }
        assertFalse(sources.isEmpty(), "no core sources found to scan");

        List<String> violations = new ArrayList<>();
        for (Path source : sources) {
            String content = Files.readString(source, StandardCharsets.UTF_8);
            for (String prefix : FORBIDDEN) {
                if (content.contains(prefix)) {
                    violations.add(CORE_SOURCES.relativize(source) + " references " + prefix);
                }
            }
        }
        assertTrue(violations.isEmpty(), "core package boundary violated:\n" + String.join("\n", violations));
    }
}
