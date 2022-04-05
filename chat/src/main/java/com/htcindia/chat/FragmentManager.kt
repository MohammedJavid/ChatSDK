package com.htcindia.chat

import androidx.appcompat.app.AppCompatActivity
import com.htcindia.chat.ui.chat.ChatFragment

class FragmentManager {
    companion object {
        /**
         * displays the fragment
         */
        fun showFragment(activity: AppCompatActivity,
                         accessToken: String,
                         identity: String,
                         roomName: String,
                         participantsList: List<String>,
                         pathProvider: String) {
            if (activity.supportFragmentManager.findFragmentById(android.R.id.content) == null) {
                activity.supportFragmentManager.beginTransaction()
                    .add(android.R.id.content, ChatFragment(
                        activity = activity,
                        accessToken = accessToken,
                        identity = identity,
                        roomName = roomName,
                        participantsList = participantsList,
                        pathProvider = pathProvider

                    ))
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
}