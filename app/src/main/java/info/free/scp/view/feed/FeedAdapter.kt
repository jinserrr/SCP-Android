package info.free.scp.view.feed

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.free.scp.bean.FeedModel
import info.free.scp.databinding.ItemFeedBinding
import info.free.scp.view.detail.DetailActivity

class FeedAdapter: ListAdapter<FeedModel, FeedAdapter.PictureHolder>(FeedDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedAdapter.PictureHolder {
        return PictureHolder(ItemFeedBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: FeedAdapter.PictureHolder, position: Int) {
        val feed = getItem(position)
        holder.apply {
            bind(createOnClickListener(feed), feed)
            itemView.tag = feed
        }
    }

    private fun createOnClickListener(feed: FeedModel): View.OnClickListener {
        return View.OnClickListener {
//            EventUtil.onEvent(it.context, EventUtil.viewPicture)
            val intent = Intent()
            intent.putExtra("link", feed.link)
            intent.setClass(it.context, DetailActivity::class.java)
            (it.context as Activity).startActivity(intent)
        }
    }

    class PictureHolder(private val binding: ItemFeedBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(listener: View.OnClickListener, item: FeedModel) {
            binding.apply {
                clickListener = listener
                feed = item
                executePendingBindings()
            }
        }
    }

    private class FeedDiffCallback : DiffUtil.ItemCallback<FeedModel>() {

        override fun areItemsTheSame(
                oldItem: FeedModel,
                newItem: FeedModel
        ): Boolean {
            return oldItem.link == newItem.link
        }

        override fun areContentsTheSame(
                oldItem: FeedModel,
                newItem: FeedModel
        ): Boolean {
            return oldItem == newItem
        }
    }
}