package com.tmobop.talkydroid.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.tmobop.talkydroid.R
import com.tmobop.talkydroid.classes.CircleTransformation
import com.tmobop.talkydroid.classes.MessageType
import com.tmobop.talkydroid.storage.UserWithMessages
import java.text.SimpleDateFormat
import java.util.*


class ConversationListAdapter(
    private val context: Context,
    private val conversationList: MutableList<UserWithMessages>
) : RecyclerView.Adapter<ConversationListAdapter.ConversationViewHolder>() {

    private var listener: ((UserWithMessages) -> Unit)? = null

    //------------------------------------------
    fun addConversation(user: UserWithMessages) {
        this.conversationList.add(0, user)
        notifyDataSetChanged()
    }

    //------------------------------------------
    fun swapConversations(users: List<UserWithMessages>) {
        this.conversationList.clear()
        this.conversationList.addAll(users)
        notifyDataSetChanged()
    }

    //------------------------------------------
    fun deleteAllConversations(){
        conversationList.clear()
        notifyDataSetChanged()
    }

    //------------------------------------------
    fun deleteConversation(user: UserWithMessages) {
        this.conversationList.remove(user)
        notifyDataSetChanged()
    }

    //------------------------------------------
    fun setOnConversationClickListener(listener: ((UserWithMessages) -> Unit)) {
        this.listener = listener
    }

    //------------------------------------------
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {

        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.conversation_selector_unit,
            parent,
            false
        )

        return ConversationViewHolder(view)
    }

    //------------------------------------------
    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {

        // Set title
        holder.textViewConversationName.text = conversationList[position].userEntity!!.userName

        // TODO -> Change with the userAvatar
        if (conversationList[position].userEntity?.avatar == "") {
            holder.imageViewConversationAvatar.setImageResource(R.drawable.ic_baseline_unknown_user)
        }
        else {
            val avatarUri = Uri.parse(conversationList[position].userEntity!!.avatar)

            // Round image
            Picasso.get()
                .load(avatarUri)
                .error(R.drawable.ic_baseline_unknown_user)
                .placeholder(R.drawable.ic_baseline_unknown_user)
                .transform(CircleTransformation())
                .into(holder.imageViewConversationAvatar)
        }

        // Check if there is messages in the conversation
        if (conversationList[position].userMessageEntities!!.isNotEmpty()) {

            val lastMessage = conversationList[position].userMessageEntities!!.last()

            when(lastMessage.messageType) {
                MessageType.LOCATION -> {
                    val cords = conversationList[position].userMessageEntities!!.last().content.split(',')
                    val latitude = cords[0]
                    val longitude = cords[1]
                    holder.textViewConversationDescription.text = "Latitude : $latitude, Longitude : $longitude"
                }
                MessageType.TEXT -> {
                // Set the description with the last message
                holder.textViewConversationDescription.text =
                    conversationList[position].userMessageEntities!!.last().content
                }
            }

            // Set the time with the last message time
            holder.textViewConversationTime.text =
                DateUtils.fromMillisToTimeString(conversationList[position].userMessageEntities!!.last().time)
        }

        // If no message
        else {
            // Set the description with a message
            holder.textViewConversationDescription.text = "No message to display..."

            // Set the time with the actual time
            holder.textViewConversationTime.text =
                DateUtils.fromMillisToTimeString(Calendar.getInstance().timeInMillis)

        }

        // Check if user is online
        if (conversationList[position].userEntity!!.online) {
            // Make background white
            holder.constraintLayoutConversation.setBackgroundColor(Color.WHITE)
            holder.imageViewConversationAvatar.alpha = 1F // Transparency
            holder.imageViewOnlineSymbol.setImageResource(R.drawable.ic_baseline_online)
        }
        else {
            // Make background gray
            holder.constraintLayoutConversation.setBackgroundColor(Color.parseColor("#FFADADAD"))
            holder.imageViewConversationAvatar.alpha = 0.5F // Transparency
            holder.imageViewOnlineSymbol.setImageResource(R.drawable.ic_baseline_offline)
        }

        // Activate on click listener
        holder.itemView.setOnClickListener {
            listener?.invoke(conversationList[position])
        }
    }

    //------------------------------------------
    override fun getItemCount(): Int = conversationList.size

    //------------------------------------------
    inner class ConversationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewConversationName = view.findViewById<TextView>(R.id.conversation_title)!!
        val textViewConversationTime = view.findViewById<TextView>(R.id.conversation_time)!!
        val imageViewConversationAvatar = view.findViewById<ImageView>(R.id.conversation_avatar)!!
        val textViewConversationDescription = view.findViewById<TextView>(R.id.conversation_description)!!
        val constraintLayoutConversation = view.findViewById<ConstraintLayout>(R.id.conversation_selector_constraint_layout)!!
        val imageViewOnlineSymbol = view.findViewById<ImageView>(R.id.conversation_online_symbole)!!
    }

    //------------------------------------------
    object DateUtils {
        fun fromMillisToTimeString(millis: Long) : String {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            return format.format(millis)
        }
    }
}