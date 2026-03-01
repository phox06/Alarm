package com.example.alarm
import java.io.Serializable

data class AlarmItem(
    val id: Int,
    var hour: Int,
    var minute: Int,
    var daysOfWeek: BooleanArray,
    var soundUriString: String?,
    var isEnabled: Boolean
) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AlarmItem

        if (id != other.id) return false
        if (!daysOfWeek.contentEquals(other.daysOfWeek)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + daysOfWeek.contentHashCode()
        return result
    }
}