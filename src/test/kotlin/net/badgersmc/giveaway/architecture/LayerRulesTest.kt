package net.badgersmc.giveaway.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import org.junit.jupiter.api.Test

/**
 * SPEAR layer-rule enforcement.
 *
 * Emitted by `/spear:init` on JVM projects (REQ-065). Mirrors the
 * three-layer hexagonal discipline SPEAR enforces: domain depends on
 * nothing, application depends only on domain, infrastructure is
 * unconstrained.
 */
class LayerRulesTest {

    @Test
    fun `spear layer dependencies are correct`() {
        Konsist
            .scopeFromProduction()
            .assertArchitecture {
                val domain = Layer("Domain", "net.badgersmc.giveaway.domain..")
                val application = Layer("Application", "net.badgersmc.giveaway.application..")
                val infrastructure = Layer("Infrastructure", "net.badgersmc.giveaway.infrastructure..")

                domain.dependsOnNothing()
                application.dependsOn(domain)
                infrastructure.dependsOn(domain, application)
            }
    }

    @Test
    fun `domain has no framework annotations or imports`() {
        val forbiddenPrefixes = listOf(
            "org.bukkit",
            "io.papermc",
            "org.springframework",
            "jakarta.persistence",
            "javax.persistence",
            "com.fasterxml.jackson",
            "io.micronaut",
            "lombok",
            "org.jetbrains.exposed",
            "com.zaxxer.hikari",
            "com.github.stefvanschie.inventoryframework",
            "net.badgersmc.nexus",
        )

        Konsist
            .scopeFromProduction()
            .files
            .withPackage("..domain..")
            .assertFalse { file ->
                file.imports.any { import ->
                    forbiddenPrefixes.any { prefix -> import.name.startsWith(prefix) }
                }
            }
    }
}
