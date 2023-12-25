package com.translate.postscreen

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface
import java.io.IOException

class ScreenDecoder {
    private lateinit var mScreenCodec: MediaCodec
    private lateinit var mRecordCodec: MediaCodec
    /*zune: 配置本地展示的画布*/
    fun startDecode(surface: Surface?) {
        try {
            initScreenCodec(surface)
//            initRecordCodec()
            mScreenCodec.start()
//            mRecordCodec.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun initRecordCodec() {
        val mediaFormat = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,  8000, 1)
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectMain)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 80)
        try {
            mRecordCodec = MediaCodec.createEncoderByType(mediaFormat.getString(MediaFormat.KEY_MIME).toString())
            mRecordCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun initScreenCodec(surface: Surface?) {
        // 配置MediaCodec
        mScreenCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val mediaFormat = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            VIDEO_WIDTH,
            VIDEO_HEIGHT
        )
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_WIDTH * VIDEO_HEIGHT)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, SCREEN_FRAME_RATE)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, SCREEN_FRAME_INTERVAL)
        mScreenCodec.configure(mediaFormat, surface, null, 0)
    }

    /*zune: 从远端拉到的数据，铺在画布上*/
    fun decodeData(data: ByteArray) {
        decodeScreenData(data)
//        decodeRecordData(data)
    }

    private fun decodeScreenData(data: ByteArray) {
        val index = mScreenCodec.dequeueInputBuffer(DECODE_TIME_OUT)
        if (index >= 0) {
            mScreenCodec.getInputBuffer(index)?.also { inputBuffer ->
                inputBuffer.clear()
                inputBuffer.put(data, 0, data.size)
            }
            mScreenCodec.queueInputBuffer(index, 0, data.size, System.currentTimeMillis(), 0)
        }
        val bufferInfo = MediaCodec.BufferInfo()
        var outputBufferIndex = mScreenCodec.dequeueOutputBuffer(bufferInfo, DECODE_TIME_OUT)
        while (outputBufferIndex > 0) {
            mScreenCodec.releaseOutputBuffer(outputBufferIndex, true)
            outputBufferIndex = mScreenCodec.dequeueOutputBuffer(bufferInfo, 0)
        }
    }

    private fun decodeRecordData(data: ByteArray) {
        val index = mRecordCodec.dequeueInputBuffer(DECODE_TIME_OUT)
        if (index >= 0) {
            mRecordCodec.getInputBuffer(index)?.also { inputBuffer ->
                inputBuffer.clear()
                inputBuffer.put(data, 0, data.size)
            }
            mRecordCodec.queueInputBuffer(index, 0, data.size, System.currentTimeMillis(), 0)
        }
        val bufferInfo = MediaCodec.BufferInfo()
        var outputBufferIndex = mRecordCodec.dequeueOutputBuffer(bufferInfo, DECODE_TIME_OUT)
        while (outputBufferIndex > 0) {
            mRecordCodec.releaseOutputBuffer(outputBufferIndex, true)
            outputBufferIndex = mRecordCodec.dequeueOutputBuffer(bufferInfo, 0)
        }
    }

    fun stopDecode() {
        mScreenCodec.release()
//        mRecordCodec.release()
    }

    companion object {
        private const val VIDEO_WIDTH = 1080
        private const val VIDEO_HEIGHT = 2400
        private const val DECODE_TIME_OUT: Long = 10000
        private const val SCREEN_FRAME_RATE = 20
        private const val SCREEN_FRAME_INTERVAL = 1
    }
}