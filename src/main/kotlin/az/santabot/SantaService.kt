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
            switchPmText = "Новая группа"
        )
    }

    private fun makeActiveGroupButtons(it: Group): List<List<InlineKeyboardButton>> {
        return listOf(
            listOf(
                InlineKeyboardButton(text = "Присоединиться", callbackData = "join:${it.id}"),
                InlineKeyboardButton(text = "Выйти", callbackData = "leave:${it.id}")
            )
        )
    }

    fun processMessage(message: Message): SendMessageRequest? {
        val state = dbService.findChatState(message.chat.id)
        val uid = message.from!!.id

        return when (message.text) {
            "/start" -> onStartCommand(message.chat.id, state)
            "/close" -> onCloseCommand(message.chat.id, state, uid)
            else -> onFreeFormMessage(message, state)
        }
    }

    @Suppress("DeferredResultUnused")
    fun processCallbackQuery(callbackQuery: CallbackQuery): Request? {
        val parts = callbackQuery.data?.split(":")
        return when (parts?.get(0)) {
            "join" -> {
                val gid = parts[1].toInt()
                onJoinButton(gid, callbackQuery)
            }
            "leave" -> {
                val gid = parts[1].toInt()
                onLeaveButton(gid, callbackQuery)
            }
            else -> null
        }
    }

    private fun updateGroupMessage(gid: Int, inlineMessageId: String?) = GlobalScope.async {
        val group = dbService.getGroup(gid)

        if (group != null) {
            val editRequest = EditMessageTextRequest(
                inlineMessageId = inlineMessageId,
                text = makeGroupMessage(group),
                parseMode = ParseMode.Markdown,
                replyMarkup = InlineKeyboardMarkup(makeActiveGroupButtons(group))
            )
            telegramService.sendRequest(editRequest)
        }
    }

    private fun onStartCommand(id: Int, state: Int?): SendMessageRequest? {
        if (state != null) return onRepeatedStart(id, state)

        dbService.saveStartChat(id)

        return SendMessageRequest(
            chatId = id,
            text = "Привет! Создаём новую группу Тайного Санты. После того как в неё добавятся все желающие, закройте " +
                    "приём в группу. После этого все получат имя и логин того, кому должны придумать подарок. А как эта " +
                    "группа будет называться?"
        )
    }

    private fun onCloseCommand(chatId: Int, state: Int?, uid: Int): SendMessageRequest? {
        if (state != null && state != 1) return null

        dbService.startCloseChat(chatId)

        val buttons = dbService.getAdminGroups(uid)
            .map { listOf(KeyboardButton(it.name)) }
            .plusElement(listOf(KeyboardButton("Отмена")))

        return SendMessageRequest(
            chatId,
            text = "Закрываем приём в группу? Хорошо. Выберите нужную группу.",
            replyMarkup = ReplyKeyboardMarkup(
                keyboard = buttons,
                resizeKeyboard = true,
                onetimeKeyboard = true
            )
        )
    }

    private fun onRepeatedStart(id: Int, state: Int): SendMessageRequest? {
        return null
    }

    private fun onFreeFormMessage(message: Message, state: Int?): SendMessageRequest? {
        val chatId = message.chat.id
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
        } else if (state == null || state == 1) {
            // No started dialog
            return SendMessageRequest(
                chatId = chatId,
                text = """Привет. Команды такие:
                    |/start - Создать новую группу Тайного Санты
                    |/close - Закрыть приём в группу
                """.trimMargin()
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

    private fun onJoinButton(
        gid: Int,
        callbackQuery: CallbackQuery
    ): AnswerCallbackQueryRequest {
        val added = dbService.addToGroup(gid, callbackQuery.from.id)

        return if (added) {
            updateGroupMessage(gid, callbackQuery.inlineMessageId)

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

    private fun onLeaveButton(
        gid: Int,
        callbackQuery: CallbackQuery
    ): AnswerCallbackQueryRequest {
        val removed = dbService.removeFromGroup(gid, callbackQuery.from.id)

        return if (removed) {
            updateGroupMessage(gid, callbackQuery.inlineMessageId)

            AnswerCallbackQueryRequest(
                callbackQueryId = callbackQuery.id,
                text = "Вышли из группы"
            )
        } else {
            AnswerCallbackQueryRequest(
                callbackQueryId = callbackQuery.id,
                text = "Вы и так не в группе"
            )
        }
    }
}
