package az.santabot.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class User
@JsonCreator constructor(
    @JsonProperty("id") val id: Int,
    @JsonProperty("first_name") val firstName: String,
    @JsonProperty("last_name") val lastName: String?,
    @JsonProperty("username") val username: String?
) {
    val display: String
        get() {
            val ret = StringBuilder(firstName)
            if (lastName != null) ret.append(" $lastName")
            if (username != null) ret.append(" ($username)")
            return ret.toString()
        }
}
