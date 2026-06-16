package com.github.`792532971`.ideaplugin.naming

import org.junit.Assert.*
import org.junit.Test

class NamingStyleTest {

    @Test
    fun `default returns CAMEL_CASE`() {
        assertEquals(NamingStyle.CAMEL_CASE, NamingStyle.default())
    }

    @Test
    fun `CAMEL_CASE converts correctly`() {
        assertEquals("camelCase", NamingStyle.CAMEL_CASE.convert(listOf("camel", "case")))
        assertEquals("userOrderList", NamingStyle.CAMEL_CASE.convert(listOf("user", "order", "list")))
    }

    @Test
    fun `SNAKE_CASE converts correctly`() {
        assertEquals("snake_case", NamingStyle.SNAKE_CASE.convert(listOf("snake", "case")))
        assertEquals("user_order_list", NamingStyle.SNAKE_CASE.convert(listOf("user", "order", "list")))
    }

    @Test
    fun `PASCAL_CASE converts correctly`() {
        assertEquals("PascalCase", NamingStyle.PASCAL_CASE.convert(listOf("pascal", "case")))
        assertEquals("UserOrderList", NamingStyle.PASCAL_CASE.convert(listOf("user", "order", "list")))
    }

    @Test
    fun `SCREAMING_SNAKE_CASE converts correctly`() {
        assertEquals("SCREAMING_SNAKE_CASE", NamingStyle.SCREAMING_SNAKE_CASE.convert(listOf("screaming", "snake", "case")))
        assertEquals("USER_ORDER_LIST", NamingStyle.SCREAMING_SNAKE_CASE.convert(listOf("user", "order", "list")))
    }

    @Test
    fun `fromDisplayName returns correct enum`() {
        assertEquals(NamingStyle.CAMEL_CASE, NamingStyle.fromDisplayName("camelCase"))
        assertEquals(NamingStyle.SNAKE_CASE, NamingStyle.fromDisplayName("snake_case"))
        assertEquals(NamingStyle.PASCAL_CASE, NamingStyle.fromDisplayName("PascalCase"))
        assertEquals(NamingStyle.SCREAMING_SNAKE_CASE, NamingStyle.fromDisplayName("SCREAMING_SNAKE_CASE"))
    }

    @Test
    fun `fromDisplayName returns null for unknown`() {
        assertNull(NamingStyle.fromDisplayName("unknown"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `convert throws on empty list`() {
        NamingStyle.CAMEL_CASE.convert(emptyList())
    }
}
