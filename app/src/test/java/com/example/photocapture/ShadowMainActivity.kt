package com.example.photocapture

import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@Implements(MainActivity::class)
class ShadowMainActivity {

    companion object {
        private var shouldShowRationaleState: Boolean = false

        fun setShouldShowRationale(value: Boolean) {
            shouldShowRationaleState = value
        }

        fun reset() {
            shouldShowRationaleState = false
        }
    }

    @Implementation
    protected fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        return shouldShowRationaleState
    }
}
