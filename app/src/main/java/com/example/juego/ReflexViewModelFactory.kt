package com.example.juego

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.juego.data.StatsRepository

class ReflexViewModelFactory(
    private val statsRepository: StatsRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReflexViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReflexViewModel(statsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}