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
                        if (userRepo.add(elem))
                            HttpStatusCode.OK
                        else
                            HttpStatusCode.NotFound
                    } ?: HttpStatusCode.BadRequest
                )
            }
        }
        route("/user/{id}") {
            get {
                parseId()?.let { id ->
                    userRepo.get(id)?.let { elem ->
                        call.respond(elem)
                    } ?: call.respond(HttpStatusCode.NotFound)
                } ?: call.respond(HttpStatusCode.BadRequest)
            }
            put {
                call.respond(
                    parseBody(userSerializer)?.let { elem ->
                        parseId()?.let { id ->
                            if (userRepo.update(id, elem))
                                HttpStatusCode.OK
                            else
                                HttpStatusCode.NotFound
                        }
                    } ?: HttpStatusCode.BadRequest
                )
            }
            delete {
                call.respond(
                    parseId()?.let { id ->
                        if (userRepo.delete(id))
                            HttpStatusCode.OK
                        else
                            HttpStatusCode.NotFound
                    } ?: HttpStatusCode.BadRequest
                )
            }
        }
        route("/user/resumes") {
            post {
                call.respond(
                    parseBody(resumeSerializer)?.let { elem ->
                        if (resumeRepo.add(elem))
                            HttpStatusCode.OK
                        else
                            HttpStatusCode.NotFound
                    } ?: HttpStatusCode.BadRequest
                )
            }
        }

        route("/user/{id}/resumes") {
            get {
                val userResumes = resumeRepo.all().filter { it.userId == parseId() }
                call.respond(userResumes)
            }
        }

        route("/user/resumes/{id}") {
            get {
                parseId()?.let { id ->
                    resumeRepo.get(id)?.let { elem ->
                        call.respond(elem)
                    } ?: call.respond(HttpStatusCode.NotFound)
                } ?: call.respond(HttpStatusCode.BadRequest)
            }
            put {
                call.respond(
                    parseBody(resumeSerializer)?.let { elem ->
                        parseId()?.let { id ->
                            if (resumeRepo.update(id, elem))
                                HttpStatusCode.OK
                            else
                                HttpStatusCode.NotFound
                        }
                    } ?: HttpStatusCode.BadRequest
                )
            }
            delete {
                call.respond(
                    parseId()?.let { id ->
                        if (resumeRepo.delete(id))
                            HttpStatusCode.OK
                        else
                            HttpStatusCode.NotFound
                    } ?: HttpStatusCode.BadRequest
                )
            }
        }
        route("/user/resumes/{id}/replies") {
            get {
                val userReplies = replyRepo.all().filter { it.resumeId == parseId() }
                call.respond(userReplies)
            }
        }
        route("/user/resumes/reply") {
            post {
                call.respond(
                    parseBody(replySerializer)?.let { elem ->
                        if (replyRepo.add(elem))
                            HttpStatusCode.OK
                        else
                            HttpStatusCode.NotFound
                    } ?: HttpStatusCode.BadRequest
                )
            }
        }
        route("/user/resumes/reply/{id}") {
            get {
                parseId()?.let { id ->
                    replyRepo.get(id)?.let { elem ->
                        call.respond(elem)
                    } ?: call.respond(HttpStatusCode.NotFound)
                } ?: call.respond(HttpStatusCode.BadRequest)
            }
            put {
                call.respond(
                    parseBody(replySerializer)?.let { elem ->
                        parseId()?.let { id ->
                            if (replyRepo.update(id, elem))
                                HttpStatusCode.OK
                            else
                                HttpStatusCode.NotFound
                        }
                    } ?: HttpStatusCode.BadRequest
                )
            }
            delete {
                call.respond(
                    parseId()?.let { id ->
                        if (replyRepo.delete(id))
                            HttpStatusCode.OK
                        else
                            HttpStatusCode.NotFound
                    } ?: HttpStatusCode.BadRequest
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
