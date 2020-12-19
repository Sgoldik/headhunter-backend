package repo

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.UpdateBuilder

@Serializable
class User (
    val username: String,
    val password: String,
    val status: Int,
    override var id: Int = -1
) : Item

class UsersTable : ItemTable<User>() {
    val username = varchar("username", 50)
    val password = varchar("password", 50)
    val status = integer("status")
    override fun fill(builder: UpdateBuilder<Int>, item: User) {
        builder[username] = item.username
        builder[password] = item.password
        builder[status] = item.status
    }

    override fun readResult(result: ResultRow) =
        User(
            result[username],
            result[password],
            result[status],
            result[id].value
        )
}

val usersTable = UsersTable()
