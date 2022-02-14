package trawler.core.internal.util

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class StringsTest {

    @Test
    fun camelToSqlName() {

        assertEquals("RICK_HIGHTOWER", "rickHightower".camelToSqlName())

        assertEquals("RICK_HIGHTOWER", "RickHightower".camelToSqlName())
    }

    @Test
    fun sqlNameToCamelUpper() {

        assertEquals("RickHightower", "RICK_HIGHTOWER".sqlNameToCamelUpper())
    }


    @Test
    fun sqlNameToCamelLower() {

        assertEquals("rickHightower", "RICK_HIGHTOWER".sqlNameToCamelLower())
    }
}
