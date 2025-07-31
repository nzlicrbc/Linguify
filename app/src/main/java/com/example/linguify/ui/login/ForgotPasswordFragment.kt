package com.example.linguify.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.linguify.R
import com.example.linguify.databinding.FragmentForgotPasswordBinding
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordFragment : Fragment() {
    private val viewModel: ForgotPasswordViewModel by viewModels()
    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInputListener()
        setupResetButton()
        setupBackToLogin()
        observeResetResults()
    }

    private fun setupInputListener() {
        binding.editTextEmail.doAfterTextChanged { email ->
            validateEmail(email.toString(), binding.textInputLayoutEmail)
        }
    }

    private fun validateEmail(email: String, inputLayout: TextInputLayout): Boolean {
        val isValid = viewModel.isEmailValid(email)
        if (!isValid) {
            inputLayout.error = getString(R.string.invalid_email)
        } else {
            inputLayout.error = null
        }
        return isValid
    }

    private fun setupResetButton() {
        binding.buttonForgotPassword.setOnClickListener {
            val email = binding.editTextEmail.text.toString()

            if (!validateEmail(email, binding.textInputLayoutEmail)) {
                return@setOnClickListener
            }

            viewModel.resetPassword(email)
        }
    }

    private fun setupBackToLogin() {
        binding.textViewBackToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_forgotPasswordFragment_to_loginFragment)
        }
    }

    private fun observeResetResults() {
        viewModel.resetResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is ResetPasswordResult.Loading -> {

                }

                is ResetPasswordResult.Success -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.forgot_password_email_sent),
                        Toast.LENGTH_LONG
                    ).show()
                    findNavController().navigate(R.id.action_forgotPasswordFragment_to_loginFragment)
                }

                is ResetPasswordResult.Error -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}