package com.example.musicplayer

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.*
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat

class MusicService: Service() {
    private var myBinder=MyBinder()
    var mediaPlayer:MediaPlayer?=null
    private lateinit var mediaSession:MediaSessionCompat
    private lateinit var runnable: Runnable
    override fun onBind(intent: Intent?): IBinder {
        mediaSession= MediaSessionCompat(baseContext,"My Music")
        return myBinder
    }
    inner class MyBinder: Binder(){
        fun currentService(): MusicService {
            return this@MusicService
        }
    }
    fun showNotification(playPauseBtn:Int){
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val prevIntent=Intent(baseContext,NotificationReceiver::class.java).setAction(ApplicationClass.PREVIOUS)
        val prevPendingIntent=PendingIntent.getBroadcast(baseContext,0,prevIntent,flag)
        val playIntent=Intent(baseContext,NotificationReceiver::class.java).setAction(ApplicationClass.PLAY)
        val playPendingIntent=PendingIntent.getBroadcast(baseContext,0,playIntent,flag)
        val nextIntent=Intent(baseContext,NotificationReceiver::class.java).setAction(ApplicationClass.NEXT)
        val nextPendingIntent=PendingIntent.getBroadcast(baseContext,0,nextIntent,flag)
        val exitIntent=Intent(baseContext,NotificationReceiver::class.java).setAction(ApplicationClass.EXIT)
        val exitPendingIntent=PendingIntent.getBroadcast(baseContext,0,exitIntent,flag)

        val imgArt= getImageArt(PlayerActivity.musicList[PlayerActivity.songPosition].path)
        val image=if(imgArt!=null){
            BitmapFactory.decodeByteArray(imgArt,0,imgArt.size)
        }
        else{
            BitmapFactory.decodeResource(resources,R.drawable.muisc_player_icon_splash_screen)
        }
        val notification=NotificationCompat.Builder(baseContext,ApplicationClass.CHANNEL_ID)

            .setContentTitle(PlayerActivity.musicList[PlayerActivity.songPosition].title)
            .setContentText(PlayerActivity.musicList[PlayerActivity.songPosition].artist)
            .setSmallIcon(R.drawable.music_icon)
            .setLargeIcon(image )
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionToken))
            .setPriority((NotificationCompat.PRIORITY_HIGH))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .addAction(R.drawable.previous_icon,"previous",prevPendingIntent)
            .addAction(playPauseBtn,"play",playPendingIntent)
            .addAction(R.drawable.next_icon,"next",nextPendingIntent)
            .addAction(R.drawable.exit_icon,"exit",exitPendingIntent)
            .build()
        startForeground(13,notification)

    }
     fun createMediaPlayer() {
         try {
             if (PlayerActivity.musicService!!.mediaPlayer == null) PlayerActivity.musicService!!.mediaPlayer =
                 MediaPlayer()
             PlayerActivity.musicService!!.mediaPlayer!!.reset()
             PlayerActivity.musicService!!.mediaPlayer!!.setDataSource(PlayerActivity.musicList[PlayerActivity.songPosition].path)
             PlayerActivity.musicService!!.mediaPlayer!!.prepare()

             PlayerActivity.binding.playPause.setIconResource(R.drawable.pause_icon)
             PlayerActivity.musicService!!.showNotification(R.drawable.pause_icon)
             PlayerActivity.binding.tvSeekbarStart.text= formatSDuration(mediaPlayer!!.currentPosition.toLong())
             PlayerActivity.binding.tvSeekbarEnd.text= formatSDuration(mediaPlayer!!.duration.toLong())
             PlayerActivity.binding.seekbarPA.progress=0
             PlayerActivity.binding.seekbarPA.max= mediaPlayer!!.duration
             PlayerActivity.nowPlayingID = PlayerActivity.musicList[PlayerActivity.songPosition].id
         } catch (e: java.lang.Exception) {
             return
         }
     }
    fun seekbarSetup() {
        runnable = Runnable {
            PlayerActivity.binding.tvSeekbarStart.text =
                formatSDuration(mediaPlayer!!.currentPosition.toLong()
                )

            PlayerActivity.binding.seekbarPA.progress=mediaPlayer!!.currentPosition
            //handler tells that after how long the code should run
            Handler(Looper.getMainLooper()).postDelayed(runnable, 200)
        }
            //it ensures that after how long the inner handler should start
            Handler(Looper.getMainLooper()).postDelayed(runnable,0)
    }
}