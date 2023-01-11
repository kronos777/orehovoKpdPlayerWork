/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tv.reference.auth

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.tv.reference.R
import com.android.tv.reference.databinding.FragmentNoFirebaseBinding
import com.android.tv.reference.playback.PlaybackFragment
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import timber.log.Timber
import java.io.IOException
import java.lang.Thread.sleep
import java.util.*


/**
 * Simple Fragment that displays some info about configuring Firebase and has a continue button
 */
class NoFirebaseFragment : Fragment() {

    var mTimer: Timer? = null
    var videoList = ArrayList<String>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNoFirebaseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Toast.makeText(activity, "we are here 1", Toast.LENGTH_SHORT).show()
        val connection = hasConnection(getActivity()!!.applicationContext)
        if(connection) {
            view.findViewById<TextView>(R.id.title).setText(R.string.no_firebase_title)
            view.findViewById<TextView>(R.id.body).setText(R.string.no_firebase_body)
            sleep(2000)
             getWebsite()
        } else {
            view.findViewById<TextView>(R.id.title).setText(R.string.no_internet_title)
            view.findViewById<TextView>(R.id.body).setText(R.string.no_internet_body)
            mTimer = Timer()
            startAlarm()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    private fun getWebsite() {

        Thread {
            val builder = StringBuilder()
            try {
                val doc: Document =
                    Jsoup.connect("http://iziboro0.beget.tech/kummedia/orehovo").get()
                val links: Elements = doc.select("li")

                val mExampleList = ArrayList<String>()
                for (link in links) {
                    mExampleList.add(Html.fromHtml(link.toString()).toString())

                }
                videoList = mExampleList

            } catch (e: IOException) {
                builder.append("Error : ").append(e.message).append("\n")
               // Log.d("errorsite", e.message.toString())
            }

            requireActivity().runOnUiThread {
                try {
                    sleep(5000)
                    findNavController().navigate(R.id.playbackFragment, Bundle().apply {
                        putString(PlaybackFragment.VIDEOURL, videoList.get(0))
                    })
                   //initializeTrashPlayer(videoList.get(0))

                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }.start()
    }


    private fun hasConnection(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        if (wifiInfo != null && wifiInfo.isConnected) {
            return true
        }
        wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
        if (wifiInfo != null && wifiInfo.isConnected) {
            return true
        }
        wifiInfo = cm.activeNetworkInfo
        return wifiInfo != null && wifiInfo.isConnected
    }

    private fun startAlarm() {
        mTimer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val hasConn = hasConnection(getActivity()!!.applicationContext)
                //Toast.makeText(activity, "запустился метод", Toast.LENGTH_SHORT).show()
                //Timber.v("метод запустился")
                //Тут отработка метода в отдельном потоке;
                activity!!.runOnUiThread(
                    Runnable {
                        //Тут выход в UI если нужно;
                        if (hasConn){
                            cancelTimer()
                            getWebsite()
                        }
                    })
            }
        }, 0 // Это задержка старта, сейчас 0;
            , 6000) // Это Ваш период в 10 минут;
            //, 600000) // Это Ваш период в 10 минут;
    }

    private fun cancelTimer() {
        if (mTimer != null) {
            mTimer!!.cancel()
            mTimer = null
           // Timber.v("метод отключился")
        }
    }

}
