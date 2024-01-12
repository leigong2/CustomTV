package com.screen;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Recording internal audio * 录音不会自己保存数据
 * 1、是需要自己新开一个线程来保存录音数据
 */
public class ScreenInternalAudioRecorder {
    private static final String TAG = "recorder";
    private static final int TIMEOUT = 500;
    private static final float MIC_VOLUME_SCALE = 1.4f;
    private AudioRecord mAudioRecord;
    private AudioRecord mAudioRecordMic;
    private final Config mConfig = new Config();
    private Thread mThread;

    //Mediacodec类可用于访问底层的媒体编解码器，即编码器/解码器组件。
    private MediaCodec mCodec;

    private long mPresentationTime;
    private long mTotalBytes;
    private MediaMuxer mMuxer;
    private boolean mMic;
    private int mTrackId = -1;

    //配置捕获其他应用程序播放的音频。
    //当捕获由其他应用程序(和你的)播放的音频信号时,你将只捕获由播放器(如AudioTrack或MediaPlayer)播放的音频信号的混合
    private AudioPlaybackCaptureConfiguration playbackConfig;

    public ScreenInternalAudioRecorder(MediaProjection mp, boolean includeMicInput) {
        mMic = includeMicInput;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            playbackConfig = new AudioPlaybackCaptureConfiguration.Builder(mp)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                    .addMatchingUsage(AudioAttributes.USAGE_GAME)
                    .build();
        }
    }

    /**
     * Audio recoding configuration
     * */
    public static class Config {
        public int channelOutMask = AudioFormat.CHANNEL_OUT_MONO;
        public int channelInMask = AudioFormat.CHANNEL_IN_MONO;
        public int encoding = AudioFormat.ENCODING_PCM_16BIT;
        public int sampleRate = 44100;
        public int bitRate = 196000;
        public int bufferSizeBytes = 1 << 17;
        public boolean privileged = true;
        public boolean legacy_app_looback = false;

        @Override
        public String toString() {
            return "channelMask=" + channelOutMask
                    + "\n   encoding=" + encoding
                    + "\n sampleRate=" + sampleRate
                    + "\n bufferSize=" + bufferSizeBytes
                    + "\n privileged=" + privileged
                    + "\n legacy app looback=" + legacy_app_looback;
        }
    }


    public void setupSimple(String outFile, Boolean isMic) throws IOException {

        mMic = isMic;
        //返回成功创建AudioRecord对象所需的最小缓冲区大小
        int size = AudioRecord.getMinBufferSize(
                mConfig.sampleRate, mConfig.channelInMask,
                mConfig.encoding) * 2;

        Log.d(TAG, "ScreenInternalAudioRecorder audio buffer size: " + size);
        mMuxer = new MediaMuxer(outFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        //数据编码方式
        AudioFormat format = new AudioFormat.Builder()
                .setEncoding(mConfig.encoding)
                .setSampleRate(mConfig.sampleRate)
                .setChannelMask(mConfig.channelOutMask)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mAudioRecord = new AudioRecord.Builder()
                        .setAudioFormat(format)
                        .setAudioPlaybackCaptureConfig(playbackConfig)
                        .build();
            }
        }

        if (mMic) {
            /*
             * 一般录音的构造方法
             * audioSource 表示数据来源 一般为麦克风 MediaRecorder.AudioSource.MIC             * sampleRateInHz 表示采样率 一般设置为 44100             * channelConfig 表示声道 一般设置为 AudioFormat.CHANNEL_IN_MONO             * audioFormat 数据编码方式 这里使用 AudioFormat.ENCODING_PCM_16BIT             * bufferSizeInBytes 数据大小 这里使用AudioRecord.getMinBufferSize 获取
             */
            mAudioRecordMic = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    mConfig.sampleRate, mConfig.channelInMask, mConfig.encoding, size);
        }

        //实例化支持给定mime类型输出数据的首选编码器、MIMETYPE_AUDIO_AAC是一种压缩格式
        mCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);

        //封装描述媒体数据格式的信息
        MediaFormat medFormat = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC, mConfig.sampleRate, 1);

        medFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        medFormat.setInteger(MediaFormat.KEY_BIT_RATE, mConfig.bitRate);
        medFormat.setInteger(MediaFormat.KEY_PCM_ENCODING, mConfig.encoding);


        //配置参数
        mCodec.configure(medFormat,
                null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        //
        mThread = new Thread(() -> {
            short[] bufferInternal = null;
            short[] bufferMic = null;
            byte[] buffer = null;

            if (mMic) {
                bufferInternal = new short[size / 2];
                bufferMic = new short[size / 2];
            } else {
                buffer = new byte[size];
            }

            while (true) {
                int readBytes = 0;
                int readShortsInternal = 0;
                int readShortsMic = 0;
                if (mMic && bufferInternal != null) {
                    readShortsInternal = mAudioRecord.read(bufferInternal, 0,
                            bufferInternal.length);
                    readShortsMic = mAudioRecordMic.read(bufferMic, 0, bufferMic.length);

                    // modify the volume
                    bufferMic = scaleValues(bufferMic,
                            readShortsMic, MIC_VOLUME_SCALE);
                    readBytes = Math.min(readShortsInternal, readShortsMic) * 2;

                    buffer = addAndConvertBuffers(bufferInternal, readShortsInternal, bufferMic,
                            readShortsMic);
                } else {
                    readBytes = mAudioRecord.read(buffer, 0, buffer.length);
                }

                //exit the loop when at end of stream
                if (readBytes < 0) {
                    Log.e(TAG, "ScreenInternalAudioRecorder read error " + readBytes +
                            ", shorts internal: " + readShortsInternal +
                            ", shorts mic: " + readShortsMic);
                    break;                }
                encode(buffer, readBytes);
            }
            endStream();
        });
    }

    private short[] scaleValues(short[] buff, int len, float scale) {
        for (int i = 0; i < len; i++) {
            int oldValue = buff[i];
            int newValue = (int) (buff[i] * scale);
            if (newValue > Short.MAX_VALUE) {
                newValue = Short.MAX_VALUE;
            } else if (newValue < Short.MIN_VALUE) {
                newValue = Short.MIN_VALUE;
            }
            buff[i] = (short) (newValue);
        }
        return buff;
    }


    private byte[] addAndConvertBuffers(short[] a1, int a1Limit, short[] a2, int a2Limit) {
        int size = Math.max(a1Limit, a2Limit);
        if (size < 0) return new byte[0];
        byte[] buff = new byte[size * 2];
        for (int i = 0; i < size; i++) {
            int sum;
            if (i > a1Limit) {
                sum = a2[i];
            } else if (i > a2Limit) {
                sum = a1[i];
            } else {
                sum = (int) a1[i] + (int) a2[i];
            }

            if (sum > Short.MAX_VALUE) sum = Short.MAX_VALUE;
            if (sum < Short.MIN_VALUE) sum = Short.MIN_VALUE;
            int byteIndex = i * 2;
            //位与（&）：二元运算符，两个为1时结果为1，否则为0
            buff[byteIndex] = (byte) (sum & 0xff);
            //规则：符号位不变，低位溢出截断，高位用符号位填充。如：8 >> 2 = 2。
            buff[byteIndex + 1] = (byte) ((sum >> 8) & 0xff);
        }
        return buff;
    }


    //编码
    private void encode(byte[] buffer, int readBytes) {
        int offset = 0;
        while (readBytes > 0) {
            int totalBytesRead = 0;
            int bufferIndex = mCodec.dequeueInputBuffer(TIMEOUT);
            if (bufferIndex < 0) {
                writeOutput();
                return;            }
            ByteBuffer buff = mCodec.getInputBuffer(bufferIndex);
            buff.clear();
            int bufferSize = buff.capacity();
            int bytesToRead = Math.min(readBytes, bufferSize);
            totalBytesRead += bytesToRead;
            readBytes -= bytesToRead;
            buff.put(buffer, offset, bytesToRead);
            offset += bytesToRead;
            mCodec.queueInputBuffer(bufferIndex, 0, bytesToRead, mPresentationTime, 0);
            mTotalBytes += totalBytesRead;
            mPresentationTime = 1000000L * (mTotalBytes / 2) / mConfig.sampleRate;
            writeOutput();
        }
    }

    private void endStream() {
        int bufferIndex = mCodec.dequeueInputBuffer(TIMEOUT);
        mCodec.queueInputBuffer(bufferIndex, 0, 0, mPresentationTime,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        writeOutput();
    }

    private void writeOutput() {
        while (true) {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int bufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT);
            if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                mTrackId = mMuxer.addTrack(mCodec.getOutputFormat());
                mMuxer.start();
                continue;            }
            if (bufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break;
            }
            if (mTrackId < 0) return;
            ByteBuffer buff = mCodec.getOutputBuffer(bufferIndex);

            if (!((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                    && bufferInfo.size != 0)) {
                mMuxer.writeSampleData(mTrackId, buff, bufferInfo);
            }
            mCodec.releaseOutputBuffer(bufferIndex, false);
        }
    }

    /**
     * start recording     *     * @throws IllegalStateException if recording fails to initialize
     */    public void start() throws IllegalStateException {
        if (mThread != null) {
            Log.e(TAG, "ScreenInternalAudioRecorder a recording is being done in parallel or stop is not called");
        }
        mAudioRecord.startRecording();
        if (mMic) mAudioRecordMic.startRecording();
        mCodec.start();
        if (mAudioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            throw new IllegalStateException("ScreenInternalAudioRecorder Audio recording failed to start");
        }
        mThread.start();
    }

    /**
     * end recording     */    public void end() {
        mAudioRecord.stop();
        if (mMic) {
            mAudioRecordMic.stop();
        }
        mAudioRecord.release();
        if (mMic) {
            mAudioRecordMic.release();
        }
        try {
            mThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mCodec.stop();
        mCodec.release();
        mMuxer.stop();
        mMuxer.release();
        mThread = null;
    }
}
