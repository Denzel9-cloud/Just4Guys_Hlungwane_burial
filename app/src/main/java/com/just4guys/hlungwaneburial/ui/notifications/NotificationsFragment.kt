package com.just4guys.hlungwaneburial.ui.notifications

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.just4guys.hlungwaneburial.data.model.Notification
import com.just4guys.hlungwaneburial.data.remote.Supabase
import com.just4guys.hlungwaneburial.databinding.FragmentNotificationsBinding
import com.just4guys.hlungwaneburial.databinding.ItemNotificationBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put

class NotificationsFragment : Fragment() {
    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvMarkAllRead.setOnClickListener {
            markAllAsRead()
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadNotifications()
        }
    }

    override fun onResume() {
        super.onResume()
        loadNotifications()
    }

    private fun loadNotifications() {
        lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true
            try {
                val session = Supabase.client.auth.currentSessionOrNull() ?: return@launch
                val userId = session.user?.id ?: return@launch

                val notifs = Supabase.client.postgrest["notifications"]
                    .select {
                        filter { eq("user_id", userId) }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }
                    .decodeList<Notification>()

                binding.llNotificationsList.removeAllViews()

                if (notifs.isEmpty()) {
                    binding.llEmptyState.visibility = View.VISIBLE
                } else {
                    binding.llEmptyState.visibility = View.GONE

                    for (notif in notifs) {
                        val itemBinding = ItemNotificationBinding.inflate(layoutInflater, binding.llNotificationsList, false)

                        itemBinding.tvNotifTitle.text = notif.title
                        itemBinding.tvNotifBody.text = notif.body
                        itemBinding.tvNotifDate.text = notif.createdAt.take(10)

                        if (notif.isRead) {
                            itemBinding.unreadIndicator.visibility = View.INVISIBLE
                            itemBinding.tvNotifTitle.alpha = 0.7f
                        } else {
                            itemBinding.unreadIndicator.visibility = View.VISIBLE
                            itemBinding.tvNotifTitle.alpha = 1.0f

                            val dotColor = when (notif.type) {
                                "claim_update" -> Color.parseColor("#0277BD")
                                "payment" -> Color.parseColor("#2E7D32")
                                "warning" -> Color.parseColor("#ED6C02")
                                else -> Color.parseColor("#D4AF37")
                            }
                            itemBinding.unreadIndicator.background.mutate().setTint(dotColor)
                        }

                        binding.llNotificationsList.addView(itemBinding.root)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun markAllAsRead() {
        lifecycleScope.launch {
            try {
                val session = Supabase.client.auth.currentSessionOrNull() ?: return@launch
                val userId = session.user?.id ?: return@launch

                Supabase.client.postgrest["notifications"].update(
                    kotlinx.serialization.json.buildJsonObject {
                        put("is_read", true)
                    }
                ) {
                    filter { eq("user_id", userId) }
                }

                Toast.makeText(requireContext(), "All notifications marked as read", Toast.LENGTH_SHORT).show()
                loadNotifications()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}