/**
 * Audio sources  */
enum class ScreenRecordingAudioSource {
    NONE,  //没有系统声音、没有麦克风
    INTERNAL,  //有系统声音、没有麦克风
    MIC,  //没有系统声音、有麦克风
    MIC_AND_INTERNAL //有系统声音、有麦克风
}