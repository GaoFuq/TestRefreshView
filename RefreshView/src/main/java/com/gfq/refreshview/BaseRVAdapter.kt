package com.gfq.refreshview

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView

/**
 *  2021/4/13 16:11
 * @auth gaofuq
 * @description
 */
abstract class BaseRVAdapter<DataBean>(
    @LayoutRes private val itemLayoutRes: Int,
    private val BR_ID: Int = 0
) :
    RecyclerView.Adapter<BaseVH>() {

    var dataList = mutableListOf<DataBean>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseVH {
        return BaseVH(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                itemLayoutRes,
                parent,
                false
            )
        )
    }


    override fun onBindViewHolder(holder: BaseVH, position: Int) {
        try {
            holder.vhBinding.setVariable(BR_ID, dataList[position])
            holder.vhBinding.executePendingBindings()
            onBindView(holder, dataList[position], position)
        } catch (e: Exception) {
            Log.e("BaseRVAdapter", "itemView not binding layout ${e.message}")
        }

    }

    fun addAll(list: MutableList<DataBean>){
        val lastIndex =dataList.size
        if (dataList.addAll(list)) {
            notifyItemRangeInserted(lastIndex, list.size)
        }
    }



    abstract fun onBindView(holder: BaseVH, data: DataBean, position: Int)

    override fun getItemCount(): Int = dataList.size

}