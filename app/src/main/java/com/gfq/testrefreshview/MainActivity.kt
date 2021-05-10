package com.gfq.testrefreshview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.gfq.refreshview.BaseVH
import com.gfq.refreshview.RefreshView
import com.gfq.testrefreshview.databinding.ItemBinding
import com.gfq.testrefreshview.databinding.NetLoseViewBinding
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)

        val refreshView = RefreshView<TestBean>(this, itemLayoutRes = R.layout.item)
//        f.addView(refreshView)
setContentView(refreshView)
        var count = 0;
        Log.e("xx","refreshView.run ")
        refreshView.run {
            dataEmptyView = DataBindingUtil.inflate(LayoutInflater.from(this@MainActivity),R.layout.date_empty_view,null,false)
//            netLoseView =
            val x = DataBindingUtil.inflate<NetLoseViewBinding>(LayoutInflater.from(this@MainActivity),R.layout.net_lose_view,null,false)
            netLoseView=x.root
            Log.e("xx","dataEmptyView netLoseView ")
            requestData = { curPage, pageSize ->
                count++
                val list = mutableListOf<TestBean>()
                if (count > 3) {
                    list.add(TestBean("张三", count))
                    list.add(TestBean("李四", count))
                }
                list
            }
            bindItemView = { holder: BaseVH, data: TestBean, position: Int ->
                val vhBinding = holder.vhBinding as ItemBinding
                with(vhBinding) {
                    tvName.text = data.name
                    tvAge.text = data.age.toString()
                }
            }
        }
    }
}