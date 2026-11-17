package com.just4guys.hlungwaneburial

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.just4guys.hlungwaneburial.data.remote.Supabase
import com.just4guys.hlungwaneburial.databinding.ActivityMainBinding
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Android 15+ enforces edge-to-edge on recent target SDKs. Apply
        // system bar insets to the activity container, not individual fragments.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.parseColor("#5C1414")
        window.navigationBarColor = Color.parseColor("#FAF7F2")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { root, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            root.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isAuthScreen = destination.id == R.id.loginFragment ||
                    destination.id == R.id.registerFragment
            binding.bottomNav.visibility = if (isAuthScreen) View.GONE else View.VISIBLE
        }

        // Protect the screen navigation as well as the initial app launch.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val publicPage = destination.id == R.id.loginFragment || destination.id == R.id.registerFragment
            if (!publicPage && Supabase.client.auth.currentSessionOrNull() == null) {
                navController.navigate(R.id.loginFragment)
            }
        }
        checkCurrentSession(navController)
    }

    private fun checkCurrentSession(navController: androidx.navigation.NavController) {
        lifecycleScope.launch {
            try {
                val currentSession = Supabase.client.auth.currentSessionOrNull()
                val currentDestination = navController.currentDestination?.id

                if (currentSession != null && currentDestination == R.id.loginFragment) {
                    navController.navigate(R.id.action_loginFragment_to_homeFragment)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}