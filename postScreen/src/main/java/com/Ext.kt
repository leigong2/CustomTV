package com

import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import java.io.File
import java.io.FileInputStream

object Ext {
    fun MediaPlayer.bindMediaToTexture(textureView: TextureView, callBack: (() -> Unit)? = null) {
        textureView.surfaceTextureListener = object : SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                setSurface(Surface(surface))
                setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
                callBack?.invoke()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            }
        }
    }

    fun MediaPlayer.prepareMediaPlayer(path: String) {
        reset()
        seekTo(0)
        try {
            setDataSource(FileInputStream(File(path)).fd);
            prepare()
        } catch (ignore: Exception) {
        }
    }
}