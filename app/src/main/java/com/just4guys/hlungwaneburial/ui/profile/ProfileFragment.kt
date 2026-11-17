package com.just4guys.hlungwaneburial.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.just4guys.hlungwaneburial.R
import com.just4guys.hlungwaneburial.data.model.Profile
import com.just4guys.hlungwaneburial.data.remote.Supabase
import com.just4guys.hlungwaneburial.databinding.FragmentProfileBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogout.setOnClickListener {
            lifecycleScope.launch {
                try {
                    Supabase.client.auth.signOut()
                    // Navigate to login, clearing the backstack
                    findNavController().navigate(R.id.loginFragment)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Logout failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.tvContactSupport.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_contactSupportFragment)
        }

        binding.tvPrivacySecurity.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_privacyFragment)
        }

        loadProfile()
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                val session = Supabase.client.auth.currentSessionOrNull() ?: return@launch
                val userId = session.user?.id ?: return@launch

                val profile = runCatching {
                    Supabase.client.postgrest["profiles"]
                        .select { filter { eq("id", userId) } }
                        .decodeSingleOrNull<Profile>()
                }.getOrNull()

                // Registration stores personal details in Supabase Auth metadata.
                // Prefer verified profile rows, then fall back to this user's own metadata.
                val metadata = session.user?.userMetadata
                fun fromAuth(key: String): String? = runCatching {
                    metadata?.get(key)?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                }.getOrNull()
                fun prefer(primary: String?, key: String): String =
                    primary?.takeIf { it.isNotBlank() } ?: fromAuth(key) ?: "Not available"
                binding.tvProfileName.text = prefer(profile?.fullName, "full_name").let {
                    if (it == "Not available") "Family Member" else it
                }
                binding.tvProfileEmail.text = profile?.email?.takeIf { it.isNotBlank() }
                    ?: session.user?.email ?: "No email"
                binding.tvProfilePhone.text = "Phone: ${prefer(profile?.phoneNumber, "phone_number")}"
                binding.tvProfileProvince.text = "Province: ${prefer(profile?.province, "province")}"
                val id = prefer(profile?.saIdNumber, "sa_id_number")
                binding.tvProfileSaId.text = "SA ID: ${if (id.length == 13 && id.all(Char::isDigit)) "*********${id.takeLast(4)}" else "Not available"}"

            } catch (e: Exception) {
                if (!isAdded) return@launch
                Toast.makeText(requireContext(), "Unable to load profile details. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}