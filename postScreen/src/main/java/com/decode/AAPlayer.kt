package com.decode

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat

class AACPlayer() {

    fun playAAC(filePath: String) {
        var extractor: MediaExtractor? = null
        try {
            // 1. 创建音频提取器
            extractor = MediaExtractor().apply {
                setDataSource(filePath)
                val format = getTrackFormat(0)
                val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

                // 2. 创建 AudioTrack
                val channelConfig = if (channelCount == 1) AudioFormat.CHANNEL_OUT_MONO
                else AudioFormat.CHANNEL_OUT_STEREO
                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    channelConfig,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    channelConfig,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                ).apply { play() }  // 3. 立即开始播放

                // 4. 初始化解码器
                val codec = MediaCodec.createDecoderByType("audio/mp4a-latm").apply {
                    configure(format, null, null, 0)
                    start()
                }

                // 选择音频轨道
                selectTrack(0)

                // 5. 解码循环
                val bufferInfo = MediaCodec.BufferInfo()
                var sawInputEOS = false
                var sawOutputEOS = false

                while (!sawOutputEOS) {
                    // 输入处理
                    if (!sawInputEOS) {
                        val inputBufferIndex = codec.dequeueInputBuffer(10000)
                        if (inputBufferIndex >= 0) {
                            val inputBuffer = codec.getInputBuffer(inputBufferIndex) ?: continue
                            val sampleSize = readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                sawInputEOS = true
                                codec.queueInputBuffer(
                                    inputBufferIndex,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                            } else {
                                codec.queueInputBuffer(
                                    inputBufferIndex,
                                    0,
                                    sampleSize,
                                    0,
                                    0
                                )
                                advance()  // 移动到下一帧
                            }
                        }
                    }

                    // 输出处理
                    val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                    when {
                        outputBufferIndex >= 0 -> {
                            val outputBuffer = codec.getOutputBuffer(outputBufferIndex) ?: continue
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)

                            // 写入AudioTrack播放
                            audioTrack?.write(outputBuffer, bufferInfo.size, AudioTrack.WRITE_BLOCKING)
                            codec.releaseOutputBuffer(outputBufferIndex, false)

                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                sawOutputEOS = true
                            }
                        }
                        outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                            // 重新获取output buffers
                        }
                        outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            // 可以处理格式变化
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // 6. 释放资源
            try {
                extractor?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun initAudioTrack() {

    }

    fun initMediaCodec() {

    }

}