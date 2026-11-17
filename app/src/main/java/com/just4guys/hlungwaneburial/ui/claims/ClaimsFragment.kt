package com.just4guys.hlungwaneburial.ui.claims

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
import com.just4guys.hlungwaneburial.data.model.Claim
import com.just4guys.hlungwaneburial.data.model.Policy
import com.just4guys.hlungwaneburial.data.remote.Supabase
import com.just4guys.hlungwaneburial.databinding.FragmentClaimsBinding
import com.just4guys.hlungwaneburial.databinding.ItemClaimBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class ClaimsFragment : Fragment() {
    private var _binding: FragmentClaimsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClaimsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSubmitNewClaim.setOnClickListener {
            findNavController().navigate(R.id.action_claimsFragment_to_submitClaimFragment)
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadClaims()
        }
    }

    override fun onResume() {
        super.onResume()
        loadClaims()
    }

    private fun loadClaims() {
        lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true
            try {
                val currentSession = Supabase.client.auth.currentSessionOrNull()
                if (currentSession == null) {
                    showEmptyState()
                    return@launch
                }
                val userId = currentSession.user?.id ?: run {
                    showEmptyState()
                    return@launch
                }

                val policiesList = Supabase.client.postgrest["policies"]
                    .select { filter { eq("user_id", userId) } }
                    .decodeList<Policy>()

                if (policiesList.isEmpty()) {
                    showEmptyState()
                    return@launch
                }

                val policyIds = policiesList.map { it.id }

                val claimsList = Supabase.client.postgrest["claims"]
                    .select {
                        filter { isIn("policy_id", policyIds) }
                        order("submitted_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }
                    .decodeList<Claim>()

                if (claimsList.isEmpty()) {
                    showEmptyState()
                } else {
                    binding.llEmptyState.visibility = View.GONE
                    binding.llClaimsList.removeAllViews()

                    for (claim in claimsList) {
                        val itemBinding = ItemClaimBinding.inflate(layoutInflater, binding.llClaimsList, false)
                        itemBinding.tvClaimNumber.text = claim.claimNumber
                        itemBinding.tvDeceasedName.text = "Deceased: ${claim.deceasedFullName}"
                        itemBinding.tvClaimDate.text = "Date: ${claim.dateOfDeath}"
                        itemBinding.tvClaimAmount.text = "R${claim.claimAmount.toInt()}"
                        itemBinding.tvClaimStatus.text = claim.status.replace("_", " ").uppercase()

                        val statusColor = when (claim.status) {
                            "approved", "paid" -> Color.parseColor("#2E7D32")
                            "under_review", "documents_checked" -> Color.parseColor("#ED6C02")
                            "rejected" -> Color.parseColor("#C62828")
                            else -> Color.parseColor("#0277BD")
                        }
                        itemBinding.tvClaimStatus.background.mutate().setTint(statusColor)

                        binding.llClaimsList.addView(itemBinding.root)
                    }
                }
            } catch (e: Exception) {
                if (_binding != null && isAdded) Toast.makeText(requireContext(), "Error loading claims: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun showEmptyState() {
        binding.llEmptyState.visibility = View.VISIBLE
        binding.llClaimsList.removeAllViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}