package az.santabot.model

import com.fasterxml.jackson.annotation.JsonProperty

class InlineQueryResponse(
    @JsonProperty("inline_query_id") val inlineQueryId: String,
    @JsonProperty("results") val results: List<InlineQueryResult>,
    @JsonProperty("is_personal") val personal: Boolean? = null,
    @JsonProperty("switch_pm_text") val switchPmText: String? = null,
    @JsonProperty("switch_pm_parameter") val switchPmParam: String? = null
) {
    val method = "answerInlineQuery"
}