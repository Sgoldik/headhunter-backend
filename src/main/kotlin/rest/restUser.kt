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
                call.respond(
                    parseBody(userSerializer)?.let { elem ->
                        if (userRepo.all().filter { it.username == elem.username }.isEmpty()) {
                            if (userRepo.add(elem)) {
                                val user = userRepo.all().find { it.username == elem.username }!!
                                call.respond(user)
                            } else {
                                HttpStatusCode.NotFound
                            }
                        } else {
                            call.respond(HttpStatusCode.Conflict, "Пользователь с таким именем уже существует")
                        }
                    } ?: call.respond(HttpStatusCode.BadRequest, "Передан неправильный объект User")
                )
            }
        }
        route("/user/login") {
            post {
                call.respond(
                        parseBody(userSerializer)?.let { elem ->
                            if (userRepo.all().filter { it.username == elem.username }.isNotEmpty()) {
                                if (elem.password == userRepo.all().find { it.username == elem.username }!!.password) {
                                    val user = userRepo.all().find { it.username == elem.username }!!
                                    call.respond(user)
                                } else {
                                    call.respond(HttpStatusCode.BadRequest, "Введен неправильный пароль")
                                }
                            } else {
                                call.respond(HttpStatusCode.NotFound, "Пользователь с таким именем не найден")
                            }
                        } ?: call.respond(HttpStatusCode.BadRequest, "Передан неправильный объект User")
                )
            }
        }
        route("/user/{id}") {
            get {
                parseId()?.let { id ->
                    userRepo.get(id)?.let { elem ->
                        call.respond(elem)
                    } ?: call.respond(HttpStatusCode.NotFound, "Пользователь с таким ID не найден")
                } ?: call.respond(HttpStatusCode.BadRequest, "Передан неправильный ID")
            }
            put {
                call.respond(
                    parseBody(userSerializer)?.let { elem ->
                        parseId()?.let { id ->
                            if (userRepo.update(id, elem))
                                call.respond(HttpStatusCode.OK, "Пользователь успешно обновлен")
                            else
                                HttpStatusCode.NotFound
                        }
                    } ?: call.respond(HttpStatusCode.BadRequest, "Передан неправильный объект User")
                )
            }
            delete {
                call.respond(
                    parseId()?.let { id ->
                        if (userRepo.delete(id))
                            call.respond(HttpStatusCode.OK, "Пользователь успешно удален")
                        else
                            call.respond(HttpStatusCode.NotFound, "Пользователь с таким ID не существует")
                    } ?: call.respond(HttpStatusCode.BadRequest, "Передан неправильный ID")
                )
            }
        }
        route("/user/resumes") {
            post {
                call.respond(
                    parseBody(resumeSerializer)?.let { elem ->
                        if (resumeRepo.add(elem))
                            call.respond(HttpStatusCode.OK, "Резюме успешно создано")
                        else
                            HttpStatusCode.NotFound
                    } ?: call.respond(HttpStatusCode.BadRequest, "Передан неправильный объект Resume")
                )
            }
            get {
                if (resumeRepo.all().isNotEmpty()) {
                    val resumes = resumeRepo.all()
                    call.respond(resumes)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Резюме не найдены")
                }
            }
        }

        route("/user/{id}/resumes") {
            get {
                if (resumeRepo.all().filter { it.userId == parseId() }.isNotEmpty()) {
                    val userResumes = resumeRepo.all().filter { it.userId == parseId() }
                    call.respond(userResumes)
                } else {
                    call.respond(HttpStatusCode.NotFound, "У этого пользователя нет ни одного резюме")
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
                call.respond(
                    parseBody(resumeSerializer)?.let { elem ->
                        parseId()?.let { id ->
                            if (resumeRepo.update(id, elem))
                                call.respond(HttpStatusCode.OK, "Резюме успешно обновлено")
                            else
                                HttpStatusCode.NotFound
                        }
                    } ?: call.respond(HttpStatusCode.BadRequest, "Передан неправильный объект Resume")
                )
            }
            delete {
                call.respond(
                    parseId()?.let { id ->
                        if (resumeRepo.delete(id))
                            call.respond(HttpStatusCode.OK, "Резюме успешно удалено")
                        else
                            call.respond(HttpStatusCode.NotFound, "Резюме с таким ID не найдено")
                    } ?: call.respond(HttpStatusCode.BadRequest, "Передан неправильный ID")
                )
            }
        }
        route("/user/resumes/{id}/replies") {
            get {
                if (replyRepo.all().filter { it.resumeId == parseId() }.isNotEmpty()) {
                    val userReplies = replyRepo.all().filter { it.resumeId == parseId() }
                    call.respond(userReplies)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Отклики для этого резюме не найдены")
                }
            }
        }
        route("/user/{id}/resumes/replies") {
            get {
                    val replies = resumeRepo.all().map { resume ->
                        replyRepo.all().filter { it.resumeId == resume.id && it.userId == parseId() }.isNotEmpty()
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
                call.respond(
                    parseBody(replySerializer)?.let { elem ->
                        if (elem.userId != resumeRepo.get(elem.resumeId)?.userId) {
                            if (userRepo.get(elem.userId)?.status == 1) {
                                if (replyRepo.all().filter { it.resumeId == elem.resumeId && it.userId == elem.userId }.isEmpty()) {
                                    if (replyRepo.add(elem))
                                        call.respond(HttpStatusCode.OK, "Отклик на резюме успешно оставлен")
                                    else
                                        HttpStatusCode.NotFound
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
                )
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
                call.respond(
                    parseBody(replySerializer)?.let { elem ->
                        parseId()?.let { id ->
                            if (replyRepo.update(id, elem))
                                HttpStatusCode.OK
                            else
                                call.respond(HttpStatusCode.NotFound, "Отклика на резюме с таким ID не существует")
                        }
                    } ?: call.respond(HttpStatusCode.BadRequest, "Передан неправильный объект Reply")
                )
            }
            delete {
                call.respond(
                    parseId()?.let { id ->
                        if (replyRepo.delete(id))
                            call.respond(HttpStatusCode.OK, "Отклик на резюме успешно удален")
                        else
                            call.respond(HttpStatusCode.NotFound, "Отклика на резюме с таким ID не существует")
                    } ?: call.respond(HttpStatusCode.BadRequest, "Передан неправильный ID")
                )
            }
        }

    }
}

fun PipelineContext<Unit, ApplicationCall>.parseId(id: String = "id") =
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
