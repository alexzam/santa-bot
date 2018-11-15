package az.santabot.util

import com.fasterxml.jackson.annotation.JsonCreator

class Either<T : Any, U : Any>
@JsonCreator private constructor(val left: T?, val right: U?) {
    companion object {
        fun <T : Any, U : Any> consLeft(left: T): Either<T, U> = Either(left, null)
        fun <T : Any, U : Any> consRight(right: U): Either<T, U> = Either(null, right)
    }
}