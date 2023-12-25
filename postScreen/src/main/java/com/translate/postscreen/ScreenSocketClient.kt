package com.translate.postscreen

import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer

class ScreenSocketClient(private val mSocketCallback: SocketCallback?, serverUri: URI?) :
    WebSocketClient(serverUri) {
    override fun onOpen(serverHandshake: ServerHandshake) {
        Log.d(TAG, "onOpen")
    }

    override fun onMessage(message: String) {}
    override fun onMessage(bytes: ByteBuffer) {
        val buf = ByteArray(bytes.remaining())
        bytes[buf]
        mSocketCallback?.onReceiveData(buf)
    }

    override fun onClose(code: Int, reason: String, remote: Boolean) {
        Log.d(TAG, "onClose =$reason")
    }

    override fun onError(ex: Exception) {
        Log.d(TAG, "onError =$ex")
    }

    interface SocketCallback {
        fun onReceiveData(data: ByteArray?)
    }

    companion object {
        private val TAG = ScreenSocketClient::class.java.simpleName
    }
}