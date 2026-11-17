package com.just4guys.hlungwaneburial.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.just4guys.hlungwaneburial.R
import com.just4guys.hlungwaneburial.data.remote.Supabase
import com.just4guys.hlungwaneburial.databinding.FragmentLoginBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch
import io.github.jan.supabase.auth.providers.Google

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString()?.trim().orEmpty()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLogin(email, password)
        }

        binding.btnGoogleLogin.setOnClickListener {
            lifecycleScope.launch {
                try {
                    Supabase.client.auth.signInWith(Google)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Google sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(requireContext(), "Password reset link will be sent by email", Toast.LENGTH_SHORT).show()
        }

        binding.tvCreateAccount.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun performLogin(email: String, password: String) {
        Supabase.configurationError()?.let { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            return
        }

        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "Logging in..."

        lifecycleScope.launch {
            try {
                // Call Supabase Auth to sign in
                Supabase.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                // If successful, navigate to Home Dashboard
                Toast.makeText(requireContext(), "Welcome back!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)

            } catch (e: Exception) {
                // If login fails (wrong password, etc.)
                Toast.makeText(requireContext(), "Login failed. Check your connection and account details.", Toast.LENGTH_LONG).show()
                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = "LOGIN"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}