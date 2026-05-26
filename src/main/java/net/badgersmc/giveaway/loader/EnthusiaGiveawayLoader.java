package net.badgersmc.giveaway.loader;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

/**
 * Paper plugin loader that resolves EnthusiaGiveaway's runtime dependencies
 * via Maven Central at server startup, before the main plugin class is loaded.
 *
 * Anything declared {@code compileOnly} in build.gradle.kts must be listed
 * here, otherwise the plugin will hit {@link ClassNotFoundException} at runtime.
 *
 * Pattern adapted from {@code D:/BadgersMC-Dev/LumaSG/.../LumaSGLoader.java}.
 */
@SuppressWarnings("UnstableApiUsage")
public final class EnthusiaGiveawayLoader implements PluginLoader {

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();

        resolver.addRepository(new RemoteRepository.Builder(
                "central", "default", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR
        ).build());

        // Kotlin runtime (excluded from shadow jar in build.gradle.kts)
        resolver.addDependency(dep("org.jetbrains.kotlin:kotlin-stdlib:2.1.0"));
        resolver.addDependency(dep("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0"));
        resolver.addDependency(dep("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.8.0"));

        // Storage stack
        resolver.addDependency(dep("com.zaxxer:HikariCP:5.1.0"));
        resolver.addDependency(dep("org.xerial:sqlite-jdbc:3.45.1.0"));
        resolver.addDependency(dep("org.jetbrains.exposed:exposed-core:0.55.0"));
        resolver.addDependency(dep("org.jetbrains.exposed:exposed-dao:0.55.0"));
        resolver.addDependency(dep("org.jetbrains.exposed:exposed-jdbc:0.55.0"));
        resolver.addDependency(dep("org.jetbrains.exposed:exposed-java-time:0.55.0"));

        classpathBuilder.addLibrary(resolver);
    }

    private static Dependency dep(String coords) {
        return new Dependency(new DefaultArtifact(coords), null);
    }
}
