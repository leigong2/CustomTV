package com.decode

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.SurfaceView
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.TextView
import com.ScreenUtils
import com.activity.TouPingReceiveActivity
import com.base.base.BaseApplication
import com.translate.postscreen.R
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.net.URI
import java.nio.ByteBuffer

object ScreenDecoder {
    var VIDEO_WIDTH = ScreenUtils.getScreenWidth()
    var VIDEO_HEIGHT = ScreenUtils.getScreenHeight()
    private val SCREEN_FRAME_BIT = ScreenUtils.getScreenHeight() * ScreenUtils.getScreenWidth()  // 比特率（比特/秒）
    private const val SCREEN_FRAME_RATE = 20  //帧率
    private const val SCREEN_FRAME_INTERVAL = 1  //I帧的频率
    private const val DECODE_TIME_OUT: Long = 10000 //超时时间
    private const val port = 50000   //端口
    private lateinit var mediaCodec: MediaCodec
    private lateinit var webSocketClient: WebSocketClient
    private val handlerThread = HandlerThread("DecoderThread").apply { start() }
    private val decodeHandler = Handler(handlerThread.looper)

    fun start(ip: String, surface: Surface, withH265: Boolean) {
        webSocketClient = object : WebSocketClient(URI("ws://${ip}:${port}")) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                BaseApplication.getInstance().handler.post {
                    (BaseApplication.getInstance().topActivity as? TouPingReceiveActivity)?.findViewById<TextView>(R.id.info)?.append("远端开启成功:${ip}\n")
                }
            }
            override fun onMessage(message: String?) {
                message?.also { json ->
                    val jsonObject = JSONObject(json)
                    val width = jsonObject.get("width")
                    val height = jsonObject.get("height")
                    VIDEO_WIDTH = width.toString().toInt()
                    VIDEO_HEIGHT = height.toString().toInt()
                    BaseApplication.getInstance().handler.post {
                        (BaseApplication.getInstance().topActivity as? TouPingReceiveActivity)?.findViewById<SurfaceView>(R.id.surfaceView)?.also {surfaceView ->
                            resetSurfaceView(surfaceView, VIDEO_WIDTH, VIDEO_HEIGHT)
                        }
                    }
                }
            }
            override fun onMessage(bytes: ByteBuffer) {
                decodeHandler.post {
                    val buf = ByteArray(bytes.remaining())
                    bytes[buf]
                    RecordDecoder.decodeRecordData(buf)
                    if (withH265) {
                        decodeH265Data(buf)
                    } else {
                        decodeH264Data(buf)
                    }
                }
            }
            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                BaseApplication.getInstance().handler.post {
                    (BaseApplication.getInstance().topActivity as? TouPingReceiveActivity)?.findViewById<TextView>(R.id.info)?.append("远端关闭:${ip}\n")
                }
                loged = false
            }
            override fun onError(ex: Exception?) {
                BaseApplication.getInstance().handler.post {
                    (BaseApplication.getInstance().topActivity as? TouPingReceiveActivity)?.findViewById<TextView>(R.id.info)?.append("远端开启失败:${ex} $ip\n")
                }
            }
        }
        webSocketClient.connect()
        if (withH265) {
            startH265MediaCodec(surface)
        } else {
            startH264MediaCodec(surface)
        }
    }

    private fun resetSurfaceView(surfaceView: SurfaceView, videoWidth: Int, videoHeight: Int) {
        val screenWidth = ScreenUtils.getScreenWidth()
        val screenHeight = ScreenUtils.getScreenHeight()
        if (videoWidth / videoHeight.toFloat() > screenWidth / screenHeight.toFloat()) {
            val layoutParams = surfaceView.layoutParams
            layoutParams.width = MATCH_PARENT
            layoutParams.height = (screenWidth.toFloat() / videoWidth * videoHeight).toInt()
            surfaceView.layoutParams = layoutParams
        } else {
            val layoutParams = surfaceView.layoutParams
            layoutParams.height = MATCH_PARENT
            layoutParams.width = (screenHeight.toFloat() / videoHeight * videoWidth).toInt()
            surfaceView.layoutParams = layoutParams
        }
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
        (BaseApplication.getInstance().topActivity as? TouPingReceiveActivity)?.findViewById<TextView>(R.id.info)?.append("H264初始化完成\n")
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

    private var  loged = false

    /*zune: H264解码*/
    private fun decodeH264Data(source: ByteArray) {
        if (source.isEmpty()) return // 确保数据完整
        if (source[0] != 2.toByte()) {
            return
        }
        val data = source.copyOfRange(1, source.size) // 移除标识字节
        val inputBufferIndex: Int = mediaCodec.dequeueInputBuffer(100)
        if (inputBufferIndex >= 0) {
            val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex) ?: return
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
            if (!loged) {
                loged = true
                BaseApplication.getInstance().handler.post {
                    (BaseApplication.getInstance().topActivity as? TouPingReceiveActivity)?.findViewById<TextView>(
                        R.id.info)?.append("H264解码完成\n")
                }
            }
        }
    }

    /*zune: H265解码*/
    private fun decodeH265Data(source: ByteArray) {
        if (source.isEmpty()) return // 确保数据完整
        if (source[0] != 2.toByte()) {
            return
        }
        val data = source.copyOfRange(1, source.size) // 移除标识字节
        Log.e("我是一条鱼：", "接收到视频数据，准备解析" )
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

    fun sendOrientation(orientation: Int) {
        webSocketClient.send(orientation.toString())
    }
}