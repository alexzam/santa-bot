package az.santabot

import az.santabot.model.*
import az.santabot.util.shuffle
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class SantaService(private val dbService: DbService) {
    enum class ChatState(val id: Int) {
        CreateGetName(0),
        Idle(1),
        CloseGetName(2);

        companion object {
            fun find(id: Int): ChatState? = values().find { it.id == id }
        }
    }

    lateinit var telegramService: TelegramService

    /**
     * Mentioned suggest
     */
    fun processInlineRequest(inlineQuery: InlineQuery): InlineQueryRequest {
        val groups = dbService.getGroups(inlineQuery.from)
        return InlineQueryRequest(
            inlineQueryId = inlineQuery.id,
            personal = true,
            results = groups.map {
                InlineQueryResultArticle(
                    id = it.id.toString(),
                    title = it.name,
                    description = "Создал: ${it.authorName}, в группе ${it.membersNum}",
                    inputMessageContent = InputTextMessageContent(
                        makeGroupMessage(it),
                        parseMode = ParseMode.Markdown
                    ),
                    replyMarkup = InlineKeyboardMarkup(makeGroupButtons(it))
                )
            },
            switchPmText = "Новая группа"
        )
    }

    /**
     * Personal message
     */
    fun processMessage(message: Message): SendMessageRequest? {
        val state = dbService.findChatState(message.chat.id)

        return when (message.text?.trim()) {
            "/start" -> onStartCommand(message.chat.id, state)
            "/close" -> onCloseCommand(message.chat.id, state, message.from!!)
            else -> onFreeFormMessage(message, state)
        }
    }

    /**
     * Button pressed
     */
    @Suppress("DeferredResultUnused")
    fun processCallbackQuery(callbackQuery: CallbackQuery): Request? {
        val parts = callbackQuery.data?.split(":")
        val group = parts?.get(1)?.toInt()?.let { dbService.getGroup(it) } ?: return null

        return when (parts[0]) {
            "join" -> {
                onJoinButton(group, callbackQuery)
            }
            "leave" -> {
                onLeaveButton(group, callbackQuery)
            }
            "tell" -> {
                onTellButton(group, callbackQuery)
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
                replyMarkup = InlineKeyboardMarkup(makeGroupButtons(group))
            )
            telegramService.sendRequest(editRequest)
        }
    }

    private fun onStartCommand(id: Int, state: ChatState): SendMessageRequest? {
        if (state != ChatState.Idle) return onBadState(id, state)

        dbService.setChatState(id, ChatState.CreateGetName)

        return SendMessageRequest(
            chatId = id,
            text = "Привет! Создаём новую группу Тайного Санты. После того как в неё добавятся все желающие, закрой " +
                    "приём в группу. После этого все получат логин того, кому должны придумать подарок. А как эта " +
                    "группа будет называться?"
        )
    }

    private fun onCloseCommand(chatId: Int, state: ChatState, user: User): SendMessageRequest? {
        if (state != ChatState.Idle) return onBadState(chatId, state)

        dbService.setChatState(chatId, ChatState.CloseGetName)

        val buttons = dbService.getGroupsForClose(user)
            .map { listOf(KeyboardButton(it.name)) }
            .plusElement(listOf(KeyboardButton("Отмена")))

        return SendMessageRequest(
            chatId,
            text = "Закрываем приём в группу? Хорошо. Выбери нужную группу.",
            replyMarkup = ReplyKeyboardMarkup(
                keyboard = buttons,
                resizeKeyboard = true,
                onetimeKeyboard = true
            )
        )
    }

    private fun onBadState(id: Int, state: ChatState): SendMessageRequest? {
        return null
    }

    private fun onFreeFormMessage(message: Message, state: ChatState): SendMessageRequest? {
        val chatId = message.chat.id
        when (state) {
            ChatState.CreateGetName -> {
                // Group creation started
                return onCreateNameGot(message, chatId)
            }
            ChatState.CloseGetName -> {
                return onCloseNameGot(chatId, message)
            }
            else -> // No started dialog
                return SendMessageRequest(
                    chatId = chatId,
                    text = """Привет. Команды такие:
                        |/start - Создать новую группу Тайного Санты
                        |/close - Закрыть приём в группу
                    """.trimMargin()
                )
        }
    }

    private fun onCloseNameGot(
        chatId: Int,
        message: Message
    ): SendMessageRequest {
        dbService.setChatState(chatId, ChatState.Idle)

        val name = message.text!!
        if (name == "Отмена") {
            return SendMessageRequest(chatId, "Ну ок")
        }

        val group = dbService.findAdminGroupByName(message.from!!, name)
            ?: return SendMessageRequest(chatId, "Нет такой группы!")

        val gid = group.id
        dbService.closeGroup(gid)
        val memberIds = dbService.getGroupMembers(gid)
        val shuffled = shuffle(memberIds)
        dbService.saveShuffled(gid, shuffled)

        return SendMessageRequest(
            chatId = chatId,
            text = "Группа закрыта и санты для всех назначены. Чтобы все об этом узнали, закинь опять группу в чат.",
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

    private fun onCreateNameGot(
        message: Message,
        chatId: Int
    ): SendMessageRequest {
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

    private fun makeGroupMessage(group: Group): String {
        val stateMessage = if (group.closed)
            "Приём в группу закрыт. Если ты в ней, можешь узнать, кому дарить."
        else
            """После того как в группу запишутся все желающие, @${group.authorName} её закроет. После этого все смогут узнать, кто
            кому дарит подарок, однако, состав изменить уже будет нельзя."""

        return """
            *Группа Тайного Санты "${group.name}"*
            Создал @${group.authorName}

            ${group.membersNum} ${inflect(group.membersNum, "участник", "участника", "участников")}

            $stateMessage
        """.trimIndent()
    }

    private fun makeGroupButtons(it: Group): List<List<InlineKeyboardButton>> {
        return if (it.closed) {
            listOf(
                listOf(
                    InlineKeyboardButton(text = "Узнать, кому дарить", callbackData = "tell:${it.id}")
                )
            )
        } else listOf(
            listOf(
                InlineKeyboardButton(text = "Присоединиться", callbackData = "join:${it.id}"),
                InlineKeyboardButton(text = "Выйти", callbackData = "leave:${it.id}")
            )
        )
    }

    private fun onJoinButton(group: Group, callbackQuery: CallbackQuery): AnswerCallbackQueryRequest {
        if (group.closed) {
            return AnswerCallbackQueryRequest(
                callbackQueryId = callbackQuery.id,
                text = "Приём в группу уже закрыт, сорри"
            )
        }

        val added = dbService.addToGroup(group.id, callbackQuery.from)

        return if (added) {
            @Suppress("DeferredResultUnused")
            updateGroupMessage(group.id, callbackQuery.inlineMessageId)

            AnswerCallbackQueryRequest(
                callbackQueryId = callbackQuery.id,
                text = "Добавились в группу"
            )
        } else {
            AnswerCallbackQueryRequest(
                callbackQueryId = callbackQuery.id,
                text = "Ты и так уже в группе"
            )
        }
    }

    private fun onLeaveButton(group: Group, callbackQuery: CallbackQuery): AnswerCallbackQueryRequest {
        if (group.closed) {
            return AnswerCallbackQueryRequest(
                callbackQueryId = callbackQuery.id,
                text = "Приём в группу уже закрыт, сорри. А потому и выход из неё тоже."
            )
        }

        val removed = dbService.removeFromGroup(group.id, callbackQuery.from)

        return if (removed) {
            @Suppress("DeferredResultUnused")
            updateGroupMessage(group.id, callbackQuery.inlineMessageId)

            AnswerCallbackQueryRequest(
                callbackQueryId = callbackQuery.id,
                text = "Вышли из группы"
            )
        } else {
            AnswerCallbackQueryRequest(
                callbackQueryId = callbackQuery.id,
                text = "Ты и так не в группе"
            )
        }
    }

    private fun onTellButton(group: Group, callbackQuery: CallbackQuery): Request? {
        if (!group.closed) {
            return AnswerCallbackQueryRequest(
                callbackQueryId = callbackQuery.id,
                text = "Не знаю, как ты нашёл эту кнопку, но группа ещё открыта"
            )
        }

        val target = dbService.findTarget(group.id, callbackQuery.from) ?: return AnswerCallbackQueryRequest(
            callbackQueryId = callbackQuery.id,
            text = "По каким-то причинам не могу найти инфу. :("
        )

        return AnswerCallbackQueryRequest(
            callbackQueryId = callbackQuery.id,
            text = "Ты даришь подарок $target",
            showAlert = true
        )
    }

    private fun inflect(num: Int, one: String, two: String, many: String): String {
        val numEff = num % 100
        return when {
            numEff in 11..19 -> many
            numEff % 10 == 1 -> one
            numEff % 10 in 2..4 -> two
            else -> many
        }
    }
}
