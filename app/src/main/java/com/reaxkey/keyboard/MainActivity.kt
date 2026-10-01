package com.reaxkey.keyboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import com.reaxkey.model.SetupState
import com.reaxkey.onboarding.SetupScreen
import com.reaxkey.ui.ReaxTheme

class MainActivity : ComponentActivity() {
    private val setup = mutableStateOf(SetupState.NOT_ENABLED)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReaxTheme { Surface { SetupScreen(setup.value,
            enable = { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
            select = { getSystemService(InputMethodManager::class.java).showInputMethodPicker() }) } } }
    }
    override fun onResume() {
        super.onResume()
        val manager = getSystemService(InputMethodManager::class.java)
        val id = manager.enabledInputMethodList.firstOrNull { it.packageName == packageName }?.id
        setup.value = SetupState.from(id != null, id != null && Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) == id)
    }
}
