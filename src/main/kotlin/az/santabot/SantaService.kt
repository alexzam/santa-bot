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
                    description = "Создал: ${it.authorLogin}, в группе ${it.membersNum}",
                    inputMessageContent = InputTextMessageContent(
                        makeGroupMessage(
                            it.name,
                            it.authorLogin,
                            it.membersNum
                        )
                    ),
                    replyMarkup = InlineKeyboardMarkup(
                        listOf(
                            listOf(
                                InlineKeyboardButton(text = "Присоединиться", callbackData = "join:${it.id}")
                            )
                        )
                    )
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

    fun processCallbackQuery(callbackQuery: CallbackQuery): Request? {
        val parts = callbackQuery.data?.split(":")
        return when (parts?.get(0)) {
            "join" -> {
                dbService.addToGroup(parts[1].toInt(), callbackQuery.from.id)
                AnswerCallbackQueryRequest(
                    callbackQueryId = callbackQuery.id,
                    text = "Добавились в группу"
                )
            }
            else -> null
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
            dbService.createGroupInChat(chatId, name, message.from!!)

            return SendMessageRequest(
                chatId = chatId,
                text = "Отлично! Группу $name создали и тебя туда добавили.",
                replyMarkup = InlineKeyboardMarkup(
                    listOf(
                        listOf(
                            InlineKeyboardButton(
                                text = "Закинуть в чат",
                                switchInlineQuery = ""
                            )
                        )
                    )
                )
            )
        }
        return null
    }

    private fun makeGroupMessage(name: String, authorLogin: String, memberNum: Int): String {
        return """
            *Группа Тайного Санты "$name"*
            Создал @$authorLogin

            $memberNum участников

            После того как в группу запишутся все жалающие, @$authorLogin её закроет. После этого все смогут узнать, кто
            кому дарит подарок, однако, состав изменить уже будет нельзя.
        """.trimIndent()
    }

    fun addToGroup(uid: Long, gid: Int) {}

    fun closeGroup(gid: Int) {}
    fun deleteGroup(gid: Int) {}
    private fun shuffle(gid: Int) {}
}