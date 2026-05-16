package com.example.raitha_vartha

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppLanguage {
    KANNADA, ENGLISH
}

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

class TipViewModel(private val repository: FirestoreRepository) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _appLanguage = MutableStateFlow(AppLanguage.KANNADA)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage
    
    private val _themeMode = MutableStateFlow(ThemeMode.DARK) 
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val tips: StateFlow<List<TipEntity>> = combine(
        repository.getAllTips()
            .onEach { 
                _isLoading.value = false 
                if (it.isNotEmpty()) _error.value = null
            }
            .catch { _ -> 
                _isLoading.value = false
                _error.value = "Network Error: Please check your internet connection."
            },
        _selectedCategory
    ) { allTips, category ->
        val filtered = when (category) {
            null, "All" -> allTips
            "My Crops" -> allTips.filter { it.isUserCrop }
            "Success Stories" -> allTips.filter { it.isSuccessStory }
            "Post Cards" -> allTips.filter { it.isPostCard && (it.isAdminApproved || it.authorEmail == "admin@example.com") } // Simplified filter
            else -> allTips.filter { it.category.equals(category, ignoreCase = true) }
        }
        filtered.sortedByDescending { it.timestamp }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun setLanguage(language: AppLanguage) {
        _appLanguage.value = language
    }
    
    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    fun toggleMyCrop(tip: TipEntity) {
        viewModelScope.launch {
            val updatedTip = tip.copy(isUserCrop = !tip.isUserCrop)
            repository.insertTip(updatedTip)
        }
    }
    
    fun updateTip(tip: TipEntity) {
        viewModelScope.launch {
            repository.insertTip(tip)
        }
    }

    fun deleteTip(tipId: String) {
        viewModelScope.launch {
            repository.deleteTip(tipId)
        }
    }
    
    fun addSuccessStory(title: String, summary: String, imageUrl: String) {
        viewModelScope.launch {
            repository.insertTip(
                TipEntity(
                    id = "story_${System.currentTimeMillis()}",
                    title = title,
                    instruction = summary,
                    category = "Success Stories",
                    imageUrl = imageUrl,
                    isSuccessStory = true,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun postExpertTip(user: UserEntity, title: String, instruction: String, category: String, imageUrl: String) {
        viewModelScope.launch {
            repository.insertTip(
                TipEntity(
                    id = "expert_${System.currentTimeMillis()}",
                    title = title,
                    instruction = instruction,
                    category = category,
                    imageUrl = imageUrl,
                    authorEmail = user.email,
                    authorName = "${user.firstName} ${user.lastName}",
                    isVerified = false,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun addPostCard(user: UserEntity, title: String, content: String, category: String, imageUri: Uri) {
        viewModelScope.launch {
            try {
                val imageUrl = repository.uploadImage(imageUri, "post_cards")
                repository.insertTip(
                    TipEntity(
                        id = "postcard_${System.currentTimeMillis()}",
                        title = title,
                        instruction = content,
                        category = category,
                        imageUrl = imageUrl,
                        isPostCard = true,
                        isAdminApproved = false, // Pending moderation
                        authorEmail = user.email,
                        authorName = "${user.firstName} ${user.lastName}",
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                _error.value = "Failed to upload post: ${e.message}"
            }
        }
    }

    fun toggleLike(tip: TipEntity, userEmail: String) {
        viewModelScope.launch {
            repository.toggleLike(tip.id, userEmail)
        }
    }

    fun addSampleTips() {
        viewModelScope.launch {
            val currentTips = repository.getAllTips().first()
            if (currentTips.isEmpty()) {
                loadSamples()
            }
        }
    }

    private suspend fun loadSamples() {
        val samples = listOf(
            TipEntity(
                id = "paddy_001",
                title = "ಭತ್ತದ ಸಮಗ್ರ ನಿರ್ವಹಣೆ | Paddy Comprehensive Care",
                instruction = "ನಾಟಿ ಮಾಡಿದ 15 ದಿನಗಳಲ್ಲಿ ಕಳೆ ತೆಗೆಯುವುದು ಬಹಳ ಮುಖ್ಯ. | It is crucial to weed within 15 days of transplanting.",
                category = "Paddy",
                imageUrl = "https://images.pexels.com/photos/235925/pexels-photo-235925.jpeg?auto=compress&cs=tinysrgb&w=800",
                timestamp = System.currentTimeMillis()
            )
        )
        samples.forEach { repository.insertTip(it) }
    }

    class Factory(private val repository: FirestoreRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TipViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TipViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
