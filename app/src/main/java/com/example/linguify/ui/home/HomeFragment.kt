package com.example.linguify.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.linguify.R
import com.example.linguify.data.manager.LoginPreferencesManager
import com.example.linguify.data.manager.StreakManager
import com.example.linguify.databinding.FragmentHomeBinding
import com.example.linguify.model.Word
import com.example.linguify.utils.loadWithCache
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var loginPreferencesManager: LoginPreferencesManager

    @Inject
    lateinit var streakManager: StreakManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupObservers()

        viewModel.loadWordCounts()
        viewModel.loadLearningWordsForReview()
        viewModel.loadRandomDiscoverImages()

        setupStreakTracking()
    }

    private fun setupStreakTracking() {
        viewLifecycleOwner.lifecycleScope.launch {
            streakManager.recordDailyActivity()

            updateStreakProgress()
        }
    }

    private fun updateStreakProgress() {
        val weeklyStreak = streakManager.getWeeklyStreak()
        val currentStreak = streakManager.getCurrentStreak()

        binding.streakProgressView.updateStreakData(weeklyStreak, currentStreak)

        Log.d("HomeFragment", "Streak updated - Current: $currentStreak, Weekly: $weeklyStreak")
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.wordCountsState.collectLatest { state ->
                when (state) {
                    is HomeViewModel.WordCountsState.Success -> {
                        updateWordCounts(state)
                        updateProgressBar(state)
                    }
                    is HomeViewModel.WordCountsState.Loading -> {
                    }
                    is HomeViewModel.WordCountsState.Error -> {
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.discoverImages.collectLatest { imageUrls ->
                if (imageUrls.isNotEmpty()) {
                    updateDiscoverImages(imageUrls)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.learningWordsCount.collectLatest { count ->
                binding.tvReviewCount.text = "$count"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.learningWordsForReview.collectLatest { words ->
                updateReviewImages(words)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.nextWordToLearn.collectLatest { nextWord ->
                if (nextWord.isNotEmpty()) {
                    binding.tvLearnNext.text = "Next: $nextWord"
                } else {
                    binding.tvLearnNext.text = " "
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.nextWordImageUrl.collectLatest { imageUrl ->
                if (imageUrl != null && imageUrl.isNotEmpty()) {
                    Glide.with(requireContext())
                        .load(imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(binding.imageLearn)
                } else {
                    binding.imageLearn.setImageResource(R.drawable.placeholder_image)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.discoverableWordCount.collectLatest { count ->
                if (count > 0) {
                    binding.tvDiscoverRange.text = "$count words to discover"
                } else {
                    binding.tvDiscoverRange.text = " "
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.username.collectLatest { username ->
                if (username.isNotEmpty()) {
                    binding.tvWelcome.text = "Welcome $username"
                } else {
                    binding.tvWelcome.text = "Welcome"
                }
            }
        }
    }

    private fun updateReviewImages(words: List<Word>) {
        val imageViews = listOf(
            binding.imageViewReview1,
            binding.imageViewReview2,
            binding.imageViewReview3
        )

        imageViews.forEach { it.visibility = View.GONE }

        words.forEachIndexed { index, word ->
            if (index < imageViews.size) {
                val imageView = imageViews[index]
                imageView.visibility = View.VISIBLE

                if (!word.imageUrl.isNullOrEmpty()) {
                    Log.d("HomeFragment", "Loading image for word ${word.text} at index $index: ${word.imageUrl}")

                    imageView.loadWithCache(word.imageUrl)
                } else {
                    Log.d("HomeFragment", "No image URL for word ${word.text}, using placeholder")
                    imageView.setImageResource(R.drawable.placeholder_image)
                }
            }
        }
    }

    private fun updateDiscoverImages(imageUrls: List<String>) {
        val imageViews = listOf(
            binding.imageDiscover1,
            binding.imageDiscover2,
            binding.imageDiscover3,
            binding.imageDiscover4
        )

        imageUrls.forEachIndexed { index, imageUrl ->
            if (index < imageViews.size) {
                Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .centerCrop()
                    .into(imageViews[index])
            }
        }

        Log.d("HomeFragment", "Updated ${imageUrls.size} discover images")
    }

    private fun updateWordCounts(state: HomeViewModel.WordCountsState.Success) {
        val trackedWordsTotal = state.knownWords + state.learningWords + state.toLearnWords
        binding.tvWordsCount.text = trackedWordsTotal.toString()

        binding.tvKnewCount.text = "${state.knownWords}"
        binding.tvToLearnCount.text = "${state.toLearnWords}"
        binding.tvStartedCount.text = "${state.learningWords}"

        binding.tvReviewCount.text = state.reviewWords.toString()

        Log.d("HomeFragment", "Word counts updated - Total tracked: $trackedWordsTotal (Known: ${state.knownWords}, Learning: ${state.learningWords}, To Learn: ${state.toLearnWords})")
    }

    private fun updateProgressBar(state: HomeViewModel.WordCountsState.Success) {
        val totalTrackedWords = state.knownWords + state.learningWords + state.toLearnWords

        val progress = if (totalTrackedWords > 0) {
            (state.knownWords * 100) / totalTrackedWords
        } else {
            0
        }

        binding.progressBar.progress = progress

        Log.d("HomeFragment", "Progress bar updated: $progress% (Known: ${state.knownWords}, Learning: ${state.learningWords}, To Learn: ${state.toLearnWords})")
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            logout()
        }

        binding.btnStartLevelTest.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_levelTestFragment)
        }

        binding.tvKnewCount.setOnClickListener {
            navigateToWordList(WORD_LIST_TYPE_KNOWN)
        }

        binding.tvToLearnCount.setOnClickListener {
            navigateToWordList(WORD_LIST_TYPE_TO_LEARN)
        }

        binding.tvStartedCount.setOnClickListener {
            navigateToWordList(WORD_LIST_TYPE_LEARNING)
        }

        binding.btnStartReview.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_reviewFragment)
        }

        binding.btnDiscover.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_flashcardFragment)
        }

        binding.btnLearn.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_learnFragment)
        }
    }

    private fun logout() {
        viewLifecycleOwner.lifecycleScope.launch {
            firebaseAuth.signOut()

            loginPreferencesManager.clearLoginState()

            findNavController().navigate(R.id.action_global_loginFragment)
        }
    }

    private fun navigateToWordList(listType: String) {
        val bundle = bundleOf("list_type" to listType)
        findNavController().navigate(R.id.action_homeFragment_to_wordListFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val WORD_LIST_TYPE_KNOWN = "known"
        const val WORD_LIST_TYPE_TO_LEARN = "to_learn"
        const val WORD_LIST_TYPE_LEARNING = "learning"
    }
}
