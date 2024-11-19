package com.screen.receive

import android.util.Log
import com.base.base.BaseApplication
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URI
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WebSocketReceiver {
    private const val port = 8282   //端口
    private lateinit var webSocketClient: WebSocketClient
    private var outputStream: OutputStream? = null
    private var tempFile: File? = null
    var onReceiveFileComplete: ((File) -> Unit)? = null
    fun init(ip: String) {
        webSocketClient = object : WebSocketClient(URI("ws://${ip}:${port}")) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.e("我是一条鱼：", "开启" )
                webSocketClient.send("我是一条鱼")
            }
            override fun onMessage(message: String?) {
                Log.i("我是一条鱼", "onMessage message = ${message}")
            }
            override fun onMessage(bytes: ByteBuffer) {
                val buf = ByteArray(bytes.remaining())
                Log.i("我是一条鱼", "onMessage bytes = ${String(buf)}")
//                val buf = ByteArray(bytes.remaining())
//                bytes[buf]
//                receive(buf)
            }
            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.e("我是一条鱼：", "远端关闭" )
            }
            override fun onError(ex: Exception?) {
                Log.e("我是一条鱼：", "异常断开，error:${ex}" )
            }
        }
        webSocketClient.connect()
    }

    fun send(msg: String) {
        Log.i("我是一条鱼", "send msg = $msg")
        webSocketClient.send(msg)
    }

    /**
     * 接收到的文件缓存3个，第一个用于播放，第二个用户缓存，第三个是下载；当下载好之后，删掉第一个，第二个用于播放，第三个用于缓存，开始下载第四个
     * 分别命名是0-1-2
     */
    private val sdf: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private fun receive(buf: ByteArray) {
        if (String(buf) == "#END#") {
            if (outputStream != null) {
                outputStream?.close()
                outputStream = null
                tempFile?.let { onReceiveFileComplete?.invoke(it) }
                tempFile = null
            }
            return
        }
        if (outputStream != null) {
            outputStream?.write(buf)
        } else {
            val fileDir = File(BaseApplication.getInstance().filesDir, "receive")
            if (!fileDir.exists()) {
                fileDir.mkdirs()
            }
            val file = File(fileDir, "${sdf.format(Date(System.currentTimeMillis()))}.ts")
            if (!file.exists()) {
                file.createNewFile()
            }
            outputStream = FileOutputStream(file)
            outputStream?.write(buf)
            tempFile = file
        }
    }

    fun release() {
        if (this::webSocketClient.isInitialized) {
            webSocketClient.closeBlocking()
        }
        if (outputStream != null) {
            outputStream?.close()
        }
    }
}