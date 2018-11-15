package az.santabot

import az.santabot.model.InlineQuery
import az.santabot.model.InlineQueryResponse
import az.santabot.model.InlineQueryResultArticle
import az.santabot.model.InputTextMessageContent

class SantaService(private val dbService: DbService) {

    fun processInlineRequest(inlineQuery: InlineQuery): InlineQueryResponse {
        val groups = dbService.getGroups(inlineQuery.from.id)
        return InlineQueryResponse(
            inlineQueryId = inlineQuery.id,
            personal = true,
            results = groups.map {
                InlineQueryResultArticle(
                    id = it.id.toString(),
                    title = it.name,
                    inputMessageContent = InputTextMessageContent("g" + it.id)
                )
            },
            switchPmText = "New group"
        )
    }

    fun startGroup(): Int {
        return 0
    }

    fun addToGroup(uid: Long, gid: Int) {}

    fun closeGroup(gid: Int) {}

    fun deleteGroup(gid: Int) {}

    private fun shuffle(gid: Int) {}
}