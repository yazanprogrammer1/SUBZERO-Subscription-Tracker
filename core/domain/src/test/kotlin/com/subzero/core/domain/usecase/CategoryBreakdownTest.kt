package com.subzero.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.subzero.core.domain.model.BillingCycle
import com.subzero.core.domain.model.Category
import com.subzero.core.domain.model.CurrencyCode
import com.subzero.core.domain.model.SubscriptionStatus
import com.subzero.core.domain.testing.eur
import com.subzero.core.domain.testing.subscription
import com.subzero.core.domain.testing.usd
import org.junit.Test

class CategoryBreakdownTest {

    private val useCase = CalculateCategoryBreakdownUseCase()

    @Test
    fun `groups active home currency subscriptions by category largest first`() {
        val result = useCase(
            listOf(
                subscription(id = "n", price = usd(1549), category = Category.ENTERTAINMENT),
                subscription(id = "s", price = usd(1199), category = Category.ENTERTAINMENT),
                subscription(id = "c", price = usd(2000), category = Category.AI),
                subscription(id = "a", price = usd(12000), billingCycle = BillingCycle.Yearly, category = Category.SOFTWARE),
                subscription(id = "p", price = usd(9999), category = Category.GAMING, status = SubscriptionStatus.PAUSED),
                subscription(id = "e", price = eur(999), category = Category.CLOUD),
            ),
            CurrencyCode.USD,
        )

        assertThat(result.map { it.category }).containsExactly(Category.ENTERTAINMENT, Category.AI, Category.SOFTWARE).inOrder()
        val entertainment = result.first()
        assertThat(entertainment.monthly).isEqualTo(usd(2748))
        assertThat(entertainment.count).isEqualTo(2)
        // 27.48 of 57.48 = 47.8%
        assertThat(entertainment.sharePercent).isEqualTo(48)
        assertThat(result.last().sharePercent).isEqualTo(17)
    }

    @Test
    fun `empty when nothing is active in the home currency`() {
        assertThat(useCase(emptyList(), CurrencyCode.USD)).isEmpty()
        assertThat(useCase(listOf(subscription(price = eur(100))), CurrencyCode.USD)).isEmpty()
    }

    @Test
    fun `zero amounts do not divide by zero`() {
        val result = useCase(listOf(subscription(price = usd(0))), CurrencyCode.USD)
        assertThat(result.single().sharePercent).isEqualTo(0)
    }
}
