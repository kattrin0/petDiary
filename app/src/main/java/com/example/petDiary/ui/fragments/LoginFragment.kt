package com.example.petDiary.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.petDiary.R
import com.example.petDiary.ui.viewmodel.AuthViewModel
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.actionCodeSettings
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginFragment : Fragment() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var authViewModel: AuthViewModel

    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var btnBack: Button
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var btnAction: Button

    private var authMode = "register" // "register" или "login"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            authMode = it.getString("auth_mode", "register")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.login_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authViewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]

        initViews(view)
        setupUI()
        setupClickListeners()
    }

    private fun initViews(view: View) {
        tvTitle = view.findViewById(R.id.tvTitle)
        tvSubtitle = view.findViewById(R.id.tvSubtitle)
        btnBack = view.findViewById(R.id.btnBack)
        etEmail = view.findViewById(R.id.etEmail)
        etPassword = view.findViewById(R.id.etPassword)
        emailLayout = view.findViewById(R.id.emailLayout)
        passwordLayout = view.findViewById(R.id.passwordLayout)
        btnAction = view.findViewById(R.id.btnAction)
    }

    private fun setupUI() {
        if (authMode == "login") {
            // Режим входа (email + пароль)
            tvTitle.text = "Вход"
            tvSubtitle.text = "Введите email и пароль"
            passwordLayout.visibility = View.VISIBLE
            btnAction.text = "Войти"
        } else {
            // Режим регистрации (только email для ссылки)
            tvTitle.text = "Регистрация"
            tvSubtitle.text = "Введите email для отправки ссылки"
            passwordLayout.visibility = View.GONE
            btnAction.text = "Отправить ссылку"
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            // Возвращаемся к выбору
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView2, AuthChoiceFragment.newInstance())
                .commit()
        }

        btnAction.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Введите email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (authMode == "login") {
                // Вход с паролем
                val password = etPassword.text.toString().trim()
                if (password.isEmpty()) {
                    Toast.makeText(requireContext(), "Введите пароль", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                loginWithEmail(email, password)
            } else {
                // Регистрация - отправка ссылки
                sendSignInLink(email)
            }
        }
    }

    private fun loginWithEmail(email: String, password: String) {
        btnAction.isEnabled = false
        btnAction.text = "Вход..."

        lifecycleScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()

                Toast.makeText(
                    requireContext(),
                    "Вход выполнен успешно!",
                    Toast.LENGTH_SHORT
                ).show()

                // Обновляем состояние авторизации — MainActivity через observer заменит экран на HomeFragment
                authViewModel.checkAuthState()

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Ошибка входа: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                btnAction.isEnabled = true
                btnAction.text = "Войти"
            }
        }
    }

    private fun sendSignInLink(email: String) {
        btnAction.isEnabled = false
        btnAction.text = "Отправка..."

        val actionCodeSettings = actionCodeSettings {
            url = "https://petdiary-d6042.firebaseapp.com"
            handleCodeInApp = true
            setAndroidPackageName("com.example.petDiary", true, "1")
        }

        lifecycleScope.launch {
            try {
                auth.sendSignInLinkToEmail(email, actionCodeSettings).await()
                saveEmailToPrefs(email)

                Toast.makeText(
                    requireContext(),
                    "Ссылка для входа отправлена на $email",
                    Toast.LENGTH_LONG
                ).show()

                // Возвращаемся к выбору
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainerView2, AuthChoiceFragment.newInstance())
                    .commit()

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Ошибка: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                btnAction.isEnabled = true
                btnAction.text = "Отправить ссылку"
            }
        }
    }

    private fun saveEmailToPrefs(email: String) {
        requireContext().getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("email_for_signin", email)
            .apply()
    }

    companion object {
        fun newInstance(mode: String): LoginFragment {
            return LoginFragment().apply {
                arguments = Bundle().apply {
                    putString("auth_mode", mode)
                }
            }
        }
    }
}