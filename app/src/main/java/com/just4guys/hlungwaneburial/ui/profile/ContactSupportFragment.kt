package com.just4guys.hlungwaneburial.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.just4guys.hlungwaneburial.databinding.FragmentContactSupportBinding

class ContactSupportFragment : Fragment() {
    private var _binding: FragmentContactSupportBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactSupportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Emergency Call
        binding.btnCallEmergency.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:0800123456"))
            startActivity(intent)
        }

        // WhatsApp
        binding.btnWhatsApp.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/27710000000"))
            startActivity(intent)
        }

        // Send Message (placeholder)
        binding.btnSendMessage.setOnClickListener {
            val subject = binding.etSubject.text?.toString()?.trim().orEmpty()
            val message = binding.etMessage.text?.toString()?.trim().orEmpty()

            if (subject.isEmpty() || message.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in both fields", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Message sent successfully!", Toast.LENGTH_LONG).show()
                binding.etSubject.text?.clear()
                binding.etMessage.text?.clear()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}