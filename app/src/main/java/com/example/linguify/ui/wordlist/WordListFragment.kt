package com.example.linguify.ui.wordlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.linguify.databinding.FragmentWordListBinding
import com.example.linguify.model.Word
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WordListFragment : Fragment() {

    private var _binding: FragmentWordListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WordListViewModel by viewModels()
    private lateinit var wordAdapter: WordListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWordListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listType = arguments?.getString("list_type") ?: ""

        val title = when (listType) {
            "known" -> "Known Words"
            "to_learn" -> "Words to Learn"
            "learning" -> "Learning Words"
            else -> "Word List"
        }
        binding.tvTitle.text = title

        setupRecyclerView()
        setupSearch()
        setupObservers()

        binding.recyclerView.apply {
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            recycledViewPool.setMaxRecycledViews(0, 15)
            isDrawingCacheEnabled = true
            drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if (!binding.swipeRefreshLayout.isRefreshing &&
                        (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5 &&
                        firstVisibleItemPosition >= 0) {
                        viewModel.loadMoreWords()
                    }
                }
            })
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadWords(listType)
        }

        viewModel.loadWords(listType)
    }

    private fun setupRecyclerView() {
        wordAdapter = WordListAdapter(
            onWordClick = { word ->
                viewModel.onWordClicked(word)
            },
            imageLoader = { word, callback ->
                viewModel.loadImageForWord(word, callback)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = wordAdapter
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.searchWords(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchWords(newText ?: "")
                return true
            }
        })
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            launch {
                viewModel.wordListState.collect { state ->
                    binding.swipeRefreshLayout.isRefreshing = state is WordListViewModel.WordListState.Loading
                    when (state) {
                        is WordListViewModel.WordListState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.recyclerView.visibility = View.GONE
                            binding.tvEmpty.visibility = View.GONE
                        }
                        is WordListViewModel.WordListState.Success -> {
                            binding.progressBar.visibility = View.GONE

                            if (state.words.isEmpty()) {
                                binding.recyclerView.visibility = View.GONE
                                binding.tvEmpty.visibility = View.VISIBLE
                                binding.tvEmpty.text = "No words found"
                            } else {
                                binding.recyclerView.visibility = View.VISIBLE
                                binding.tvEmpty.visibility = View.GONE

                                wordAdapter.submitList(state.words)
                            }
                        }
                        is WordListViewModel.WordListState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.recyclerView.visibility = View.GONE
                            binding.tvEmpty.visibility = View.VISIBLE
                            binding.tvEmpty.text = "Error: ${state.message}"
                        }
                    }
                }
            }

            launch {
                viewModel.navigateToWordDetail.collect { wordId ->
                    wordId?.let {
                        val action = WordListFragmentDirections.actionWordListFragmentToWordDetailFragment(it)
                        findNavController().navigate(action)
                        viewModel.onWordDetailNavigated()
                    }
                }
            }

            launch {
                viewModel.isLoadingMore.collect { isLoading ->
                    binding.loadMoreProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
