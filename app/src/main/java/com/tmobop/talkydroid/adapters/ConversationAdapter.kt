package com.tmobop.talkydroid.adapters

import android.app.Application
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tmobop.talkydroid.R
import com.tmobop.talkydroid.classes.MessageUI
import com.tmobop.talkydroid.classes.MessageUI.Companion.TYPE_MESSAGE_RECEIVED
import com.tmobop.talkydroid.classes.MessageUI.Companion.TYPE_MESSAGE_SEND
import java.text.SimpleDateFormat
import java.util.*

class ConversationAdapter(context: Context) : RecyclerView.Adapter<MessageViewHolder>() {

    private val messages: ArrayList<MessageUI> = ArrayList()

    //------------------------------------------
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {

        val context = parent.context

        return when (viewType) {
            TYPE_MESSAGE_SEND -> {
                val view = LayoutInflater.from(context).inflate(R.layout.bubble_msg_send, parent, false)
                MessageSendViewHolder(view)
            }
            TYPE_MESSAGE_RECEIVED -> {
                val view = LayoutInflater.from(context).inflate(R.layout.bubble_msg_received, parent, false)
                MessageReceivedViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    //------------------------------------------
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {

        val item = messages[position]

        when (holder) {
            is MessageSendViewHolder -> holder.bind(item)
            is MessageReceivedViewHolder -> holder.bind(item)
            else -> throw IllegalArgumentException()
        }
    }

    //------------------------------------------
    override fun getItemCount(): Int = messages.size

    //------------------------------------------
    override fun getItemViewType(position: Int): Int {
        val message = messages[position]

        return if(App.user == message.user) {
            TYPE_MESSAGE_SEND
        }
        else {
            TYPE_MESSAGE_RECEIVED
        }
    }

    //------------------------------------------
    class MessageSendViewHolder(view: View) : MessageViewHolder(view) {

        private val messageContent = view.findViewById<TextView>(R.id.message_send)
        private val messageTime = view.findViewById<TextView>(R.id.time_message_send)


        override fun bind(message: MessageUI) {
            messageContent.text = message.content
            messageTime.text = DateUtils.fromMillisToTimeString(message.time)
        }
    }

    //------------------------------------------
    class MessageReceivedViewHolder(view: View) : MessageViewHolder(view) {

        private val messageContent = view.findViewById<TextView>(R.id.message_received)
        private val messageTime = view.findViewById<TextView>(R.id.time_message_received)
        private val messageUser = view.findViewById<TextView>(R.id.user_message_received)

        override fun bind(message: MessageUI) {
            messageContent.text = message.content
            messageTime.text = DateUtils.fromMillisToTimeString(message.time)
            messageUser.text = message.user
        }
    }

    //------------------------------------------
    fun addMessage(message: MessageUI){
        messages.add(message)
        notifyDataSetChanged()
    }

    //------------------------------------------
    object DateUtils {
        fun fromMillisToTimeString(millis: Long) : String {
            val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return format.format(millis)
        }
    }
}

//------------------------------------------
open class MessageViewHolder (view: View) : RecyclerView.ViewHolder(view) {
    open fun bind(message:MessageUI) {}
}

//------------------------------------------
class App: Application() {
    companion object {
        lateinit var user:String
    }
}