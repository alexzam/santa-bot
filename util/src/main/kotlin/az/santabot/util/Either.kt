package az.santabot.util

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("UNCHECKED_CAST")
class Either<T : Any, U : Any>
@JsonCreator constructor(value: Any) {
    private val left: T? = value as? T
    private val right: U? = value as? U

    @JsonValue
    fun getValue(): Any = left ?: right!!

    fun <R> fold(leftF: (T) -> R, rightF: (U) -> R): R = if (left != null) leftF(left) else rightF(right!!)
}