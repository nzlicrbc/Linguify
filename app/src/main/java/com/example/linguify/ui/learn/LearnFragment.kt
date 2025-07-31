package com.example.linguify.ui.learn

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.linguify.R
import com.example.linguify.databinding.FragmentLearnBinding
import com.example.linguify.model.Word
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LearnFragment : Fragment() {

    private var _binding: FragmentLearnBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LearnViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLearnBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()

        viewModel.loadWordsToLearn()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnLearn.setOnClickListener {
            viewModel.currentWord.value?.let { word ->
                navigateToWordDetail(word.id)
            }
        }

        binding.btnSkip.setOnClickListener {
            viewModel.moveToNextWord()
        }

        binding.btnPlayPronunciation.setOnClickListener {
            viewModel.playPronunciation()
        }

        binding.btnGoToWordList.setOnClickListener {
            findNavController().navigate(R.id.action_learnFragment_to_flashcardFragment)
        }

        binding.lottieLoading.apply {
            setAnimation(R.raw.loading_words)
            playAnimation()
        }

        binding.lottieImageLoading.apply {
            setAnimation(R.raw.loading_image)
            playAnimation()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loadingState.collectLatest { state ->
                when (state) {
                    is LearnViewModel.LoadingState.Loading -> {
                        showLoadingState()
                    }
                    is LearnViewModel.LoadingState.Success -> {
                        showContentState()
                    }
                    is LearnViewModel.LoadingState.Empty -> {
                        showEmptyState()
                    }
                    is LearnViewModel.LoadingState.Error -> {
                        showErrorState(state.message)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentWord.collectLatest { word ->
                word?.let {
                    updateWordUI(it)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.wordImageUrl.collectLatest { imageUrl ->
                loadWordImage(imageUrl)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pronunciationState.collectLatest { state ->
                when (state) {
                    is LearnViewModel.PronunciationState.Playing -> {
                        binding.btnPlayPronunciation.isEnabled = false
                    }
                    is LearnViewModel.PronunciationState.Idle -> {
                        binding.btnPlayPronunciation.isEnabled = true
                    }
                    is LearnViewModel.PronunciationState.Error -> {
                        binding.btnPlayPronunciation.isEnabled = true
                        Toast.makeText(
                            requireContext(),
                            "Pronunciation could not be played",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun loadWordImage(imageUrl: String?) {
        Log.d("LearnFragment", "Attempting to load image URL: $imageUrl")

        if (imageUrl.isNullOrEmpty()) {
            Log.e("LearnFragment", "Image URL is null or empty")
            binding.imageWord.setImageResource(R.drawable.placeholder_image)
            binding.lottieImageLoading.visibility = View.GONE
            return
        }

        binding.lottieImageLoading.visibility = View.VISIBLE

        val largeImageUrl = imageUrl.replace("h=350", "h=600")

        Glide.with(requireContext())
            .load(largeImageUrl)
            .override(Target.SIZE_ORIGINAL)
            .centerCrop()
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.placeholder_image)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.e("LearnFragment", "Image load FAILED: ${e?.message}")
                    binding.lottieImageLoading.visibility = View.GONE
                    binding.imageWord.setImageResource(R.drawable.placeholder_image)
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.d("LearnFragment", "Image loaded SUCCESSFULLY")
                    binding.lottieImageLoading.visibility = View.GONE
                    return false
                }
            })
            .into(binding.imageWord)
    }

    private fun showLoadingState() {
        binding.loadingContent.visibility = View.VISIBLE
        binding.learnContent.visibility = View.GONE
        binding.noWordsContent.visibility = View.GONE
    }

    private fun showContentState() {
        binding.loadingContent.visibility = View.GONE
        binding.learnContent.visibility = View.VISIBLE
        binding.noWordsContent.visibility = View.GONE
    }

    private fun showEmptyState() {
        binding.loadingContent.visibility = View.GONE
        binding.learnContent.visibility = View.GONE
        binding.noWordsContent.visibility = View.VISIBLE
    }

    private fun showErrorState(errorMessage: String) {
        binding.loadingContent.visibility = View.GONE
        binding.learnContent.visibility = View.GONE
        binding.noWordsContent.visibility = View.VISIBLE
        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
    }

    private fun updateWordUI(word: Word) {
        binding.tvWordText.text = word.text
        binding.tvPhonetic.text = word.phoneticSpelling ?: ""

        binding.btnLearn.text = "Learn"
    }

    private fun navigateToWordDetail(wordId: String) {
        val action = LearnFragmentDirections.actionLearnFragmentToWordDetailFragment(wordId)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}