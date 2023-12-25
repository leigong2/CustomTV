package com.decode

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer

object ScreenDecoder {
    private const val VIDEO_WIDTH = 2400
    private const val VIDEO_HEIGHT = 1080
    private const val SCREEN_FRAME_BIT = 2400 * 1080  // 比特率（比特/秒）
    private const val SCREEN_FRAME_RATE = 20  //帧率
    private const val SCREEN_FRAME_INTERVAL = 1  //I帧的频率
    private const val DECODE_TIME_OUT: Long = 10000 //超时时间
    private const val port = 50000   //端口
    private lateinit var mediaCodec: MediaCodec
    private lateinit var webSocketClient: WebSocketClient

    fun start(ip: String, surface: Surface) {
        webSocketClient = object : WebSocketClient(URI("ws://${ip}:${port}")) {
            override fun onOpen(handshakedata: ServerHandshake?) {}
            override fun onMessage(message: String?) {}
            override fun onMessage(bytes: ByteBuffer) {
                val buf = ByteArray(bytes.remaining())
                bytes[buf]
                decodeH264Data(buf)
            }
            override fun onClose(code: Int, reason: String?, remote: Boolean) {}
            override fun onError(ex: Exception?) {}
        }
        webSocketClient.connect()
        startH264MediaCodec(surface)
    }


    // 配置MediaCodec
    private fun startH264MediaCodec(surface: Surface) {
        mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, VIDEO_WIDTH, VIDEO_HEIGHT)
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, SCREEN_FRAME_BIT)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, SCREEN_FRAME_RATE)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, SCREEN_FRAME_INTERVAL)
        val headerSps = byteArrayOf(0, 0, 0, 1, 103, 66, -128, 31, -38, 1, 64, 22, -24, 6, -48, -95, 53)
        val headerPps = byteArrayOf(0, 0 ,0, 1, 104, -50, 6, -30)
        mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(headerSps));
        mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(headerPps));
        mediaCodec.configure(mediaFormat, surface, null, 0)
        mediaCodec.start()
    }


    // 配置MediaCodec
    private fun startH265MediaCodec(surface: Surface) {
        mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC)
        val mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, VIDEO_WIDTH, VIDEO_HEIGHT)
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, SCREEN_FRAME_BIT)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, SCREEN_FRAME_RATE)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, SCREEN_FRAME_INTERVAL)
        mediaCodec.configure(mediaFormat, surface, null, 0)
        mediaCodec.start()
    }

    /*zune: H264解码*/
    private fun decodeH264Data(data: ByteArray) {
        val inputBuffers: Array<ByteBuffer> = mediaCodec.getInputBuffers()
        val inputBufferIndex: Int = mediaCodec.dequeueInputBuffer(100)
        if (inputBufferIndex >= 0) {
            val inputBuffer = inputBuffers[inputBufferIndex]
            inputBuffer.clear()
            inputBuffer.put(data, 0, data.size)
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, data.size, System.currentTimeMillis(), 0)
        } else {
            return
        }
        val bufferInfo = MediaCodec.BufferInfo()
        var outputBufferIndex: Int = mediaCodec.dequeueOutputBuffer(bufferInfo, 100)
        while (outputBufferIndex >= 0) {
            mediaCodec.releaseOutputBuffer(outputBufferIndex, true)
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
        }
    }

    /*zune: H265解码*/
    private fun decodeH265Data(data: ByteArray) {
        val index = mediaCodec.dequeueInputBuffer(DECODE_TIME_OUT)
        if (index >= 0) {
            mediaCodec.getInputBuffer(index)?.also { inputBuffer ->
                inputBuffer.clear()
                inputBuffer.put(data, 0, data.size)
            }
            mediaCodec.queueInputBuffer(index, 0, data.size, System.currentTimeMillis(), 0)
        }
        val bufferInfo = MediaCodec.BufferInfo()
        var outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, DECODE_TIME_OUT)
        while (outputBufferIndex > 0) {
            mediaCodec.releaseOutputBuffer(outputBufferIndex, true)
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
        }
    }

    fun release() {
        webSocketClient.close()
        mediaCodec.release()
    }
}