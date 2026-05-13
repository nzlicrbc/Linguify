package com.example.linguify.ui.review

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.linguify.R
import com.example.linguify.databinding.*
import com.example.linguify.model.ReviewQuestionType
import com.example.linguify.model.ReviewSession
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReviewFragment : Fragment() {

    private var _binding: FragmentReviewBinding? = null
    private val binding get() = _binding!!

    private var _contextSentenceBinding: LayoutContextQuestionBinding? = null
    private var _definitionBinding: LayoutDefinitionQuestionBinding? = null

    private val viewModel: ReviewViewModel by viewModels()

    private var currentQuestionIndex = 0
    private var questions: List<ReviewQuestionType> = emptyList()
    private var selectedOptionIndex: Int? = null

    private var currentButtons: List<MaterialButton> = emptyList()
    private var pendingMoveToNext: Runnable? = null

    private enum class OptionState { DEFAULT, SELECTED, CORRECT, WRONG }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReviewBinding.inflate(inflater, container, false)

        _contextSentenceBinding = binding.layoutContextSentence.let {
            LayoutContextQuestionBinding.bind(it.root)
        }

        _definitionBinding = binding.layoutDefinition.let {
            LayoutDefinitionQuestionBinding.bind(it.root)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()

        viewModel.loadReviewSession()
    }

    private fun setupUI() {
        binding.btnClose.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSubmit.setOnClickListener {
            submitAnswer()
        }

        binding.btnSkip.setOnClickListener {
            skipQuestion()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.reviewState.collect { state ->
                when (state) {
                    is ReviewViewModel.ReviewState.Loading -> showLoading()
                    is ReviewViewModel.ReviewState.SessionReady -> {
                        hideLoading()
                        questions = state.session.questions
                        if (questions.isNotEmpty()) showQuestion(0)
                    }
                    is ReviewViewModel.ReviewState.QuestionAnswered -> showAnswerFeedback(state.isCorrect)
                    is ReviewViewModel.ReviewState.SessionCompleted -> showSessionResults(state.session)
                    is ReviewViewModel.ReviewState.Error -> {
                        showError(state.message)
                        binding.btnSubmit.isEnabled = true
                    }
                }
            }
        }
    }

    private fun showQuestion(index: Int) {
        if (index >= questions.size) {
            viewModel.completeSession()
            return
        }

        currentQuestionIndex = index
        val question = questions[index]

        resetQuestionUI()

        binding.progressBar.max = questions.size
        binding.progressBar.progress = index + 1
        binding.tvProgress.text = "${index + 1}/${questions.size}"

        when (question) {
            is ReviewQuestionType.MultipleChoice -> showMultipleChoice(question)
            is ReviewQuestionType.ContextSentence -> showContextSentence(question)
            is ReviewQuestionType.Definition -> showDefinition(question)
        }
    }

    private fun showMultipleChoice(question: ReviewQuestionType.MultipleChoice) {
        binding.layoutQuestionContainer.isVisible = true
        binding.layoutMultipleChoice.isVisible = true

        binding.tvWordDisplay.isVisible = true
        binding.tvWordDisplay.text = question.word.text
        binding.tvQuestionText.text = question.questionText

        binding.btnOption1.text = question.options[0]
        binding.btnOption2.text = question.options[1]
        binding.btnOption3.text = question.options[2]
        binding.btnOption4.text = question.options[3]

        setupOptionButtons(listOf(
            binding.btnOption1,
            binding.btnOption2,
            binding.btnOption3,
            binding.btnOption4
        ))
    }

    private fun showContextSentence(question: ReviewQuestionType.ContextSentence) {
        binding.layoutQuestionContainer.isVisible = true
        binding.layoutContextSentence.root.isVisible = true

        binding.tvQuestionText.text = getString(R.string.complete_the_sentence)
        _contextSentenceBinding?.tvSentence?.text = question.sentence

        _contextSentenceBinding?.let { b ->
            b.btnOption1.text = question.options[0]
            b.btnOption2.text = question.options[1]
            b.btnOption3.text = question.options[2]
            b.btnOption4.text = question.options[3]

            setupOptionButtons(listOf(b.btnOption1, b.btnOption2, b.btnOption3, b.btnOption4))
        }
    }

    private fun showDefinition(question: ReviewQuestionType.Definition) {
        binding.layoutQuestionContainer.isVisible = true
        binding.layoutDefinition.root.isVisible = true

        binding.tvQuestionText.text = getString(R.string.which_definition_matches, question.word.text)

        _definitionBinding?.let { b ->
            b.btnOption1.text = question.definitions[0]
            b.btnOption2.text = question.definitions[1]
            b.btnOption3.text = question.definitions[2]
            b.btnOption4.text = question.definitions[3]

            setupOptionButtons(listOf(b.btnOption1, b.btnOption2, b.btnOption3, b.btnOption4))
        }
    }

    private fun setupOptionButtons(buttons: List<MaterialButton>) {
        currentButtons = buttons
        selectedOptionIndex = null

        buttons.forEachIndexed { index, button ->
            button.isClickable = true
            applyOptionState(button, OptionState.DEFAULT)

            button.setOnClickListener(null)
            button.setOnClickListener { view ->
                if (view.isClickable) selectOption(index, buttons)
            }
        }

        binding.btnSubmit.isEnabled = false
    }

    private fun selectOption(index: Int, buttons: List<MaterialButton>) {
        if (selectedOptionIndex == index) return

        buttons.forEach { applyOptionState(it, OptionState.DEFAULT) }
        applyOptionState(buttons[index], OptionState.SELECTED)

        selectedOptionIndex = index
        binding.btnSubmit.isEnabled = true
    }

    private fun submitAnswer() {
        val selectedIndex = selectedOptionIndex ?: run {
            Toast.makeText(context, getString(R.string.please_select_option), Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmit.isEnabled = false
        currentButtons.forEach { it.isClickable = false }

        val question = questions[currentQuestionIndex]
        val correctIndex = when (question) {
            is ReviewQuestionType.MultipleChoice -> question.correctIndex
            is ReviewQuestionType.ContextSentence -> question.correctIndex
            is ReviewQuestionType.Definition -> question.correctIndex
        }

        val isCorrect = selectedIndex == correctIndex
        showAnswerColors(selectedIndex, correctIndex, isCorrect)

        viewModel.submitAnswer(currentQuestionIndex, selectedIndex)
    }

    private fun showAnswerColors(selectedIndex: Int, correctIndex: Int, isCorrect: Boolean) {
        if (selectedIndex < currentButtons.size && correctIndex < currentButtons.size) {
            if (isCorrect) {
                applyOptionState(currentButtons[selectedIndex], OptionState.CORRECT)
            } else {
                applyOptionState(currentButtons[selectedIndex], OptionState.WRONG)
                applyOptionState(currentButtons[correctIndex], OptionState.CORRECT)
            }
        }
    }

    private fun applyOptionState(button: MaterialButton, state: OptionState) {
        val bgColor: Int
        val strokeColor: Int
        when (state) {
            OptionState.DEFAULT -> {
                bgColor = 0xFFFFFFFF.toInt()
                strokeColor = 0xFFE2E8F0.toInt()
            }
            OptionState.SELECTED -> {
                bgColor = 0xFFEFF6FF.toInt()
                strokeColor = 0xFF3B82F6.toInt()
            }
            OptionState.CORRECT -> {
                bgColor = 0xFFF0FDF4.toInt()
                strokeColor = 0xFF22C55E.toInt()
            }
            OptionState.WRONG -> {
                bgColor = 0xFFFFF1F2.toInt()
                strokeColor = 0xFFF87171.toInt()
            }
        }
        button.backgroundTintList = ColorStateList.valueOf(bgColor)
        button.strokeColor = ColorStateList.valueOf(strokeColor)
    }

    private fun showAnswerFeedback(isCorrect: Boolean) {
        pendingMoveToNext?.let { binding.root.removeCallbacks(it) }

        val message = if (isCorrect) getString(R.string.answer_correct) else getString(R.string.answer_incorrect)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

        val delay = if (isCorrect) 2000L else 3000L
        pendingMoveToNext = Runnable { moveToNextQuestion() }
        binding.root.postDelayed(pendingMoveToNext!!, delay)
    }

    private fun moveToNextQuestion() {
        currentQuestionIndex++
        showQuestion(currentQuestionIndex)
    }

    private fun skipQuestion() {
        viewModel.skipQuestion(currentQuestionIndex)
    }

    private fun showSessionResults(session: ReviewSession) {
        val totalQuestions = session.questions.size
        val score = session.score ?: 0

        Toast.makeText(
            context,
            getString(R.string.session_completed, score, totalQuestions),
            Toast.LENGTH_LONG
        ).show()

        findNavController().navigateUp()
    }

    private fun resetQuestionUI() {
        binding.layoutMultipleChoice.isVisible = false
        binding.layoutContextSentence.root.isVisible = false
        binding.layoutDefinition.root.isVisible = false
        binding.tvWordDisplay.isVisible = false

        currentButtons.forEach { button ->
            button.setOnClickListener(null)
            button.isClickable = true
            applyOptionState(button, OptionState.DEFAULT)
        }
        currentButtons = emptyList()

        selectedOptionIndex = null
        binding.btnSubmit.isEnabled = false
    }

    private fun showLoading() {
        binding.progressLoading.isVisible = true
        binding.layoutQuestionContainer.isVisible = false
        binding.tvProgress.text = ""
        binding.progressBar.progress = 0
    }

    private fun hideLoading() {
        binding.progressLoading.isVisible = false
        binding.layoutQuestionContainer.isVisible = true
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pendingMoveToNext?.let { binding.root.removeCallbacks(it) }
        pendingMoveToNext = null

        currentButtons.forEach { it.setOnClickListener(null) }
        currentButtons = emptyList()

        _definitionBinding = null
        _contextSentenceBinding = null
        _binding = null
    }
}
