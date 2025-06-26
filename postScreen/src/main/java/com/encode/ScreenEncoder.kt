package com.encode

import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import android.widget.TextView
import com.ScreenUtils
import com.activity.TouPingPostActivity
import com.base.base.BaseApplication
import com.encode.RecordEncoder.addByteToFirst
import com.translate.postscreen.R
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.Objects
import kotlin.experimental.and

object ScreenEncoder {
    //不同手机支持的编码最大分辨率不同
    var VIDEO_WIDTH = ScreenUtils.getScreenWidth()
    var VIDEO_HEIGHT = ScreenUtils.getScreenHeight()
    private val SCREEN_FRAME_BIT = ScreenUtils.getScreenHeight().toFloat() * ScreenUtils.getScreenWidth()  // 比特率（比特/秒）
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
    private var isChangeOrientation = false

    fun start(mediaProjection: MediaProjection, withH265: Boolean) {
        if (this::webSocketServer.isInitialized) {
            return
        }
        this.mediaProjection = mediaProjection
        val inetSocketAddress = InetSocketAddress(port)
        webSocketServer = object : WebSocketServer(inetSocketAddress) {
            override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
                Log.e("我是一条鱼：", "有人连接进来了ip:${conn?.remoteSocketAddress}" )
                conn?.apply {
                    webSocket = this
                    val json = "{\"width\":${ScreenUtils.getScreenWidth()},\"height\":${ScreenUtils.getScreenHeight()}}"
                    webSocket.send(json)
                }
            }

            override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
                Log.e("我是一条鱼：", "远端关闭" )
            }

            override fun onMessage(conn: WebSocket?, message: String?) {
                if (Objects.equals(message, Configuration.ORIENTATION_LANDSCAPE.toString())) {
                    VIDEO_WIDTH = ScreenUtils.getScreenHeight()
                    VIDEO_HEIGHT = ScreenUtils.getScreenWidth()
                } else if (Objects.equals(message, Configuration.ORIENTATION_PORTRAIT.toString())) {
                    VIDEO_WIDTH = ScreenUtils.getScreenWidth()
                    VIDEO_HEIGHT = ScreenUtils.getScreenHeight()
                }
                isChangeOrientation = true
                if (withH265) {
                    initH265MediaCodec()
                } else {
                    initH264MediaCodec()
                }
                isChangeOrientation = false
            }
            override fun onError(conn: WebSocket?, ex: Exception?) {
                Log.e("我是一条鱼：", "异常断开，error:${ex}" )
            }
            override fun onStart() {
                Log.e("我是一条鱼：", "远端启动" )
            }
        }
        Log.e("我是一条鱼：", "开启远端服务ip:${inetSocketAddress}" )

        webSocketServer.start()
        isPlaying = true
        if (withH265) {
            initH265MediaCodec()
            Thread { startH265Encode() }.start()
        } else {
            initH264MediaCodec()
            Thread { startH264Encode() }.start()
        }
    }

    /*zune: 初始化mediaCodeC*/
    private fun initH264MediaCodec() {
        if (this::mediaCodec.isInitialized) {
            mediaCodec.stop()
        }
        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, VIDEO_WIDTH, VIDEO_HEIGHT)
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, SCREEN_FRAME_BIT.toInt())
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, SCREEN_FRAME_RATE)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, SCREEN_FRAME_INTERVAL)
        // Android 13 新增推荐参数
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mediaFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileMain)
            mediaFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel42)
            mediaFormat.setInteger(MediaFormat.KEY_LATENCY, 1) // 低延迟模式
            mediaFormat.setInteger(MediaFormat.KEY_MAX_B_FRAMES, 0) // 禁用B帧
        }
        try {
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val surface = mediaCodec.createInputSurface()
            mediaProjection.createVirtualDisplay("ScreenRecorder", VIDEO_WIDTH, VIDEO_HEIGHT, 1, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface, null, null)
            mediaCodec.start()
            Log.e("我是一条鱼：", "H264初始化完成" )
        } catch (e: IOException) {
            Log.e("我是一条鱼：", "H264初始化失败" )
            e.printStackTrace()
        }
    }

    private fun startH264Encode() {
        val bufferInfo = MediaCodec.BufferInfo()
        while (isPlaying) {
            if (isChangeOrientation) {
                continue
            }
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
            sendMessage(bytes.addByteToFirst(2))
        } else if (type == NAL_SLICE_IDR) {
            // I帧，前面添加sps和pps
            val bytes = ByteArray(vBufferInfo.size)
            byteBuffer[bytes]
            val newBuf = ByteArray(sps_pps_buf.size + bytes.size)
            System.arraycopy(sps_pps_buf, 0, newBuf, 0, sps_pps_buf.size)
            System.arraycopy(bytes, 0, newBuf, sps_pps_buf.size, bytes.size)
            sendMessage(newBuf.addByteToFirst(2))
        }
    }


    /*zune: 初始化mediaCodeC*/
    private fun initH265MediaCodec() {
        val mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, VIDEO_WIDTH, VIDEO_HEIGHT)
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, SCREEN_FRAME_BIT.toInt())
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, SCREEN_FRAME_RATE)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, SCREEN_FRAME_INTERVAL)
        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC)
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val surface = mediaCodec.createInputSurface()
            mediaProjection.createVirtualDisplay("ScreenRecorder", VIDEO_WIDTH, VIDEO_HEIGHT, 1, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface, null, null)
            mediaCodec.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /*zune: 开始编码*/
    private fun startH265Encode() {
        val bufferInfo = MediaCodec.BufferInfo()
        while (isPlaying) {
            if (isChangeOrientation) {
                continue
            }
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
            Log.e("我是一条鱼：", "准备传输视频数据" )
            sendMessage(newBytes.addByteToFirst(2))
        } else {
            val bytes = ByteArray(bufferInfo.size)
            byteBuffer[bytes]
            /*zune: 解码数据发送给远端*/
            Log.e("我是一条鱼：", "准备传输视频数据" )
            sendMessage(bytes.addByteToFirst(2))
        }
    }

    /*zune: 关闭流*/
    fun close() {
        try {
            isPlaying = false
            if (this::webSocketServer.isInitialized) {
                webSocketServer.stop()
            }
            if (this::webSocket.isInitialized) {
                webSocket.close()
            }
            if (this::mediaCodec.isInitialized) {
                mediaCodec.release()
            }
            mediaProjection.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendMessage(byteArray: ByteArray) {
        if (this::webSocket.isInitialized && webSocket.isOpen) {
            webSocket.send(byteArray)
        }
    }
}