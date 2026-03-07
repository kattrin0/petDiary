package com.example.petDiary.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.petDiary.R

class AuthChoiceFragment : Fragment() {

    private lateinit var cardRegister: CardView
    private lateinit var cardLogin: CardView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.auth_choice_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cardRegister = view.findViewById(R.id.cardRegister)
        cardLogin = view.findViewById(R.id.cardLogin)

        cardRegister.setOnClickListener {
            // Регистрация (новый пользователь) - только email
            openLoginFragment("register")
        }

        cardLogin.setOnClickListener {
            // Вход (существующий пользователь) - email + пароль
            openLoginFragment("login")
        }
    }

    private fun openLoginFragment(mode: String) {
        // Передаем режим в LoginFragment через Bundle
        val bundle = Bundle().apply {
            putString("auth_mode", mode)
        }

        // Используем Navigation Component для перехода
        // Если у вас нет Navigation Graph, используйте replace
        val loginFragment = LoginFragment.newInstance(mode)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView2, loginFragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        fun newInstance() = AuthChoiceFragment()
    }
}