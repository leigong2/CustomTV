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
import com.encode.RecordEncoder
import com.google.gson.GsonBuilder
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.io.IOException
import java.net.URI
import java.nio.ByteBuffer

object RecordDecoder {
    private const val RECORD_FRAME_BIT = 96000  // 比特率（比特/秒）
    private lateinit var mediaCodec: MediaCodec
    private lateinit var audioTrack: AudioTrack
    private val bufferInfo = MediaCodec.BufferInfo()
    private var isMediaCodecRunning = false

    fun start() {
        initMediaCodec()
        initAudioTrack()
        isMediaCodecRunning = true
    }

    fun decodeRecordData(source: ByteArray) {
        try {
            if (!isMediaCodecRunning) {
                return
            }
            if (source.isEmpty()) return // 确保数据完整
            if (source[0] != 1.toByte()) {
                return
            }
            val data = source.copyOfRange(1, source.size) // 移除标识字节
            // 检查ADTS头有效性 (0xFFFx)
            if (data.size < 7 || (data[0].toInt() and 0xFF != 0xFF || data[1].toInt() and 0xF0 != 0xF0)) {
                Log.w("我是一条鱼: ", "Invalid ADTS header")
                return
            }
            val inputBufferIndex: Int = mediaCodec.dequeueInputBuffer(10000)
            if (inputBufferIndex >= 0) {
                val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)
                inputBuffer?.clear()
                inputBuffer?.put(data, 0 , data.size)
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, data.size, 0, 0)
            }
            var retryCount = 0
            var outputBufferIndex: Int
            do {
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000) // 增加超时到20ms
                when {
                    outputBufferIndex >= 0 -> {
                        val outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex) ?: break
                        // 获取解码后的PCM数据
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        // 播放音频
                        audioTrack.write(outputBuffer, bufferInfo.size, AudioTrack.WRITE_BLOCKING)
                        mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                        outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
                        Log.d("我是一条鱼：", "音频解码成功，开始播放${GsonBuilder().create().toJson(data)}")
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

    // 配置MediaCodec
    private fun initMediaCodec() {
        mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 1)
        mediaFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC)
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1)
        mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 44100)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, RECORD_FRAME_BIT)
        mediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1)
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        // 添加编解码器特定数据 (CSD-0)
        val csd = byteArrayOf(0x12, 0x10) // 示例值，实际需要匹配编码器
        mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(csd))
        try {
            mediaCodec.configure(mediaFormat, null, null, 0)
            mediaCodec.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun initAudioTrack() {
        val streamType = AudioManager.STREAM_MUSIC
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val mode = AudioTrack.MODE_STREAM
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        audioTrack = AudioTrack(streamType, sampleRate, channelConfig, audioFormat, minBufferSize, mode)
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
        isMediaCodecRunning = false
        mediaCodec.release()
        audioTrack.stop()
        audioTrack.release()
        Log.e("我是一条鱼：", "mediaCodec.release" )
    }
}