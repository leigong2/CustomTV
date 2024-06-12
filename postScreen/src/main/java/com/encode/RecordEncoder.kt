package com.encode

import android.app.Service
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.Log
import com.base.base.BaseApplication
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer


@Deprecated("H264无法完成声音的投屏")
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
        if (!this::webSocketServer.isInitialized) {
            webSocketServer = object : WebSocketServer(InetSocketAddress(port)) {
                override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
                    Log.i("zune: ", "onOpen")
                    conn?.apply { webSocket = this }
                }

                override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
                    Log.i("zune: ", "onClose: $code, $reason")
                }

                override fun onMessage(conn: WebSocket?, message: String?) {
                    Log.i("zune: ", "onMessage: $message")
                }
                override fun onError(conn: WebSocket?, ex: Exception?) {
                    Log.i("zune: ", "onError: $ex")
                }
                override fun onStart() {}
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
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8192)
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
            val channelConfig: Int = AudioFormat.CHANNEL_OUT_MONO
            val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            recorder = AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, Math.max(minBufferSize, 2048))
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
        var chunkAudio: ByteArray
        var outBitSize: Int
        var outPacketSize: Int
        val inputIndex: Int = mediaCodec.dequeueInputBuffer(-1);//同解码器
        if (inputIndex >= 0) {
            inputBuffer = mediaCodec.getInputBuffer(inputIndex)!!;//同解码器
            inputBuffer.clear();//同解码器
            inputBuffer.limit(chunkPCM.size);
            inputBuffer.put(chunkPCM);//PCM数据填充给inputBuffer
            mediaCodec.queueInputBuffer(inputIndex, 0, chunkPCM.size, 0, 0);//通知编码器 编码
        }
        var outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000)
        while (outputIndex >= 0) {
            outBitSize = bufferInfo.size
            outPacketSize = outBitSize + 7 //7为ADTS头部的大小
            outputBuffer = mediaCodec.getOutputBuffer(outputIndex)!! //拿到输出Buffer
            outputBuffer.position(bufferInfo.offset)
            outputBuffer.limit(bufferInfo.offset + outBitSize)
            chunkAudio = ByteArray(outPacketSize)
            addADTStoPacket(44100, chunkAudio, outPacketSize) //添加ADTS
            outputBuffer[chunkAudio, 7, outBitSize] //将编码得到的AAC数据 取出到byte[]中 偏移量offset=7
            outputBuffer.position(bufferInfo.offset)
            Log.i("RecordEncoder: ", chunkAudio.toString())
            if (this::webSocket.isInitialized && webSocket.isOpen) {
                webSocket.send(chunkAudio)
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
    private fun addADTStoPacket(sampleRateType: Int, packet: ByteArray, packetLen: Int) {
        val profile = 2 // AAC LC
        val chanCfg = 2 // CPE
        packet[0] = 0xFF.toByte()
        packet[1] = 0xF9.toByte()
        packet[2] = ((profile - 1 shl 6) + (sampleRateType shl 2) + (chanCfg shr 2)).toByte()
        packet[3] = ((chanCfg and 3 shl 6) + (packetLen shr 11)).toByte()
        packet[4] = (packetLen and 0x7FF shr 3).toByte()
        packet[5] = ((packetLen and 7 shl 5) + 0x1F).toByte()
        packet[6] = 0xFC.toByte()
    }

    /*zune: 关闭流*/
    fun close() {
        try {
            isPlaying = false
            webSocketServer.stop()
            webSocket.close()
            mediaCodec.release()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}