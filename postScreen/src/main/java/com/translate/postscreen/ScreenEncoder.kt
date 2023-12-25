package com.translate.postscreen

import android.hardware.display.DisplayManager
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import java.io.IOException
import java.nio.ByteBuffer

/**
 * 采用H.265编码 H.265/HEVC的编码架构大致上和H.264/AVC的架构相似 H.265又称为HEVC(全称High Efficiency Video Coding，高效率视频编码)
 */
class ScreenEncoder(
    private val mSocketManager: SocketManager,
    private val mMediaProjection: MediaProjection
) : Thread() {
    private lateinit var mScreenCodec: MediaCodec
    private lateinit var mRecordCodec: MediaCodec
    private var mPlaying = true

    // 记录vps pps sps
    private lateinit var vps_pps_sps: ByteArray
    fun startScreenEncode() {
        initScreenCodec()
        start()
    }

    fun startRecordEncode() {
        initRecordCodec()
        start()
    }

    private fun initScreenCodec() {
        val mediaFormat = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            VIDEO_WIDTH,
            VIDEO_HEIGHT
        )
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
        // 比特率（比特/秒）
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_WIDTH * VIDEO_HEIGHT)
        // 帧率
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, SCREEN_FRAME_RATE)
        // I帧的频率
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, SCREEN_FRAME_INTERVAL)
        try {
            mScreenCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            mScreenCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val surface = mScreenCodec.createInputSurface()
            mMediaProjection.createVirtualDisplay(
                "ScreenRecord",
                VIDEO_WIDTH,
                VIDEO_HEIGHT,
                1,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                surface,
                null,
                null
            )
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

    override fun run() {
        runScreenCodec()
//        runRecordCodec()
    }

    private fun runScreenCodec() {
        mScreenCodec.start()
        val bufferInfo = MediaCodec.BufferInfo()
        while (mPlaying) {
            val outPutBufferId = mScreenCodec.dequeueOutputBuffer(bufferInfo, SOCKET_TIME_OUT)
            if (outPutBufferId >= 0) {
                val byteBuffer = mScreenCodec.getOutputBuffer(outPutBufferId)
                encodeData(byteBuffer, bufferInfo, true)
                mScreenCodec.releaseOutputBuffer(outPutBufferId, false)
            }
        }
    }

    private fun runRecordCodec() {
        mRecordCodec.start()
        val recordBufferInfo = MediaCodec.BufferInfo()
        while (mPlaying) {
            val outPutBufferId = mRecordCodec.dequeueOutputBuffer(recordBufferInfo, SOCKET_TIME_OUT)
            if (outPutBufferId >= 0) {
                val byteBuffer = mRecordCodec.getOutputBuffer(outPutBufferId)
                encodeData(byteBuffer, recordBufferInfo, false)
                mRecordCodec.releaseOutputBuffer(outPutBufferId, false)
            }
        }
    }

    private fun encodeData(byteBuffer: ByteBuffer?, bufferInfo: MediaCodec.BufferInfo, isVideo: Boolean) {
        var offSet = 4
        if (byteBuffer!![2].toInt() == 0x01) {
            offSet = 3
        }
        val type = byteBuffer[offSet].toInt() and 0x7E shr 1
        if (type == TYPE_FRAME_VPS) {
            vps_pps_sps = ByteArray(bufferInfo.size)
            byteBuffer[vps_pps_sps]
        } else if (type == TYPE_FRAME_INTERVAL) {
            val bytes = ByteArray(bufferInfo.size)
            byteBuffer[bytes]
            val newBytes = ByteArray(vps_pps_sps.size + bytes.size)
            System.arraycopy(vps_pps_sps, 0, newBytes, 0, vps_pps_sps.size)
            System.arraycopy(bytes, 0, newBytes, vps_pps_sps.size, bytes.size)
            mSocketManager.sendData(newBytes, isVideo)
        } else {
            val bytes = ByteArray(bufferInfo.size)
            byteBuffer[bytes]
            mSocketManager.sendData(bytes, isVideo)
        }
    }

    fun stopEncode() {
        mPlaying = false
        mScreenCodec.release()
        mMediaProjection.stop()
    }

    companion object {
        //不同手机支持的编码最大分辨率不同
        private const val VIDEO_WIDTH = 2400
        private const val VIDEO_HEIGHT = 1080
        private const val SCREEN_FRAME_RATE = 20
        private const val SCREEN_FRAME_INTERVAL = 1
        private const val SOCKET_TIME_OUT: Long = 10000
        // I帧
        private const val TYPE_FRAME_INTERVAL = 19
        // vps帧
        private const val TYPE_FRAME_VPS = 32
    }
}