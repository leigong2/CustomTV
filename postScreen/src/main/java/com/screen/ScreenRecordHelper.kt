package com.screen

import ScreenRecordingAudioSource
import Settings
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import com.base.base.BaseApplication
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenRecordHelper constructor(
    private var context: Context,
    private val listener: OnVideoRecordListener?,
    data: Intent
) {
    private val settings: Settings by lazy { Settings.getInstance(context) }
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaRecorder: MediaRecorder? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private val displayMetrics: DisplayMetrics = context.resources.displayMetrics
    private var saveFile: File? = null
    private var fileName: String? = null
    private var audioFile: File? = null
    private var mAudio: ScreenInternalAudioRecorder? = null
    var source: ScreenRecordingAudioSource? = null

    init {
        Log.d(TAG, "init: com.screen.ScreenRecordHelper")
        mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        mediaProjection = mediaProjectionManager?.getMediaProjection(RESULT_OK, data)
        mAudio = ScreenInternalAudioRecorder(mediaProjection, true)
    }

    fun setUpAudioRecorder(mic: Boolean) {
        mAudio = ScreenInternalAudioRecorder(mediaProjection, mic)
    }

    fun startRecord(source: ScreenRecordingAudioSource) {
        this.source = source
        try {
            if (mediaProjectionManager == null) {
                Log.d(TAG, "mediaProjectionManager == null，当前装置不支持录屏")
                showToast("not_support")
                return
            }
            if (source == ScreenRecordingAudioSource.NONE) {
                Log.d(TAG, "startRecord: ScreenRecordingAudioSource.NONE")
                if (initRecorder(false)) {
                    mediaRecorder?.start()
                    listener?.onStartRecord()
                } else {
                    showToast("not_support")
                }
            } else if (source == ScreenRecordingAudioSource.MIC) {
                Log.d(TAG, "startRecord: ScreenRecordingAudioSource.MIC")
                if (initRecorder(true)) {
                    mediaRecorder?.start()
                    listener?.onStartRecord()
                } else {
                    showToast("not_support")
                }
            } else if (source == ScreenRecordingAudioSource.MIC_AND_INTERNAL) {
                Log.d(TAG, "startRecord: ScreenRecordingAudioSource.MIC_AND_INTERNAL")
                audioFile = File.createTempFile("temp", ".aac", context.cacheDir)
                mAudio?.setupSimple(audioFile?.absolutePath, true)
                if (initRecorder(false)) {
                    mediaRecorder?.start()
                    mAudio?.start()
                    listener?.onStartRecord()
                } else {
                    showToast("not_support")
                }
            } else if (source == ScreenRecordingAudioSource.INTERNAL) {
                audioFile = File.createTempFile("temp", ".aac", context.cacheDir)
                mAudio?.setupSimple(audioFile?.absolutePath, false)
                if (initRecorder(false)) {
                    mediaRecorder?.start()
                    mAudio?.start()
                    listener?.onStartRecord()
                } else {
                    showToast("not_support")
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "startRecord:error $e")
        }
    }

    fun restartRecord() {
        stopRecord()
    }

    private fun showToast(resId: String) {
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
    }

    /**
     * if you has parameters, the recordAudio will be invalid
     * 释放资源
     */
    fun stopRecord() {
        try {
            mediaRecorder?.apply {
                stop()
            }
        } catch (e: Exception) {
            Log.e(TAG, "stopRecorder() error！${e.message}")
        } finally {
            mediaRecorder?.reset()
            listener?.onEndRecord()
            if (source == ScreenRecordingAudioSource.MIC_AND_INTERNAL || source == ScreenRecordingAudioSource.INTERNAL) {
                mAudio?.end()
            }
        }
    }

    private fun getFormatTime(time: Long): String? {
        val format = SimpleDateFormat("yyyyMMddHHMMSS", Locale.getDefault())
        val d1 = Date(time)
        return format.format(d1)
    }

    fun saveFile(source: ScreenRecordingAudioSource, callBack: CallBack?) {
        Thread {
            val externalFilesDir = BaseApplication.getInstance().getExternalFilesDir("")
            val file = File(externalFilesDir, "record")
            if (!file.exists()) {
                file.mkdirs()
            }
            val newFile = File(file, "${file.listFiles()?.size?:0}.ts")
            if (source == ScreenRecordingAudioSource.MIC_AND_INTERNAL || source == ScreenRecordingAudioSource.INTERNAL) {
                if (saveFile != null) {
                    val mMuxer = ScreenRecordingMuxer(
                        MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
                        newFile.absolutePath,
                        saveFile?.absolutePath,
                        audioFile?.absolutePath
                    )
                    mMuxer.mux()
                    saveFile?.delete()
                    audioFile?.delete()
                    saveFile = null
                    audioFile = null
                }
            } else if (source == ScreenRecordingAudioSource.NONE || source == ScreenRecordingAudioSource.MIC) {
                saveFile?.renameTo(newFile)
            }
            callBack?.startFileCommand(newFile.absolutePath)
        }.start()

    }

    interface CallBack {
        fun startFileCommand(path: String)
    }


    /**
     * 初始化录屏参数
     */
    private fun initRecorder(isMic: Boolean): Boolean {
        Log.d(TAG, "initRecorder")
        var result = true
        val f = File(settings.getPathData())
        if (!f.exists()) {
            f.mkdir()
        }
        fileName = getFormatTime(System.currentTimeMillis())
        saveFile = File(settings.getPathData(), "ScreenRecord_${fileName}.tmp")
        saveFile?.apply {
            if (exists()) {
                delete()
            }
        }
        mediaRecorder = MediaRecorder()
        val width = getVideoSizeWidth()
        val height = getVideoSizeHeight()

        mediaRecorder?.apply {

            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)

            if (isMic && source == ScreenRecordingAudioSource.MIC) {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
                setAudioChannels(1)
                setAudioEncodingBitRate(44100)
                setAudioSamplingRate(192000)
            }

            setOutputFile(saveFile!!.absolutePath)
            setVideoSize(width, height)
            setRecorderResolution(settings.getResolutionData())
            setCaptureRate(VIDEO_CAPTURE_RATE)
            setVideoFrameRate(VIDEO_FRAME_RATE)
            try {
                prepare()
                virtualDisplay = mediaProjection?.createVirtualDisplay(
                    "MainScreen", width, height, displayMetrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, surface, null, null
                )
            } catch (e: Exception) {
                Log.e(TAG, "IllegalStateException preparing MediaRecorder: ${e.message}")
                e.printStackTrace()
                result = false
            }
        }
        return result
    }

    /**
     * 退出Service释放资源
     */
    fun clearAll() {
        mediaRecorder?.release()
        mediaRecorder = null
        virtualDisplay?.release()
        virtualDisplay = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    private fun getVideoSizeWidth(): Int {
        if (settings.getResolutionData() == Settings.RESOLUTION_1920_1080) {
            return VIDEO_SIZE_MAX_WIDTH_1920
        } else if (settings.getResolutionData() == Settings.RESOLUTION_1280_720) {
            return VIDEO_SIZE_MAX_WIDTH_1280
        }
        return VIDEO_SIZE_MAX_WIDTH_1280
    }

    private fun getVideoSizeHeight(): Int {
        if (settings.getResolutionData() == Settings.RESOLUTION_1920_1080) {
            return VIDEO_SIZE_MAX_HEIGHT_1080
        } else if (settings.getResolutionData() == Settings.RESOLUTION_1280_720) {
            return VIDEO_SIZE_MAX_HEIGHT_720
        }
        return VIDEO_SIZE_MAX_HEIGHT_720
    }

    /**
     * 设置录屏分辨率
     */
    private fun setRecorderResolution(string: String) {
        if (string == Settings.RESOLUTION_1920_1080) {
            mediaRecorder?.setVideoEncodingBitRate(VIDEO_TIMES * VIDEO_SIZE_MAX_WIDTH_1920 * VIDEO_SIZE_MAX_HEIGHT_1080)
        } else {
            mediaRecorder?.setVideoEncodingBitRate(VIDEO_TIMES * VIDEO_SIZE_MAX_WIDTH_1280 * VIDEO_SIZE_MAX_HEIGHT_720)
        }
    }


    companion object {
        private const val VIDEO_TIMES = 5
        private const val VIDEO_CAPTURE_RATE = 30.0
        private const val VIDEO_FRAME_RATE = 30
        private const val VIDEO_SIZE_MAX_WIDTH_1920 = 1920
        private const val VIDEO_SIZE_MAX_HEIGHT_1080 = 1080
        private const val VIDEO_SIZE_MAX_WIDTH_1280 = 1280
        private const val VIDEO_SIZE_MAX_HEIGHT_720 = 720
        private const val TAG = "zuneRecord"
    }

    interface OnVideoRecordListener {
        fun onBeforeRecord()
        fun onStartRecord()
        fun onPauseRecord()
        fun onCancelRecord()
        fun onEndRecord()
    }
}
