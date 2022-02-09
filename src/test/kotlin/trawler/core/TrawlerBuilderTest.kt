package trawler.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class TrawlerBuilderTest {

    @Test
    fun test() {
        val trawlerResult = TrawlerBuilder().withConfigDirectories("./src/test/resources").build()

        assertNotNull(trawlerResult)

        if (!trawlerResult.success) {
            println(trawlerResult.message())
            trawlerResult.message().causedBy.forEach {
                println(it.message)
            }
        }

        assertTrue(trawlerResult.success)
        val trawler = trawlerResult.result()

        assertTrue(trawler.models.any { it.name == "Employee" })

        assertTrue(trawler.models.any { it.name == "Department" })

        assertTrue(trawler.models.find { it.name == "Department" }!!.associations.any { it.name == "employees" })

        val employeesAssociation = trawler.models.find { it.name == "Department" }!!.associations.find { it.name == "employees" }!!

        assertTrue(employeesAssociation.many)


    }
}
