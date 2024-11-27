package com.customtv.webplay

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.base.base.BaseActivity
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class WebListActivity : BaseActivity() {

    companion object {
        fun start(context: Context, path: String) {
            val intent = Intent(context, WebListActivity::class.java)
            intent.putExtra("path", path)
            context.startActivity(intent)
        }
    }

    private val mData: MutableList<JSONObject> = arrayListOf()
    override fun getLayoutId(): Int {
        return R.layout.activity_web_list
    }

    override fun initView() {
        findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = GridLayoutManager(this@WebListActivity, 4, RecyclerView.VERTICAL, false)
            adapter = object : Adapter<ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                    val viewHolder: ViewHolder = object : ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_home, parent, false)) {}
                    viewHolder.itemView.onFocusChangeListener =
                        View.OnFocusChangeListener { v: View, hasFocus: Boolean -> v.setBackgroundResource(if (hasFocus) R.drawable.bg_select else R.drawable.bg_normal) }
                    viewHolder.itemView.setOnClickListener { v: View ->
                        val position = v.tag as Int
                        val jsonObject: JSONObject = mData[position]
                        val url = jsonObject.getString("url")
                        WebViewPlayActivity.start(this@WebListActivity, url = url)
                    }
                    return viewHolder
                }

                override fun getItemCount(): Int {
                    return mData.size
                }

                override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                    holder.itemView.tag = position
                    val imageView = holder.itemView.findViewById<ImageView>(R.id.image_view)
                    val textName = holder.itemView.findViewById<TextView>(R.id.text_name)
                    val duration = holder.itemView.findViewById<TextView>(R.id.duration)
                    val jsonObject: JSONObject = mData[position]
                    val brief = jsonObject.getString("brief")
                    val img = jsonObject.getString("img")
                    val title = jsonObject.getString("title")
                    Glide.with(imageView).load("https:${img}").centerCrop().into(imageView)
                    textName.text = title
                    duration.text = brief
                }
            }
        }
        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val sb = StringBuilder()
                val inputStream = resources.assets.open(intent.getStringExtra("path")!!)
                val data = ByteArray(1024)
                var offset: Int
                while (inputStream.read(data).also { offset = it } > -1) {
                    val temp = String(data, 0, offset)
                    sb.append(temp)
                }
                val jsonArray = JSONArray(sb.toString())
                for (i in 0 until jsonArray.length()) {
                    mData.add(jsonArray[i] as JSONObject)
                }
            }
            findViewById<RecyclerView>(R.id.recyclerView).apply {
                adapter?.notifyDataSetChanged()
            }
        }
    }
}