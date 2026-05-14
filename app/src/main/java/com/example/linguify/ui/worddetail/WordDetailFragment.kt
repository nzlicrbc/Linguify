package com.example.linguify.ui.worddetail

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
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
import com.example.linguify.databinding.FragmentWordDetailBinding
import com.example.linguify.model.Word
import com.example.linguify.model.WordLearningStatus
import com.example.linguify.utils.loadWithCache
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WordDetailFragment : Fragment() {

    private var _binding: FragmentWordDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WordDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWordDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnPlayPronunciation.setOnClickListener {
            viewModel.playPronunciation()
        }

        binding.btnMarkAsKnown.setOnClickListener {
            viewModel.updateWordStatus(WordLearningStatus.KNOWN)
            updateStatusButtonsUI(WordLearningStatus.KNOWN)
        }

        binding.btnMarkAsToLearn.setOnClickListener {
            viewModel.updateWordStatus(WordLearningStatus.TO_LEARN)
            updateStatusButtonsUI(WordLearningStatus.TO_LEARN)
        }

        binding.btnMarkAsLearning.setOnClickListener {
            viewModel.updateWordStatus(WordLearningStatus.LEARNING)
            updateStatusButtonsUI(WordLearningStatus.LEARNING)
        }

        binding.btnShowYouglish.setOnClickListener {
            viewModel.currentWord.value?.let { word ->
                showYouglishDialog(word.text)
            }
        }

        binding.lottieImageLoading.apply {
            setAnimation(R.raw.loading_image)
            playAnimation()
        }
    }

    private fun showYouglishDialog(wordText: String) {
        val youglishDialog = YouGlishDialog(requireContext(), wordText)
        youglishDialog.show()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.wordState.collectLatest { state ->
                when (state) {
                    is WordDetailViewModel.WordState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.scrollView.visibility = View.GONE
                    }
                    is WordDetailViewModel.WordState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.scrollView.visibility = View.VISIBLE
                        updateWordUI(state.word)
                        updateStatusButtonsUI(state.word.status)
                    }
                    is WordDetailViewModel.WordState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pronunciationState.collectLatest { state ->
                when (state) {
                    is WordDetailViewModel.PronunciationState.Playing -> {
                        binding.btnPlayPronunciation.isEnabled = false
                    }
                    is WordDetailViewModel.PronunciationState.Idle -> {
                        binding.btnPlayPronunciation.isEnabled = true
                    }
                    is WordDetailViewModel.PronunciationState.Error -> {
                        binding.btnPlayPronunciation.isEnabled = true
                        Toast.makeText(requireContext(), getString(R.string.pronunciation_error), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.wordImageUrl.collectLatest { pexelsImageUrl ->
                if (pexelsImageUrl != null) {
                    binding.ivWordImage.loadWithCache(pexelsImageUrl)
                } else {
                    binding.ivWordImage.setImageResource(R.drawable.placeholder_image)
                }
                binding.lottieImageLoading.visibility = View.GONE
            }
        }
    }

    private fun updateStatusButtonsUI(status: WordLearningStatus) {
        binding.btnMarkAsKnown.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray_400))
        binding.btnMarkAsToLearn.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray_400))
        binding.btnMarkAsLearning.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray_400))

        when (status) {
            WordLearningStatus.KNOWN -> {
                binding.btnMarkAsKnown.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.status_known))
            }
            WordLearningStatus.TO_LEARN -> {
                binding.btnMarkAsToLearn.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.status_to_learn))
            }
            WordLearningStatus.LEARNING -> {
                binding.btnMarkAsLearning.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.status_learning))
            }
            else -> {

            }
        }
    }

    private fun loadWordImage(word: Word) {
        binding.lottieImageLoading.visibility = View.VISIBLE

        val imageUrl = word.imageUrl

        if (!imageUrl.isNullOrEmpty()) {
            Log.d("WordDetailFragment", "Loading image from URL: $imageUrl")

            binding.ivWordImage.loadWithCache(imageUrl)
            binding.lottieImageLoading.visibility = View.GONE
        } else {
            Log.d("WordDetailFragment", "No image URL found, loading from Pexels for: ${word.text}")
            viewModel.loadWordImageFromPexels(word.text)
        }

        binding.tvWordOverImage.text = word.text
    }

    private fun loadRandomImage(wordText: String) {
        val imageCategories = listOf(
            "nature", "landscape", "people", "city", "abstract",
            "travel", "technology", "food", "animals", "business"
        )

        val randomCategory = imageCategories.random()
        val imageUrl = "https://source.unsplash.com/random/800x600?$randomCategory"

        binding.lottieImageLoading.visibility = View.VISIBLE

        Glide.with(requireContext())
            .load(imageUrl)
            .transition(DrawableTransitionOptions.withCrossFade())
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.lottieImageLoading.visibility = View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.lottieImageLoading.visibility = View.GONE
                    return false
                }
            })
            .into(binding.ivWordImage)
    }

    private fun updateWordUI(word: Word) {
        binding.tvWord.text = word.text
        binding.tvWordType.text = word.wordType ?: ""
        binding.tvPhonetic.text = word.phoneticSpelling ?: ""

        val cefrLevel = when (word.level) {
            "beginner" -> "A1-A2"
            "intermediate" -> "B1-B2"
            "advanced" -> "C1-C2"
            else -> ""
        }
        binding.tvLevel.text = cefrLevel

        val translations = word.translation.split("/").map { it.trim() }
        val formattedTranslation = if (translations.size > 1) {
            translations.joinToString("\n") { "• $it" }
        } else {
            word.translation
        }
        binding.tvTranslation.text = formattedTranslation

        binding.tvDefinition.text = word.definition ?: ""
        binding.definitionLayout.visibility = if (word.definition.isNullOrEmpty()) View.GONE else View.VISIBLE

        val examplesText = if (!word.example.isNullOrEmpty()) {
            word.example.split(".").filter { it.isNotBlank() }
                .joinToString("\n\n") { "• ${it.trim()}." }
        } else {
            ""
        }
        binding.tvExamples.text = examplesText
        binding.examplesLayout.visibility = if (examplesText.isEmpty()) View.GONE else View.VISIBLE

        val synonymsText = word.synonyms.joinToString(", ")
        binding.tvSynonyms.text = synonymsText
        binding.synonymsLayout.visibility = if (synonymsText.isEmpty()) View.GONE else View.VISIBLE

        val antonymsText = word.antonyms.joinToString(", ")
        binding.tvAntonyms.text = antonymsText
        binding.antonymsLayout.visibility = if (antonymsText.isEmpty()) View.GONE else View.VISIBLE

        loadWordImage(word)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}