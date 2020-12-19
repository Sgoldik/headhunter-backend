package repo

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.UpdateBuilder

@Serializable
class Reply (
    val resumeId: Int,
    val userId: Int,
    override var id: Int = -1
) : Item

class RepliesTable : ItemTable<Reply>() {
    val resumeId = integer("resumeId").references(resumesTable.id, onDelete = ReferenceOption.CASCADE)
    val userId = integer("userId").references(usersTable.id, onDelete = ReferenceOption.CASCADE)
    override fun fill(builder: UpdateBuilder<Int>, item: Reply) {
        builder[resumeId] = item.resumeId
        builder[userId] = item.userId
    }

    override fun readResult(result: ResultRow) =
        Reply(
            result[resumeId],
            result[userId],
            result[id].value
        )
}

val repliesTable = RepliesTable()
