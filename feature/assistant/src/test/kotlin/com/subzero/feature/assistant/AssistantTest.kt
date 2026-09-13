package com.subzero.feature.assistant

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.domain.assistant.AnswerItem
import com.subzero.core.domain.assistant.AnswerKind
import com.subzero.core.domain.assistant.AssistantAnswer
import com.subzero.core.domain.assistant.LocalAssistant
import com.subzero.core.domain.model.Category
import com.subzero.core.domain.model.DeclaredUsage
import com.subzero.core.domain.model.SubscriptionId
import com.subzero.core.domain.testing.FakeSubscriptionRepository
import com.subzero.core.domain.testing.FakeUserPreferencesRepository
import com.subzero.core.domain.testing.date
import com.subzero.core.domain.testing.fixedClock
import com.subzero.core.domain.testing.subscription
import com.subzero.core.domain.testing.usd
import com.subzero.core.domain.usecase.GetUpcomingPaymentsUseCase
import com.subzero.core.testing.rule.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AssistantTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val compose = createComposeRule()

    private val today = date("2026-09-12")
    private val clock = fixedClock(today)
    private val repository = FakeSubscriptionRepository()
    private val preferences = FakeUserPreferencesRepository()
    private val chatgpt = subscription(id = "c", name = "ChatGPT", price = usd(2000), category = Category.AI, anchorDate = date("2026-09-20"), usage = DeclaredUsage.RARELY)

    private fun viewModel() = AssistantViewModel(
        assistant = LocalAssistant(GetUpcomingPaymentsUseCase(clock)),
        repository = repository,
        preferences = preferences,
        clock = clock,
    )

    @Test
    fun `asking records the question and a structured answer`() = runTest {
        repository.seed(chatgpt)
        val vm = viewModel()
        vm.setInput("How much am I spending on AI?")
        vm.send()

        val messages = vm.uiState.value.messages
        assertThat(messages).hasSize(2)
        assertThat((messages[0] as AssistantMessage.Question).text).isEqualTo("How much am I spending on AI?")
        val answer = (messages[1] as AssistantMessage.Answer).answer
        assertThat(answer.kind).isEqualTo(AnswerKind.SPEND_IN_CATEGORY)
        assertThat(answer.amount).isEqualTo(usd(2000))
        assertThat(vm.uiState.value.input).isEmpty()
        assertThat(vm.uiState.value.isThinking).isFalse()
    }

    @Test
    fun `blank input is ignored`() = runTest {
        val vm = viewModel()
        vm.setInput("   ")
        vm.send()
        assertThat(vm.uiState.value.messages).isEmpty()
        assertThat(vm.uiState.value.canSend).isFalse()
    }

    private var opened: SubscriptionId? = null
    private var asked: String? = null

    private fun setContent(state: AssistantUiState) {
        compose.setContent {
            SubzeroTheme(darkTheme = true) {
                AssistantScreen(
                    state = state,
                    onInput = {},
                    onSend = {},
                    onAsk = { asked = it },
                    onBack = {},
                    onOpenSubscription = { opened = it },
                    onReviewSubscriptions = {},
                )
            }
        }
    }

    @Test
    fun `suggestions ask the question`() {
        setContent(AssistantUiState())
        compose.onNodeWithText("Works offline.", substring = true).assertIsDisplayed()
        compose.onNodeWithTag(AssistantTestTags.suggestion(2)).performClick()
        assertThat(asked).isEqualTo("What can I cancel?")
    }

    @Test
    fun `answer cards word the facts and open subscriptions`() {
        val answer = AssistantAnswer(
            kind = AnswerKind.CANCEL_CANDIDATES,
            amount = usd(2000),
            items = listOf(AnswerItem(chatgpt, usd(2000))),
            count = 1,
        )
        setContent(
            AssistantUiState(
                messages = listOf(
                    AssistantMessage.Question(0, "What can I cancel?"),
                    AssistantMessage.Answer(1, answer, today),
                ),
            ),
        )
        compose.onNodeWithText("Based on what you told me, these get little use:").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Together they cost $20.00 a month. Would you like to review them?").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("ChatGPT").performScrollTo().performClick()
        assertThat(opened).isEqualTo(SubscriptionId("c"))
    }

    @Test
    fun `unknown answers offer the supported questions`() {
        setContent(AssistantUiState(messages = listOf(AssistantMessage.Answer(0, AssistantAnswer(AnswerKind.UNKNOWN), today))))
        compose.onNodeWithText("I can answer questions about what you spend", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `thinking indicator shows while waiting`() {
        setContent(AssistantUiState(messages = listOf(AssistantMessage.Question(0, "hi")), isThinking = true))
        compose.onNodeWithTag(AssistantTestTags.THINKING).assertIsDisplayed()
    }

    @Test
    fun `typing enables send`() {
        setContent(AssistantUiState(input = "how much"))
        compose.onNodeWithTag(AssistantTestTags.INPUT).performTextInput("?")
        compose.onNodeWithTag(AssistantTestTags.SEND).assertIsDisplayed()
    }
}
