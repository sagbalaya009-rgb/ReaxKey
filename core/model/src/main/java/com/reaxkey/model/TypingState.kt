package com.reaxkey.model

import java.util.Locale

enum class Shift { OFF, ONCE, LOCKED }
enum class Layer { LETTERS, NUMBERS, SYMBOLS }

class TypingState {
    var shift = Shift.OFF
        private set
    var layer = Layer.LETTERS
    private var lastShiftAt = Long.MIN_VALUE
    fun shift(now: Long) {
        shift = when {
            shift == Shift.LOCKED -> Shift.OFF
            lastShiftAt != Long.MIN_VALUE && now - lastShiftAt in 0..400 -> Shift.LOCKED
            shift == Shift.OFF -> Shift.ONCE
            else -> Shift.OFF
        }
        lastShiftAt = now
    }
    fun autoCap(enabled: Boolean) { if (shift != Shift.LOCKED) shift = if(enabled) Shift.ONCE else Shift.OFF }
    fun text(key: String, locale: Locale): String {
        val result = if (shift != Shift.OFF) key.uppercase(locale) else key
        if (shift == Shift.ONCE) shift = Shift.OFF
        return result
    }
    fun reset() { shift = Shift.OFF; layer = Layer.LETTERS; lastShiftAt = Long.MIN_VALUE }
}

data class LanguagePack(val tag: String, val name: String, val rows: List<List<String>>, val alternatives: Map<String,List<String>>, val decimal: String) {
    companion object {
        /** Tab-separated header, three space-separated key rows, then key=alternatives. */
        fun parse(data: String): LanguagePack {
            val lines = data.lineSequence().filter { it.isNotBlank() && !it.startsWith("#") }.toList()
            require(lines.size >= 4)
            val header = lines[0].split('\t')
            require(header.size == 3 && header[0].isNotBlank())
            val rows = lines.subList(1,4).map { it.split(' ').filter(String::isNotBlank) }
            require(rows.all { it.size in 1..12 })
            val alternatives = lines.drop(4).associate { line ->
                val parts = line.split('=', limit=2); require(parts.size == 2)
                parts[0] to parts[1].split(' ').filter(String::isNotBlank)
            }
            return LanguagePack(header[0], header[1], rows, alternatives, header[2])
        }
    }
}
