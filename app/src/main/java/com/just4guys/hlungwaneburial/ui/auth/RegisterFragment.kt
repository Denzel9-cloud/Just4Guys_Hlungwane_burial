package com.just4guys.hlungwaneburial.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.just4guys.hlungwaneburial.R
import com.just4guys.hlungwaneburial.data.remote.Supabase
import com.just4guys.hlungwaneburial.databinding.FragmentRegisterBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonPrimitive

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val provinces = listOf(
        "Gauteng", "KwaZulu-Natal", "Western Cape", "Eastern Cape",
        "Limpopo", "Mpumalanga", "North West", "Free State", "Northern Cape"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupProvinceDropdown()

        binding.btnRegister.setOnClickListener {
            handleRegistration()
        }

        binding.tvBackToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun setupProvinceDropdown() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, provinces)
        binding.actvProvince.setAdapter(adapter)
    }

    private fun handleRegistration() {
        Supabase.configurationError()?.let { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            return
        }

        val fullName = binding.etFullName.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val saId = binding.etSaId.text?.toString()?.trim().orEmpty()
        val phone = binding.etPhone.text?.toString()?.trim().orEmpty()
        val province = binding.actvProvince.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()

        // Basic Validation
        if (fullName.isEmpty() || email.isEmpty() || saId.isEmpty() || phone.isEmpty() || province.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (saId.length != 13 || !saId.all { it.isDigit() }) {
            Toast.makeText(requireContext(), "SA ID must be exactly 13 digits", Toast.LENGTH_SHORT).show()
            return
        }

        val passwordError = when {
            password.length < 8 -> "Use at least 8 characters"
            password.none { it.isUpperCase() } -> "Include an uppercase letter (A-Z)"
            password.none { it.isLowerCase() } -> "Include a lowercase letter (a-z)"
            password.none { it.isDigit() } -> "Include a number (0-9)"
            password.none { !it.isLetterOrDigit() && !it.isWhitespace() } -> "Include a symbol such as @, # or !"
            else -> null
        }
        if (passwordError != null) {
            binding.tilPassword.error = passwordError
            binding.etPassword.requestFocus()
            return
        }
        binding.tilPassword.error = null

        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            binding.etConfirmPassword.requestFocus()
            return
        }
        binding.tilConfirmPassword.error = null

        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = "Creating account..."

        // Call Supabase Auth
        lifecycleScope.launch {
            try {
                Supabase.client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                    this.data = buildJsonObject {
                        put("full_name", JsonPrimitive(fullName))
                        put("sa_id_number", JsonPrimitive(saId))
                        put("phone_number", JsonPrimitive(phone))
                        put("province", JsonPrimitive(province))
                    }
                }
                Toast.makeText(requireContext(), "Account created successfully! Please log in.", Toast.LENGTH_LONG).show()

                // Go back to login
                findNavController().navigate(R.id.action_registerFragment_to_loginFragment)

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Registration failed. Check your connection or account details.", Toast.LENGTH_LONG).show()
                binding.btnRegister.isEnabled = true
                binding.btnRegister.text = "CREATE ACCOUNT"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}