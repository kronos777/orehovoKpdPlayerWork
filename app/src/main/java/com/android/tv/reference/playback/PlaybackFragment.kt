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
package com.android.tv.reference.playback

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.text.Html
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.viewModels
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.navigation.fragment.findNavController
import com.android.tv.reference.R
import com.android.tv.reference.auth.NoFirebaseFragment
import com.android.tv.reference.browse.BrowseViewModel
import com.android.tv.reference.shared.datamodel.Video
import com.android.tv.reference.shared.datamodel.VideoType
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.gms.cast.tv.CastReceiverContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.time.Duration


/** Fragment that plays video content with ExoPlayer. */
class PlaybackFragment : VideoSupportFragment() {

    private lateinit var video: Video

    private var exoplayer: ExoPlayer? = null
    private val viewModel: PlaybackViewModel by viewModels()

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    private lateinit var myUrl: String

    var videoList = ArrayList<String>()

    lateinit var currentVideo: String
    lateinit var nextVideo: String

    private lateinit var viewModelBrowse: BrowseViewModel

    private val uiPlaybackStateListener = object : PlaybackStateListener {
        override fun onChanged(state: VideoPlaybackState) {
            // While a video is playing, the screen should stay on and the device should not go to
            // sleep. When in any other state such as if the user pauses the video, the app should
            // not prevent the device from going to sleep.
            view?.keepScreenOn = state is VideoPlaybackState.Play

            when (state) {
                is VideoPlaybackState.Prepare -> startPlaybackFromWatchProgress(state.startPosition)
                is VideoPlaybackState.End -> {
                    // To get to playback, the user always goes through browse first. Deep links for
                    // directly playing a video also go to browse before playback. If playback
                    // finishes the entire video, the PlaybackFragment is popped off the back stack
                    // and the user returns to browse.
                    //findNavController().popBackStack()
                    findNavController().navigate(R.id.playbackFragment, Bundle().apply {
                            putString(VIDEOURL, nextVideo)
                    })
                }
                is VideoPlaybackState.Error ->
                    findNavController().navigate(
                        PlaybackFragmentDirections
                            .actionPlaybackFragmentToPlaybackErrorFragment(
                                state.video,
                                state.exception
                            )
                    )
                else -> {
                    // Do nothing.
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val connection = hasConnection(getActivity()!!.applicationContext)
        if(!connection) {
          findNavController().navigate(R.id.noFirebaseFragment)
      }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*my video object*/
        val myVideo = Video(
            "https://atv-reference-app.firebaseapp.com/clips-supercharged/supercharged-flatmap",
            "KPD group",
            "videoItem In this mini series, Surma introduces you to the various functional methods that JavaScript Arrays have to offer. In this episode: map & filter!",
            "https://atv-reference-app.firebaseapp.com/clips-supercharged/supercharged-map-and-filter",
            "http://iziboro0.beget.tech/kummedia/pposad/kpd.mp4",
            "https://storage.googleapis.com/atv-reference-app-videos/clips-supercharged/supercharged-map-and-filter-thumbnail.png",
            "https://storage.googleapis.com/atv-reference-app-videos/clips-supercharged/supercharged-map-and-filter-background.jpg",
            "Supercharged Clips",
            VideoType.CLIP,
        )

        deleteCache(activity!!.applicationContext)
        //video uri
        //https://storage.googleapis.com/atv-reference-app-videos/clips-supercharged/supercharged-map-and-filter.mp4

        /*my video object*/
        // Get the video data.
       // video = PlaybackFragmentArgs.fromBundle(requireArguments()).video
       /* if (video == null) {
            video = myVideo
        }
*/
        /*
if (PlaybackFragmentArgs.fromBundle(requireArguments()).video == null) {
    parseParams()
}
        if (PlaybackFragmentArgs.fromBundle(requireArguments()).video != null) {
            video = PlaybackFragmentArgs.fromBundle(requireArguments()).video
        } else if(PlaybackFragmentArgs.fromBundle(requireArguments()).video == null) {
            parseParams()
        } else if(video == null) {
            video = myVideo
        }*/

        parseParams()
      /*  val connection = hasConnection(getActivity()!!.applicationContext)
        if(connection) {*/
        Timber.v("my url $myUrl")
            if(myUrl.length < 5) {
                video = myVideo
            } else {
                //Timber.v("my url2 $myUrl")
                video = Video(
                    "https://atv-reference-app.firebaseapp.com/clips-supercharged/supercharged-flatmap",
                    "KPD group",
                    "videoItem In this mini series, Surma introduces you to the various functional methods that JavaScript Arrays have to offer. In this episode: map & filter!",
                    "https://atv-reference-app.firebaseapp.com/clips-supercharged/supercharged-map-and-filter",
                    myUrl,
                    "https://storage.googleapis.com/atv-reference-app-videos/clips-supercharged/supercharged-map-and-filter-thumbnail.png",
                    "https://storage.googleapis.com/atv-reference-app-videos/clips-supercharged/supercharged-map-and-filter-background.jpg",
                    "Supercharged Clips",
                    VideoType.CLIP,
                )
            }
            //video = myVideo
            getWebsite()
            // Create the MediaSession that will be used throughout the lifecycle of this Fragment.
            createMediaSession()
       /* } else {
            findNavController().navigate(R.id.noFirebaseFragment)
        }*/



      /*  viewModelBrowse = ViewModelProvider(this).get(BrowseViewModel::class.java)
        viewModelBrowse.browseContent.observe(
            this,
            {
                for (item in it) {

                        Timber.v("videoItem ${item.videoList.get(videoList.size -1).id}")
                }

            }
        )
*/
        goStartFragmentBackPressed()
       // hideControlsOverlay(true)
       // isControlsOverlayAutoHideEnabled = false
    }

    private fun goStartFragmentBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().navigate(R.id.noFirebaseFragment)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.addPlaybackStateListener(uiPlaybackStateListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.removePlaybackStateListener(uiPlaybackStateListener)
    }

    override fun onStart() {
        super.onStart()
        hideControlsOverlay(false)
        //initializePlayer()
    }

    override fun onStop() {
        super.onStop()
        destroyPlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Releasing the mediaSession due to inactive playback and setting token for cast to null.
        mediaSession.release()
        CastReceiverContext.getInstance().mediaManager.setSessionCompatToken(null)
    }

    private fun initializePlayer() {
        val dataSourceFactory = DefaultDataSource.Factory(requireContext())
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory).
            createMediaSource(MediaItem.fromUri((video.videoUri)))
        exoplayer = ExoPlayer.Builder(requireContext()).build().apply {
            setMediaSource(mediaSource)
            prepare()
            addListener(PlayerEventListener())
            prepareGlue(this)
            mediaSessionConnector.setPlayer(object: ForwardingPlayer(this) {
                override fun stop() {
                    // Treat stop commands as pause, this keeps ExoPlayer, MediaSession, etc.
                    // in memory to allow for quickly resuming. This also maintains the playback
                    // position so that the user will resume from the current position when backing
                    // out and returning to this video
                    Timber.v("Playback stopped at $currentPosition")
                    // This both prevents playback from starting automatically and pauses it if
                    // it's already playing
                    playWhenReady = false
                }
            })

            mediaSession.isActive = true

        }

        viewModel.onStateChange(VideoPlaybackState.Load(video))
    }

    override fun showControlsOverlay(runAnimation: Boolean) {
        // We will do nothing here, and thus controls will never be shown
    }

    fun getNextVideo(urlString: String) {


        if(videoList.size > 1) {
            for (index in videoList.indices) {
                val cval = videoList.get(index).trim().dropLast(4)
                val tval = urlString.trim().dropLast(4)
                if(cval == tval) {
                    //Toast.makeText(activity, "???????????????????? ??????????????", Toast.LENGTH_SHORT).show()
                    // val gval =  videoList.get(index + 1)
                    // Timber.v("val next vit $tval and $cval next $gval")
                    if(index == videoList.size -1) {
                        nextVideo = videoList.get(0)
                        Timber.v("val next vit $nextVideo")
                    } else {
                        nextVideo = videoList.get(index + 1)
                    }
                }
            }
        } else if (videoList.size == 1) {
            nextVideo = videoList.get(0)
        }

        //url = videoList.indexOf(urlString).toString()

    }

    private fun destroyPlayer() {
        mediaSession.isActive = false
        mediaSessionConnector.setPlayer(null)
        exoplayer?.let {
            // Pause the player to notify listeners before it is released.
            it.pause()
            it.release()
            exoplayer = null
        }
    }

    private fun prepareGlue(localExoplayer: ExoPlayer) {
        ProgressTransportControlGlue(
            requireContext(),
            LeanbackPlayerAdapter(
                requireContext(),
                localExoplayer,
                PLAYER_UPDATE_INTERVAL_MILLIS.toInt()
        ),
            onProgressUpdate

        ).apply {
            host = VideoSupportFragmentGlueHost(this@PlaybackFragment)
          //  host.showControlsOverlay(false)
           // host.hideControlsOverlay(false)
           // host.isControlsOverlayAutoHideEnabled = false

           /* title = video.name*/
            // Enable seek manually since PlaybackTransportControlGlue.getSeekProvider() is null,
            // so that PlayerAdapter.seekTo(long) will be called during user seeking.
            // TODO(gargsahil@): Add a PlaybackSeekDataProvider to support video scrubbing.
            isSeekEnabled = true
        }
    }

    private fun createMediaSession() {
        mediaSession = MediaSessionCompat(requireContext(), MEDIA_SESSION_TAG)

        mediaSessionConnector = MediaSessionConnector(mediaSession).apply {
            setQueueNavigator(SingleVideoQueueNavigator(video, mediaSession))
        }
        CastReceiverContext.getInstance().mediaManager.setSessionCompatToken(
            mediaSession.sessionToken)
    }

    private fun startPlaybackFromWatchProgress(startPosition: Long) {
        Timber.v("Starting playback from $startPosition")
        exoplayer?.apply {
            seekTo(startPosition)
            playWhenReady = true
        }
    }

    private val onProgressUpdate: () -> Unit = {
        // TODO(benbaxter): Calculate when end credits are displaying and show the next episode for
        //  episodic content.
    }

    inner class PlayerEventListener : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
           // Timber.w(error, "Playback error")
            viewModel.onStateChange(VideoPlaybackState.Error(video, error))
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            when {
                isPlaying -> viewModel.onStateChange(
                    VideoPlaybackState.Play(video))
                exoplayer!!.playbackState == Player.STATE_ENDED -> viewModel.onStateChange(
                    VideoPlaybackState.End(video))
                else -> viewModel.onStateChange(
                    VideoPlaybackState.Pause(video, exoplayer!!.currentPosition))

            }
        }
    }

    private fun getWebsite() {

        Thread {
            val builder = StringBuilder()
            try {
                val doc: Document =
                    Jsoup.connect("http://iziboro0.beget.tech/kummedia/pposad/").get()
                val links: Elements = doc.select("li")

                val mExampleList = ArrayList<String>()
                for (link in links) {
                    mExampleList.add(Html.fromHtml(link.toString()).toString())

                }
                videoList = mExampleList

            } catch (e: IOException) {
                goToMainFragment()
                //builder.append("Error : ").append(e.message).append("\n")
               //  Log.d("errorsite", e.message.toString())
            }

            requireActivity().runOnUiThread {
                try {
                    //playVideo()
                    //full screen option

                    if (myUrl.length > 5) {
                        initializeTrashPlayer(myUrl)
                        currentVideo = myUrl
                        getNextVideo(currentVideo)
                    } else {
                        currentVideo = videoList.get(0)
                        getNextVideo(currentVideo)
                        initializeTrashPlayer(videoList.get(0))
                    }

                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    fun goToMainFragment() {
        fragmentManager?.beginTransaction()
            ?.replace(R.id.nav_host_fragment, NoFirebaseFragment())
            ?.addToBackStack(null)
            ?.commit()
    }

    private fun initializeTrashPlayer(urlVideo: String) {
        val dataSourceFactory = DefaultDataSource.Factory(requireContext())
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory).
        createMediaSource(MediaItem.fromUri((urlVideo)))

        exoplayer = ExoPlayer.Builder(requireContext()).build().apply {
            setMediaSource(mediaSource)
            prepare()
            addListener(PlayerEventListener())
            prepareGlue(this)

            mediaSessionConnector.setPlayer(object: ForwardingPlayer(this) {
                override fun stop() {
                    // Treat stop commands as pause, this keeps ExoPlayer, MediaSession, etc.
                    // in memory to allow for quickly resuming. This also maintains the playback
                    // position so that the user will resume from the current position when backing
                    // out and returning to this video
                   // Timber.v("Playback stopped at $currentPosition")
                    // This both prevents playback from starting automatically and pauses it if
                    // it's already playing
                    playWhenReady = false
                }
            })

            mediaSession.isActive = true
        }


        viewModel.onStateChange(VideoPlaybackState.Load(video))
    }


    fun deleteCache(context: Context) {
        try {
            val dir: File = context.cacheDir
            deleteDir(dir)
        } catch (e: Exception) {
        }
    }

    fun deleteDir(dir: File?): Boolean {
        return if (dir != null && dir.isDirectory()) {
            val children: Array<String> = dir.list()
            for (i in children.indices) {
                val success = deleteDir(File(dir, children[i]))
                if (!success) {
                    return false
                }
            }
            dir.delete()
        } else if (dir != null && dir.isFile()) {
            dir.delete()
        } else {
            false
        }
    }

    private fun parseParams() {
        val args = requireArguments()
        /*if (!args.containsKey(VIDEO.toString())) {
            throw RuntimeException("Param video is not exists")
        }*/
       myUrl = args.getString(VIDEOURL).toString()
        /*if (mode != MODE_EDIT && mode != MODE_ADD) {
            throw RuntimeException("Unknown screen mode $mode")
        }
        screenMode = mode
        if (screenMode == MODE_EDIT) {
            if (!args.containsKey(PAYMENT_ITEM_ID)) {
                throw RuntimeException("Param shop item id is absent")
            }
            paymentItemId = args.getInt(PAYMENT_ITEM_ID, PaymentItem.UNDEFINED_ID)
        }*/
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


    companion object {
        // Update the player UI fairly often. The frequency of updates affects several UI components
        // such as the smoothness of the progress bar and time stamp labels updating. This value can
        // be tweaked for better performance.
        private val PLAYER_UPDATE_INTERVAL_MILLIS = Duration.ofMillis(5000).toMillis()

        // A short name to identify the media session when debugging.
        private const val MEDIA_SESSION_TAG = "ReferenceAppKotlin"

        const val VIDEOURL : String = ""

    }







}
