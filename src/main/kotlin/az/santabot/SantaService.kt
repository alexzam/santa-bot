package az.santabot

import az.santabot.model.*

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
            else -> onFreeFormMessage(message)
        }
    }

    private fun onStartCommand(id: Int): SendMessageRequest? {
        val state = dbService.findChatState(id)
        if (state != null) return onRepeatedStart(id, state)

        dbService.saveStartChat(id)

        return SendMessageRequest(
            chatId = id,
            text = "Привет! Создаём новую группу Тайного Санты. После того как в неё добавятся все желающие, закройте " +
                    "приём в группу. После этого все получат имя и логин того, кому должны придумать подарок. А как эта " +
                    "группа будет называться?"
        )
    }

    private fun onRepeatedStart(id: Int, state: Int): SendMessageRequest? {
        return null
    }

    private fun onFreeFormMessage(message: Message): SendMessageRequest? {
        val chatId = message.chat.id
        val state = dbService.findChatState(chatId)
        if (state == 0) {
            // Group creation started
            val name = message.text ?: "Unnamed"
            val groupId = dbService.createGroupInChat(chatId, name)

            return SendMessageRequest(
                chatId = chatId,
                text = "Отлично! Группу $name создали и тебя туда добавили.",
                replyMarkup = InlineKeyboardMarkup(
                    listOf(
                        listOf(
                            InlineKeyboardButton(
                                text = "Закинуть в чат",
                                switchInlineQuery = "group$groupId"
                            )
                        )
                    )
                )
            )
        }
        return null
    }

    fun addToGroup(uid: Long, gid: Int) {}

    fun closeGroup(gid: Int) {}

    fun deleteGroup(gid: Int) {}
    private fun shuffle(gid: Int) {}
}