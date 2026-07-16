package net.badgersmc.giveaway.application

import net.badgersmc.giveaway.domain.Template
import java.util.UUID

/**
 * Starts a giveaway from a named template.  Used by the admin menu and the
 * console command path ("/giveaway template <name>") for automation.
 */
class StartGiveawayFromTemplate(
    val templates: List<Template>,
    private val scheduleGiveaway: ScheduleGiveaway,
) {
    /** [adminUuid] defaults to the nil UUID (console) so the console path
     *  works without changes; admin-menu callers should pass the player UUID. */
    operator fun invoke(
        templateName: String,
        adminUuid: UUID = UUID(0, 0),
    ): StartFromTemplateResult {
        val tmpl = templates.firstOrNull { it.name.equals(templateName, ignoreCase = true) }
            ?: return StartFromTemplateResult.NotFound

        val result = scheduleGiveaway(
            title = tmpl.title,
            description = tmpl.description,
            durationSeconds = tmpl.durationSeconds,
            command = tmpl.command,
            maxWinners = tmpl.maxWinners,
            adminUuid = adminUuid,
        )
        return when (result) {
            is ScheduleResult.Created -> StartFromTemplateResult.Created(result.giveaway)
            is ScheduleResult.Invalid -> StartFromTemplateResult.Invalid("${result.field}: ${result.reason}")
        }
    }
}

sealed class StartFromTemplateResult {
    data class Created(val giveaway: net.badgersmc.giveaway.domain.Giveaway) : StartFromTemplateResult()
    data class Invalid(val reason: String) : StartFromTemplateResult()
    data object NotFound : StartFromTemplateResult()
}
