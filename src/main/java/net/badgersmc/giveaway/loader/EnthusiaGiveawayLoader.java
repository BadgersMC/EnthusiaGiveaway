package net.badgersmc.giveaway.loader;

import net.badgersmc.nexus.paper.loader.NexusPaperPluginLoader;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Paper {@link io.papermc.paper.plugin.loader.PluginLoader} for EnthusiaGiveaway.
 *
 * <p>The Nexus base class contributes the standard runtime library set
 * (kotlin-stdlib, kotlin-reflect, kotlinx-coroutines-core-jvm, kaml-jvm,
 * classgraph, slf4j-api). This subclass adds EG-specific dependencies:
 * HikariCP, sqlite-jdbc, and the Exposed ORM modules.
 *
 * <p>Keep {@link #additionalLibraries()} in sync with the {@code compileOnly}
 * coordinates declared in build.gradle.kts.
 */
@SuppressWarnings("UnstableApiUsage")
public final class EnthusiaGiveawayLoader extends NexusPaperPluginLoader {

    @Override
    @NotNull
    protected List<String> additionalLibraries() {
        return List.of(
                "com.zaxxer:HikariCP:5.1.0",
                "org.xerial:sqlite-jdbc:3.45.1.0",
                "org.jetbrains.exposed:exposed-core:0.55.0",
                "org.jetbrains.exposed:exposed-dao:0.55.0",
                "org.jetbrains.exposed:exposed-jdbc:0.55.0",
                "org.jetbrains.exposed:exposed-java-time:0.55.0"
        );
    }
}
