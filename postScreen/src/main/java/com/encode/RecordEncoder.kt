package com.encode

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import com.base.base.BaseApplication
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer


object RecordEncoder {
    //不同手机支持的编码最大分辨率不同
    private const val RECORD_FRAME_BIT = 96000  // 比特率（比特/秒）
    private val bufferBytes = ByteArray(2048)

    private const val port = 40000   //端口
    private lateinit var mediaCodec: MediaCodec
    private lateinit var recorder: AudioRecord
    private lateinit var webSocketServer: WebSocketServer
    private lateinit var webSocket: WebSocket
    private var isPlaying = true

    fun start(mediaProjection: MediaProjection) {
        initAudioRecord(mediaProjection)
        Log.e("我是一条鱼：", "初始化麦克风 成功 ${this::webSocketServer.isInitialized}" )
        if (!this::webSocketServer.isInitialized) {
            webSocketServer = object : WebSocketServer(InetSocketAddress(port)) {
                override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
                    Log.e("我是一条鱼：", "webSocketServer 连接成功" )
                    conn?.apply { webSocket = this }
                }

                override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
                    Log.e("我是一条鱼：", "webSocketServer 关闭成功" )
                }

                override fun onMessage(conn: WebSocket?, message: String?) {
                    Log.i("zune: ", "onMessage: $message")
                }
                override fun onError(conn: WebSocket?, ex: Exception?) {
                    Log.e("我是一条鱼：", "webSocketServer onError ${ex}" )
                }
                override fun onStart() {
                    Log.e("我是一条鱼：", "webSocketServer start 成功" )
                }
            }
        }
        webSocketServer.start()
        initRecordMediaCodec()
        isPlaying = true
        Thread { startRecordEncode() }.start()
    }

    /*zune: 初始化mediaCodeC*/
    private fun initRecordMediaCodec() {
        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 1)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, RECORD_FRAME_BIT)
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        try {
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mediaCodec.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /*zune: 初始化麦克风*/
    private fun initAudioRecord(mediaProjection: MediaProjection) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //数据编码方式
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(44100)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val playbackConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .build()
            recorder = AudioRecord.Builder()
                .setAudioFormat(format)
                .setAudioPlaybackCaptureConfig(playbackConfig)
                .build()
        } else {
            val audioSource = MediaRecorder.AudioSource.MIC
            val sampleRate = 44100
            val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO
            val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            recorder = AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, minBufferSize)
        }
    }

    private fun startRecordEncode() {
        recorder.startRecording()
        while (isPlaying) {
            val read: Int = recorder.read(bufferBytes, 0, 2048)
            if (read >= 0) {
                val audio = ByteArray(read)
                System.arraycopy(bufferBytes, 0, audio, 0, read)
                encodePCMData(audio) // PCM数据 编码
            }
        }
    }

    private fun encodePCMData(chunkPCM: ByteArray) {
        val bufferInfo = MediaCodec.BufferInfo()
        val inputBuffer: ByteBuffer
        var outputBuffer: ByteBuffer
        val inputIndex: Int = mediaCodec.dequeueInputBuffer(10000)//同解码器
        if (inputIndex >= 0) {
            inputBuffer = mediaCodec.getInputBuffer(inputIndex) ?: return//同解码器
            inputBuffer.clear();//同解码器
            inputBuffer.put(chunkPCM, 0, 2048);//PCM数据填充给inputBuffer
            mediaCodec.queueInputBuffer(inputIndex, 0, 2048, System.nanoTime()/1000, 0);//通知编码器 编码
        }
        var outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000)
        while (outputIndex >= 0) {
            outputBuffer = mediaCodec.getOutputBuffer(outputIndex) ?: return //拿到输出Buffer
            // 获取编码后的AAC数据
            val aacData = ByteArray(bufferInfo.size).apply {
                outputBuffer.get(this, 0, bufferInfo.size)
            }
            // 添加ADTS头
            val aacFullData = addAdtsHeader(aacData, 44100, 1)
            // 传输或保存数据
            if (this::webSocket.isInitialized && webSocket.isOpen) {
                webSocket.send(aacFullData.addByteToFirst(1))
            }
            mediaCodec.releaseOutputBuffer(outputIndex, false)
            outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000)
        }
    }

    /**
     * 添加ADTS头
     *
     * @param packet
     * @param packetLen
     */
    fun addAdtsHeader(aacData: ByteArray, sampleRate: Int, channels: Int): ByteArray {
        val adtsHeader = ByteArray(7)
        val frameLength = aacData.size + 7
        adtsHeader[0] = 0xFF.toByte()
        adtsHeader[1] = 0xF9.toByte()
        adtsHeader[2] = ((1 shl 6) + (sampleRate shl 2) + (channels shr 2)).toByte()
        adtsHeader[3] = (((channels and 3) shl 6) + (frameLength shr 11)).toByte()
        adtsHeader[4] = ((frameLength and 0x7FF) shr 3).toByte()
        adtsHeader[5] = (((frameLength and 7) shl 5) + 0x1F).toByte()
        adtsHeader[6] = 0xFC.toByte()
        return adtsHeader + aacData
    }

    fun ByteArray.addByteToFirst(prefix: Byte): ByteArray {
        val result = ByteArray(size + 1)
        result[0] = prefix
        for (i in indices) {
            result[i + 1] = this[i]
        }
        return result
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
            Log.e("我是一条鱼：", "关闭流媒体" )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}