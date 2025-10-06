package com.example.auto_saver

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CategoryTest {

    @Test
    fun `Category can be created with valid data`() {
        val category = Category(
            id = 1,
            userId = 100,
            name = "Food & Dining"
        )

        assertThat(category.id).isEqualTo(1)
        assertThat(category.userId).isEqualTo(100)
        assertThat(category.name).isEqualTo("Food & Dining")
    }

    @Test
    fun `Category equality works correctly`() {
        val category1 = Category(1, 100, "Food")
        val category2 = Category(1, 100, "Food")
        val category3 = Category(2, 100, "Transport")

        assertThat(category1).isEqualTo(category2)
        assertThat(category1).isNotEqualTo(category3)
    }

    @Test
    fun `Category can have different names for same user`() {
        val category1 = Category(1, 100, "Food")
        val category2 = Category(2, 100, "Transport")

        assertThat(category1.userId).isEqualTo(category2.userId)
        assertThat(category1.name).isNotEqualTo(category2.name)
    }

    @Test
    fun `Category handles long names`() {
        val longName = "Food, Dining, Restaurants, Groceries & Beverages"
        val category = Category(1, 100, longName)

        assertThat(category.name).isEqualTo(longName)
        assertThat(category.name.length).isGreaterThan(40)
    }

    @Test
    fun `Category handles special characters in name`() {
        val category = Category(1, 100, "Food & Drink 🍔")

        assertThat(category.name).contains("&")
        assertThat(category.name).contains("🍔")
    }
}

