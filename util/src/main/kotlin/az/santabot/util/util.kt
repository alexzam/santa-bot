package az.santabot.util

import kotlin.random.Random
import kotlin.random.nextUBytes

@OptIn(ExperimentalUnsignedTypes::class)
fun genToken() = Random.nextUBytes(10).map { it.toString(16) }.fold("") { acc, s -> acc + s }