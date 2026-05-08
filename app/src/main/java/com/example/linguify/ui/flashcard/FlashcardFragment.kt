package com.example.linguify.ui.flashcard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.linguify.R
import com.example.linguify.databinding.FragmentFlashcardBinding
import com.example.linguify.model.Word
import com.example.linguify.utils.SwipeGestureDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FlashcardFragment : Fragment() {

    private var _binding: FragmentFlashcardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FlashcardViewModel by viewModels()

    private lateinit var swipeGestureDetector: SwipeGestureDetector

    private var isCardFlipped = false

    private val TOTAL_BLOCKS = 100
    private val ACTIVE_BLOCK_COLOR = R.color.purple_500
    private val INACTIVE_BLOCK_COLOR = R.color.light_gray
    private val CURRENT_BLOCK_COLOR = R.color.purple_700

    private val WORDS_PER_SET = 20

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFlashcardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardContainer.isClickable = true
        binding.cardContainer.isFocusable = true

        binding.setIndicatorContainer.visibility = View.VISIBLE
        binding.setIndicatorGrid.visibility = View.VISIBLE

        setupUI()
        setupObservers()
        setupSwipeDetector()

        createEmptySetIndicator()

        viewModel.loadWordsForCurrentLevel()
    }

    private fun createEmptySetIndicator() {
        binding.setIndicatorGrid.removeAllViews()

        val blockSizePx = resources.getDimensionPixelSize(R.dimen.set_indicator_block_size)
        val blockMarginPx = resources.getDimensionPixelSize(R.dimen.set_indicator_block_margin)

        binding.setIndicatorGrid.columnCount = 10
        binding.setIndicatorGrid.rowCount = 2

        for (i in 1..WORDS_PER_SET) {
            val blockView = View(requireContext())

            val row = (i - 1) / 10
            val col = (i - 1) % 10

            val params = GridLayout.LayoutParams()
            params.width = blockSizePx
            params.height = blockSizePx
            params.setMargins(blockMarginPx, blockMarginPx, blockMarginPx, blockMarginPx)

            params.rowSpec = GridLayout.spec(row, 1, GridLayout.CENTER)
            params.columnSpec = GridLayout.spec(col, 1, GridLayout.CENTER)

            blockView.layoutParams = params
            blockView.setBackgroundColor(ContextCompat.getColor(requireContext(), INACTIVE_BLOCK_COLOR))
            blockView.tag = i

            binding.setIndicatorGrid.addView(blockView)
        }

        Log.d("FlashcardFragment", "Empty set indicator created with ${binding.setIndicatorGrid.childCount} blocks")
    }

    private fun setupUI() {
        binding.cardContainer.setOnClickListener {
            Log.d("FlashcardFragment", "Card container clicked")
            flipCard()
        }

        binding.btnPronunciation.setOnClickListener {
            viewModel.currentWord.value?.let { word ->
                viewModel.playPronunciation(word)
            }
        }

        binding.btnKnow.setOnClickListener {
            viewModel.currentWord.value?.let { word ->
                viewModel.markWordAsKnown(word)
                swipeGestureDetector.animateSwipe(SwipeGestureDetector.SwipeDirection.RIGHT)
            }
        }

        binding.btnLearn.setOnClickListener {
            viewModel.currentWord.value?.let { word ->
                viewModel.markWordToLearn(word)
                swipeGestureDetector.animateSwipe(SwipeGestureDetector.SwipeDirection.LEFT)
            }
        }

        binding.btnSkip.setOnClickListener {
            findNavController().navigate(R.id.action_flashcardFragment_to_homeFragment)
        }
    }

    private fun setupSetIndicator() {
        binding.setIndicatorGrid.removeAllViews()

        val blockSizePx = resources.getDimensionPixelSize(R.dimen.set_indicator_block_size)
        val blockMarginPx = resources.getDimensionPixelSize(R.dimen.set_indicator_block_margin)
        val wordCount = viewModel.wordsInCurrentSet.value ?: WORDS_PER_SET

        val currentSetIndex = viewModel.wordListState.value.let {
            if (it is FlashcardViewModel.WordListState.Success) it.setIndex else 1
        }

        binding.setIndicatorGrid.columnCount = 10
        binding.setIndicatorGrid.rowCount = 2

        for (i in 1..Math.min(wordCount, WORDS_PER_SET)) {
            val blockView = View(requireContext())

            val row = (i - 1) / 10
            val col = (i - 1) % 10

            val params = GridLayout.LayoutParams()
            params.width = blockSizePx
            params.height = blockSizePx
            params.setMargins(blockMarginPx, blockMarginPx, blockMarginPx, blockMarginPx)

            params.rowSpec = GridLayout.spec(row, 1, GridLayout.CENTER)
            params.columnSpec = GridLayout.spec(col, 1, GridLayout.CENTER)

            blockView.layoutParams = params
            blockView.setBackgroundColor(ContextCompat.getColor(requireContext(), INACTIVE_BLOCK_COLOR))
            blockView.tag = i

            binding.setIndicatorGrid.addView(blockView)
        }

        binding.setIndicatorContainer.visibility = View.VISIBLE
    }

    private fun updateSetIndicator(currentIndex: Int, totalWords: Int, setIndex: Int, totalWordCount: Int = 0) {
        binding.setIndicatorGrid.removeAllViews()

        Log.d("FlashcardFragment", "updateSetIndicator called - currentIndex: $currentIndex, totalWords: $totalWords, setIndex: $setIndex, totalWordCount: $totalWordCount")

        val blockSizePx = resources.getDimensionPixelSize(R.dimen.set_indicator_block_size)
        val blockMarginPx = resources.getDimensionPixelSize(R.dimen.set_indicator_block_margin)

        val startNumber = ((setIndex - 1) * WORDS_PER_SET) + 1

        val wordsInCurrentSet = if (totalWordCount > 0) {
            val remainingWords = totalWordCount - ((setIndex - 1) * WORDS_PER_SET)
            remainingWords.coerceAtMost(WORDS_PER_SET).coerceAtLeast(0)
        } else {
            totalWords
        }

        val endNumber = startNumber + wordsInCurrentSet - 1

        Log.d("FlashcardFragment", "Range values: $startNumber - $endNumber (wordsInCurrentSet: $wordsInCurrentSet)")

        binding.setIndicatorGrid.columnCount = 10
        binding.setIndicatorGrid.rowCount = 2

        for (i in 1..wordsInCurrentSet.coerceAtMost(WORDS_PER_SET)) {
            val blockView = View(requireContext())

            val row = (i - 1) / 10
            val col = (i - 1) % 10

            val params = GridLayout.LayoutParams()
            params.width = blockSizePx
            params.height = blockSizePx
            params.setMargins(blockMarginPx, blockMarginPx, blockMarginPx, blockMarginPx)

            params.rowSpec = GridLayout.spec(row, 1, GridLayout.CENTER)
            params.columnSpec = GridLayout.spec(col, 1, GridLayout.CENTER)

            blockView.layoutParams = params

            blockView.setBackgroundColor(ContextCompat.getColor(requireContext(), INACTIVE_BLOCK_COLOR))
            blockView.tag = i

            binding.setIndicatorGrid.addView(blockView)
        }

        updateBlockColors(currentIndex)
    }

    private fun updateBlockColors(currentIndex: Int) {
        for (i in 0 until binding.setIndicatorGrid.childCount) {
            val blockView = binding.setIndicatorGrid.getChildAt(i)

            if (i < currentIndex) {
                blockView.setBackgroundColor(ContextCompat.getColor(requireContext(), ACTIVE_BLOCK_COLOR))
            } else if (i == currentIndex) {
                blockView.setBackgroundColor(ContextCompat.getColor(requireContext(), CURRENT_BLOCK_COLOR))
            } else {
                blockView.setBackgroundColor(ContextCompat.getColor(requireContext(), INACTIVE_BLOCK_COLOR))
            }
        }
    }

    private fun resetCardWithoutRepositioning() {
        binding.tvWordText.visibility = View.VISIBLE
        binding.tvWordType.visibility = View.VISIBLE
        binding.tvWordMeaning.visibility = View.GONE

        isCardFlipped = false

        binding.cardContainer.translationX = 0f
        binding.cardContainer.translationY = 0f
        binding.cardContainer.rotation = 0f
        binding.cardContainer.alpha = 1f
        binding.cardContainer.scaleX = 1.0f
        binding.cardContainer.scaleY = 1.0f

        Log.d("FlashcardFragment", "Card has been reset without repositioning")
    }

    private fun setupSwipeDetector() {
        swipeGestureDetector = SwipeGestureDetector(binding.cardContainer) { direction ->
            when (direction) {
                SwipeGestureDetector.SwipeDirection.LEFT -> {
                    viewModel.currentWord.value?.let { word ->
                        viewModel.markWordToLearn(word)
                        showNextWord()
                    }
                }
                SwipeGestureDetector.SwipeDirection.RIGHT -> {
                    viewModel.currentWord.value?.let { word ->
                        viewModel.markWordAsKnown(word)
                        showNextWord()
                    }
                }
            }
        }

        binding.cardContainer.setOnTouchListener(swipeGestureDetector)
    }

    private fun flipCard() {
        Log.d("FlashcardFragment", "flipCard called")

        isCardFlipped = !isCardFlipped

        if (isCardFlipped) {
            binding.tvWordText.visibility = View.GONE
            binding.tvWordType.visibility = View.GONE
            binding.tvWordMeaning.visibility = View.VISIBLE
            Log.d("FlashcardFragment", "Showing meaning")
        } else {
            binding.tvWordText.visibility = View.VISIBLE
            binding.tvWordType.visibility = View.VISIBLE
            binding.tvWordMeaning.visibility = View.GONE
            Log.d("FlashcardFragment", "Showing word")
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.wordListState.collect { state ->
                when (state) {
                    is FlashcardViewModel.WordListState.Loading -> {
                        showLoading()
                    }
                    is FlashcardViewModel.WordListState.Success -> {
                        hideLoading()
                        updateSetIndicator(
                            state.currentIndex,
                            state.totalWords,
                            state.setIndex,
                            state.totalWordCount
                        )
                    }
                    is FlashcardViewModel.WordListState.Error -> {
                        hideLoading()
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is FlashcardViewModel.WordListState.Completed -> {
                        viewModel.loadNextWordSet()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentWord.collect { word ->
                word?.let {
                    updateWordCard(it)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pronunciationState.collect { state ->
                when (state) {
                    is FlashcardViewModel.PronunciationState.Playing -> {
                        binding.btnPronunciation.isEnabled = false
                        binding.btnPronunciation.alpha = 0.5f
                    }
                    is FlashcardViewModel.PronunciationState.Idle -> {
                        binding.btnPronunciation.isEnabled = true
                        binding.btnPronunciation.alpha = 1.0f
                    }
                    is FlashcardViewModel.PronunciationState.Error -> {
                        binding.btnPronunciation.isEnabled = true
                        binding.btnPronunciation.alpha = 1.0f
                        Toast.makeText(context, getString(R.string.pronunciation_error), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateWordCard(word: Word) {
        binding.tvWordText.text = word.text
        binding.tvWordType.text = word.wordType ?: ""

        val translations = word.translation.split("/").map { it.trim() }
        val formattedTranslation = if (translations.size > 1) {
            translations.joinToString("\n") { "• $it" }
        } else {
            word.translation
        }

        binding.tvWordMeaning.text = formattedTranslation

        binding.tvWordMeaning.visibility = View.GONE
        binding.tvWordText.visibility = View.VISIBLE
        binding.tvWordType.visibility = View.VISIBLE

        isCardFlipped = false

        binding.cardContainer.translationX = 0f
        binding.cardContainer.translationY = 0f
        binding.cardContainer.rotation = 0f
        binding.cardContainer.alpha = 1f
    }

    override fun onPause() {
        super.onPause()
        viewModel.saveLastWordIndex()
    }

    private fun showNextWord() {
        viewModel.saveLastWordIndex()
        viewModel.moveToNextWord()

        resetCardWithoutRepositioning()
    }

    private fun showLoading() {
        binding.progressLoading.visibility = View.VISIBLE
        binding.cardContainer.visibility = View.GONE
        binding.layoutButtons.visibility = View.GONE
        binding.setIndicatorContainer.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.progressLoading.visibility = View.GONE
        binding.cardContainer.visibility = View.VISIBLE
        binding.layoutButtons.visibility = View.VISIBLE
        binding.setIndicatorContainer.visibility = View.VISIBLE
    }

    private fun resetCardPosition() {
        binding.tvWordText.visibility = View.VISIBLE
        binding.tvWordType.visibility = View.VISIBLE
        binding.tvWordMeaning.visibility = View.GONE

        isCardFlipped = false

        binding.cardContainer.rotation = 0f
        binding.cardContainer.scaleX = 1.0f
        binding.cardContainer.scaleY = 1.0f
        binding.cardContainer.translationX = 0f
        binding.cardContainer.translationY = 0f
        binding.cardContainer.alpha = 1.0f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
