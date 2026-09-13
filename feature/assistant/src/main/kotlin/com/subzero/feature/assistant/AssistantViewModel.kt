package com.subzero.feature.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subzero.core.domain.assistant.Assistant
import com.subzero.core.domain.assistant.AssistantAnswer
import com.subzero.core.domain.assistant.AssistantContext
import com.subzero.core.domain.repository.SubscriptionRepository
import com.subzero.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** One turn of the conversation. Answers are structured; the UI words them. */
sealed interface AssistantMessage {
    val id: Long

    data class Question(override val id: Long, val text: String) : AssistantMessage
    data class Answer(override val id: Long, val answer: AssistantAnswer, val today: LocalDate) : AssistantMessage
}

data class AssistantUiState(
    val messages: List<AssistantMessage> = emptyList(),
    val input: String = "",
    val isThinking: Boolean = false,
) {
    val canSend: Boolean get() = input.isNotBlank() && !isThinking
}

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val assistant: Assistant,
    private val repository: SubscriptionRepository,
    private val preferences: UserPreferencesRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private var nextId = 0L

    fun setInput(text: String) = _uiState.update { it.copy(input = text) }

    fun send() = ask(_uiState.value.input)

    /** Asks [question]; suggestions and the text field both land here. */
    fun ask(question: String) {
        val text = question.trim()
        if (text.isEmpty() || _uiState.value.isThinking) return
        _uiState.update {
            it.copy(messages = it.messages + AssistantMessage.Question(nextId++, text), input = "", isThinking = true)
        }
        viewModelScope.launch {
            val today = LocalDate.now(clock)
            val context = AssistantContext(
                subscriptions = repository.getSubscriptions(),
                priceChanges = repository.observeAllPriceChanges().first(),
                homeCurrency = preferences.preferences.first().homeCurrency,
                today = today,
            )
            val answer = assistant.ask(text, context)
            _uiState.update {
                it.copy(messages = it.messages + AssistantMessage.Answer(nextId++, answer, today), isThinking = false)
            }
        }
    }
}
