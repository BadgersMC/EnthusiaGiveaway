package net.badgersmc.giveaway.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import org.junit.jupiter.api.Test

/**
 * SPEAR layer-rule enforcement.
 *
 * The forbidden-domain-annotations denylist is enforced separately by
 * `/spear:arch` reading the YAML block in `docs/implementation.md`.
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
}
