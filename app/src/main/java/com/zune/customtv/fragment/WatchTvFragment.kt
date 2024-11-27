package com.zune.customtv.fragment

import android.annotation.SuppressLint
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
import com.customtv.webplay.WebConstant
import com.customtv.webplay.WebListActivity
import com.customtv.webplay.WebViewPlayActivity
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.zune.customtv.R
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

    private var mLastPlayPosition = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val inflate: View = inflater.inflate(getLayoutId(), container, false)
        mLastPlayPosition = SpUtil.getInstance().getIntValue("LastPlayPosition", -1)
        initData()
        initView(inflate)
        return inflate
    }

    private fun initData() {
        for ((index, liveUrl) in WebConstant.liveUrls.withIndex()) {
            mData.add(TvBean(name = WebConstant.channelNames[index], arrayListOf<UrlBean>().apply { add(UrlBean(url = liveUrl, timeout = 0)) }))
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

                override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
                    val textView = holder.itemView.findViewById<TextView>(R.id.textView)
                    val itemData = mData[position]
                    textView.text = itemData.name
                    holder.itemView.onFocusChangeListener = View.OnFocusChangeListener { v: View, hasFocus: Boolean ->
                        v.setBackgroundResource(if (hasFocus) R.drawable.bg_select else R.drawable.bg_normal)
                    }
                    holder.itemView.setOnClickListener {
                        mLastPlayPosition = position
                        SpUtil.getInstance().setIntValue("LastPlayPosition", position)
                        context?.let {
                            if (mData[mLastPlayPosition].urls[0].url.endsWith(".txt")) {
                                WebListActivity.start(it, mData[mLastPlayPosition].urls[0].url)
                            } else {
                                WebViewPlayActivity.start(it, mLastPlayPosition)
                            }
                        }
                    }
                }
            }
        }
        if (mLastPlayPosition >= 0) {
            lifecycleScope.launch {
                delay(300)
                context?.let {
                    if (mData[mLastPlayPosition].urls[0].url.endsWith(".txt")) {
                        WebListActivity.start(it, mData[mLastPlayPosition].urls[0].url)
                    } else {
                        WebViewPlayActivity.start(it, mLastPlayPosition)
                    }
                }
            }
        }
    }

    private fun getLayoutId(): Int {
        return R.layout.fragment_watch_tv
    }


    @Deprecated("这个太卡了，先不用了")
    private fun initDataDeprecated() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val file = File(BaseApplication.getInstance().getExternalFilesDir(""), "tvSuccessfulLive.json");
                val sb = StringBuilder()
                if (!file.exists()) {
                    val inputStream = resources.assets.open("tvSuccessfulLive.json")
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
                val list = GsonBuilder().create().fromJson<MutableList<MutableMap<String, MutableList<MutableMap<String, Any>>>>>(
                    sb.toString(),
                    object : TypeToken<MutableList<MutableMap<String, MutableList<MutableMap<String, Any>>>>>() {}.type
                )
                val resultList = mergeList(list)
                for (value in resultList) {
                    for (key in value.keys) {
                        if (isFilter(key)) {
                            continue
                        }
                        val mutableList = value[key]
                        val urlBeans = ArrayList<UrlBean>()
                        for (urls in (mutableList ?: mutableListOf())) {
                            val url = urls["url"]?.toString() ?: ""
                            val timeout = urls["timeout"]?.toString()?.toDouble()?.toInt() ?: 0
                            val urlBean = UrlBean(url, timeout)
                            urlBeans.add(urlBean)
                        }
                        if (urlBeans.isEmpty()) {
                            continue
                        }
                        val tvBean = TvBean(name = (if (key.contains("宁卫视")) "辽宁卫视" else key), urls = urlBeans)
                        mData.add(tvBean)
                    }
                }
            }
            mData.sortWith(Comparator { o1, o2 ->
                if (o1.name.contains("CCTV") && !o2.name.contains("CCTV")) {
                    return@Comparator -1
                }
                if (!o1.name.contains("CCTV") && o2.name.contains("CCTV")) {
                    return@Comparator 1
                }
                if (o1.name.contains("CCTV") && o2.name.contains("CCTV")) {
                    val sbO1 = StringBuilder()
                    for (char1 in o1.name) {
                        if (char1 in '0'..'9') {
                            sbO1.append(char1)
                        }
                    }
                    val sbO2 = StringBuilder()
                    for (char2 in o2.name) {
                        if (char2 in '0'..'9') {
                            sbO2.append(char2)
                        }
                    }
                    try {
                        return@Comparator sbO1.toString().toInt() - sbO2.toString().toInt()
                    } catch (ignore: Exception) {
                    }
                }
                return@Comparator o1.name.compareTo(o2.name)
            })
            view?.findViewById<RecyclerView>(R.id.recyclerView)?.adapter?.notifyDataSetChanged()
        }
    }

    private fun isFilter(key: String): Boolean {
        if (key.contains("伴音") || key.contains("Q群") || key.contains("听电视") || key.contains("广播") || key.contains("FM") || key.contains("车道")) {
            return true
        }
        if (!key.contains("CCTV") && !key.contains("卫视") && !key.contains("卡通") && !key.contains("卡通") && !key.contains("河南") && !key.contains("郑州")) {
            return true
        }
        return false
    }

    private fun mergeList(list: MutableList<MutableMap<String, MutableList<MutableMap<String, Any>>>>): MutableList<MutableMap<String, MutableList<MutableMap<String, Any>>>> {
        val result: MutableList<MutableMap<String, MutableList<MutableMap<String, Any>>>> = ArrayList()
        val resultKey = ArrayList<String>()
        for (value in list) {
            for (key in value.keys) {
                if (isFilter(key)) {
                    continue
                }
                val valueValue = value[key] ?: arrayListOf()
                if (resultKey.contains(key.replace("19201080", ""))) {
                    result.forEach for1@{ valueResult ->
                        for (keyResult in valueResult.keys) {
                            if (key.replace("19201080", "") == keyResult.replace("19201080", "")) {
                                val urls = valueResult[keyResult] ?: arrayListOf()
                                valueValue.addAll(urls)
                                return@for1
                            }
                        }
                    }
                    continue
                }
                val hashMap = HashMap<String, MutableList<MutableMap<String, Any>>>()
                hashMap[key.replace("19201080", "")] = valueValue
                resultKey.add(key.replace("19201080", ""))
                result.add(hashMap)
            }
        }
        return result
    }

    private suspend fun saveToLocal(sb: StringBuilder) {
        withContext(Dispatchers.IO) {
            val file = File(BaseApplication.getInstance().getExternalFilesDir(""), "tvSuccessfulLive.json");
            val fw = FileWriter(file)
            fw.write(sb.toString())
            fw.close()
        }
    }

    private suspend fun getFromLocal(): String {
        return withContext(Dispatchers.IO) {
            val file = File(BaseApplication.getInstance().getExternalFilesDir(""), "tvSuccessfulLive.json");
            val fr = FileReader(file)
            val result = fr.readText()
            fr.close()
            result
        }
    }
}