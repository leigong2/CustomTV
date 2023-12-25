package com.translate.postscreen

import android.media.projection.MediaProjection
import java.net.InetSocketAddress

class SocketManager {
    private val mScreenSocketServer: ScreenSocketServer = ScreenSocketServer(InetSocketAddress(SOCKET_PORT))
    private val mRecordSocketServer: RecordSocketServer = RecordSocketServer(InetSocketAddress(SOCKET_PORT))
    private lateinit var mScreenEncoder: ScreenEncoder
    private lateinit var mRecordEncoder: ScreenEncoder

    fun start(mediaProjection: MediaProjection) {
        mScreenSocketServer.start()
        mScreenEncoder = ScreenEncoder(this, mediaProjection)
        mScreenEncoder.startScreenEncode()

//        mRecordSocketServer.start()
//        mRecordEncoder = ScreenEncoder(this, mediaProjection)
//        mRecordEncoder.startRecordEncode()
    }

    fun close() {
        try {
            mScreenSocketServer.stop()
            mScreenSocketServer.close()
//            mRecordSocketServer.stop()
//            mRecordSocketServer.close()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        mScreenEncoder.stopEncode()
//        mRecordEncoder.stopEncode()
    }

    fun sendData(bytes: ByteArray?, isVideo: Boolean) {
        if (isVideo) {
            mScreenSocketServer.sendData(bytes)
        } else {
//            mRecordSocketServer.sendData(bytes)
        }
    }

    companion object {
        private const val SOCKET_PORT = 50000
    }
}