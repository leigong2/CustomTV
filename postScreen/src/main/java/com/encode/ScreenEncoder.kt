package com.encode

import android.hardware.display.DisplayManager
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import kotlin.experimental.and

object ScreenEncoder {
    //不同手机支持的编码最大分辨率不同
    private const val VIDEO_WIDTH = 2400
    private const val VIDEO_HEIGHT = 1080
    private const val SCREEN_FRAME_BIT = 2400 * 1080  // 比特率（比特/秒）
    private const val SCREEN_FRAME_RATE = 20  //帧率
    private const val SCREEN_FRAME_INTERVAL = 1  //I帧的频率
    private const val ENCODE_TIME_OUT: Long = 10000 //超时时间
    private const val TYPE_FRAME_INTERVAL = 19 // I帧
    private const val TYPE_FRAME_VPS = 32// vps帧

    private const val NAL_SLICE:Byte = 1
    private const val NAL_SLICE_IDR:Byte = 5
    private const val NAL_SPS:Byte = 7
    private lateinit var sps_pps_buf: ByteArray

    private const val port = 50000   //端口
    private lateinit var mediaProjection: MediaProjection
    private lateinit var mediaCodec: MediaCodec
    private lateinit var webSocketServer: WebSocketServer
    private lateinit var webSocket: WebSocket
    private lateinit var h265_vps_pps_sps: ByteArray  // 记录vps pps sps
    private var isPlaying = true

    fun start(mediaProjection: MediaProjection) {
        this.mediaProjection = mediaProjection
        if (!this::webSocketServer.isInitialized) {
            webSocketServer = object : WebSocketServer(InetSocketAddress(port)) {
                override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
                    Log.d("ScreenEncoder", "onOpen")
                    conn?.apply { webSocket = this }
                }

                override fun onClose(
                    conn: WebSocket?,
                    code: Int,
                    reason: String?,
                    remote: Boolean
                ) {
                }

                override fun onMessage(conn: WebSocket?, message: String?) {}
                override fun onError(conn: WebSocket?, ex: Exception?) {}
                override fun onStart() {}
            }
        }
        webSocketServer.start()
        initH264MediaCodec()
        isPlaying = true
        Thread { startH264Encode() }.start()
    }

    /*zune: 初始化mediaCodeC*/
    private fun initH264MediaCodec() {
        val mediaFormat = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            VIDEO_WIDTH,
            VIDEO_HEIGHT
        )
        mediaFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, SCREEN_FRAME_BIT)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, SCREEN_FRAME_RATE)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, SCREEN_FRAME_INTERVAL)
        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val surface = mediaCodec.createInputSurface()
            mediaProjection.createVirtualDisplay(
                "ScreenRecorder",
                VIDEO_WIDTH,
                VIDEO_HEIGHT,
                1,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                surface,
                null,
                null
            )
            mediaCodec.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun startH264Encode() {
        val bufferInfo = MediaCodec.BufferInfo()
        while (isPlaying) {
            val outPutBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, ENCODE_TIME_OUT)
            if (outPutBufferId >= 0) {
                mediaCodec.getOutputBuffer(outPutBufferId)?.apply {
                    encodeH264Data(this, bufferInfo)
                }
                mediaCodec.releaseOutputBuffer(outPutBufferId, false)
            }
        }
    }

    private fun encodeH264Data(byteBuffer: ByteBuffer, vBufferInfo: MediaCodec.BufferInfo) {
        var offset = 4
        //判断帧的类型
        if (byteBuffer[2].toInt() == 0x01) {
            offset = 3
        }
        val type: Byte = byteBuffer[offset] and 0x1f
        /*如果送来的流的第一帧Frame有pps和sps，可以不需要配置format.setByteBuffer的”csd-0” （sps） 和”csd-1”（pps）；
          否则必须配置相应的pps和sps,通常情况下sps和pps如下
          SPS帧和 PPS帧合在了一起发送,PS为 [4，len-8] PPS为后4个字节*/
        if (type == NAL_SPS) {
            sps_pps_buf = ByteArray(vBufferInfo.size)
            byteBuffer.get(sps_pps_buf)
        } else if (type == NAL_SLICE /* || type == NAL_SLICE_IDR */) {
            val bytes = ByteArray(vBufferInfo.size)
            byteBuffer[bytes]
            if (this::webSocket.isInitialized && webSocket.isOpen) {
                webSocket.send(bytes)
            }
        } else if (type == NAL_SLICE_IDR) {
            // I帧，前面添加sps和pps
            val bytes = ByteArray(vBufferInfo.size)
            byteBuffer[bytes]
            val newBuf = ByteArray(sps_pps_buf.size + bytes.size)
            System.arraycopy(sps_pps_buf, 0, newBuf, 0, sps_pps_buf.size)
            System.arraycopy(bytes, 0, newBuf, sps_pps_buf.size, bytes.size)
            if (this::webSocket.isInitialized && webSocket.isOpen) {
                webSocket.send(newBuf)
            }
        }
    }


    /*zune: 初始化mediaCodeC*/
    private fun initH265MediaCodec() {
        val mediaFormat = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_HEVC,
            VIDEO_WIDTH,
            VIDEO_HEIGHT
        )
        mediaFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, SCREEN_FRAME_BIT)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, SCREEN_FRAME_RATE)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, SCREEN_FRAME_INTERVAL)
        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC)
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val surface = mediaCodec.createInputSurface()
            mediaProjection.createVirtualDisplay(
                "ScreenRecorder",
                VIDEO_WIDTH,
                VIDEO_HEIGHT,
                1,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                surface,
                null,
                null
            )
            mediaCodec.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /*zune: 开始编码*/
    private fun startH265Encode() {
        val bufferInfo = MediaCodec.BufferInfo()
        while (isPlaying) {
            val outPutBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, ENCODE_TIME_OUT)
            if (outPutBufferId >= 0) {
                mediaCodec.getOutputBuffer(outPutBufferId)?.apply {
                    encodeH265Data(this, bufferInfo)
                }
                mediaCodec.releaseOutputBuffer(outPutBufferId, false)
            }
        }
    }

    /*zune: H265数据编码*/
    private fun encodeH265Data(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        var offSet = 4
        if (byteBuffer[2].toInt() == 0x01) {
            offSet = 3
        }
        val type = byteBuffer[offSet].toInt() and 0x7E shr 1
        if (type == TYPE_FRAME_VPS) {
            h265_vps_pps_sps = ByteArray(bufferInfo.size)
            byteBuffer[h265_vps_pps_sps]
        } else if (type == TYPE_FRAME_INTERVAL) {
            val bytes = ByteArray(bufferInfo.size)
            byteBuffer[bytes]
            val newBytes = ByteArray(h265_vps_pps_sps.size + bytes.size)
            System.arraycopy(h265_vps_pps_sps, 0, newBytes, 0, h265_vps_pps_sps.size)
            System.arraycopy(bytes, 0, newBytes, h265_vps_pps_sps.size, bytes.size)
            /*zune: 解码数据发送给远端*/
            if (this::webSocket.isInitialized && webSocket.isOpen) {
                webSocket.send(newBytes)
            }
        } else {
            val bytes = ByteArray(bufferInfo.size)
            byteBuffer[bytes]
            /*zune: 解码数据发送给远端*/
            if (this::webSocket.isInitialized && webSocket.isOpen) {
                webSocket.send(bytes)
            }
        }
    }

    /*zune: 关闭流*/
    fun close() {
        try {
            isPlaying = false
            webSocketServer.stop()
            webSocket.close()
            mediaCodec.release()
            mediaProjection.stop()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}