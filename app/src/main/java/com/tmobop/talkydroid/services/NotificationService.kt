package com.tmobop.talkydroid.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.tmobop.talkydroid.R
import com.tmobop.talkydroid.activities.MainActivity
import com.tmobop.talkydroid.classes.GET_UART_DEVICES_SUCCESS
import com.tmobop.talkydroid.classes.ModRfUartManager
import com.tmobop.talkydroid.storage.UserWithMessagesViewModel
import java.util.*
import kotlin.collections.ArrayList

class NotificationService : Service(), ModRfUartManager.Listener, LifecycleOwner {

    // Notification variables
    private val channelId = "Notification from Service"
    private var notificationManager: NotificationManager? = null
    private val notificationId = 101

    // Hardware variables
    private lateinit var mModRfUartManager: ModRfUartManager

    // Database
    private var userWithMessagesViewModel: UserWithMessagesViewModel? = null

    //------------------------------------------------------------------------
    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Get the notification service
            notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Create the notification channel
            createNotificationChannel(
                CHANNEL_ID,
                "TalkyDroid_channel",
                "TalkyDroid_description"
            )
        }
    }

    //------------------------------------------------------------------------
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        // Get the database
        userWithMessagesViewModel = MainActivity.userWithMessagesViewModel

        // Get the hardware
        mModRfUartManager = ModRfUartManager(this, this)

        // Handle the notification intent
        handleNotificationIntent(intent)

        return START_NOT_STICKY
    }

    //------------------------------------------------------------------------
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    //------------------------------------------------------------------------
    private fun createNotificationChannel(id: String, name: String, description: String) {

        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(id, name, importance)

        channel.description = description
        channel.enableLights(true)
        channel.lightColor = Color.BLUE
        channel.enableVibration(true)
        channel.vibrationPattern =
            longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)

        notificationManager?.createNotificationChannel(channel)
    }

    //------------------------------------------------------------------------
    private fun sendNotification(content: String, senderName: String) {

        val replyLabel = "Enter your reply here"

        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel(replyLabel)
            .build()

        val resultIntent = Intent(this, MainActivity::class.java)

        val resultPendingIntent = PendingIntent.getActivity(
            this,
            0,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val icon = Icon.createWithResource(
            this,
            R.drawable.ic_baseline_send
        )

        val replyAction = Notification.Action.Builder(
            icon,
            "Reply", resultPendingIntent
        )
            .addRemoteInput(remoteInput)
            .build()

        val newMessageNotification = Notification.Builder(this, MainActivity.CHANNEL_ID)
            .setColor(
                ContextCompat.getColor(
                    this,
                    R.color.colorNotification
                )
            )
            .setSmallIcon(R.drawable.ic_walkie_talkie)
            .setContentTitle(senderName)
            .setContentText(content)
            .addAction(replyAction).build()

        val notificationManager = getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        notificationManager.notify(
            notificationId,
            newMessageNotification
        )
    }

    //------------------------------------------------------------------------
    private fun handleNotificationIntent(intent: Intent) {

        val remoteInput = RemoteInput.getResultsFromIntent(intent)

        if (remoteInput != null) {

            val inputString = remoteInput.getCharSequence(
                MainActivity.KEY_TEXT_REPLY
            ).toString()


            Toast.makeText(this, inputString, Toast.LENGTH_LONG).show()

            lateinit var repliedNotification: Notification

            // Test if hardware connected
            when (mModRfUartManager.getDevice()) {

                // If Hardware connected
                GET_UART_DEVICES_SUCCESS -> {
                    // TODO --> Send message to Hardware
                    // TODO --> Add msg sent to database
                    repliedNotification = Notification.Builder(this, MainActivity.CHANNEL_ID)
                        .setSmallIcon(
                            android.R.drawable.ic_dialog_info
                        )
                        .setContentText("Reply sent")
                        .build()
                }

                // else
                else -> {
                    // Display a error message
                    repliedNotification = Notification.Builder(this, MainActivity.CHANNEL_ID)
                        .setColor(
                            ContextCompat.getColor(
                                this,
                                R.color.red
                            )
                        )
                        .setSmallIcon(
                            android.R.drawable.stat_notify_error
                        )
                        .setContentText("Reply cannot be sent (Hardware not connected)")
                        .build()
                }
            }

            notificationManager?.notify(notificationId, repliedNotification)
        }
    }

    //------------------------------------------------------------------------
    //private fun isAppRunning(context: Context, packageName: String): Boolean {
    //    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    //    val procInfos = activityManager.runningAppProcesses
    //    if (procInfos != null) {
    //        for (processInfo in procInfos) {
    //            if (processInfo.processName == packageName) {
    //                return true
    //            }
    //        }
    //    }
    //    return false
    //}

    //------------------------------------------------------------------------
    companion object {
        const val CHANNEL_ID = "channel_id"
        const val KEY_TEXT_REPLY = "key_text_reply"
    }

    //-------------------------------------------------------------------
    override fun onTextReceived(string: String, senderUUID: String) {
        if (senderUUID != "") {

            userWithMessagesViewModel!!.getAllUsersWithMessages().observe(this,
                { usersWithMessage ->

                    // Get the sender entity
                    val senderEntity = usersWithMessage.findLast { userWithMessages ->
                        userWithMessages.userEntity!!.userId == UUID.fromString(senderUUID)
                    }

                    // Check if sender entity is in database
                    if (senderEntity != null) {
                        // Get the sender name
                        val senderName = senderEntity.userEntity!!.userName

                        // Send the notification
                        sendNotification(string, senderName)
                    }
                }
            )
        }
    }

    //-------------------------------------------------------------------
    override fun onLocationReceived(location: String, senderUUID: String) {

        if (senderUUID != "") {

            userWithMessagesViewModel!!.getAllUsersWithMessages().observe(this,
                { usersWithMessage ->

                // Get the sender entity
                val senderEntity = usersWithMessage.findLast { userWithMessages ->
                    userWithMessages.userEntity!!.userId == UUID.fromString(senderUUID)
                }

                // Check if sender entity is in database
                if (senderEntity != null) {
                    // Get the sender name
                    val senderName = senderEntity.userEntity!!.userName

                    // Get the coords
                    val cords = location.split(',')
                    val latitude = cords[0]
                    val longitude = cords[1]

                    // Send the notification
                    sendNotification("Latitude = $latitude, Longitude = $longitude", senderName)
                }
            })
        }
    }

    //-------------------------------------------------------------------
    override fun onError(customText: String, e: Exception) {
        return
    }

    //-------------------------------------------------------------------
    override fun onDiscoverProgress(currentAddress: Int, totalAddresses: Int) {
        return
    }

    //-------------------------------------------------------------------
    override fun onDiscoverFinished(devicesFoundInChannel: ArrayList<ModRfUartManager.DeviceInChannel>) {
        return
    }

    //-------------------------------------------------------------------
    override fun onDeviceJoinedNetwork(device: ModRfUartManager.DeviceInChannel) {
        return
    }

    //-------------------------------------------------------------------
    override fun onDeviceAttached() {
        return
    }

    //-------------------------------------------------------------------
    override fun onDeviceDetached() {
        return
    }

    //-------------------------------------------------------------------
    override fun onDeviceOpened() {
        return
    }

    //-------------------------------------------------------------------
    override fun onDeviceOpenError() {
        return
    }

    override fun getLifecycle(): Lifecycle {
        TODO("Not yet implemented")
    }
}