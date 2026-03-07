package com.example.petDiary.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.petDiary.R
import com.example.petDiary.ui.SettingsDialog
import com.example.petDiary.ui.fragments.AuthChoiceFragment
import com.example.petDiary.ui.viewmodel.AuthViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var toolbar: Toolbar
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "Pet Diary"

        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        observeAuthState()
        handleSignInLink(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                showSettingsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSettingsDialog() {
        val settingsDialog = SettingsDialog()
        settingsDialog.show(supportFragmentManager, SettingsDialog.TAG)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSignInLink(intent)
    }

    private fun observeAuthState() {
        authViewModel.isAuthenticated.observe(this) { isAuthenticated ->
            if (isAuthenticated) {
                setAuthBarsVisible(true)
                navigateToHome()
            } else {
                setAuthBarsVisible(false)
                navigateToAuth()
            }
        }
    }

    private fun setAuthBarsVisible(visible: Boolean) {
        toolbar.visibility = if (visible) android.view.View.VISIBLE else android.view.View.GONE
        bottomNavigationView.visibility = if (visible) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun navigateToHome() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        val navHostFragment = NavHostFragment.create(R.navigation.nav_fragment)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView2, navHostFragment)
            .commit()
        supportFragmentManager.executePendingTransactions()
        (supportFragmentManager.findFragmentById(R.id.fragmentContainerView2) as? NavHostFragment)
            ?.navController
            ?.let { bottomNavigationView.setupWithNavController(it) }
    }

    private fun navigateToAuth() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView2, AuthChoiceFragment.newInstance())
            .commit()
    }

    private fun handleSignInLink(intent: Intent?) {
        intent?.data?.toString()?.let { link ->
            authViewModel.handleSignInLink(link)
        }
    }
}