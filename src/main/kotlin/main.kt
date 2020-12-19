import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import repo.*
import rest.restUser

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start()
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    install(DefaultHeaders)
    // This uses use the logger to log every call (request/response)
    install(CallLogging)
    Database.connect(
        "jdbc:mysql://s29.webhost1.ru:3306/sgoldik_hunter?serverTimezone=Europe/Moscow",
        driver = "com.mysql.jdbc.Driver",
        user = "sgoldik_hunter",
        password = "f5_vWhs%"
    )
    transaction {
        SchemaUtils.create(usersTable)
        SchemaUtils.create(resumesTable)
        SchemaUtils.create(repliesTable)
    }

//    routing {
//        route("/*") {
//            get {
//                call.respond("dsadasdas")
//            }
//        }
//    }

    restUser<Item>(
        RepoDSL(usersTable),
        User.serializer(),
        RepoDSL(resumesTable),
        Resume.serializer(),
        RepoDSL(repliesTable),
        Reply.serializer()
    )
}