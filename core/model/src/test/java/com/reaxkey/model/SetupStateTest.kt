package com.reaxkey.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SetupStateTest {
    @Test fun disabledTakesPrecedenceOverStaleSelection() {
        assertEquals(SetupState.NOT_ENABLED, SetupState.from(false, true))
    }
    @Test fun enablingDoesNotImplySelection() {
        assertEquals(SetupState.ENABLED_NOT_SELECTED, SetupState.from(true, false))
    }
    @Test fun selectedRequiresBothFlags() {
        assertEquals(SetupState.SELECTED, SetupState.from(true, true))
    }
}
