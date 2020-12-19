package rest

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.*
import io.ktor.serialization.json
import io.ktor.server.testing.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import repo.*
import kotlin.test.Test
import kotlin.test.assertEquals


class RestRepoTest {
        @Test

    fun restRepoMapTest() {
        Database.connect(
            "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver"
        )
        transaction {
            SchemaUtils.create(usersTable)
            SchemaUtils.create(resumesTable)
            SchemaUtils.create(repliesTable)
        }

        testUser  {

            restUser<Item>(
                    RepoDSL(usersTable),
                    User.serializer(),
                    RepoDSL(resumesTable),
                    Resume.serializer(),
                    RepoDSL(repliesTable),
                    Reply.serializer()


            )
        }
    }



    private fun testUser(
        restModule: Application.() -> Unit
    ) {
        withTestApplication({// withTestApplication
            install(ContentNegotiation) {
                json()
            }
            restModule()
        }) {

        }
    }

}

fun TestApplicationCall.assertStatus(status: HttpStatusCode) =
        assertEquals(status, response.status())

fun TestApplicationRequest.setBodyAndHeaders(body: String) {
    setBody(body)
    addHeader("Content-Type", "application/json")
    addHeader("Accept", "application/json")
}

fun <T> TestApplicationCall.parseResponse(
        serializer: KSerializer<T>
) =
        try {
            Json.decodeFromString(
                    serializer,
                    response.content ?: ""
            )
        } catch (e: Throwable) {
            null
        }