package nl.avisi.kotlinwebtest

import nl.avisi.kotlinwebtest.expressions.property
import nl.avisi.kotlinwebtest.jdbc.JdbcServer
import nl.avisi.kotlinwebtest.jdbc.dsl.jdbc
import nl.avisi.kotlinwebtest.jdbc.dsl.row
import nl.avisi.kotlinwebtest.jdbc.dsl.validate
import nl.avisi.kotlinwebtest.properties.dsl.properties
import org.junit.Before
import org.junit.Test

class JdbcDslTest : WebTest() {

    companion object {
        private lateinit var server: JdbcServer
    }

    override fun configure() {
        jdbc {
            default {
                request driver "org.h2.Driver"
                request url "jdbc:h2:tcp://localhost/~/database"
            }
        }
    }

    @Before
    fun before() {
        server = JdbcServer()
    }

    @Test
    fun test() {
        test("JDBC Test") {
            properties {
                configuration.properties["property_duur"] = "duur"
            }
            step jdbc {
                name = "Test querry"
                request query "select * from cars"
                validate {
                    row_count(8)
                    row_count(2, "COMMENT")
                    column("COMMENT") matches "duur"
                    column("COMMENT") matches property("property_duur")
                    column("COMMENT") matches_regex "duu.*."
                    column("COMMENT") matches_regex property("property_duur")
                    row {
                        row_index = 1
                        column("COMMENT") matches "duur"
                        column("COMMENT") matches property("property_duur")
                        column("COMMENT") matches_regex "duu.*."
                        column("COMMENT") matches_regex property("property_duur")
                        column("COMMENT") is_null false
                    }
                    row {
                        row_index = 2
                        column("COMMENT") is_null true
                        column("TIJD", "TIJD2") time_difference_less_than 5001
                        column("TIJD", "TIJD2") time_difference_greater_than 1

                    }
                }
            }
        }
        execute()
    }

}