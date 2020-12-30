package com.tmobop.talkydroid.adapters

import android.app.Application
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.tmobop.talkydroid.R
import com.tmobop.talkydroid.classes.MessageType
import com.tmobop.talkydroid.classes.MessageUI.Companion.TYPE_FILE_RECEIVED
import com.tmobop.talkydroid.classes.MessageUI.Companion.TYPE_FILE_SEND
import com.tmobop.talkydroid.classes.MessageUI.Companion.TYPE_IMAGE_RECEIVED
import com.tmobop.talkydroid.classes.MessageUI.Companion.TYPE_IMAGE_SEND
import com.tmobop.talkydroid.classes.MessageUI.Companion.TYPE_LOCATION_RECEIVED
import com.tmobop.talkydroid.classes.MessageUI.Companion.TYPE_LOCATION_SEND
import com.tmobop.talkydroid.classes.MessageUI.Companion.TYPE_MESSAGE_RECEIVED
import com.tmobop.talkydroid.classes.MessageUI.Companion.TYPE_MESSAGE_SEND
import com.tmobop.talkydroid.storage.MessageEntity
import java.text.SimpleDateFormat
import java.util.*

class ConversationAdapter(private val context: Context, private val messageList: MutableList<MessageEntity>) : RecyclerView.Adapter<ConversationAdapter.MessageViewHolder>() {

    //------------------------------------------
    fun swapMessages(messages: List<MessageEntity>) {
        this.messageList.clear()
        this.messageList.addAll(messages)
        notifyDataSetChanged()
    }

    //------------------------------------------
    fun addMessage(message: MessageEntity) {
        this.messageList.add(0, message)
        notifyDataSetChanged()
    }

    //------------------------------------------
    fun deleteAllMessages() {
        messageList.clear()
        notifyDataSetChanged()
    }

    //------------------------------------------
    override fun getItemCount(): Int = messageList.size

    //------------------------------------------
    open class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        open fun bind(context: Context, message: MessageEntity) {}
    }

    //------------------------------------------
    class MessageSendViewHolder(view: View) : MessageViewHolder(view) {
        private val textViewMessageContent = view.findViewById<TextView>(R.id.message_send)
        private val textViewMessageTime = view.findViewById<TextView>(R.id.time_message_send)

        override fun bind(context: Context, message: MessageEntity) {
            textViewMessageContent.text = message.content
            textViewMessageTime.text = DateUtils.fromMillisToTimeString(message.time)
        }
    }

    //------------------------------------------
    class MessageReceivedViewHolder(view: View) : MessageViewHolder(view) {
        private val textViewMessageContent = view.findViewById<TextView>(R.id.message_received)
        private val textViewMessageTime = view.findViewById<TextView>(R.id.time_message_received)
        private val imageViewMessageAvatar = view.findViewById<ImageView>(R.id.avatar_message_received)

        override fun bind(context: Context, message: MessageEntity) {
            textViewMessageContent.text = message.content
            textViewMessageTime.text = DateUtils.fromMillisToTimeString(message.time)
            imageViewMessageAvatar.setImageResource(R.drawable.ic_baseline_unknown_user)
        }
    }

    //------------------------------------------
    class ImageSendViewHolder(view: View) : MessageViewHolder(view) {
        private val imageViewMessageContent = view.findViewById<ImageView>(R.id.image_send)
        private val textViewMessageTime = view.findViewById<TextView>(R.id.time_image_send)

        override fun bind(context: Context, message: MessageEntity) {
            imageViewMessageContent.setImageURI(Uri.parse(message.content))
            textViewMessageTime.text = DateUtils.fromMillisToTimeString(message.time)
        }
    }

    //------------------------------------------
    class ImageReceivedViewHolder(view: View) : MessageViewHolder(view) {
        private val imageViewMessageContent = view.findViewById<ImageView>(R.id.image_received)
        private val textViewMessageTime = view.findViewById<TextView>(R.id.time_image_received)
        private val imageViewMessageAvatar =
            view.findViewById<ImageView>(R.id.avatar_image_received)

        override fun bind(context: Context, message: MessageEntity) {
            imageViewMessageContent.setImageURI(Uri.parse(message.content))
            textViewMessageTime.text = DateUtils.fromMillisToTimeString(message.time)
            imageViewMessageAvatar.setImageResource(R.drawable.ic_baseline_unknown_user)
        }
    }

    //------------------------------------------
    class LocationSendViewHolder(view: View) : MessageViewHolder(view), OnMapReadyCallback {

        private val mapViewLocation = view.findViewById<MapView>(R.id.map_loc_send)
        private val textViewMessageTime = view.findViewById<TextView>(R.id.time_loc_send)
        private lateinit var map: GoogleMap
        private lateinit var context: Context
        private lateinit var latLng: LatLng
        private var longitude: Double = 0.0
        private var latitude: Double = 0.0

        init {
            with(mapViewLocation) {
                // Initialise the MapView
                onCreate(null)
                // Set the map ready callback to receive the GoogleMap object
                getMapAsync(this@LocationSendViewHolder)
            }
        }

        override fun bind(context: Context, message: MessageEntity){
            this.context = context

            // Display the message time
            textViewMessageTime.text = DateUtils.fromMillisToTimeString(message.time)

            // Recover latitude and longitude from message
            val cords = message.content.split(',')
            latitude = cords[0].toDouble()
            longitude = cords[1].toDouble()
            latLng = LatLng(latitude, longitude)
            setLocation()


        }

        override fun onMapReady(googleMap: GoogleMap?) {
            MapsInitializer.initialize(context.applicationContext)
            map = googleMap ?: return
            setLocation()
        }

        private fun setLocation() {
            if (!::map.isInitialized) return
            with(map) {
                moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11f))
                addMarker(MarkerOptions().position(latLng))
                mapType = GoogleMap.MAP_TYPE_NORMAL
            }
            mapViewLocation.onResume()
        }
    }

    //------------------------------------------
    class LocationReceivedViewHolder(view: View) : MessageViewHolder(view), OnMapReadyCallback {

        private val mapViewLocation = view.findViewById<MapView>(R.id.map_loc_received)
        private val textViewMessageTime = view.findViewById<TextView>(R.id.time_location_received)
        private lateinit var map: GoogleMap
        private lateinit var context: Context
        private lateinit var latLng: LatLng
        private var longitude: Double = 0.0
        private var latitude: Double = 0.0

        //TODO bind private val imageViewAvatar = view.findViewById<ImageView>(R.id.avatar_location_received)

        init {
            with(mapViewLocation) {
                // Initialise the MapView
                onCreate(null)
                // Set the map ready callback to receive the GoogleMap object
                getMapAsync(this@LocationReceivedViewHolder)
            }
        }

        override fun bind(context: Context, message: MessageEntity){
            this.context = context

            // Display the message time
            textViewMessageTime.text = DateUtils.fromMillisToTimeString(message.time)

            // Recover latitude and longitude from message
            val cords = message.content.split(',')
            latitude = cords[0].toDouble()
            longitude = cords[1].toDouble()
            latLng = LatLng(latitude, longitude)
            setLocation()
        }

        override fun onMapReady(googleMap: GoogleMap?) {
            MapsInitializer.initialize(context.applicationContext)
            map = googleMap ?: return
            setLocation()
        }

        private fun setLocation() {
            if (!::map.isInitialized) return
            with(map) {
                moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11f))
                addMarker(MarkerOptions().position(latLng))
                mapType = GoogleMap.MAP_TYPE_NORMAL
            }
            mapViewLocation.onResume()
        }
    }

    //------------------------------------------
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        return when (viewType) {
            TYPE_MESSAGE_SEND -> {
                val view = LayoutInflater.from(parent.context).inflate(
                    R.layout.bubble_msg_send,
                    parent,
                    false
                )
                MessageSendViewHolder(view)
            }
            TYPE_MESSAGE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context).inflate(
                    R.layout.bubble_msg_received,
                    parent,
                    false
                )
                MessageReceivedViewHolder(view)
            }
            TYPE_IMAGE_SEND -> {
                val view = LayoutInflater.from(parent.context).inflate(
                    R.layout.bubble_img_send,
                    parent,
                    false
                )
                ImageSendViewHolder(view)
            }
            TYPE_IMAGE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context).inflate(
                    R.layout.bubble_img_received,
                    parent,
                    false
                )
                ImageReceivedViewHolder(view)
            }
            TYPE_LOCATION_SEND -> {
                val view = LayoutInflater.from(parent.context).inflate(
                    R.layout.bubble_loc_send,
                    parent,
                    false
                )
                LocationSendViewHolder(view)
            }
            TYPE_LOCATION_RECEIVED -> {
                val view = LayoutInflater.from(parent.context).inflate(
                    R.layout.bubble_loc_received,
                    parent,
                    false
                )
                LocationReceivedViewHolder(view)
            }
            // TODO -> Add :TYPE_FILE_SEND, TYPE_FILE_RECEIVED
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = messageList[position]

        when (App.userUUID) {
            message.senderId ->
                return when (message.messageType) {
                    MessageType.TEXT -> TYPE_MESSAGE_SEND
                    MessageType.IMAGE -> TYPE_IMAGE_SEND
                    MessageType.FILE -> TYPE_FILE_SEND
                    MessageType.LOCATION -> TYPE_LOCATION_SEND
                    else -> throw IllegalArgumentException("Invalid type")
                }
            message.receiverId ->
                return when (message.messageType) {
                    MessageType.TEXT -> TYPE_MESSAGE_RECEIVED
                    MessageType.IMAGE -> TYPE_IMAGE_RECEIVED
                    MessageType.FILE -> TYPE_FILE_RECEIVED
                    MessageType.LOCATION -> TYPE_LOCATION_RECEIVED
                    else -> throw IllegalArgumentException("Invalid type")
                }
            else -> throw java.lang.IllegalArgumentException("Wrong conversation")
        }
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]
        when (holder) {
            is MessageSendViewHolder -> holder.bind(context, message)
            is MessageReceivedViewHolder -> holder.bind(context, message)
            is ImageSendViewHolder -> holder.bind(context, message)
            is ImageReceivedViewHolder -> holder.bind(context, message)
            is LocationSendViewHolder -> holder.bind(context, message)
            is LocationReceivedViewHolder -> holder.bind(context, message)
            else -> throw IllegalArgumentException()
        }
    }

    //------------------------------------------
    object DateUtils {
        fun fromMillisToTimeString(millis: Long): String {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            return format.format(millis)
        }
    }

    //------------------------------------------
    class App: Application() {
        companion object {
            lateinit var userUUID: UUID
        }
    }
}

