package com.tmobop.talkydroid.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.tmobop.talkydroid.R
import com.tmobop.talkydroid.classes.GET_UART_DEVICES_SUCCESS
import com.tmobop.talkydroid.classes.ModRfUartManager
import com.tmobop.talkydroid.classes.NO_DRIVER_AVAILABLE
import com.tmobop.talkydroid.storage.TalkyDroidDatabase
import com.tmobop.talkydroid.storage.UserWithMessagesViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception

class SettingsActivity : AppCompatActivity(), ModRfUartManager.Listener {

    private lateinit var btnBackImageButton: ImageButton
    private lateinit var mainActivity: Intent
    private lateinit var textConnectionInfos: TextView

    private lateinit var mModRfUartManager: ModRfUartManager

    private lateinit var connectedDeviceInfoText: String

    // ViewModel variables
    private lateinit var userWithMessagesViewModel: UserWithMessagesViewModel

    //-------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Button delete users
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
        TODO("Not yet implemented")
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
            textConnectionInfos.text = "No device connected"
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
}

