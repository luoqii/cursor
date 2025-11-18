package com.example.photocapture

import android.Manifest
import android.app.Application
import android.content.DialogInterface
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowApplication

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    @Before
    fun setUpPermissions() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val shadowApplication: ShadowApplication = Shadows.shadowOf(application)
        shadowApplication.denyPermissions(Manifest.permission.CAMERA)
    }

    @Ignore("Temporarily disabled per request")
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

    @Test
    fun launcherDisplaysPrimaryControls() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            val captureButton = activity.findViewById<MaterialButton>(R.id.captureButton)
            val recordButton = activity.findViewById<MaterialButton>(R.id.recordButton)
            val photoPreview = activity.findViewById<ImageView>(R.id.photoPreview)
            val videoPreview = activity.findViewById<VideoView>(R.id.videoPreview)
            val photoTimestamp = activity.findViewById<TextView>(R.id.photoTimestampText)
            val videoTimestamp = activity.findViewById<TextView>(R.id.videoTimestampText)

            assertThat(captureButton).isNotNull()
            assertThat(captureButton.text.toString()).isEqualTo(activity.getString(R.string.take_photo))
            assertThat(recordButton.text.toString()).isEqualTo(activity.getString(R.string.record_video))

            assertThat(photoPreview.visibility).isEqualTo(View.VISIBLE)
            assertThat(videoPreview.visibility).isEqualTo(View.GONE)

            assertThat(photoTimestamp.text.toString()).isEmpty()
            assertThat(videoTimestamp.text.toString()).isEmpty()
        }

        scenario.close()
    }
}
