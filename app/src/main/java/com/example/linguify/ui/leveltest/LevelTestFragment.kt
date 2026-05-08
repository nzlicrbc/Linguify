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
                            updateQuestionUI(result.questions[viewModel.currentQuestionIndex.value])
                        } else {
                            showError(getString(R.string.no_questions_found))
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
            viewModel.currentQuestionIndex.collect { index ->
                val state = viewModel.testQuestions.value
                if (state is LevelTestViewModel.TestQuestionsState.Success && index < state.questions.size) {
                    updateQuestionUI(state.questions[index])
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.quizCompleted.collect { userLevel ->
                if (userLevel != null) {
                    showResults(userLevel)
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
        binding.btnOptionA.setOnClickListener { onOptionSelected(0) }
        binding.btnOptionB.setOnClickListener { onOptionSelected(1) }
        binding.btnOptionC.setOnClickListener { onOptionSelected(2) }
        binding.btnOptionD.setOnClickListener { onOptionSelected(3) }
    }

    private fun updateQuestionUI(question: TestQuestion) {
        val questionsSize = (viewModel.testQuestions.value as? LevelTestViewModel.TestQuestionsState.Success)?.questions?.size ?: 0
        binding.tvQuestionNumber.text = getString(R.string.question_progress, viewModel.currentQuestionIndex.value + 1, questionsSize)
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

    private fun onOptionSelected(selectedOptionIndex: Int) {
        binding.btnOptionA.isEnabled = false
        binding.btnOptionB.isEnabled = false
        binding.btnOptionC.isEnabled = false
        binding.btnOptionD.isEnabled = false

        viewModel.answerQuestion(selectedOptionIndex)
    }

    private fun showResults(userLevel: UserLevel) {
        binding.layoutQuestions.visibility = View.GONE
        binding.layoutResults.visibility = View.VISIBLE
        binding.tvTestResults.text = getString(R.string.test_completed, userLevel.displayName)

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
