package az.santabot

import az.santabot.model.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class SantaService(private val dbService: DbService) {

    lateinit var telegramService: TelegramService

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
                        makeGroupMessage(it),
                        parseMode = ParseMode.Markdown
                    ),
                    replyMarkup = InlineKeyboardMarkup(makeActiveGroupButtons(it))
                )
            },
            switchPmText = "New group"
        )
    }

    private fun makeActiveGroupButtons(it: Group): List<List<InlineKeyboardButton>> {
        return listOf(
            listOf(
                InlineKeyboardButton(text = "Присоединиться", callbackData = "join:${it.id}")
            )
        )
    }

    fun processMessage(message: Message): SendMessageRequest? {
        return when (message.text) {
            "/start" -> onStartCommand(message.chat.id)
            else -> onFreeFormMessage(message)
        }
    }

    suspend fun processCallbackQuery(callbackQuery: CallbackQuery): Request? {
        val parts = callbackQuery.data?.split(":")
        return when (parts?.get(0)) {
            "join" -> {
                val gid = parts[1].toInt()
                val added = dbService.addToGroup(gid, callbackQuery.from.id)

                if (added) {
                    GlobalScope.async {
                        val group = dbService.getGroup(gid)

                        if (group != null) {
                            val editRequest = EditMessageTextRequest(
                                inlineMessageId = callbackQuery.inlineMessageId,
                                text = makeGroupMessage(group),
                                parseMode = ParseMode.Markdown,
                                replyMarkup = InlineKeyboardMarkup(makeActiveGroupButtons(group))
                            )
                            telegramService.sendRequest(editRequest)
                        }
                    }

                    AnswerCallbackQueryRequest(
                        callbackQueryId = callbackQuery.id,
                        text = "Добавились в группу"
                    )
                } else {
                    AnswerCallbackQueryRequest(
                        callbackQueryId = callbackQuery.id,
                        text = "Вы и так уже в группе"
                    )
                }
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

    private fun makeGroupMessage(group: Group): String {
        return """
            *Группа Тайного Санты "${group.name}"*
            Создал @${group.authorLogin}

            ${group.membersNum} участников

            После того как в группу запишутся все желающие, @${group.authorLogin} её закроет. После этого все смогут узнать, кто
            кому дарит подарок, однако, состав изменить уже будет нельзя.
        """.trimIndent()
    }

    fun closeGroup(gid: Int) {}
    fun deleteGroup(gid: Int) {}

    private fun <T> shuffle(ids: List<T>): Map<T, T> {
        if (ids.size < 2) {
            return ids.zip(ids).toMap()
        }

        val map = ids.zip(ids.shuffled()).toMap().toMutableMap()

        val toReshuffle = map.filter { entry -> entry.key == entry.value }
        toReshuffle.keys
            .forEach {
                var key: T
                do {
                    key = map.keys.random()
                } while (key == it)

                map[it] = map[key]!!
                map[key] = it
            }

        return map
    }
}