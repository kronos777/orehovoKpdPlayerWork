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

import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.tv.reference.R
import com.android.tv.reference.databinding.FragmentNoFirebaseBinding
import com.android.tv.reference.playback.PlaybackFragment
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.io.IOException
import java.lang.Thread.sleep
import java.util.ArrayList

/**
 * Simple Fragment that displays some info about configuring Firebase and has a continue button
 */
class NoFirebaseFragment : Fragment() {


    var videoList = ArrayList<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNoFirebaseBinding.inflate(inflater, container, false)
       /* binding.continueButton.setOnClickListener {
            findNavController()
                .navigate(NoFirebaseFragmentDirections.actionNoFirebaseFragmentToBrowseFragment())
        }*/
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getWebsite()
        //Toast.makeText(activity, "we are here 1", Toast.LENGTH_SHORT).show()
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
                Log.d("errorsite", e.message.toString())
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

}
