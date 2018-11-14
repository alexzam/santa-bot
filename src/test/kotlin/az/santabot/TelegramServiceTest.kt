package az.santabot

import az.santabot.model.SetWebhookRequest
import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.test.Test

@Test
fun testSerialization() {
    println(ObjectMapper().writeValueAsString(SetWebhookRequest("http://test/tg")))
}