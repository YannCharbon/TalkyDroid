package com.tmobop.talkydroid.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.tmobop.talkydroid.R
import java.util.*


class LoginActivity : AppCompatActivity() {

    private lateinit var userNameEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var avatarImageButton: ImageButton

    private val pickImage = 100
    private var imageUri: Uri? = null

    private val userINFO : String = "user_information"
    private lateinit var sharedPreferences: SharedPreferences
    private val userNameKey = "userNameKey"
    private val userUUIDKey = "userUUIDKey"
    private val userAvatarPathKey = "userAvatarPathKey"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //------------------------------ Login ----------------------------------------
        // If UserName is not empty -> login, else -> display error
        userNameEditText = findViewById(R.id.login_username_editText)
        loginButton = findViewById(R.id.login_button)
        avatarImageButton = findViewById(R.id.login_avatar)

        //---------------------- get the user UUID back -------------------------------

        sharedPreferences = getSharedPreferences(userINFO, Context.MODE_PRIVATE)
        val editor: Editor = sharedPreferences.edit()

        // If no user UUID in memory --> generate a new one, else --> recover the old one
        if (sharedPreferences.contains(userUUIDKey)) {
            Toast.makeText(
                this,
                "userUUID : " + sharedPreferences.getString(userUUIDKey, "unknown"),
                Toast.LENGTH_LONG
            ).show()
        }
        else {
            val newUUID = UUID.randomUUID().toString()
            editor.putString(userUUIDKey, newUUID)
            Toast.makeText(this, "NEW UUID : $newUUID", Toast.LENGTH_LONG).show()
        }

        // If userName in memory --> display it on the screen
        if (sharedPreferences.contains(userNameKey)) {
            userNameEditText.setText(sharedPreferences.getString(userNameKey, "unknown"))
        }

        // If userAvatarPath in memory --> display it on the screen
        if (sharedPreferences.contains(userAvatarPathKey)) {
            if (isStoragePermissionGranted()) {
                val avatarPath = sharedPreferences.getString(userAvatarPathKey, "")
                avatarImageButton.setImageURI(Uri.parse(avatarPath))
            }
            else {
                Toast.makeText(this, "Cannot show the profile picture, because the app do not have the permission required", Toast.LENGTH_LONG).show()
            }
        }
        else {
            avatarImageButton.setImageResource(R.drawable.ic_baseline_unknown_user)
        }

        //---- Login button
        loginButton.setOnClickListener {
            if (userNameEditText.text.isEmpty()) {
                userNameEditText.error = "The item Username cannot be empty !";
            }
            else {
                // push username to shared preferences
                editor.putString(userNameKey, userNameEditText.text.toString())
                editor.apply()

                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra(USER_UUID, sharedPreferences.getString(userUUIDKey, "unknown"))
                intent.putExtra(USER_NAME, userNameEditText.text.toString())
                startActivity(intent)
                finish()
            }
        }

        //---- Avatar image button
        avatarImageButton.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, pickImage)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //------------------------ Send image -----------------------------
        if (resultCode == RESULT_OK && requestCode == pickImage) {
            imageUri = data?.data
            avatarImageButton.setImageURI(Uri.parse(imageUri.toString()))

            // Push the avatar image path to shared preferences
            sharedPreferences = getSharedPreferences(userINFO, Context.MODE_PRIVATE)
            val editor: Editor = sharedPreferences.edit()
            editor.putString(userAvatarPathKey, imageUri.toString())
            Toast.makeText(this, imageUri.toString(), Toast.LENGTH_LONG).show()
            editor.apply()

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

    companion object {
        const val USER_UUID = "user_uuid"
        const val USER_NAME = "user_name"
    }
}