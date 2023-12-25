package com.zune.customtv.fragment
import android.view.View
import android.widget.EditText
import com.base.base.BaseFragment
import com.activity.TouPingPostActivity
import com.zune.customtv.R
import com.activity.TouPingReceiveActivity

class TouPingFragment: BaseFragment() {

    override fun getLayoutId(): Int {
        return R.layout.fragment_touping
    }

    override fun initView(view: View?) {
        val editText = view?.findViewById<EditText>(R.id.setIp)?:return
        editText.setText("192.168.1.6")
        editText.onFocusChangeListener =
            View.OnFocusChangeListener { v, hasFocus -> v?.setBackgroundResource(if (hasFocus) R.drawable.bg_select else R.drawable.bg_normal) }
        view.findViewById<View>(R.id.post)?.onFocusChangeListener =
            View.OnFocusChangeListener { v, hasFocus -> v?.setBackgroundResource(if (hasFocus) R.drawable.bg_select else R.drawable.bg_normal) }
        view.findViewById<View>(R.id.receive)?.onFocusChangeListener =
            View.OnFocusChangeListener { v, hasFocus -> v?.setBackgroundResource(if (hasFocus) R.drawable.bg_select else R.drawable.bg_normal) }
        view.findViewById<View>(R.id.post)?.setOnClickListener {
            TouPingPostActivity.start(requireContext())
        }
        view.findViewById<View>(R.id.receive)?.setOnClickListener {
            TouPingReceiveActivity.start(editText.text.toString(), requireContext())
        }
    }
}