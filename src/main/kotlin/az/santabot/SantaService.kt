package az.santabot

import az.santabot.model.*
import az.santabot.util.Either

class SantaService(private val dbService: DbService) {

    fun processInlineRequest(inlineQuery: InlineQuery): InlineQueryRequest {
        val groups = dbService.getGroups(inlineQuery.from.id)
        return InlineQueryRequest(
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

    fun processMessage(message: Message): SendMessageRequest? {
        return when (message.text) {
            "/start" -> onStartCommand(message.chat.id)
            else -> null
        }
    }

    private fun onStartCommand(id: Int): SendMessageRequest {
        return SendMessageRequest(
            chatId = Either.consLeft(id),
            text = "Привет! Создаём новую группу Тайного Санты. После того как в неё добавятся все желающие, закройте " +
                    "приём в группу. После этого все получат имя и логин того, кому должны придумать подарок. А как эта " +
                    "группа будет называться?"
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