package com.zune.customtv

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.LinearInterpolator
import com.base.base.BaseConstant
import com.zune.customtv.bean.BaseDataBean
import java.util.Objects

object PlayActivityExt {

    @JvmStatic
    fun showLoading(ivLoading: View): ObjectAnimator {
        val oa = ObjectAnimator.ofFloat(ivLoading, "rotation", 0f, 360f)
        com.base.base.BaseApplication.getInstance().handler.post {
            ivLoading.visibility = View.VISIBLE
            oa.duration = 1000
            oa.repeatCount = ValueAnimator.INFINITE
            oa.interpolator = LinearInterpolator()
            oa.start()
        }
        return oa
    }

    @JvmStatic
    fun hideLoading(ivLoading: View, oa: ObjectAnimator?) {
        com.base.base.BaseApplication.getInstance().handler.post {
            ivLoading.visibility = View.GONE
            oa?.cancel()
        }
    }

    @JvmStatic
    fun findTitleByUrl(url: String): String {
        for (dataBean in NetDataManager.sBaseData) {
            val sb = StringBuilder(BaseConstant.URL_GET_MEDIA)
            if (dataBean.o?.keyParts?.pubSymbol != null) {
                sb.append("&pub=").append(dataBean.o?.keyParts?.pubSymbol)
            }
            sb.append("&track=").append(dataBean.o?.keyParts?.track)
            if (dataBean.o?.keyParts?.issueDate != null) {
                sb.append("&issue=").append(dataBean.o?.keyParts?.issueDate)
            }
            if (dataBean.o?.keyParts?.docID != null) {
                sb.append("&docid=").append(dataBean.o?.keyParts?.docID)
            }
            sb.append("&fileformat=").append("mp4%2Cm4v")
            if (Objects.equals(url, sb.toString())) {
                return dataBean.o?.title?:""
            }
        }
        return ""
    }
}