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
import com.example.linguify.databinding.FragmentLoginBinding
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {
    private val viewModel: LoginViewModel by viewModels()
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInputListeners()
        setupLoginButton()
        setupAdditionalLinks()
        observeLoginResults()
        setupRememberMeCheckbox()
    }

    private fun setupRememberMeCheckbox() {
        binding.checkBoxRememberMe.isChecked = true
    }

    private fun setupInputListeners() {
        binding.editTextEmail.doAfterTextChanged { email ->
            validateEmail(email.toString(), binding.textInputLayoutEmail)
        }

        binding.editTextPassword.doAfterTextChanged { password ->
            validatePassword(password.toString(), binding.textInputLayoutPassword)
        }
    }

    private fun setupLoginButton() {
        binding.buttonLogin.setOnClickListener {
            performLogin()
        }
    }

    private fun setupAdditionalLinks() {
        binding.textViewForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }

        binding.textViewSignup.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun validateEmail(email: String, emailLayout: TextInputLayout): Boolean {
        if (email.isEmpty()) {
            emailLayout.error = getString(R.string.email_required)
            emailLayout.isErrorEnabled = true
            return false
        } else if (!viewModel.isEmailValid(email)) {
            emailLayout.error = getString(R.string.invalid_email)
            emailLayout.isErrorEnabled = true
            return false
        } else {
            emailLayout.isErrorEnabled = false
            return true
        }
    }

    private fun validatePassword(password: String, passwordLayout: TextInputLayout): Boolean {
        if (password.isEmpty()) {
            passwordLayout.error = getString(R.string.password_required)
            passwordLayout.isErrorEnabled = true
            return false
        } else if (!viewModel.isPasswordValid(password)) {
            passwordLayout.error = getString(R.string.invalid_password)
            passwordLayout.isErrorEnabled = true
            return false
        } else {
            passwordLayout.isErrorEnabled = false
            return true
        }
    }

    private fun performLogin() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        val rememberMe = binding.checkBoxRememberMe.isChecked

        val isEmailValid = validateEmail(email, binding.textInputLayoutEmail)
        val isPasswordValid = validatePassword(password, binding.textInputLayoutPassword)

        if (isEmailValid && isPasswordValid) {
            viewModel.login(email, password, rememberMe)
        }
    }

    private fun observeLoginResults() {
        viewModel.loginResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is LoginResult.Loading -> {
                    binding.progressBarLoading.visibility = View.VISIBLE
                    binding.buttonLogin.isEnabled = false
                }

                is LoginResult.Success -> {
                    binding.progressBarLoading.visibility = View.GONE
                    binding.buttonLogin.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.welcome_message, result.firebaseUser.email),
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                }

                is LoginResult.Error -> {
                    binding.progressBarLoading.visibility = View.GONE
                    binding.buttonLogin.isEnabled = true
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}