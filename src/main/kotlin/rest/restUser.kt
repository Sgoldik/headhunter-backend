package rest

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import repo.*

fun <T : Item> Application.restUser(
    userRepo: Repo<User>,
    userSerializer: KSerializer<User>,
    resumeRepo: Repo<Resume>,
    resumeSerializer: KSerializer<Resume>,
    replyRepo: Repo<Reply>,
    replySerializer: KSerializer<Reply>
) {
    routing {
        route("/user") {
            post {
                parseBody(userSerializer)?.let { elem ->
                    if (userRepo.all().filter { it.username == elem.username }.isEmpty()) {
                        if (userRepo.add(elem)) {
                            val user = userRepo.all().find { it.username == elem.username }!!
                            call.respond(user)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    } else {
                        call.respond(HttpStatusCode.Conflict, "Пользователь с таким именем уже существует")
                    }
                } ?: call.respond(HttpStatusCode.BadRequest, "Передан неправильный объект User")
            }
        }
        route("/user/login") {
            post {
                parseBody(userSerializer)?.let { elem ->
                    val user = userRepo.all().find { it.username == elem.username }!!
                    if (elem.password == user.password) {
                        call.respond(user)
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Введен неправильный пароль")
                    }
                } ?: call.respond(HttpStatusCode.BadRequest, "Передан неправильный объект User")
            }
        }
        route("/user/{id}") {
            put {
                parseBody(userSerializer)?.let { elem ->
                    parseId()?.let { id ->
                        if (userRepo.update(id, elem))
                            call.respond(HttpStatusCode.OK, "Пользователь успешно обновлен")
                        else
                            call.respond(HttpStatusCode.NotFound)
                    }
                } ?: call.respond(HttpStatusCode.BadRequest, "Передан неправильный объект User")
            }
            delete {
                parseId()?.let { id ->
                    if (userRepo.delete(id))
                        call.respond(HttpStatusCode.OK, "Пользователь успешно удален")
                    else
                        call.respond(HttpStatusCode.NotFound, "Пользователь с таким ID не существует")
                } ?: call.respond(HttpStatusCode.BadRequest, "Передан неправильный ID")
            }
        }
        route("/user/resumes") {
            post {
                parseBody(resumeSerializer)?.let { elem ->
                    if (resumeRepo.add(elem))
                        call.respond(HttpStatusCode.OK, "Резюме успешно создано")
                    else
                        call.respond(HttpStatusCode.NotFound)
                } ?: call.respond(HttpStatusCode.BadRequest, "Передан неправильный объект Resume")
            }
            get {
                val resumes = resumeRepo.all()
                if (resumes.isNotEmpty()) {
                    call.respond(resumes)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Резюме не найдены")
                }
            }
        }

        route("/user/{id}/resumes") {
            get {
                val resumes = resumeRepo.all().filter { it.userId == parseId() }
                if (resumes.isNotEmpty()) {
                    call.respond(resumes)
                } else {
                    call.respond(listOf<Resume>())
                }
            }
        }

        route("/user/resumes/{id}") {
            get {
                parseId()?.let { id ->
                    resumeRepo.get(id)?.let { elem ->
                        call.respond(elem)
                    } ?: call.respond(HttpStatusCode.NotFound, "Резюме с таким ID не существует")
                } ?: call.respond(HttpStatusCode.BadRequest, "Передан неправильный ID")
            }
            put {
                parseBody(resumeSerializer)?.let { elem ->
                    parseId()?.let { id ->
                        if (resumeRepo.update(id, elem))
                            call.respond(HttpStatusCode.OK, "Резюме успешно обновлено")
                        else
                            call.respond(HttpStatusCode.NotFound)
                    }
                } ?: call.respond(HttpStatusCode.BadRequest, "Передан неправильный объект Resume")
            }
            delete {
                parseId()?.let { id ->
                    if (resumeRepo.delete(id))
                        call.respond(HttpStatusCode.OK, "Резюме успешно удалено")
                    else
                        call.respond(HttpStatusCode.NotFound, "Резюме с таким ID не найдено")
                } ?: call.respond(HttpStatusCode.BadRequest, "Передан неправильный ID")
            }
        }
        route("/user/resumes/{id}/replies") {
            get {
                val replies = replyRepo.all().filter { it.resumeId == parseId() }
                if (replies.isNotEmpty()) {
                    call.respond(replies)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Отклики для этого резюме не найдены")
                }
            }
        }
        route("/user/{id}/resumes/allReplies") {
            get {
                val userResumes = resumeRepo.all().filter { it.userId == parseId() }
                val allReplies = replyRepo.all()
                val replies = arrayListOf<Reply>()
                for (resume in userResumes) {
                    val taskReplies = allReplies.filter { it.resumeId == resume.id }
                    for (reply in taskReplies) {
                        replies.add(reply)
                    }
                }

                call.respond(replies)
            }
        }
        route("/user/{id}/resumes/replies") {
            get {
                val repliesArray = replyRepo.all()
                val replies = resumeRepo.all().map { resume ->
                    repliesArray.filter { it.resumeId == resume.id && it.userId == parseId() }.isNotEmpty()
                }
                call.respond(replies)
            }
        }

        route("/user/resumes/replies") {
            get {
                if (replyRepo.all().isNotEmpty()) {
                    val replies = replyRepo.all()
                    call.respond(replies)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Отклики не найдены")
                }
            }
        }
        route("/user/resumes/reply") {
            post {
                parseBody(replySerializer)?.let { elem ->
                    if (elem.userId != resumeRepo.get(elem.resumeId)?.userId) {
                        if (userRepo.get(elem.userId)?.status == 1) {
                            if (replyRepo.all().filter { it.resumeId == elem.resumeId && it.userId == elem.userId }.isEmpty()) {
                                if (replyRepo.add(elem))
                                    call.respond(HttpStatusCode.OK, "Отклик на резюме успешно оставлен")
                                else
                                    call.respond(HttpStatusCode.NotFound)
                            } else {
                                call.respond(HttpStatusCode.Conflict, "Вы уже оставляли отклик на это резюме")
                            }
                        } else {
                            call.respond(HttpStatusCode.Forbidden, "Вы должны быть менеджером компании, чтобы оставлять отклики на резюме")
                        }
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Вы не можете оставить отклик на своё резюме")
                    }
                } ?: call.respond(HttpStatusCode.BadRequest, "Передан неправильный объект Reply")
            }
        }
        route("/user/{id}/resumes/{resumeId}/reply") {
            get {
                parseId()?.let { id ->
                    replyRepo.all().find { it.userId == id && it.resumeId == resumeId() }?.let { elem ->
                        call.respond(elem)
                    } ?: call.respond(HttpStatusCode.NotFound, "Отклик на резюме не найден")
                } ?: call.respond(HttpStatusCode.BadRequest, "Передан неправильный ID")
            }
        }
        route("/user/resumes/reply/{id}") {
            get {
                parseId()?.let { id ->
                    replyRepo.get(id)?.let { elem ->
                        call.respond(elem)
                    } ?: call.respond(HttpStatusCode.NotFound, "Отклик на резюме не найден")
                } ?: call.respond(HttpStatusCode.BadRequest, "Передан неправильный ID")
            }
            put {
                parseBody(replySerializer)?.let { elem ->
                    parseId()?.let { id ->
                        if (replyRepo.update(id, elem))
                            call.respond(HttpStatusCode.OK)
                        else
                            call.respond(HttpStatusCode.NotFound, "Отклика на резюме с таким ID не существует")
                    }
                } ?: call.respond(HttpStatusCode.BadRequest, "Передан неправильный объект Reply")
            }
            delete {
                parseId()?.let { id ->
                    if (replyRepo.delete(id))
                        call.respond(HttpStatusCode.OK, "Отклик на резюме успешно удален")
                    else
                        call.respond(HttpStatusCode.NotFound, "Отклика на резюме с таким ID не существует")
                } ?: call.respond(HttpStatusCode.BadRequest, "Передан неправильный ID")
            }
        }

    }
}

fun PipelineContext<Unit, ApplicationCall>.parseId(id: String = "id") =
    call.parameters[id]?.toIntOrNull()

fun PipelineContext<Unit, ApplicationCall>.resumeId(id: String = "resumeId") =
        call.parameters[id]?.toIntOrNull()

suspend fun <T> PipelineContext<Unit, ApplicationCall>.parseBody(
    serializer: KSerializer<T>
) =
    try {
        Json.decodeFromString(
            serializer,
            call.receive()
        )
    } catch (e: Throwable) {
        null
    }
