package com.decode

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer

object RecordDecoder {
    private const val RECORD_FRAME_BIT = 96000  // 比特率（比特/秒）
    private const val port = 40000   //端口
    private lateinit var mediaCodec: MediaCodec
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
    }

    private fun decodeRecordData(data: ByteArray) {
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

    fun release() {
        webSocketClient.close()
        mediaCodec.release()
    }
}