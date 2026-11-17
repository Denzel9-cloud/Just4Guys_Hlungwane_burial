package com.just4guys.hlungwaneburial.ui.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.just4guys.hlungwaneburial.R
import com.just4guys.hlungwaneburial.data.model.Notification
import com.just4guys.hlungwaneburial.data.model.Policy
import com.just4guys.hlungwaneburial.data.model.Profile
import com.just4guys.hlungwaneburial.data.remote.Supabase
import com.just4guys.hlungwaneburial.databinding.FragmentHomeBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogout.setOnClickListener {
            lifecycleScope.launch {
                try {
                    Supabase.client.auth.signOut()
                    findNavController().navigate(R.id.action_homeFragment_to_loginFragment)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Logout failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.cardMyPolicy.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_myPolicyFragment)
        }
        binding.cardSubmitClaim.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_claimsFragment)
        }
        binding.cardTrackClaim.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_claimsFragment)
        }
        binding.cardPayments.setOnClickListener {
            Toast.makeText(requireContext(), "Payments history coming soon", Toast.LENGTH_SHORT).show()
        }

        // Pull to refresh — this is the NEW part
        binding.swipeRefresh.setOnRefreshListener {
            loadDashboardData()
        }

        loadDashboardData()
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true
            try {
                val currentSession = Supabase.client.auth.currentSessionOrNull()
                if (currentSession == null) return@launch
                val userId = currentSession.user?.id ?: return@launch

                val profileResponse = Supabase.client.postgrest["profiles"]
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<Profile>()

                profileResponse?.let { profile ->
                    val name = profile.fullName?.split(" ")?.firstOrNull() ?: "Family Member"
                    binding.tvWelcome.text = "Welcome back, $name"
                }

                val policiesResponse = Supabase.client.postgrest["policies"]
                    .select {
                        filter { eq("user_id", userId) }
                        limit(1)
                    }
                    .decodeSingleOrNull<Policy>()

                if (policiesResponse != null) {
                    val policy = policiesResponse
                    binding.tvPolicyStatus.text = policy.policyType
                    binding.tvPolicyDetails.text = "Cover: R${policy.coverAmount.toInt()} | Premium: R${policy.monthlyPremium.toInt()}/mo"

                    binding.tvStatusBadge.text = policy.status.uppercase()
                    val badgeBg = binding.tvStatusBadge.background.mutate()
                    val color = when (policy.status) {
                        "active" -> Color.parseColor("#2E7D32")
                        "pending" -> Color.parseColor("#ED6C02")
                        "suspended", "cancelled" -> Color.parseColor("#C62828")
                        else -> Color.parseColor("#6E635A")
                    }
                    badgeBg.setTint(color)
                } else {
                    binding.tvPolicyStatus.text = "No policy active"
                    binding.tvStatusBadge.text = "NONE"
                    binding.tvStatusBadge.background.mutate().setTint(Color.parseColor("#6E635A"))
                }

                val notificationsResponse = Supabase.client.postgrest["notifications"]
                    .select {
                        filter { eq("user_id", userId) }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(1)
                    }
                    .decodeSingleOrNull<Notification>()

                if (notificationsResponse != null) {
                    binding.tvLatestNotification.text = "${notificationsResponse.title}: ${notificationsResponse.body}"
                }

            } catch (e: Exception) {
                e.printStackTrace()
                binding.tvLatestNotification.text = "Could not load notifications."
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}