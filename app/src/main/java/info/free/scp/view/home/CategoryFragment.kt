package info.free.scp.view.home

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import info.free.scp.R
import info.free.scp.SCPConstants
import info.free.scp.bean.ScpModel
import info.free.scp.util.PreferenceUtil
import info.free.scp.view.base.BaseAdapter
import info.free.scp.view.base.BaseFragment
import info.free.scp.view.category.CategoryActivity
import info.free.scp.view.category.CategoryAdapter
import info.free.scp.view.category.ScpAdapter
import kotlinx.android.synthetic.main.activity_category.*
import kotlinx.android.synthetic.main.fragment_category.*

class CategoryFragment : BaseFragment() {
    private var categoryType = -1
    private val categoryCount = PreferenceUtil.getCategoryCount()
    private val categoryList: MutableList<Any> = emptyList<Any>().toMutableList()
    private val scpList: MutableList<ScpModel> = emptyList<ScpModel>().toMutableList()
    private var categoryAdapter: CategoryAdapter? = null
    private var scpAdapter: ScpAdapter? = null
    private var currentCategoryPosition = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_category, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val lm = LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false)
        rv_category_list?.layoutManager = lm
        initData()
    }

    private fun initData() {
        categoryType = arguments?.getInt("category_type")?:-1
        categoryList.clear()
        scpList.clear()
        // 一级目录
        when (categoryType) {
            SCPConstants.Category.SERIES -> {
                categoryList.addAll((0 until (5000/categoryCount)).map { it*categoryCount })
            }
            SCPConstants.Category.SERIES_CN -> {
                categoryList.addAll((0 until (1000/categoryCount)).map { it*categoryCount })
            }
            SCPConstants.Category.TALES -> {
                // 1021
                categoryList.addAll(arrayOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K",
                        "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "0-9"))
            }
            SCPConstants.Category.TALES_CN -> {
                // 1021
                categoryList.addAll(arrayOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K",
                        "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "0-9"))
            }
            SCPConstants.Category.EVENT -> {
                categoryList.addAll(arrayOf("实验记录", "探索报告", "事故/事件报告", "访谈记录", "独立补充材料"))
            }
            SCPConstants.Category.TALES_BY_TIME -> {
                categoryList.addAll(arrayOf("2014", "2015", "2016", "2017", "2018"))
            }
        }

        if (categoryAdapter == null) {
            mContext?.let {
                categoryAdapter = CategoryAdapter(it, categoryType, categoryList)
                rv_category_list?.adapter = categoryAdapter
                categoryAdapter?.mOnItemClickListener = object : BaseAdapter.OnItemClickListener {
                    override fun onItemClick(view: View, position: Int) {
                        Log.i(tag, "onItemClick")
                        currentCategoryPosition = position
                        val subCateIntent = Intent(it, CategoryActivity::class.java)
                        subCateIntent.putExtra("categoryType", categoryType)
                        subCateIntent.putExtra("position", position)
                        startActivity(subCateIntent)
                    }
                }
            }
        } else {
            categoryAdapter?.notifyDataSetChanged()
            rv_category_list?.adapter = categoryAdapter
            if (currentCategoryPosition > 0) {
                rv_scp_list?.scrollToPosition(currentCategoryPosition)
            }
        }
    }

    fun refreshTheme() {

    }

    companion object {

        fun newInstance(categoryType: Int): CategoryFragment {
            val fragment = CategoryFragment()
            val args = Bundle()
            args.putInt("category_type", categoryType)
//            args.putString(ARG_PARAM2, param2)
            fragment.arguments = args
            return fragment
        }
    }
}