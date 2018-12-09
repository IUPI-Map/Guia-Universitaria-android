package com.archivouni.guiauniversitaria

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment

object PermissionUtils {
    fun requestPermission(activity: AppCompatActivity, requestId: Int, permission: String, finishActivity: Boolean) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            RationaleDialog().newInstance(requestId, finishActivity)
                    .show(activity.supportFragmentManager, "dialog")
        } else {
            ActivityCompat.requestPermissions(activity, arrayOf(permission), requestId)
        }
    }

    fun isPermissionGranted(grantPermissions: Array<String>, grantResults: IntArray, permission: String): Boolean {
        grantPermissions.forEachIndexed { index, _ ->
            if (permission == grantPermissions[index])
                return grantResults[index] == PackageManager.PERMISSION_GRANTED
        }
        return false
    }

    class RationaleDialog: DialogFragment() {
        companion object {
            private const val ARGUMENT_PERMISSION_REQUEST_CODE = "requestCode"
            private const val ARGUMENT_FINISH_ACTIVITY = "finish"
        }

        private var mFinishActivity = false

        fun newInstance(requestCode: Int, finishActivity: Boolean): RationaleDialog {
            val arguments = Bundle()
            arguments.putInt(ARGUMENT_PERMISSION_REQUEST_CODE, requestCode)
            arguments.putBoolean(ARGUMENT_FINISH_ACTIVITY, finishActivity)
            val dialog = RationaleDialog()
            dialog.arguments = arguments
            return dialog
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val arguments = arguments
            val requestCode = arguments!!.getInt(ARGUMENT_PERMISSION_REQUEST_CODE)
            mFinishActivity = arguments.getBoolean(ARGUMENT_FINISH_ACTIVITY)

            return AlertDialog.Builder(activity)
                    .setMessage(R.string.permission_rationale_location)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                        ActivityCompat.requestPermissions(activity as Activity,
                                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                                requestCode)
                        mFinishActivity = false
                    }
                    .create()
        }

        override fun onDismiss(dialog: DialogInterface?) {
            super.onDismiss(dialog)
            if (mFinishActivity) {
                Toast.makeText(activity,
                        R.string.permission_required_toast,
                        Toast.LENGTH_SHORT)
                        .show()
                activity!!.finish()
            }
        }
    }

    class PermissionDeniedDialog: DialogFragment() {
        companion object {
            private const val ARGUMENT_FINISH_ACTIVITY = "finish"
        }

        private var mFinishActivity = false

        fun newInstance(finishActivity: Boolean): PermissionDeniedDialog {
            val arguments = Bundle()
            arguments.putBoolean(ARGUMENT_FINISH_ACTIVITY, finishActivity)
            val dialog = PermissionDeniedDialog()
            dialog.arguments = arguments
            return dialog
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            mFinishActivity = arguments!!.getBoolean(ARGUMENT_FINISH_ACTIVITY)
            return AlertDialog.Builder(activity)
                    .setMessage(R.string.location_permission_denied)
                    .setPositiveButton(android.R.string.ok, null)
                    .create()
        }

        override fun onDismiss(dialog: DialogInterface?) {
            super.onDismiss(dialog)
            if (mFinishActivity) {
                Toast.makeText(activity, R.string.permission_required_toast,
                        Toast.LENGTH_SHORT)
                        .show()
                activity!!.finish()
            }
        }
    }
}