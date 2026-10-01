package com.reaxkey.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.reaxPreferences by preferencesDataStore("preferences")
data class UserPreferences(val theme:String="SYSTEM",val language:String="en",val haptics:Boolean=true)
class Preferences(context:Context) {
    private val store=context.applicationContext.reaxPreferences
    val flow=store.data.map { UserPreferences(it[THEME] ?: "SYSTEM",it[LANGUAGE] ?: "en",it[HAPTICS] ?: true) }
    suspend fun theme(value:String) { store.edit { it[THEME]=value } }
    suspend fun language(value:String) { store.edit { it[LANGUAGE]=value } }
    suspend fun haptics(value:Boolean) { store.edit { it[HAPTICS]=value } }
    companion object { private val THEME=stringPreferencesKey("theme"); private val LANGUAGE=stringPreferencesKey("language"); private val HAPTICS=booleanPreferencesKey("haptics") }
}
