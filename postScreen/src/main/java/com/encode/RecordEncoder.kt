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
import java.io.IOException
import java.nio.ByteBuffer


object RecordEncoder {
    //不同手机支持的编码最大分辨率不同
    private const val RECORD_FRAME_BIT = 96000  // 比特率（比特/秒）
    private val bufferBytes = ByteArray(2048)

    private lateinit var mediaCodec: MediaCodec
    private lateinit var recorder: AudioRecord
    private var isRecording = true

    fun start(mediaProjection: MediaProjection) {
        initAudioRecord(mediaProjection)
        initRecordMediaCodec()
        isRecording = true
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
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
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
        while (isRecording) {
            val read: Int = recorder.read(bufferBytes, 0, bufferBytes.size)
            if (read >= 0) {
                val audio = ByteArray(read)
                System.arraycopy(bufferBytes, 0, audio, 0, read)
                encodePCMData(audio) // PCM数据 编码
            }
        }
    }

    private fun encodePCMData(chunkPCM: ByteArray) {
        try {
            val bufferInfo = MediaCodec.BufferInfo()
            val inputBuffer: ByteBuffer
            var outputBuffer: ByteBuffer
            val inputIndex: Int = mediaCodec.dequeueInputBuffer(-1)//同解码器
            if (inputIndex >= 0) {
                inputBuffer = mediaCodec.getInputBuffer(inputIndex) ?: return//同解码器
                inputBuffer.clear();//同解码器
                inputBuffer.put(chunkPCM);//PCM数据填充给inputBuffer
                mediaCodec.queueInputBuffer(inputIndex, 0, chunkPCM.size, System.nanoTime()/1000, 0);//通知编码器 编码
            }
            var outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
            while (outputIndex >= 0) {
                outputBuffer = mediaCodec.getOutputBuffer(outputIndex) ?: return //拿到输出Buffer
                val outBitSize = bufferInfo.size
                val outPacketSize = outBitSize + 7 //7为ADTS头部的大小
                outputBuffer.position(bufferInfo.offset)
                outputBuffer.limit(bufferInfo.offset + outBitSize)
                val chunkAudio = ByteArray(outPacketSize)
                addADTStoPacket(44100, chunkAudio, outPacketSize) //添加ADTS
                outputBuffer.get(chunkAudio, 7, outBitSize) //将编码得到的AAC数据 取出到byte[]中 偏移量offset=7
                outputBuffer.position(bufferInfo.offset)
                // 传输或保存数据
                ScreenEncoder.sendMessage(chunkAudio.addByteToFirst(1))
                mediaCodec.releaseOutputBuffer(outputIndex, false)
                outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
            }
        } catch (ignore: Exception) {
        }
    }

    /**
     * 添加ADTS头
     *
     * @param packet
     * @param packetLen
     */
    fun addADTStoPacket(sampleRateType: Int, packet: ByteArray, packetLen: Int) {
        val chanCfg = 1 // CPE
        packet[0] = 0xFF.toByte()
        packet[1] = 0xF9.toByte()
        packet[2] = ((1 shl 6) + (4 shl 2) + (chanCfg shr 2)).toByte()
        packet[3] = (((chanCfg and 3) shl 6) + (packetLen shr 11)).toByte()
        packet[4] = ((packetLen and 0x7FF) shr 3).toByte()
        packet[5] = (((packetLen and 7) shl 5) + 0x1F).toByte()
        packet[6] = 0xFC.toByte()
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
            isRecording = false
            if (this::mediaCodec.isInitialized) {
                mediaCodec.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}