package com.gfq.refreshview

import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.scwang.smart.refresh.layout.api.RefreshLayout

/**
 *  2021/5/10 11:07
 * @auth gaofuq
 * @description
 */
class RefreshView<DataBean>(context: Context) : FrameLayout(context), LifecycleObserver {
    constructor(context: Context, itemLayoutRes: Int, brId: Int = 0) : this(context) {
        adapter = object : BaseRVAdapter<DataBean>(itemLayoutRes, brId) {
            override fun onBindView(holder: BaseVH, data: DataBean, position: Int) {
                bindItemView?.invoke(holder, data, position)
            }
        }
        recyclerView.adapter = adapter
    }

    lateinit var adapter: BaseRVAdapter<DataBean>

    private var curPage = 1
    private var pageSize = 10
    private var pageCount = 100


    val recyclerView = RecyclerView(context)
    val smartRefreshLayout = SmartRefreshLayout(context)
    var bindItemView: ((BaseVH, DataBean, Int) -> Unit?)? = null

    var layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)

    var netLoseView: View? = null
    var dataEmptyView: View? = null
    var errorView: View? = null

    var isAutoRefreshOnCreate = true
    var isAutoRefreshOnResume = false

    var requestData: ((curPage: Int, pageSize: Int) -> MutableList<DataBean>?)? = null

    private val statusViewContainer = FrameLayout(context)


    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun autoRefreshOnResume() {
        Log.e("xx", "autoRefreshOnResume")
        if (isAutoRefreshOnResume) {
            callRefresh(smartRefreshLayout)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun autoRefreshOnCreate() {
        Log.e("xx", "autoRefreshOnCreate")
        if (isAutoRefreshOnCreate) {
            callRefresh(smartRefreshLayout)
        }
    }


    init {
        if (context is ComponentActivity) {
            context.lifecycle.addObserver(this)
        } else if (context is Fragment) {
            context.lifecycle.addObserver(this)
        }

        statusViewContainer.addView(
            recyclerView,
            FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        )
        smartRefreshLayout.addView(
            statusViewContainer,
            SmartRefreshLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        )
        this.addView(smartRefreshLayout, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))


        recyclerView.layoutManager = layoutManager
//        adapter.dataList =

        smartRefreshLayout.run {
            setRefreshHeader(MaterialHeader(context))
            setRefreshFooter(ClassicsFooter(context))
            setOnRefreshListener {
                callRefresh(it)
            }

            setOnLoadMoreListener {
                callLoadMore(it)
            }
        }

    }

    private fun callLoadMore(refreshLayout: RefreshLayout) {
        if (isNetworkConnected(context)) {
            removeNetLoseView()
            doLoadMore(refreshLayout)
        } else {
            refreshLayout.finishLoadMore(false)
            addNetLoseView()
        }
    }

    private fun callRefresh(refreshLayout: RefreshLayout) {
        Log.e("xx", "OnRefreshListener")
        if (isNetworkConnected(context)) {
            removeNetLoseView()
            doRefresh(refreshLayout)
        } else {
            refreshLayout.finishRefresh(false)
            addNetLoseView()
        }
    }

    private fun doLoadMore(refreshLayout: RefreshLayout) {
        curPage++
        refreshLayout.autoLoadMore()
        if (curPage > pageCount) {
            curPage = pageCount
            refreshLayout.finishLoadMoreWithNoMoreData()
            return
        }
        if (requestData == null) return
        val dataList = requestData!!.invoke(curPage, pageSize)
        if (dataList.isNullOrEmpty()) {
            refreshLayout.finishLoadMore(false)
        } else {
            adapter.addAll(dataList)
            refreshLayout.finishLoadMore(true)
        }
    }

    private fun doRefresh(refreshLayout: RefreshLayout) {
        curPage = 1
        refreshLayout.autoRefresh()
        if (requestData == null) return
        val dataList = requestData!!.invoke(curPage, pageSize)
        if (dataList.isNullOrEmpty()) {
            addDataEmptyView()
            refreshLayout.finishRefresh(false)
        } else {
            removeDataEmptyView()
            adapter.dataList = dataList
            refreshLayout.finishRefresh(true)
        }
    }

    private fun removeDataEmptyView() {
        if (dataEmptyView == null) {
            return
        }
        val parent = dataEmptyView!!.parent
        if (parent != null) {
            statusViewContainer. removeView(dataEmptyView)
        }
    }

    private fun addDataEmptyView() {
        if (dataEmptyView == null) {
            dataEmptyView = TextView(context).apply {
                setBackgroundColor(Color.WHITE)
                text = "一点数据也没有"
                layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
                gravity = Gravity.CENTER
            }
        }
        val parent = dataEmptyView!!.parent
        if (parent == null) {
            statusViewContainer.addView(dataEmptyView)
        }
    }

    private fun addNetLoseView() {
        if (netLoseView == null) {
            netLoseView = TextView(context).apply {
                setBackgroundColor(Color.WHITE)
                text = "请检查网络"
                layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
                gravity = Gravity.CENTER
            }
        }
        val parent = netLoseView!!.parent
        if (parent == null) {
            statusViewContainer.addView(netLoseView)
        }
    }

    private fun removeNetLoseView() {
        if (netLoseView == null) {
            return
        }
        val parent = netLoseView!!.parent
        if (parent != null) {
            statusViewContainer.removeView(netLoseView)
        }

    }

    private fun isNetworkConnected(context: Context?): Boolean {
        if (context != null) {
            val mConnectivityManager =
                context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            var mNetworkInfo: NetworkInfo? = null
            if (mConnectivityManager != null) {
                mNetworkInfo = mConnectivityManager.activeNetworkInfo
            }
            //获取连接对象
            if (mNetworkInfo != null) {
                //判断是TYPE_MOBILE网络
                if (ConnectivityManager.TYPE_MOBILE == mNetworkInfo.type) {
//                    LogManager.i("AppNetworkMgr", "网络连接类型为：TYPE_MOBILE");
                    //判断移动网络连接状态
                    val STATE_MOBILE =
                        mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)!!
                            .state
                    if (STATE_MOBILE == NetworkInfo.State.CONNECTED) {
//                        LogManager.i("AppNetworkMgrd", "网络连接类型为：TYPE_MOBILE, 网络连接状态CONNECTED成功！");
                        return mNetworkInfo.isAvailable
                    }
                }
                //判断是TYPE_WIFI网络
                if (ConnectivityManager.TYPE_WIFI == mNetworkInfo.type) {
//                    LogManager.i("AppNetworkMgr", "网络连接类型为：TYPE_WIFI");
                    //判断WIFI网络状态
                    val STATE_WIFI =
                        mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)!!
                            .state
                    if (STATE_WIFI == NetworkInfo.State.CONNECTED) {
//                        LogManager.i("AppNetworkMgr", "网络连接类型为：TYPE_WIFI, 网络连接状态CONNECTED成功！");
                        return mNetworkInfo.isAvailable
                    }
                }
            }
        }
        return false
    }
}