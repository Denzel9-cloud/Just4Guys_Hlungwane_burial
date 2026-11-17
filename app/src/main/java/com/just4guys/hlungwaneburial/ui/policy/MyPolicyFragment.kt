package com.just4guys.hlungwaneburial.ui.policy

import android.graphics.Color
import androidx.appcompat.app.AlertDialog
import com.just4guys.hlungwaneburial.data.model.PackageApplication
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.math.BigDecimal
import com.just4guys.hlungwaneburial.domain.policy.FuneralPolicyCalculator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.just4guys.hlungwaneburial.data.model.Beneficiary
import com.just4guys.hlungwaneburial.data.model.Payment
import com.just4guys.hlungwaneburial.data.model.Policy
import com.just4guys.hlungwaneburial.data.remote.Supabase
import com.just4guys.hlungwaneburial.databinding.FragmentMyPolicyBinding
import com.just4guys.hlungwaneburial.databinding.ItemBeneficiaryBinding
import com.just4guys.hlungwaneburial.databinding.ItemPaymentBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class MyPolicyFragment : Fragment() {
    private var _binding: FragmentMyPolicyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyPolicyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnChoosePackage.setOnClickListener { showPackageChoices() }
        binding.swipeRefresh.setOnRefreshListener {
            loadPolicyData()
        }

        loadPolicyData()
    }

    private val packageNames = listOf(
        "Basic Dignity", "Standard Family", "Executive / Prestige", "Royal / Presidential"
    )
    private val packagePrices = listOf(
        "R12,000–R16,000", "R22,000–R28,000", "R38,000–R48,000", "R65,000–R90,000+"
    )
    private val packageDetails = listOf(
        "Flat-lid pine/veneer coffin, hearse and family sedan, coffin spray, graveside gazebo and 20 chairs.",
        "Three-tier gloss wood casket, hearse and 2 family cars, rose/lily flowers, 100 programmes, tent and 50 chairs.",
        "Half-view solid wood casket, luxury hearse, 2 executive cars and VIP Quantum, luxury flowers, 200 programmes and 100 chairs.",
        "Presidential dome casket, presidential hearse, 3 luxury sedans and escort, floral blanket, 300+ booklets, marquee and 150+ chairs."
    )

    private fun showPackageChoices() {
        val options = packageNames.indices.map { i -> "Option ${i + 1}: ${packageNames[i]}\n${packagePrices[i]} (estimated once-off)" }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Choose your funeral package")
            .setItems(options) { _, index ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Option ${index + 1}: ${packageNames[index]}")
                    .setMessage("Estimated once-off price: ${packagePrices[index]}\n\n${packageDetails[index]}\n\nSubmit this choice for staff approval?")
                    .setNegativeButton("Back", null)
                    .setPositiveButton("Submit selection") { _, _ -> submitPackageChoice(index) }
                    .show()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun submitPackageChoice(index: Int) {
        val userId = Supabase.client.auth.currentSessionOrNull()?.user?.id
        if (userId == null) {
            Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_LONG).show()
            return
        }
        binding.btnChoosePackage.isEnabled = false
        lifecycleScope.launch {
            try {
                Supabase.client.postgrest["package_applications"].insert(
                    PackageApplication(userId = userId, optionNumber = index + 1)
                )
                Toast.makeText(requireContext(), "Selection submitted for staff approval", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Could not save selection: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                _binding?.btnChoosePackage?.isEnabled = true
            }
        }
    }

    private fun loadPolicyData() {
        lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true
            try {
                val currentSession = Supabase.client.auth.currentSessionOrNull()
                if (currentSession == null) return@launch
                val userId = currentSession.user?.id ?: return@launch

                val policy = Supabase.client.postgrest["policies"]
                    .select {
                        filter { eq("user_id", userId) }
                        limit(1)
                    }
                    .decodeSingleOrNull<Policy>()

                if (policy != null) {
                    binding.tvPolicyNumber.text = "Policy Number: ${policy.policyNumber}"
                    binding.tvPolicyType.text = "Type: ${policy.policyType}"
                    binding.tvPolicyCover.text = "Cover Amount: R${policy.coverAmount.toInt()}"
                    binding.tvPolicyPremium.text = "Monthly Premium: R${policy.monthlyPremium.toInt()}"
                    binding.tvPolicyStartDate.text = "Start Date: ${policy.startDate}"
                    val start = runCatching { LocalDate.parse(policy.startDate.take(10)) }.getOrNull()
                    binding.tvWaitingPeriod.text = if (start != null) {
                        val end = FuneralPolicyCalculator.trialEndsOn(start)
                        val label = if (LocalDate.now().isBefore(end)) "In waiting period" else "Waiting period completed"
                        "3-month waiting period ends: $end ($label)"
                    } else "3-month waiting period: start date unavailable"
                    binding.tvPremiumSummary.text = "Payment summary: Checking recorded payments..."

                    binding.tvPolicyStatus.text = policy.status.uppercase()
                    val color = when (policy.status) {
                        "active" -> Color.parseColor("#2E7D32")
                        "pending" -> Color.parseColor("#ED6C02")
                        "suspended", "cancelled" -> Color.parseColor("#C62828")
                        else -> Color.parseColor("#6E635A")
                    }
                    binding.tvPolicyStatus.background.mutate().setTint(color)

                    loadBeneficiaries(policy.id)
                    loadPayments(policy.id)
                } else {
                    binding.tvPremiumSummary.text = "No policy available"
                    Toast.makeText(requireContext(), "No policy found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun loadBeneficiaries(policyId: String) {
        lifecycleScope.launch {
            try {
                val beneficiaries = Supabase.client.postgrest["beneficiaries"]
                    .select { filter { eq("policy_id", policyId) } }
                    .decodeList<Beneficiary>()

                binding.llBeneficiaries.removeAllViews()
                if (beneficiaries.isEmpty()) {
                    val tv = TextView(requireContext()).apply { text = "No beneficiaries added yet." }
                    binding.llBeneficiaries.addView(tv)
                } else {
                    for (ben in beneficiaries) {
                        val itemBinding = ItemBeneficiaryBinding.inflate(layoutInflater, binding.llBeneficiaries, false)
                        itemBinding.tvBeneficiaryName.text = ben.fullName
                        itemBinding.tvBeneficiaryRelation.text = ben.relationship
                        binding.llBeneficiaries.addView(itemBinding.root)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load beneficiaries", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadPayments(policyId: String) {
        lifecycleScope.launch {
            try {
                val payments = Supabase.client.postgrest["payments"]
                    .select {
                        filter { eq("policy_id", policyId) }
                        order("payment_date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }
                    .decodeList<Payment>()

                // A payment record is not necessarily settled. Count confirmed payments only.
                val settled = payments.filter { it.status.lowercase() in setOf("completed", "paid", "successful", "success") }
                val total = settled.fold(BigDecimal.ZERO) { sum, pay -> sum + BigDecimal.valueOf(pay.amount) }
                binding.tvPremiumSummary.text = "Confirmed payments: ${settled.size} • Total: R${total.setScale(2)}\n" +
                    "Outstanding balance: awaiting verified billing schedule"
                binding.llPayments.removeAllViews()
                if (payments.isEmpty()) {
                    val tv = TextView(requireContext()).apply { text = "No payments recorded yet." }
                    binding.llPayments.addView(tv)
                } else {
                    for (pay in payments) {
                        val itemBinding = ItemPaymentBinding.inflate(layoutInflater, binding.llPayments, false)
                        itemBinding.tvPaymentReference.text = pay.reference ?: "Premium Payment"
                        itemBinding.tvPaymentDate.text = pay.paymentDate
                        itemBinding.tvPaymentAmount.text = "R${pay.amount.toInt()}"
                        binding.llPayments.addView(itemBinding.root)
                    }
                }
            } catch (e: Exception) {
                binding.tvPremiumSummary.text = "Payment summary unavailable — pull down to retry"
                Toast.makeText(requireContext(), "Failed to load payments", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}