package com.decode

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer

@Deprecated("H264无法完成声音的投屏")
object RecordDecoder {
    private const val RECORD_FRAME_BIT = 96000  // 比特率（比特/秒）
    private const val port = 40000   //端口
    private lateinit var mediaCodec: MediaCodec
    private lateinit var audioTrack: AudioTrack
    private lateinit var webSocketClient: WebSocketClient

    fun start(ip: String) {
        webSocketClient = object : WebSocketClient(URI("ws://${ip}:${port}")) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.i("zune: ", "onOpen: ${handshakedata.toString()}")
            }
            override fun onMessage(message: String?) {
                Log.i("zune: ", "onMessage: $message")
            }
            override fun onMessage(bytes: ByteBuffer) {
                val buf = ByteArray(bytes.remaining())
                bytes[buf]
                decodeRecordData(buf)
            }
            override fun onClose(code: Int, reason: String?, remote: Boolean) {}
            override fun onError(ex: Exception?) {
                Log.i("zune: ", "onError: $ex")
            }
        }
        webSocketClient.connect()
        startRecordMediaCodec()
        startPlay()
    }

    private fun decodeRecordData(data: ByteArray) {
        val inputBuffers: Array<ByteBuffer> = mediaCodec.getInputBuffers()
        val inputBufferIndex: Int = mediaCodec.dequeueInputBuffer(10000)
        if (inputBufferIndex >= 0) {
            val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)?:inputBuffers[inputBufferIndex]
            inputBuffer.clear()
            inputBuffer.put(data, 0, data.size)
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, data.size, 10000, 0)
        } else {
            return
        }
        val bufferInfo = MediaCodec.BufferInfo()
        var outputBufferIndex: Int = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000)
        var chunkPCM: ByteArray
        while (outputBufferIndex >= 0) {
            chunkPCM = ByteArray(bufferInfo.size)
            audioTrack.write(chunkPCM, 0, bufferInfo.size)
            mediaCodec.releaseOutputBuffer(outputBufferIndex, true)
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
        }
    }


    // 配置MediaCodec
    private fun startRecordMediaCodec() {
        mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 1)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, RECORD_FRAME_BIT)
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8192)
        mediaFormat.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 0);
        mediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, 0);
        mediaCodec.configure(mediaFormat, null, null, 0)
        mediaCodec.start()
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
            Math.max(minBufferSize, 2048), mode
        )
        audioTrack.play()
    }


    fun release() {
        webSocketClient.close()
        mediaCodec.release()
    }
}