package com.example.musicplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

class NotificationReceiver:BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when(intent?.action){
            ApplicationClass.PREVIOUS->prevNextSong(increment = false,context=context!!)
            ApplicationClass.PLAY->if(PlayerActivity.isPlaying) pauseMusic() else playMusic()
            ApplicationClass.NEXT->prevNextSong(increment = true,context=context!!)
            ApplicationClass.EXIT->{
              exitApplication()
            }
        }
    }
    private fun playMusic(){
        PlayerActivity.isPlaying=true
        PlayerActivity.musicService!!.mediaPlayer!!.start()
        PlayerActivity.musicService!!.showNotification(R.drawable.pause_icon)
        PlayerActivity.binding.playPause.setIconResource(R.drawable.pause_icon)
        NowPlaying.binding.playPauseBtnNP.setIconResource(R.drawable.pause_icon)
    }
    private fun pauseMusic(){
        PlayerActivity.isPlaying=false
        PlayerActivity.musicService!!.mediaPlayer!!.pause()
        PlayerActivity.musicService!!.showNotification(R.drawable.play_icon)
        PlayerActivity.binding.playPause.setIconResource(R.drawable.play_icon)
        NowPlaying.binding.playPauseBtnNP.setIconResource(R.drawable.play_icon)
    }
    private fun prevNextSong(increment:Boolean,context:Context/*helps in setting glide image*/){
        setSongPosition(increment=increment)
        PlayerActivity.musicService!!.createMediaPlayer()
        Glide.with(context)
            .load(PlayerActivity.musicList[PlayerActivity.songPosition].artUri)
            .apply(
                RequestOptions().placeholder(R.drawable.muisc_player_icon_splash_screen)
                    .centerCrop()
            )
            .into(PlayerActivity.binding.songImg)
        PlayerActivity.binding.songName.text = PlayerActivity.musicList[PlayerActivity.songPosition].title
        Glide.with(context)
            .load(PlayerActivity.musicList[PlayerActivity.songPosition].artUri)
            .apply(
                RequestOptions().placeholder(R.drawable.muisc_player_icon_splash_screen)
                    .centerCrop()
            )
            .into(NowPlaying.binding.songImgNP)
        NowPlaying.binding.songNameNP.text = PlayerActivity.musicList[PlayerActivity.songPosition].title
        playMusic()
    }
}