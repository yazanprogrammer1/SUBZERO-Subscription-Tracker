package com.subzero.feature.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subzero.core.domain.assistant.Assistant
import com.subzero.core.domain.assistant.AssistantAnswer
import com.subzero.core.domain.assistant.AssistantContext
import com.subzero.core.domain.assistant.AssistantFocus
import com.subzero.core.domain.assistant.AssistantTurn
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

    /** What the last answer was about, so "when is it charged?" keeps the same subject. */
    private var focus: AssistantFocus? = null

    fun setInput(text: String) = _uiState.update { it.copy(input = text) }

    fun send() = ask(_uiState.value.input)

    /** Asks [question]; suggestions, follow-up chips and the text field all land here. */
    fun ask(question: String) {
        val text = question.trim()
        if (text.isEmpty() || _uiState.value.isThinking) return
        val history = historyFrom(_uiState.value.messages)
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
                // A year of records is enough for every period the assistant answers about.
                payments = repository.getPaymentRecordsBetween(from = today.minusYears(1), to = today),
                focus = focus,
                history = history,
            )
            val answer = assistant.ask(text, context)
            focus = answer.focus ?: focus
            _uiState.update {
                it.copy(messages = it.messages + AssistantMessage.Answer(nextId++, answer, today), isThinking = false)
            }
        }
    }

    /**
     * The turns a hosted model is given for continuity. Only text it produced itself is replayed;
     * structured answers are worded by the UI, so there is no transcript of them to send.
     */
    private fun historyFrom(messages: List<AssistantMessage>): List<AssistantTurn> =
        messages.mapNotNull { message ->
            when (message) {
                is AssistantMessage.Question -> AssistantTurn(fromUser = true, text = message.text)
                is AssistantMessage.Answer -> message.answer.text?.let { AssistantTurn(fromUser = false, text = it) }
            }
        }.takeLast(MAX_HISTORY_TURNS)

    private companion object {
        const val MAX_HISTORY_TURNS = 8
    }
}
