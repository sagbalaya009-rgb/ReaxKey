package com.reaxkey.search

import java.text.Normalizer
import java.util.Locale

object Lexical {
    fun normalize(value:String):String=Normalizer.normalize(value,Normalizer.Form.NFKD).replace(Regex("\\p{M}+"),"").lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+")," ").trim().replace(Regex("\\s+")," ")
    fun match(value:String):String=normalize(value).split(' ').filter(String::isNotBlank).take(12).joinToString(" AND ") { "\"$it\"*" }
}
