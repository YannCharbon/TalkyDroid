package com.tmobop.talkydroid.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.squareup.picasso.Picasso
import com.tmobop.talkydroid.R
import com.tmobop.talkydroid.classes.CircleTransformation
import com.tmobop.talkydroid.classes.GET_UART_DEVICES_SUCCESS
import com.tmobop.talkydroid.classes.ModRfUartManager
import com.tmobop.talkydroid.classes.NO_DRIVER_AVAILABLE
import com.tmobop.talkydroid.services.NotificationService
import com.tmobop.talkydroid.storage.TalkyDroidDatabase
import com.tmobop.talkydroid.storage.UserEntity
import com.tmobop.talkydroid.storage.UserWithMessagesViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class SettingsActivity : AppCompatActivity(), ModRfUartManager.Listener {

    private lateinit var btnBackImageButton: ImageButton
    private lateinit var mainActivity: Intent
    private lateinit var textConnectionInfos: TextView

    private lateinit var mModRfUartManager: ModRfUartManager

    private lateinit var connectedDeviceInfoText: String

    // Shared preferences
    private val userINFO : String = "user_information"
    private lateinit var sharedPreferences: SharedPreferences
    private val userNameKey = "userNameKey"
    private val userAvatarPathKey = "userAvatarPathKey"

    // ViewModel variables
    private lateinit var userWithMessagesViewModel: UserWithMessagesViewModel

    //-------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        //------------------------------- User profil ----------------------------------
        // Get the UI elements
        val textViewUserName = findViewById<TextView>(R.id.settings_textView_username)
        val imageViewUserAvatar = findViewById<ImageView>(R.id.settings_imageView_userAvatar)

        // Get shared preferences
        sharedPreferences = getSharedPreferences(userINFO, Context.MODE_PRIVATE)

        // Display userName
        if (sharedPreferences.contains(userNameKey)) {
            textViewUserName.text = sharedPreferences.getString(userNameKey, "unknown")
        }

        // Display userAvatar
        if (sharedPreferences.contains(userAvatarPathKey)) {
            if (isStoragePermissionGranted()) {
                val avatarPath = sharedPreferences.getString(userAvatarPathKey, "")

                // Round image
                Picasso.get()
                    .load(Uri.parse(avatarPath))
                    .error(R.drawable.ic_baseline_unknown_user)
                    .placeholder(R.drawable.ic_baseline_unknown_user)
                    .transform(CircleTransformation())
                    .into(imageViewUserAvatar)
            }
            else {
                Toast.makeText(this, "Cannot show the profile picture, because the app do not have the permission required", Toast.LENGTH_LONG).show()
            }
        }
        else {
            imageViewUserAvatar.setImageResource(R.drawable.ic_baseline_unknown_user)
        }

        //-------------------------- Change user profil button -------------------------
        val btnChangeProfil = findViewById<LinearLayout>(R.id.settings_linearLayout_Button_change_user_profil)

        btnChangeProfil.setOnClickListener{
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
            finish()
        }

        //--------------------------- Delete all users button --------------------------
        val buttonDeleteConvs = findViewById<Button>(R.id.settings_button_deleteUsers)

        userWithMessagesViewModel = ViewModelProvider(this).get(UserWithMessagesViewModel::class.java)

        buttonDeleteConvs.setOnClickListener{
            lifecycleScope.launch {
                userWithMessagesViewModel.deleteAllUsers()
            }
            Toast.makeText(this, "All users and messages deleted", Toast.LENGTH_SHORT).show()
        }

        //------------------------------ ModRfUartManager -------------------------------
        mModRfUartManager = ModRfUartManager(this, this)

        //-------------------------------- Back button ----------------------------------
        btnBackImageButton = findViewById(R.id.settings_toolbar_back_arrow_imageButton)

        btnBackImageButton.setOnClickListener {
            mainActivity = Intent(this, MainActivity::class.java)
            startActivityIfNeeded(mainActivity, 0)
            finish()
        }

        //------------------------- Text connection informations ------------------------
        textConnectionInfos = findViewById(R.id.settings_text_connection_informations)

        runOnUiThread {
            when (mModRfUartManager.getDevice()) {
                GET_UART_DEVICES_SUCCESS -> {
                    val deviceProperties = mModRfUartManager.getDeviceProporties()!!
                    connectedDeviceInfoText =
                        "Name : " + deviceProperties.deviceName + "\n" +
                                "Manufacturer : " + deviceProperties.manufacturerName + "\n" +
                                "Product name : " + deviceProperties.productName + "\n" +
                                "id : " + deviceProperties.id
                    textConnectionInfos.text = connectedDeviceInfoText
                }
            }
        }
    }

    override fun onTextReceived(string: String, senderUUID: String) {
        TODO("Not yet implemented")
    }

    override fun onLocationReceived(location: String, senderUUID: String) {
        TODO("Not yet implemented")
    }

    override fun onError(customText: String, e: Exception) {
        runOnUiThread {
            Toast.makeText(this, customText + e.toString(), Toast.LENGTH_LONG).show()
        }
    }

    override fun onDiscoverProgress(currentAddress: Int, totalAddresses: Int) {
        TODO("Not yet implemented")
    }

    override fun onDiscoverFinished(devicesFoundInChannel: ArrayList<ModRfUartManager.DeviceInChannel>) {
        TODO("Not yet implemented")
    }

    override fun onDeviceJoinedNetwork(device: ModRfUartManager.DeviceInChannel) {
        runOnUiThread {
            Toast.makeText(this, "New device joined network", Toast.LENGTH_SHORT).show()
        }

        lifecycleScope.launch {
            val newConversation = UserEntity(
                UUID.fromString(device.userUUID),
                device.userName,
                "",
                online = true
            )
            userWithMessagesViewModel.insertUser(newConversation)
            userWithMessagesViewModel.setUserOnline(UUID.fromString(device.userUUID))
        }
    }

    override fun onDeviceAttached() {

        runOnUiThread {
            when (mModRfUartManager.getDevice()) {
                GET_UART_DEVICES_SUCCESS -> {
                    val deviceProperties = mModRfUartManager.getDeviceProporties()!!
                    connectedDeviceInfoText =
                        "Name : " + deviceProperties.deviceName + "\n" +
                                "Manufacturer : " + deviceProperties.manufacturerName + "\n" +
                                "Product name : " + deviceProperties.productName + "\n" +
                                "id : " + deviceProperties.id
                    textConnectionInfos.text = connectedDeviceInfoText
                }
            }
        }
    }

    override fun onDeviceDetached() {
        runOnUiThread {
            Toast.makeText(this, "Device detached", Toast.LENGTH_SHORT).show()

            lifecycleScope.launch {
                userWithMessagesViewModel.setAllUsersOffline()
            }
        }
    }

    override fun onDeviceOpened() {
        runOnUiThread {
            Toast.makeText(this, "Device opened", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDeviceOpenError() {
        runOnUiThread {
            Toast.makeText(this, "Cannot open Device : permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    //-------------------------- Permission to read storage ----------------------------------------
    private fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission granted
                true
            } else {
                // Permission revoked
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    1
                )
                false
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            // Permission granted
            true
        }
    }
}

