package com.example.travenorcustomer.features.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travenorcustomer.data.AuthenticationRepository
import com.example.travenorcustomer.features.viewModelModule
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class AppRootViewModel(
    private val repository: AuthenticationRepository
) : ViewModel(){
    val state = repository.authSession()

}