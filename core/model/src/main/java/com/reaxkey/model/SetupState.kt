package com.reaxkey.model

enum class SetupState { NOT_ENABLED, ENABLED_NOT_SELECTED, SELECTED;
    companion object {
        fun from(enabled: Boolean, selected: Boolean): SetupState = when {
            !enabled -> NOT_ENABLED
            selected -> SELECTED
            else -> ENABLED_NOT_SELECTED
        }
    }
}
