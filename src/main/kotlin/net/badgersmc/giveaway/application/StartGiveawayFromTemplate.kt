package net.badgersmc.giveaway.application

import net.badgersmc.giveaway.domain.Template

/**
 * Starts a giveaway from a named template.  Used by the console command
 * path ("/giveaway template <name>") for automation from other plugins.
 */
class StartGiveawayFromTemplate(
    val templates: List<Template>,
    private val scheduleGiveaway: ScheduleGiveaway,
) {
    operator fun invoke(templateName: String): StartFromTemplateResult {
        val tmpl = templates.firstOrNull { it.name.equals(templateName, ignoreCase = true) }
            ?: return StartFromTemplateResult.NotFound

        val result = scheduleGiveaway(
            title = tmpl.title,
            durationSeconds = tmpl.durationSeconds,
            command = tmpl.command,
            maxWinners = tmpl.maxWinners,
            adminUuid = java.util.UUID(0, 0), // console
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
