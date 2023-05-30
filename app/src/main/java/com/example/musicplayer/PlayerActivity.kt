package com.example.musicplayer

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.databinding.ActivityPlayerBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PlayerActivity : AppCompatActivity() ,ServiceConnection, MediaPlayer.OnCompletionListener{

    companion object {
        lateinit var musicList: ArrayList<Music>
        var songPosition: Int = 0
        var isPlaying: Boolean = false
        var musicService: MusicService? = null

        @SuppressLint("StaticFieldLeak")
        lateinit var binding: ActivityPlayerBinding
        var repeat:Boolean=false
        var min_15:Boolean=false
        var min_30:Boolean=false
        var min_60:Boolean=false
        var nowPlayingID:String=""
        var isFavourite:Boolean=false
        var fIndex:Int =-1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.coolPink)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //For starting service
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, this, BIND_AUTO_CREATE)
        startService(intent)
        initialiseLayout()
        binding.backBtnPA.setOnClickListener{
            finish()
        }
        binding.playPause.setOnClickListener {
            if (isPlaying)
                pauseMusic()
            else
                playMusic()
        }
        binding.previousBtn.setOnClickListener { prevSong(increment = false) }
        binding.nextBtn.setOnClickListener { prevSong(increment = true) }

        binding.seekbarPA.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser) musicService!!.mediaPlayer!!.seekTo(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?)=Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) =Unit
        })
        binding.repeatBtnPA.setOnClickListener{
            if(!repeat){
            repeat=true
            binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this,R.color.purple_500))
        }
            else {
                repeat = false
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_pink))

            }   }
        binding.equalizerBtnPA.setOnClickListener {
            try {
                val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                eqIntent.putExtra(
                    AudioEffect.EXTRA_AUDIO_SESSION,
                    musicService!!.mediaPlayer!!.audioSessionId
                )
                eqIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, baseContext.packageName)
                // package name is used to tell that for which app we want to change the audio
                eqIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                startActivityForResult(eqIntent, 13)
            }catch (e:java.lang.Exception){
                Toast.makeText(this,"Equalizer Feature Not Supported!!",Toast.LENGTH_SHORT).show()
            }
        }
        binding.timerBtnPA.setOnClickListener {
            val timer= min_15|| min_30|| min_60
            if(!timer)
            showBottomSheetDialog()
            else{
                val builder= MaterialAlertDialogBuilder(this)
                builder.setTitle("Stop Timer")
                    .setMessage("Do You Want To Stop Timer?")
                    .setPositiveButton("Yes"){ /*dialog */_, /* its result*/ _ ->
                        min_15=false
                        min_30=false
                        min_60=false
                        binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this,R.color.cool_pink) )
                    }
                    .setNegativeButton("No"){dialog, _ ->
                        dialog.dismiss()
                    }
                val customDialog=builder.create()
                customDialog.show()
                customDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
                customDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED)

            }
        }
        binding.sharBtnPA.setOnClickListener {
            val shareIntent=Intent()
                shareIntent.action=Intent.ACTION_SEND
                shareIntent.type="audio/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(musicList[songPosition].path))
            startActivity(Intent.createChooser(shareIntent,"Sharing Music File!!"))
        }
        binding.favouriteBtnPA.setOnClickListener {
            if(isFavourite){
                isFavourite=false
                binding.favouriteBtnPA.setImageResource((R.drawable.favourite_empty_icon))
                FavouriteActivity.favouriteSongs.removeAt(fIndex)
            }
            else{

                    isFavourite=true
                    binding.favouriteBtnPA.setImageResource((R.drawable.favourite_empty_icon))
                    FavouriteActivity.favouriteSongs.add(musicList[songPosition])
            }
        }
    }

    private fun setLayout() {
        fIndex= favouriteChecker(musicList[songPosition].id)
        Glide.with(this)
            .load(musicList[songPosition].artUri)
            .apply(
                RequestOptions().placeholder(R.drawable.muisc_player_icon_splash_screen)
                    .centerCrop()
            )
            .into(binding.songImg)
        binding.songName.text = musicList[songPosition].title
        if (repeat)
            binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.purple_500))
        if(min_15|| min_30|| min_60)
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this,R.color.purple_500))
        if(isFavourite) binding.favouriteBtnPA.setImageResource(R.drawable.favourite_icon)
        else
            binding.favouriteBtnPA.setImageResource(R.drawable.favourite_empty_icon)
    }

    private fun createMediaPlayer() {
        try {
            if (musicService!!.mediaPlayer == null) musicService!!.mediaPlayer = MediaPlayer()
            musicService!!.mediaPlayer!!.reset()
            musicService!!.mediaPlayer!!.setDataSource(musicList[songPosition].path)
            musicService!!.mediaPlayer!!.prepare()
            musicService!!.mediaPlayer!!.start()
            isPlaying = true
            binding.playPause.setIconResource(R.drawable.pause_icon)
            musicService!!.showNotification(R.drawable.pause_icon)
            binding.tvSeekbarStart.text= formatSDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
            binding.tvSeekbarEnd.text= formatSDuration(musicService!!.mediaPlayer!!.duration.toLong())
            binding.seekbarPA.progress=0
            binding.seekbarPA.max= musicService!!.mediaPlayer!!.duration
            musicService!!.mediaPlayer!!.setOnCompletionListener (this)
            nowPlayingID= musicList[songPosition].id
        } catch (e: java.lang.Exception) {
            return
        }
    }

    private fun initialiseLayout() {
        songPosition = intent.getIntExtra("index", 0)
        when (intent.getStringExtra("class")) {
            "NowPlaying"->{
                setLayout()
                binding.tvSeekbarStart.text= formatSDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
                binding.tvSeekbarEnd.text= formatSDuration(musicService!!.mediaPlayer!!.duration.toLong())
                binding.seekbarPA.progress= musicService!!.mediaPlayer!!.currentPosition
                binding.seekbarPA.max= musicService!!.mediaPlayer!!.duration
                if(isPlaying) binding.playPause.setIconResource(R.drawable.pause_icon)
                else
                    binding.playPause.setIconResource(R.drawable.play_icon)
            }
            "MusicAdapterSearch"->{
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musicList= ArrayList()
                musicList.addAll(MainActivity.musicListSearch)
                setLayout()
            }
            "MusicAdapter" -> {
                //For starting service
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musicList = ArrayList()
                musicList.addAll(MainActivity.MusicListNA)
                setLayout()
                createMediaPlayer()
            }
            "MainActivity" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musicList = ArrayList()
                musicList.addAll(MainActivity.MusicListNA)
                musicList.shuffle()
                setLayout()

            }
        }
    }

    private fun playMusic() {
        binding.playPause.setIconResource((R.drawable.pause_icon))
        musicService!!.showNotification(R.drawable.pause_icon)
        isPlaying = true
        musicService!!.mediaPlayer!!.start()
    }

    private fun pauseMusic() {
        binding.playPause.setIconResource((R.drawable.play_icon))
        musicService!!.showNotification(R.drawable.play_icon)
        isPlaying = false
        musicService!!.mediaPlayer!!.pause()
    }

    private fun prevSong(increment: Boolean) {
        if (increment) {
            setSongPosition(increment = true)
            setLayout()
            createMediaPlayer()
        } else {
            setSongPosition(increment = false)
            setLayout()
            createMediaPlayer()
        }

    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder=service as MusicService.MyBinder
        musicService=binder.currentService()
        createMediaPlayer()
        musicService!!.seekbarSetup()

    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicService=null
    }

    override fun onCompletion(mp: MediaPlayer?) {
        setSongPosition(increment = true)
        createMediaPlayer()
        try{
            setLayout()
        }catch (e:java.lang.Exception){return}
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode==13||resultCode== RESULT_OK)
            return
    }
    @SuppressLint("SuspiciousIndentation")
    private fun showBottomSheetDialog(){
         var dialog=BottomSheetDialog(this@PlayerActivity)
        dialog.setContentView(R.layout.bottom_sheet_dialog)
        dialog.show()
        dialog.findViewById<LinearLayout>(R.id.min_15)?.setOnClickListener{
            Toast.makeText(baseContext,"Music will stop after 15 minutes",Toast.LENGTH_SHORT).show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this,R.color.purple_500))
            min_15=true
            Thread{Thread.sleep((15*60000).toLong())
            if(min_15)
            exitApplication()
            }.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_30)?.setOnClickListener{
            Toast.makeText(baseContext,"Music will stop after 30 minutes",Toast.LENGTH_SHORT).show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this,R.color.purple_500))
            min_30=true
            Thread{Thread.sleep((30*60000).toLong())
                if(min_30)
                    exitApplication()
            }.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_60)?.setOnClickListener{
            Toast.makeText(baseContext,"Music will stop after 60 minutes",Toast.LENGTH_SHORT).show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this,R.color.purple_500))
            min_60=true
            Thread{Thread.sleep((60*60000).toLong())
                if(min_60)
                    exitApplication()
            }.start()
            dialog.dismiss()
        }
    }
}

