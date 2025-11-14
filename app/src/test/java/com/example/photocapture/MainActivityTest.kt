package com.example.photocapture

import android.content.DialogInterface
import android.net.Uri
import android.provider.Settings
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    @Test
    fun clickingOpenSettingsButton_launchesSettingsIntent() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        var activity: MainActivity? = null
        scenario.onActivity { launchedActivity ->
            activity = launchedActivity
            val method = MainActivity::class.java.getDeclaredMethod("showPermissionDeniedDialog")
            method.isAccessible = true
            method.invoke(launchedActivity)
        }

        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        assertThat(dialog).isNotNull()

        val positiveButton = dialog!!.getButton(DialogInterface.BUTTON_POSITIVE)
        assertThat(positiveButton).isNotNull()

        positiveButton.performClick()

        val nonNullActivity = requireNotNull(activity)
        val startedIntent = Shadows.shadowOf(nonNullActivity).nextStartedActivity
        assertThat(startedIntent).isNotNull()
        assertThat(startedIntent!!.action).isEqualTo(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        assertThat(startedIntent.data).isEqualTo(
            Uri.fromParts("package", nonNullActivity.packageName, null)
        )

        scenario.close()
    }
}
