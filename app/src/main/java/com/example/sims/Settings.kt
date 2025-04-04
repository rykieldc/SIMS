package com.example.sims

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.sims.databinding.FragmentSettingsBinding

class Settings : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private var userName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val view = binding.root

        val changePasswordButton = binding.changePWBtn
        val logoutButton = binding.logoutBtn
        val profileButton = binding.profileBtn
        val aboutButton = binding.aboutBtn

        userName = arguments?.getString("username")

        changePasswordButton.setOnClickListener {
            val intent = Intent(requireContext(), ChangePasswordActivity::class.java)
            intent.putExtra("username", userName)
            startActivity(intent)
        }

        logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        profileButton.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(ProfilePage())
        }


        aboutButton.setOnClickListener {
            val intent = Intent(requireActivity(), AboutPage::class.java)
            startActivity(intent)
        }

        return view
    }

    private fun showLogoutConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_logout_confirmation, null)
        val noButton = dialogView.findViewById<View>(R.id.noBtn)
        val yesButton = dialogView.findViewById<View>(R.id.yesBtn)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        noButton.setOnClickListener {
            dialog.dismiss()
        }

        yesButton.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(requireActivity(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
