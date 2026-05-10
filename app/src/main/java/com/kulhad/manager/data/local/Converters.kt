package com.kulhad.manager.data.local

import androidx.room.TypeConverter

/**
 * All entity columns store primitive types directly (Long, Int, String, Double, Boolean).
 * Boolean is auto-converted by Room to INTEGER. This class is reserved for any future
 * complex converter needs.
 */
class Converters {
    @TypeConverter
    fun stringFromLong(value: Long?): String? = value?.toString()

    @TypeConverter
    fun longFromString(value: String?): Long? = value?.toLongOrNull()
}
