package com.zune.customtv.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.base.base.BaseApplication
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.zune.customtv.R
import com.zune.customtv.WatchTvActivity
import com.zune.customtv.bean.TvBean
import com.zune.customtv.bean.UrlBean
import com.zune.customtv.utils.SpUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class WatchTvFragment : Fragment() {

    companion object {
        val mData: ArrayList<TvBean> = ArrayList()
    }

    private var mLastPlayPosition = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val inflate: View = inflater.inflate(getLayoutId(), container, false)
        mLastPlayPosition = SpUtil.getInstance().getIntValue("LastPlayPosition", 0)
        initView(inflate)
        initData()
        return inflate
    }

    private fun initData() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val file = File(BaseApplication.getInstance().getExternalFilesDir(""), "tvLiveM3u8.json");
                val sb = StringBuilder()
                if (!file.exists()) {
                    val inputStream = resources.assets.open("tvLiveM3u8.json")
                    val data = ByteArray(1024)
                    var offset: Int
                    while (inputStream.read(data).also { offset = it } > -1) {
                        val temp = String(data, 0, offset)
                        sb.append(temp)
                    }
                    saveToLocal(sb)
                } else {
                    sb.append(getFromLocal())
                }
                val list = GsonBuilder().create().fromJson<MutableMap<String, MutableList<MutableMap<String, Any>>>>(
                    sb.toString(),
                    object : TypeToken<MutableMap<String, MutableList<MutableMap<String, String>>>>() {}.type
                )
                for (key in list.keys) {
                    val value = list[key] ?: arrayListOf()
                    val urlBeans = ArrayList<UrlBean>()
                    for (mutableMap in value) {
                        val url = mutableMap["url"]?.toString() ?: ""
                        val timeout = mutableMap["timeout"]?.toString()?.toInt() ?: 0
                        val urlBean = UrlBean(url, timeout)
                        urlBeans.add(urlBean)
                    }
                    val tvBean = TvBean(name = key, urls = urlBeans)
                    mData.add(tvBean)
                }
            }
            view?.findViewById<RecyclerView>(R.id.recyclerView)?.adapter?.notifyDataSetChanged()
            delay(300)
            context?.let { WatchTvActivity.start(it, mLastPlayPosition) }
        }
    }

    private suspend fun saveToLocal(sb: StringBuilder) {
        withContext(Dispatchers.IO) {
            val file = File(BaseApplication.getInstance().getExternalFilesDir(""), "tvLiveM3u8.json");
            val fw = FileWriter(file)
            fw.write(sb.toString())
            fw.close()
        }
    }

    private suspend fun getFromLocal() : String {
        return withContext(Dispatchers.IO) {
            val file = File(BaseApplication.getInstance().getExternalFilesDir(""), "tvLiveM3u8.json");
            val fr = FileReader(file)
            val result = fr.readText()
            fr.close()
            result
        }
    }

    private fun initView(view: View?) {
        view?.findViewById<RecyclerView>(R.id.recyclerView)?.apply {
            layoutManager = GridLayoutManager(context, 4, RecyclerView.VERTICAL, false)
            adapter = object : Adapter<ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                    return object : ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_watch_tv, parent, false)) {
                    }
                }

                override fun getItemCount(): Int {
                    return mData.size;
                }

                override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                    val textView = holder.itemView.findViewById<TextView>(R.id.textView)
                    val itemData = mData[position]
                    textView.text = itemData.name
                    holder.itemView.onFocusChangeListener = View.OnFocusChangeListener { v: View, hasFocus: Boolean ->
                        v.setBackgroundResource(if (hasFocus) R.drawable.bg_select else R.drawable.bg_normal)
                    }
                    holder.itemView.setOnClickListener {
                        WatchTvActivity.start(it.context, position)
                        SpUtil.getInstance().setIntValue("LastPlayPosition", position)
                    }
                }
            }
        }
    }

    private fun getLayoutId(): Int {
        return R.layout.fragment_watch_tv
    }
}