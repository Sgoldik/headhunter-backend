package repo

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.UpdateBuilder

@Serializable
class Resume (
    val userId: Int,
    val title: String,
    val description: String,
    override var id: Int = -1
) : Item

class ResumesTable : ItemTable<Resume>() {
    val userId = integer("userId").references(usersTable.id, onDelete = ReferenceOption.CASCADE)
    val title = varchar("title", 50)
    val description = varchar("description", 500)
    override fun fill(builder: UpdateBuilder<Int>, item: Resume) {
        builder[userId] = item.userId
        builder[title] = item.title
        builder[description] = item.description
    }

    override fun readResult(result: ResultRow) =
        Resume(
            result[userId],
            result[title],
            result[description],
            result[id].value
        )
}

val resumesTable = ResumesTable()
