package com.example.linguify.ui.review

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

    private var currentButtons: List<Button> = emptyList()

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
                    is ReviewViewModel.ReviewState.Loading -> {
                        showLoading()
                    }
                    is ReviewViewModel.ReviewState.SessionReady -> {
                        hideLoading()
                        questions = state.session.questions
                        if (questions.isNotEmpty()) {
                            showQuestion(0)
                        }
                    }
                    is ReviewViewModel.ReviewState.QuestionAnswered -> {
                        android.util.Log.d("ReviewFragment", "Question answered: isCorrect = ${state.isCorrect}")
                        showAnswerFeedback(state.isCorrect)
                    }
                    is ReviewViewModel.ReviewState.SessionCompleted -> {
                        showSessionResults(state.session)
                    }
                    is ReviewViewModel.ReviewState.Error -> {
                        showError(state.message)
                        binding.btnSubmit.isEnabled = true
                    }
                }
            }
        }
    }

    private fun showQuestion(index: Int) {
        android.util.Log.d("ReviewFragment", "showQuestion called with index: $index")

        if (index >= questions.size) {
            android.util.Log.d("ReviewFragment", "All questions completed, finishing session")
            viewModel.completeSession()
            return
        }

        currentQuestionIndex = index
        val question = questions[index]

        android.util.Log.d("ReviewFragment", "Showing question ${index + 1}/${questions.size}: ${question.word.text}")

        resetQuestionUI()

        binding.progressBar.max = questions.size
        binding.progressBar.progress = index + 1
        binding.tvProgress.text = "${index + 1}/${questions.size}"

        when (question) {
            is ReviewQuestionType.MultipleChoice -> {
                android.util.Log.d("ReviewFragment", "Setting up Multiple Choice question")
                showMultipleChoice(question)
            }
            is ReviewQuestionType.ContextSentence -> {
                android.util.Log.d("ReviewFragment", "Setting up Context Sentence question")
                showContextSentence(question)
            }
            is ReviewQuestionType.Definition -> {
                android.util.Log.d("ReviewFragment", "Setting up Definition question")
                showDefinition(question)
            }
        }

        android.util.Log.d("ReviewFragment", "Question setup complete")
    }

    private fun showMultipleChoice(question: ReviewQuestionType.MultipleChoice) {
        binding.layoutQuestionContainer.isVisible = true
        binding.layoutMultipleChoice.isVisible = true

        binding.tvQuestionText.text = question.questionText
        binding.tvWordDisplay.text = question.word.text

        binding.btnOption1.text = question.options[0]
        binding.btnOption2.text = question.options[1]
        binding.btnOption3.text = question.options[2]
        binding.btnOption4.text = question.options[3]

        val buttons = listOf(
            binding.btnOption1,
            binding.btnOption2,
            binding.btnOption3,
            binding.btnOption4
        )

        setupOptionButtons(buttons)
    }

    private fun showContextSentence(question: ReviewQuestionType.ContextSentence) {
        binding.layoutQuestionContainer.isVisible = true
        binding.layoutContextSentence.root.isVisible = true

        binding.tvQuestionText.text = "Complete the sentence:"
        _contextSentenceBinding?.tvSentence?.text = question.sentence

        _contextSentenceBinding?.let { contextBinding ->
            contextBinding.btnOption1.text = question.options[0]
            contextBinding.btnOption2.text = question.options[1]
            contextBinding.btnOption3.text = question.options[2]
            contextBinding.btnOption4.text = question.options[3]

            val buttons = listOf(
                contextBinding.btnOption1,
                contextBinding.btnOption2,
                contextBinding.btnOption3,
                contextBinding.btnOption4
            )

            setupOptionButtons(buttons)
        }
    }

    private fun showDefinition(question: ReviewQuestionType.Definition) {
        binding.layoutQuestionContainer.isVisible = true
        binding.layoutDefinition.root.isVisible = true

        binding.tvQuestionText.text = "Which definition matches '${question.word.text}'?"

        _definitionBinding?.let { defBinding ->
            defBinding.btnOption1.text = question.definitions[0]
            defBinding.btnOption2.text = question.definitions[1]
            defBinding.btnOption3.text = question.definitions[2]
            defBinding.btnOption4.text = question.definitions[3]

            val buttons = listOf(
                defBinding.btnOption1,
                defBinding.btnOption2,
                defBinding.btnOption3,
                defBinding.btnOption4
            )

            setupOptionButtons(buttons)
        }
    }

    private fun setupOptionButtons(buttons: List<Button>) {
        android.util.Log.d("ReviewFragment", "Setting up ${buttons.size} option buttons")

        currentButtons = buttons
        selectedOptionIndex = null

        buttons.forEachIndexed { index, button ->
            button.isSelected = false
            button.isClickable = true
            button.setBackgroundColor(context?.getColor(android.R.color.transparent) ?: 0)

            button.setOnClickListener(null)

            button.setOnClickListener { view ->
                android.util.Log.d("ReviewFragment", "Button $index clicked (text: ${button.text})")

                if (view.isClickable) {
                    selectOption(index, buttons)
                }
            }

            android.util.Log.d("ReviewFragment", "Button $index setup: text='${button.text}', clickable=${button.isClickable}")
        }

        binding.btnSubmit.isEnabled = false
        android.util.Log.d("ReviewFragment", "All ${buttons.size} buttons setup complete")
    }

    private fun selectOption(index: Int, buttons: List<Button>) {
        android.util.Log.d("ReviewFragment", "selectOption called with index: $index")

        if (selectedOptionIndex == index) {
            android.util.Log.d("ReviewFragment", "Same option selected, ignoring")
            return
        }

        buttons.forEach {
            it.isSelected = false
            it.setBackgroundColor(context?.getColor(android.R.color.transparent) ?: 0)
        }

        buttons[index].isSelected = true
        buttons[index].setBackgroundColor(context?.getColor(android.R.color.holo_blue_light) ?: 0)

        selectedOptionIndex = index
        binding.btnSubmit.isEnabled = true

        android.util.Log.d("ReviewFragment", "Option $index selected, selectedOptionIndex: $selectedOptionIndex, submit enabled: ${binding.btnSubmit.isEnabled}")
    }

    private fun submitAnswer() {
        val selectedIndex = selectedOptionIndex

        android.util.Log.d("ReviewFragment", "submitAnswer called - selectedIndex: $selectedIndex")
        android.util.Log.d("ReviewFragment", "Submit button enabled: ${binding.btnSubmit.isEnabled}")
        android.util.Log.d("ReviewFragment", "Current buttons size: ${currentButtons.size}")

        if (selectedIndex == null) {
            android.util.Log.w("ReviewFragment", "No option selected!")
            Toast.makeText(context, "Please select an option", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmit.isEnabled = false
        android.util.Log.d("ReviewFragment", "Submit button disabled")

        currentButtons.forEach {
            it.isClickable = false
            android.util.Log.d("ReviewFragment", "Button disabled: ${it.text}")
        }

        val question = questions[currentQuestionIndex]
        val correctIndex = when (question) {
            is ReviewQuestionType.MultipleChoice -> question.correctIndex
            is ReviewQuestionType.ContextSentence -> question.correctIndex
            is ReviewQuestionType.Definition -> question.correctIndex
        }

        val isCorrect = selectedIndex == correctIndex

        android.util.Log.d("ReviewFragment", "Answer check - selected: $selectedIndex, correct: $correctIndex, isCorrect: $isCorrect")

        showAnswerColors(selectedIndex, correctIndex, isCorrect)

        android.util.Log.d("ReviewFragment", "Calling viewModel.submitAnswer")
        viewModel.submitAnswer(currentQuestionIndex, selectedIndex)
    }

    private fun showAnswerColors(selectedIndex: Int, correctIndex: Int, isCorrect: Boolean) {
        if (selectedIndex < currentButtons.size && correctIndex < currentButtons.size) {
            if (isCorrect) {
                currentButtons[selectedIndex].setBackgroundColor(
                    context?.getColor(android.R.color.holo_green_light) ?: 0
                )
                android.util.Log.d("ReviewFragment", "Correct answer - button $selectedIndex turned green")
            } else {
                currentButtons[selectedIndex].setBackgroundColor(
                    context?.getColor(android.R.color.holo_red_light) ?: 0
                )
                currentButtons[correctIndex].setBackgroundColor(
                    context?.getColor(android.R.color.holo_green_light) ?: 0
                )
                android.util.Log.d("ReviewFragment", "Wrong answer - button $selectedIndex turned red, button $correctIndex turned green")
            }
        }
    }

    private fun showAnswerFeedback(isCorrect: Boolean) {
        val message = if (isCorrect) "Correct! ✓" else "Incorrect ✗"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

        android.util.Log.d("ReviewFragment", "Answer feedback: $message")

        if (isCorrect) {
            binding.root.postDelayed({
                moveToNextQuestion()
            }, 2000)
        } else {
            binding.root.postDelayed({
                moveToNextQuestion()
            }, 3000)
        }
    }

    private fun moveToNextQuestion() {
        android.util.Log.d("ReviewFragment", "Moving to next question")

        currentQuestionIndex++
        android.util.Log.d("ReviewFragment", "Current question index: $currentQuestionIndex")

        showQuestion(currentQuestionIndex)
    }

    private fun skipQuestion() {
        moveToNextQuestion()
    }

    private fun showSessionResults(session: ReviewSession) {
        val totalQuestions = session.questions.size
        val score = session.score ?: 0

        Toast.makeText(
            context,
            "Session completed!\nScore: $score/$totalQuestions",
            Toast.LENGTH_LONG
        ).show()

        findNavController().navigateUp()
    }

    private fun resetQuestionUI() {
        android.util.Log.d("ReviewFragment", "Resetting question UI")

        binding.layoutMultipleChoice.isVisible = false
        binding.layoutContextSentence.root.isVisible = false
        binding.layoutDefinition.root.isVisible = false

        currentButtons.forEach { button ->
            button.setOnClickListener(null)
            button.isSelected = false
            button.isClickable = true
            button.setBackgroundColor(context?.getColor(android.R.color.transparent) ?: 0)
        }
        currentButtons = emptyList()

        selectedOptionIndex = null
        binding.btnSubmit.isEnabled = false

        android.util.Log.d("ReviewFragment", "Question UI reset complete")
    }

    private fun showLoading() {
        binding.progressLoading.isVisible = true
        binding.layoutQuestionContainer.isVisible = false
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
        currentButtons.forEach { it.setOnClickListener(null) }
        currentButtons = emptyList()

        _definitionBinding = null
        _contextSentenceBinding = null
        _binding = null
    }
}