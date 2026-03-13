package org.dokiteam.doki.ai.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.dokiteam.doki.R
import org.dokiteam.doki.ai.domain.DuplicatePair
import kotlin.math.roundToInt

class DuplicateMangaAdapter(
    private val onRemove: (DuplicatePair) -> Unit,
) : ListAdapter<DuplicatePair, DuplicateMangaAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleView: TextView = view.findViewById(R.id.ai_dup_title)
        private val matchView: TextView = view.findViewById(R.id.ai_dup_match)
        private val sourceView: TextView = view.findViewById(R.id.ai_dup_source)
        private val removeButton: Button = view.findViewById(R.id.ai_dup_remove_btn)

        fun bind(item: DuplicatePair) {
            titleView.text = item.title
            matchView.text = "Similar to: ${item.matchTitle}"
            val pct = (item.similarity * 100).roundToInt()
            sourceView.text = "Source: ${item.source}  •  ${pct}% match"
            removeButton.setOnClickListener { onRemove(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_duplicate_manga, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<DuplicatePair>() {
            override fun areItemsTheSame(a: DuplicatePair, b: DuplicatePair) = a.mangaId == b.mangaId
            override fun areContentsTheSame(a: DuplicatePair, b: DuplicatePair) = a == b
        }
    }
}
