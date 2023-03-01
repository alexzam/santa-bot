package az.santabot

import az.santabot.util.shuffle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TelegramServiceTest {

    @Test
    fun testShuffle() {
        val shuffled0 = shuffle(listOf(1))
        assertEquals(1, shuffled0[1])

        val shuffled1 = shuffle(listOf(1, 2))
        assertEquals(2, shuffled1[1])
        assertEquals(1, shuffled1[2])

        val shuffled2 = shuffle(listOf(1, 2, 3, 4, 5))
        println(shuffled2)
    }
}