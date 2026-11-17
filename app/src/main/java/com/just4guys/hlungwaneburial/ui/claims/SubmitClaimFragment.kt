package com.just4guys.hlungwaneburial.ui.claims

import android.app.DatePickerDialog
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
import com.just4guys.hlungwaneburial.data.model.Policy
import com.just4guys.hlungwaneburial.data.remote.Supabase
import com.just4guys.hlungwaneburial.databinding.FragmentSubmitClaimBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.time.LocalDate
import java.math.BigDecimal
import com.just4guys.hlungwaneburial.domain.policy.FuneralPolicyCalculator

class SubmitClaimFragment : Fragment() {
    private var _binding: FragmentSubmitClaimBinding? = null
    private val binding get() = _binding!!

    private var userPolicies: List<Policy> = emptyList()
    private var selectedPolicyId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubmitClaimBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadPolicies()

        // Date Picker
        binding.etDateOfDeath.setOnClickListener {
            showDatePicker()
        }

        binding.btnSubmitClaim.setOnClickListener {
            submitClaim()
        }
    }

    private fun loadPolicies() {
        selectedPolicyId = null
        binding.actvPolicy.setText("", false)
        binding.actvPolicy.hint = "Loading your policies..."
        binding.actvPolicy.isEnabled = false
        binding.tilPolicy.setEndIconOnClickListener { openPolicyOptions() }
        binding.actvPolicy.setOnClickListener { openPolicyOptions() }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val session = Supabase.client.auth.currentSessionOrNull()
                val userId = session?.user?.id
                if (userId == null) {
                    binding.tvClaimPolicySummary.text = "Please sign in to view your policies."
                    return@launch
                }
                userPolicies = Supabase.client.postgrest["policies"]
                    .select { filter { eq("user_id", userId) } }
                    .decodeList<Policy>()

                if (userPolicies.isEmpty()) {
                    binding.actvPolicy.hint = "No policies available"
                    binding.tvClaimPolicySummary.text =
                        "No policies are linked to your account. Please contact Hlungwane Burial to register or link your policy."
                    return@launch
                }
                val labels = userPolicies.map { policy ->
                    "${policy.policyNumber} • ${policy.policyType} • R${String.format(Locale.US, "%,.2f", policy.coverAmount)} • ${policy.status}"
                }
                binding.actvPolicy.setAdapter(
                    ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, labels)
                )
                binding.actvPolicy.setOnItemClickListener { _, _, position, _ ->
                    val policy = userPolicies.getOrNull(position) ?: return@setOnItemClickListener
                    selectedPolicyId = policy.id
                    val start = runCatching { LocalDate.parse(policy.startDate.take(10)) }.getOrNull()
                    val waiting = start?.let { FuneralPolicyCalculator.trialEndsOn(it) }
                    binding.tvClaimPolicySummary.text = "Policy: ${policy.policyNumber}\n" +
                        "Cover limit: R${String.format(Locale.US, "%.2f", policy.coverAmount)}\n" +
                        "Status: ${policy.status}\n" +
                        (if (waiting != null) "3-month waiting period ends: $waiting\n" else "Waiting-period date unavailable\n") +
                        "During the trial period, an estimated R1,000 deduction may apply. " +
                        "Arrears and final payout require staff verification."
                    binding.etClaimAmount.setText(String.format(Locale.US, "%.2f", policy.coverAmount))
                }
                binding.actvPolicy.hint = "Select your policy"
                binding.actvPolicy.isEnabled = true
            } catch (e: Exception) {
                binding.actvPolicy.hint = "Policies unavailable"
                binding.tvClaimPolicySummary.text =
                    "Could not load policies. Check your connection and try reopening this screen."
                Toast.makeText(context, "Could not load policies: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openPolicyOptions() {
        if (userPolicies.isEmpty()) {
            Toast.makeText(context, "No linked policies available", Toast.LENGTH_SHORT).show()
        } else {
            binding.actvPolicy.showDropDown()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val dialog = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selected = Calendar.getInstance()
                selected.set(year, month, day)
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                binding.etDateOfDeath.setText(fmt.format(selected.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.maxDate = System.currentTimeMillis()
        dialog.show()
    }

    private fun submitClaim() {
        val deceasedName = binding.etDeceasedName.text?.toString()?.trim().orEmpty()
        val deceasedId = binding.etDeceasedSaId.text?.toString()?.trim().orEmpty()
        val dateOfDeath = binding.etDateOfDeath.text?.toString()?.trim().orEmpty()
        val amountStr = binding.etClaimAmount.text?.toString()?.trim().orEmpty()
        val notes = binding.etNotes.text?.toString()?.trim().orEmpty()

        if (selectedPolicyId == null) {
            Toast.makeText(requireContext(), "Please select a policy", Toast.LENGTH_SHORT).show()
            return
        }
        if (deceasedName.isEmpty() || deceasedId.isEmpty() || dateOfDeath.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (deceasedId.length != 13 || !deceasedId.all { it.isDigit() }) {
            Toast.makeText(requireContext(), "SA ID must be 13 digits", Toast.LENGTH_SHORT).show()
            return
        }
        val selectedPolicy = userPolicies.firstOrNull { it.id == selectedPolicyId }
        if (selectedPolicy == null) {
            Toast.makeText(requireContext(), "Please select a valid policy", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedPolicy.status.lowercase() in setOf("cancelled", "lapsed")) {
            Toast.makeText(requireContext(), "This policy requires staff review before a claim can be submitted", Toast.LENGTH_LONG).show()
            return
        }
        val claimAmount = amountStr.toDoubleOrNull()
        if (claimAmount == null || claimAmount <= 0) {
            Toast.makeText(requireContext(), "Enter a valid claim amount", Toast.LENGTH_SHORT).show()
            return
        }

        if (!claimAmount.isFinite() || claimAmount > selectedPolicy.coverAmount) {
            Toast.makeText(requireContext(), "Claim amount cannot exceed the selected policy cover", Toast.LENGTH_LONG).show()
            return
        }
        val deathDate = runCatching { LocalDate.parse(dateOfDeath) }.getOrNull()
        val startDate = runCatching { LocalDate.parse(selectedPolicy.startDate.take(10)) }.getOrNull()
        if (deathDate == null || deathDate.isAfter(LocalDate.now()) ||
            (startDate != null && deathDate.isBefore(startDate))) {
            Toast.makeText(requireContext(), "Enter a valid date of death within the policy period", Toast.LENGTH_LONG).show()
            return
        }
        // Informational estimate only. Do not submit client-calculated deductions as authoritative.
        val inTrial = startDate?.let { FuneralPolicyCalculator.withinThreeMonthTrial(it, deathDate) }
        if (inTrial == true) {
            val estimate = FuneralPolicyCalculator.estimateClaim(BigDecimal.valueOf(claimAmount), BigDecimal.ZERO, true)
            binding.tvClaimPolicySummary.text = "Trial-period claim. Provisional estimate (excluding unverified arrears): R${estimate.estimatedNetBenefit}. Final amount subject to staff verification."
        }
        binding.btnSubmitClaim.isEnabled = false
        binding.btnSubmitClaim.text = "Submitting..."

        lifecycleScope.launch {
            try {
                val claimNumber = "HB-CLM-${System.currentTimeMillis().toString().takeLast(8)}"

                Supabase.client.postgrest["claims"].insert(
                    buildJsonObject {
                        put("policy_id", selectedPolicyId)
                        put("claim_number", claimNumber)
                        put("deceased_full_name", deceasedName)
                        put("deceased_sa_id", deceasedId)
                        put("date_of_death", dateOfDeath)
                        put("claim_amount", claimAmount)
                        put("status", "submitted")
                        put("notes", notes)
                    }
                )

                Toast.makeText(requireContext(), "Claim $claimNumber submitted successfully!", Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to submit: ${e.message}", Toast.LENGTH_LONG).show()
                binding.btnSubmitClaim.isEnabled = true
                binding.btnSubmitClaim.text = "SUBMIT CLAIM"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}