package com.example.linguify.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.linguify.R
import com.example.linguify.databinding.FragmentRegisterBinding
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment() {
    private val viewModel: RegisterViewModel by viewModels()
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInputListeners()
        setupRegisterButton()
        setupBackToLogin()
        observeRegistrationResult()
    }

    private fun observeRegistrationResult() {
        viewModel.registerResult.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is RegisterResult.Success -> {
                    binding.buttonRegister.isEnabled = true
                    findNavController().navigate(R.id.action_registerFragment_to_onboardingFragment)
                }
                is RegisterResult.Error -> {
                    binding.buttonRegister.isEnabled = true
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                }
                is RegisterResult.Loading -> {
                    binding.buttonRegister.isEnabled = false
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupInputListeners() {
        binding.editTextUsername.doAfterTextChanged { username ->
            validateUsername(username.toString(), binding.textInputLayoutUsername)
        }

        binding.editTextEmail.doAfterTextChanged { email ->
            validateEmail(email.toString(), binding.textInputLayoutEmail)
        }

        binding.editTextPassword.doAfterTextChanged { password ->
            validatePassword(password.toString(), binding.textInputLayoutPassword)
        }

        binding.editTextConfirmPassword.doAfterTextChanged { confirmPassword ->
            validateConfirmPassword(
                confirmPassword.toString(),
                binding.textInputLayoutConfirmPassword,
                binding.editTextPassword.text.toString()
            )
        }
    }

    private fun validateUsername(username: String, inputLayout: TextInputLayout) {
        if (username.length < 3) {
            inputLayout.error = getString(R.string.error_username_length)
        } else {
            inputLayout.error = null
        }
    }

    private fun validateEmail(email: String, inputLayout: TextInputLayout) {
        if (email.isEmpty()) {
            inputLayout.error = getString(R.string.error_field_required)
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputLayout.error = getString(R.string.invalid_email)
        } else {
            inputLayout.error = null
        }
    }

    private fun validatePassword(password: String, inputLayout: TextInputLayout) {
        if (password.length < 6) {
            inputLayout.error = getString(R.string.error_password_length)
        } else {
            inputLayout.error = null
        }
    }

    private fun validateConfirmPassword(
        confirmPassword: String,
        inputLayout: TextInputLayout,
        password: String
    ) {
        if (confirmPassword != password) {
            inputLayout.error = getString(R.string.error_password_mismatch)
        } else {
            inputLayout.error = null
        }
    }

    private fun setupRegisterButton() {
        binding.buttonRegister.setOnClickListener {
            val username = binding.editTextUsername.text.toString()
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()
            val confirmPassword = binding.editTextConfirmPassword.text.toString()

            if (username.length < 3) {
                binding.textInputLayoutUsername.error = getString(R.string.error_username_length)
                return@setOnClickListener
            }

            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.textInputLayoutEmail.error = getString(R.string.invalid_email)
                return@setOnClickListener
            }

            if (password.length < 6) {
                binding.textInputLayoutPassword.error = getString(R.string.error_password_length)
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                binding.textInputLayoutConfirmPassword.error =
                    getString(R.string.error_password_mismatch)
                return@setOnClickListener
            }

            viewModel.registerUser(username, email, password)
        }
    }

    private fun setupBackToLogin() {
        binding.textViewLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }
}