import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import com.base.base.BaseApplication
import java.io.File

class Settings(context: Context?) {

    companion object {
        private lateinit var settings: SharedPreferences
        private const val DATA = "screen_record_settings"
        private const val WARNING_DONT_SHOW= "warning_dont_show"
        private const val RESOLUTION_DATA= "resolution_data"
        private const val SAVE_PATH= "save_path"
        private const val VIDEO_SET= "video_set"
        private const val SYSTEM_VOLUME= "system_volume"
        private const val MIC= "mic"
        private const val AUDIO_SET= "audio_set"
        private const val SERVICE_RUNNING = "service_running"
        private const val SP_KEY_FILE_SERIAL_NUMBER = "sp_key_file_serial_number"

        private val DEFAULT_SAVE_PATH = BaseApplication.getInstance().filesDir.path+"/Screen Record"
        private var instance : Settings? = null

        fun  getInstance(context: Context): Settings {
            if (instance == null)  // NOT thread safe!  
                instance = Settings(context)

            return instance!!
        }

        //The unit is MB  
        const val LOW_SPACE_STANDARD :Long = 1024
        const val CANT_RECORD_STANDARD :Long = 200
        const val RESOLUTION_1280_720 = "1280x720p"
        const val RESOLUTION_1920_1080 = "1920x1080p"
        private const val TAG="Settings"
    }

    var fileName: String = ""

    init {
        Log.d(TAG, "Settings: init")
        settings = context!!.getSharedPreferences(DATA, 0)
    }

    fun getSystemAudio(): Boolean {
        return settings.getBoolean(SYSTEM_VOLUME, true)
    }

    fun setSystemAudio(boolean: Boolean){
        settings.edit()
            .putBoolean(SYSTEM_VOLUME, boolean)
            .apply()
    }

    fun getMic(): Boolean {
        return settings.getBoolean(MIC, false)
    }

    fun setMic(boolean: Boolean){
        settings.edit()
            .putBoolean(MIC, boolean)
            .apply()
    }

    fun saveWarningData(boolean: Boolean) {
        settings.edit()
            .putBoolean(WARNING_DONT_SHOW, boolean)
            .apply()
    }

    fun getWarningData():Boolean {
        return settings.getBoolean(WARNING_DONT_SHOW, false)
    }

    fun savePathData(string: String) {
        settings.edit()
            .putString(SAVE_PATH, string)
            .apply()
    }

    fun getPathData():String {
        return settings.getString(SAVE_PATH, DEFAULT_SAVE_PATH)!!
    }

    fun setRunningState(b:Boolean) {
        settings.edit()
            .putBoolean(SERVICE_RUNNING, b)
            .apply()
    }

    fun getRunningState():Boolean {
        return settings.getBoolean(SERVICE_RUNNING, false)
    }

    fun saveResolutionData(string: String) {
        settings.edit()
            .putString(RESOLUTION_DATA, string)
            .apply()
    }

    fun getResolutionData():String {
        return settings.getString(RESOLUTION_DATA, RESOLUTION_1920_1080)!!
    }

    //return unit is MB  
    fun getRemainSpace(): Long {
        val external: File = BaseApplication.getInstance().filesDir
        return external.freeSpace / 1000000
    }
}
