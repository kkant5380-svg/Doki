package org.dokiteam.doki.ai.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.dokiteam.doki.R
import org.dokiteam.doki.ai.domain.DuplicatePair

@AndroidEntryPoint
class AiLibraryFragment : Fragment() {

    private val viewModel: AiLibraryViewModel by viewModels()
    private lateinit var adapter: DuplicateMangaAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_ai_library, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.ai_library_recycler)
        val progressBar = view.findViewById<ProgressBar>(R.id.ai_library_progress)
        val statusText = view.findViewById<TextView>(R.id.ai_library_status)
        val scanButton = view.findViewById<Button>(R.id.ai_library_scan_btn)
        val summaryButton = view.findViewById<Button>(R.id.ai_library_summary_btn)
        val emptyText = view.findViewById<TextView>(R.id.ai_library_empty)

        adapter = DuplicateMangaAdapter { duplicate -> confirmRemove(duplicate) }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        scanButton.setOnClickListener { viewModel.scanForDuplicates() }
        summaryButton.setOnClickListener { viewModel.loadSummary() }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            progressBar.isVisible = loading
            scanButton.isEnabled = !loading
            summaryButton.isEnabled = !loading
        }

        viewModel.statusMessage.observe(viewLifecycleOwner) { msg ->
            statusText.text = msg
            statusText.isVisible = msg.isNotEmpty()
        }

        viewModel.duplicates.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            emptyText.isVisible = list.isEmpty()
            recyclerView.isVisible = list.isNotEmpty()
        }
    }

    private fun confirmRemove(duplicate: DuplicatePair) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Entry?")
            .setMessage("Remove \"${duplicate.title}\" from ${duplicate.source}?")
            .setPositiveButton("Remove") { _, _ -> viewModel.removeEntry(duplicate) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        fun newInstance() = AiLibraryFragment()
    }
}
