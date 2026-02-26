package com.example.alarm
import java.io.Serializable

data class AlarmItem(
    val id: Int,                    // Unique ID for PendingIntent
    var hour: Int,
    var minute: Int,
    var daysOfWeek: BooleanArray,   // [Sun, Mon, Tue, Wed, Thu, Fri, Sat]
    var soundUriString: String?,
    var isEnabled: Boolean
) : Serializable {

    // Auto-generated equals() and hashCode() are needed for BooleanArray
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