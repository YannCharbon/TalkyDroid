package com.tmobop.talkydroid.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tmobop.talkydroid.R
import com.tmobop.talkydroid.classes.ConversationUI
import java.text.SimpleDateFormat
import java.util.*

class ConversationListAdapter(context: Context, var listener: OnItemClickListener) : RecyclerView.Adapter<ConversationListAdapter.ConversationViewHolder>() {

    private val conversationList: ArrayList<ConversationUI> = ArrayList()

    //------------------------------------------
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.conversation_selector_unit, parent, false)
        return ConversationViewHolder(view)
    }

    //------------------------------------------
    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val item = conversationList[position]
        holder.bind(item, listener)
    }

    //------------------------------------------
    override fun getItemCount(): Int = conversationList.size

    inner class ConversationViewHolder (view: View) : RecyclerView.ViewHolder(view) {

        private val conversationTitle = view.findViewById<TextView>(R.id.conversation_title)
        private val conversationTime = view.findViewById<TextView>(R.id.conversation_time)
        private val conversationAvatar = view.findViewById<ImageView>(R.id.conversation_avatar)
        private val conversationDescription = view.findViewById<TextView>(R.id.conversation_description)

        fun bind(conversation: ConversationUI, action: OnItemClickListener) {
            // bind texts
            conversationTitle.text = conversation.title
            conversationTime.text = DateUtils.fromMillisToTimeString(conversation.time)
            conversationDescription.text = conversation.description
            // TO DO --> Add conversationAvatar

            // on click listener
            itemView.setOnClickListener{
                action.onItemClick(conversation, adapterPosition)
            }
        }
    }

    //------------------------------------------
    fun addConversation(conversation: ConversationUI){
        conversationList.add(conversation)
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
interface OnItemClickListener {
    fun onItemClick(item : ConversationUI, position: Int)
}