package com.example.linguify.ui.leveltest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.linguify.model.TestQuestion
import com.example.linguify.utils.UserLevel
import com.example.linguify.databinding.FragmentLevelTestBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.example.linguify.R

@AndroidEntryPoint
class LevelTestFragment : Fragment() {

    private var _binding: FragmentLevelTestBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LevelTestViewModel by viewModels()
    private var currentQuestionIndex = 0
    private lateinit var currentQuestion: TestQuestion
    private var score = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLevelTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupListeners()

        binding.btnClose.setOnClickListener {
            findNavController().navigate(R.id.action_levelTestFragment_to_homeFragment)
        }

        viewModel.loadTestQuestions()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.testQuestions.collect { result ->
                when (result) {
                    is LevelTestViewModel.TestQuestionsState.Loading -> {
                        showLoading()
                    }
                    is LevelTestViewModel.TestQuestionsState.Success -> {
                        hideLoading()
                        if (result.questions.isNotEmpty()) {
                            updateQuestionUI(result.questions[currentQuestionIndex])
                        } else {
                            showError("No questions found. Please try again later.")
                        }
                    }
                    is LevelTestViewModel.TestQuestionsState.Error -> {
                        hideLoading()
                        showError(result.message)
                    }
                    LevelTestViewModel.TestQuestionsState.Initial -> {
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.levelSaveState.collect { result ->
                when (result) {
                    is LevelTestViewModel.LevelSaveState.Loading -> {
                        binding.progressBarSaving.visibility = View.VISIBLE
                    }
                    is LevelTestViewModel.LevelSaveState.Success -> {
                        binding.progressBarSaving.visibility = View.GONE
                        navigateToHome()
                    }
                    is LevelTestViewModel.LevelSaveState.Error -> {
                        binding.progressBarSaving.visibility = View.GONE
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                    }
                    LevelTestViewModel.LevelSaveState.Initial -> {
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnOptionA.setOnClickListener { checkAnswer(0) }
        binding.btnOptionB.setOnClickListener { checkAnswer(1) }
        binding.btnOptionC.setOnClickListener { checkAnswer(2) }
        binding.btnOptionD.setOnClickListener { checkAnswer(3) }
    }

    private fun updateQuestionUI(question: TestQuestion) {
        currentQuestion = question
        binding.tvQuestionNumber.text = "Question ${currentQuestionIndex + 1}/${viewModel.testQuestions.value.let { if (it is LevelTestViewModel.TestQuestionsState.Success) it.questions.size else 0 }}"
        binding.tvQuestion.text = question.questionText
        binding.btnOptionA.text = question.options[0]
        binding.btnOptionB.text = question.options[1]
        binding.btnOptionC.text = question.options[2]
        binding.btnOptionD.text = question.options[3]

        binding.btnOptionA.isEnabled = true
        binding.btnOptionB.isEnabled = true
        binding.btnOptionC.isEnabled = true
        binding.btnOptionD.isEnabled = true
    }

    private fun checkAnswer(selectedOptionIndex: Int) {
        binding.btnOptionA.isEnabled = false
        binding.btnOptionB.isEnabled = false
        binding.btnOptionC.isEnabled = false
        binding.btnOptionD.isEnabled = false

        if (selectedOptionIndex == currentQuestion.correctOptionIndex) {
            score++
        }

        val questionsSize = viewModel.testQuestions.value.let {
            if (it is LevelTestViewModel.TestQuestionsState.Success) it.questions.size else 0
        }

        if (currentQuestionIndex + 1 < questionsSize) {
            currentQuestionIndex++
            updateQuestionUI(
                (viewModel.testQuestions.value as LevelTestViewModel.TestQuestionsState.Success)
                    .questions[currentQuestionIndex]
            )
        } else {
            finishTest()
        }
    }

    private fun finishTest() {
        val questionsSize = viewModel.testQuestions.value.let {
            if (it is LevelTestViewModel.TestQuestionsState.Success) it.questions.size else 0
        }

        val userLevel = when {
            score < questionsSize * 0.3 -> UserLevel.BEGINNER
            score < questionsSize * 0.7 -> UserLevel.INTERMEDIATE
            else -> UserLevel.ADVANCED
        }

        binding.layoutQuestions.visibility = View.GONE
        binding.layoutResults.visibility = View.VISIBLE
        binding.tvTestResults.text = "Test completed!\nYour level: ${userLevel.displayName}"

        binding.btnContinue.setOnClickListener {
            viewModel.saveUserLevel(userLevel)
        }
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.action_levelTestFragment_to_homeFragment)
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.layoutQuestions.visibility = View.GONE
        binding.layoutResults.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.layoutQuestions.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
