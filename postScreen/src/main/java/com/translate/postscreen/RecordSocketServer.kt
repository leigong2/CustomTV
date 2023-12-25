package com.translate.postscreen

import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress

class RecordSocketServer(inetSocketAddress: InetSocketAddress?) :
    WebSocketServer(inetSocketAddress) {
    private val TAG = RecordSocketServer::class.java.simpleName
    private lateinit var mWebSocket: WebSocket
    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        Log.d(TAG, "onOpen")
        mWebSocket = conn
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        Log.d(TAG, "onClose:$reason")
    }

    override fun onMessage(conn: WebSocket, message: String) {}
    override fun onError(conn: WebSocket?, ex: Exception?) {
        Log.d(TAG, "onError:$ex")
    }

    override fun onStart() {
        Log.d(TAG, "onStart")
    }

    fun sendData(bytes: ByteArray?) {
        if (mWebSocket.isOpen) {
            // 通过WebSocket 发送数据
            mWebSocket.send(bytes)
        }
    }

    fun close() {
        mWebSocket.close()
    }
}