package com.zune.customtv.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.activity.TouPingPostActivity
import com.activity.TouPingReceiveActivity
import com.base.base.BaseFragment
import com.zune.customtv.R
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections


class TouPingFragment : BaseFragment() {

    override fun getLayoutId(): Int {
        return R.layout.fragment_touping
    }

    override fun initView(view: View?) {
        val editText = view?.findViewById<EditText>(R.id.setIp) ?: return
        val ipAddress = getIPAddress()
        editText.setText(ipAddress ?: "192.168.1.3")
        editText.onFocusChangeListener =
            View.OnFocusChangeListener { v, hasFocus -> v?.setBackgroundResource(if (hasFocus) R.drawable.bg_select else R.drawable.bg_normal) }
        view.findViewById<View>(R.id.post)?.onFocusChangeListener =
            View.OnFocusChangeListener { v, hasFocus -> v?.setBackgroundResource(if (hasFocus) R.drawable.bg_select else R.drawable.bg_normal) }
        view.findViewById<View>(R.id.receive)?.onFocusChangeListener =
            View.OnFocusChangeListener { v, hasFocus -> v?.setBackgroundResource(if (hasFocus) R.drawable.bg_select else R.drawable.bg_normal) }
        view.findViewById<View>(R.id.post)?.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 23) {
                val permission = ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.RECORD_AUDIO
                )
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf<String>(Manifest.permission.RECORD_AUDIO),
                        1
                    )
                    return@setOnClickListener
                }
            }
            TouPingPostActivity.start(requireContext())
        }
        view.findViewById<View>(R.id.receive)?.setOnClickListener {
            TouPingReceiveActivity.start(editText.text.toString(), requireContext())
        }
    }

    private fun getIPAddress(): String? {
        try {
            val interfaces: List<NetworkInterface> = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs: List<InetAddress> = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.getHostAddress()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("getIPAddress", e.toString())
        }
        return ""
    }
}