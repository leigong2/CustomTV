package com.decode

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.AudioTrack.OnPlaybackPositionUpdateListener
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.google.gson.GsonBuilder
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer

object RecordDecoder {
    private const val RECORD_FRAME_BIT = 96000  // 比特率（比特/秒）
    private const val port = 40000   //端口
    private lateinit var mediaCodec: MediaCodec
    private lateinit var audioTrack: AudioTrack
    private lateinit var webSocketClient: WebSocketClient
    private val bufferInfo = MediaCodec.BufferInfo()
    private val handlerThread = HandlerThread("DecoderThread").apply { start() }
    private val decodeHandler = Handler(handlerThread.looper)

    fun start(ip: String) {
        webSocketClient = object : WebSocketClient(URI("ws://${ip}:${port}")) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.i("zune: ", "onOpen: ${handshakedata.toString()}")
            }
            override fun onMessage(message: String?) {
                Log.i("zune: ", "onMessage: $message")
            }
            override fun onMessage(bytes: ByteBuffer) {
                decodeHandler.post {
                    val buf = ByteArray(bytes.remaining())
                    bytes[buf]
                    decodeRecordData(buf)
                }
            }
            override fun onClose(code: Int, reason: String?, remote: Boolean) {}
            override fun onError(ex: Exception?) {
                Log.i("zune: ", "onError: $ex")
            }
        }
        webSocketClient.connect()
        startRecordMediaCodec()
        startPlay()
        Log.e("我是一条鱼：", "解码器初始化完成" )
    }

    fun decodeRecordData(source: ByteArray) {
        try {
            if (source.size < 8) return // 确保数据完整
            if (!isCodecRunning) {
                return
            }
            val (hasADTS, aacData) = if (source[0] == 1.toByte()) {
                true to source.copyOfRange(1, source.size) // 移除标识字节
            } else {
                false to source
            }
            val inputBufferIndex: Int = mediaCodec.dequeueInputBuffer(10000)
            Log.e("我是一条鱼：", "aacData = 转化完成 ${GsonBuilder().create().toJson(aacData)}" )
            // 移除ADTS头（如果存在）
            val data = if (hasADTS && aacData.size >= 7
                && aacData[0] == 0xFF.toByte()
                && aacData[1] == 0xF1.toByte()
            ) {
                aacData.copyOfRange(7, aacData.size) // 移除ADTS头
            } else {
                aacData // 无ADTS头
            }
            Log.e("我是一条鱼：", "data = 移除头完成 ${GsonBuilder().create().toJson(data)}" )
            Log.e("我是一条鱼：", "inputBufferIndex = ${inputBufferIndex}")
            if (inputBufferIndex >= 0) {
                val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)
                Log.e("我是一条鱼：", "inputBuffer = ${inputBuffer != null}")
                inputBuffer?.clear()
                inputBuffer?.put(data)
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, data.size, 0, 0)
                Log.e("我是一条鱼：", "queueInputBuffer")
            }
            var retryCount = 0
            var outputBufferIndex: Int
            do {
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000) // 增加超时到20ms
                Log.e("我是一条鱼：", "retry:$retryCount outputBufferIndex = ${outputBufferIndex}" )
                when {
                    outputBufferIndex >= 0 -> {
                        val outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex)
                        // 获取解码后的PCM数据
                        val pcmData = ByteArray(bufferInfo.size).apply {
                            outputBuffer?.get(this, 0, bufferInfo.size)
                        }
                        // 播放音频
                        Log.e("我是一条鱼：", "解码成功，准备播放" )
                        audioTrack.write(pcmData, 0, bufferInfo.size)
                        mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                        outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000)
                        break // 成功获取输出后跳出循环
                    }
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val newFormat = mediaCodec.outputFormat
                        Log.d("我是一条鱼：", "Output format changed: $newFormat")
                    }
                    outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        retryCount++
                        if (retryCount > 5) {
                            Log.w("我是一条鱼：", "Output buffer timeout after 5 retries")
                            break
                        }
                    }
                }
            } while (true)
        } catch (ignore: Exception) {
            Log.e("我是一条鱼：", "报错：${ignore}" )
        }
    }

    private var isCodecRunning = false

    // 配置MediaCodec
    private fun startRecordMediaCodec() {
        try {
            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            val mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 1)
            mediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1) // 标识含ADTS头
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, RECORD_FRAME_BIT)
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            mediaCodec.configure(mediaFormat, null, null, 0)
            mediaCodec.start()
            isCodecRunning = true
            Log.e("我是一条鱼：", "mediaCodec.start" )
        } catch (ignore: Exception) {
            isCodecRunning = false
        }
    }

    private fun startPlay() {
        val streamType = AudioManager.STREAM_MUSIC
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val mode = AudioTrack.MODE_STREAM
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        audioTrack = AudioTrack(
            streamType, sampleRate, channelConfig, audioFormat,
            minBufferSize, mode
        )
        audioTrack.play()
        audioTrack.setPlaybackPositionUpdateListener(object : OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: AudioTrack?) {
                // 当播放头到达标记时触发
                Log.i("我是一条鱼：", "播放准备完成 Marker reached!");
            }

            override fun onPeriodicNotification(track: AudioTrack?) {
                val playbackHeadPosition = track?.playbackHeadPosition;
                Log.i("我是一条鱼：", "播放成功，position: $playbackHeadPosition");
            }
        })
    }

    fun release() {
        webSocketClient.close()
        mediaCodec.release()
        audioTrack.stop()
        audioTrack.release()
        Log.e("我是一条鱼：", "mediaCodec.release" )
    }
}