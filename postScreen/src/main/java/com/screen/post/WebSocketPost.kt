package com.screen.post

import android.util.Log
import com.base.base.BaseApplication
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.io.File
import java.io.FileInputStream
import java.net.InetSocketAddress

object WebSocketPost {
    private const val port = 40000   //端口
    private lateinit var webSocketServer: WebSocketServer
    private var webSocket: WebSocket? = null
    fun init() {
        val inetSocketAddress = InetSocketAddress(port)
        webSocketServer = object : WebSocketServer(inetSocketAddress) {
            override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
                Log.e("我是一条鱼：", "有人连接进来了ip:${conn?.remoteSocketAddress}" )
                conn?.apply { webSocket = this }
            }

            override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
                Log.e("我是一条鱼：", "远端关闭" )
                webSocket  = null
            }

            override fun onMessage(conn: WebSocket?, message: String?) {
            }
            override fun onError(conn: WebSocket?, ex: Exception?) {
                Log.e("我是一条鱼：", "异常断开，error:${ex}" )
            }
            override fun onStart() {
                Log.e("我是一条鱼：", "远端启动" )
            }
        }
        Log.e("我是一条鱼：", "开启远端服务ip:${inetSocketAddress}" )
        webSocketServer.start()
    }

    fun post(path: String) {
        val fileInputStream = FileInputStream(File(path))
        while (true) {
            val bytes = ByteArray(1024)
            val len = fileInputStream.read(bytes)
            if (len == -1) {
                break
            }
            sendMsg(bytes)
        }
        fileInputStream.close()
        File(path).delete()
        sendMsg("#END#".toByteArray())
    }

    private fun sendMsg(bytes: ByteArray) {
        webSocket?.send(bytes)
    }

    fun stopConnect() {
        if (this::webSocketServer.isInitialized) {
            webSocketServer?.stop()
        }
    }
}