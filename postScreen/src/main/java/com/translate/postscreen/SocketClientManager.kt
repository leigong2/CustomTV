package com.translate.postscreen

import android.view.Surface
import java.net.URI
import java.net.URISyntaxException

class SocketClientManager : ScreenSocketClient.SocketCallback {
    private lateinit var mScreenDecoder: ScreenDecoder
    private lateinit var mSocketClient: ScreenSocketClient
    fun start(ip: String, surface: Surface?) {
        mScreenDecoder = ScreenDecoder()
        mScreenDecoder.startDecode(surface)
        try {
            // 需要修改为服务端的IP地址与端口
            val uri = URI("ws://${ip}:" + SOCKET_PORT)
            mSocketClient = ScreenSocketClient(this, uri)
            mSocketClient.connect()
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    fun stop() {
        mSocketClient.close()
        mScreenDecoder.stopDecode()
    }

    override fun onReceiveData(data: ByteArray?) {
        if (data != null) {
            mScreenDecoder.decodeData(data)
        }
    }

    companion object {
        private const val SOCKET_PORT = 50000
    }
}