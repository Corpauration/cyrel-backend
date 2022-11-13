package fr.corpauration.homework

import com.fasterxml.jackson.databind.JsonNode
import fr.corpauration.group.ADMIN
import fr.corpauration.group.GroupEntity
import fr.corpauration.group.GroupRepository
import fr.corpauration.group.HOMEWORK_RESP
import fr.corpauration.user.UserRepository
import fr.corpauration.user.UserType
import fr.corpauration.utils.*
import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import java.time.LocalDate
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType


@Path("/homework")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
class HomeworkResource {
    @Inject
    @RepositoryGenerator(
        table = "homeworks",
        id = UUID::class,
        entity = HomeworkEntity::class
    )
    lateinit var homeworkRepository: HomeworkRepository

    @Inject
    lateinit var groupRepository: GroupRepository

    @Inject
    lateinit var identity: SecurityIdentity

    @Inject
    lateinit var userRepository: UserRepository

    @GET
    @Path("/{id}")
    @AccountExist
    @Produces(MediaType.APPLICATION_JSON)
    fun getById(@PathParam("id") id: UUID): Uni<HomeworkEntity> {
        return homeworkRepository.findById(id)
    }

    @POST
    @AccountExist
    @NeedToBeProfessorOrInGroups(HOMEWORK_RESP)
    fun createHomework(json: JsonNode): Uni<Void> {
        if (!json.hasNonNull("title") || !json.get("title").isTextual || !json.hasNonNull(
                "type"
            ) || !json.get("type").isInt || !json.hasNonNull("content") || !json.get("content").isTextual || !json.hasNonNull(
                "date"
            ) || !json.get("date").isTextual || !json.hasNonNull("group") || !json.get("group").isInt
        ) throw HomeworkMalformed()

        return userRepository.findBy(identity.principal.name, "email").collect().asList().onItem()
            .transform { it[0] }
            .flatMap {
                if (it.groups.map { it.id }
                        .contains(json.get("group").asInt()) || it.type == UserType.PROFESSOR.ordinal)
                    homeworkRepository.save(
                        HomeworkEntity(
                            title = json.get("title").asText(),
                            content = json.get("content").asText(),
                            type = json.get("type").asInt(),
                            date = LocalDate.parse(json.get("date").asText()),
                            group = if (it.type != UserType.PROFESSOR.ordinal) it.groups.filter {
                                it.id == json.get("group").asInt()
                            }[0] else GroupEntity(id = json.get("group").asInt())
                        )
                    )
                else throw UnauthorizedGroupTarget()
            }
    }

    @PUT
    @Path("/{id}")
    @AccountExist
    @NeedToBeProfessorOrInGroups(HOMEWORK_RESP)
    fun update(@PathParam("id") id: UUID, json: JsonNode): Uni<Void> {
        return homeworkRepository.findById(id).flatMap {
            homework ->
            userRepository.findBy(identity.principal.name, "email").collect().asList().onItem()
                .transform { it[0] }
                .flatMap {
                    user ->
                    if ((user.groups.map { it.id }
                            .contains(homework.group.id) || user.type == UserType.PROFESSOR.ordinal) && (json.hasNonNull(
                            "group"
                        ) && json.get("group").isInt && (user.groups.map { it.id }.contains(
                            json.get("group").asInt()
                        ) || user.type == UserType.PROFESSOR.ordinal) || !json.hasNonNull("group"))
                    ) {
                        if (json.hasNonNull("title") && json.get("title").isTextual) homework.title =
                            json.get("title").asText()
                        if (json.hasNonNull("content") && json.get("content").isTextual) homework.content =
                            json.get("content").asText()
                        if (json.hasNonNull("type") && json.get("type").isInt) homework.type = json.get("type").asInt()
                        if (json.hasNonNull("date") && json.get("date").isTextual) homework.date =
                            LocalDate.parse(json.get("date").asText())
                        if (json.hasNonNull("group") && json.get("group").isInt) homework.group =
                            GroupEntity(id = json.get("group").asInt())
                        homeworkRepository.update(homework)
                    } else throw UnauthorizedGroupTarget()
                }
        }
    }

    @DELETE
    @Path("/{id}")
    @AccountExist
    @NeedToBeProfessorOrInGroups(HOMEWORK_RESP)
    fun delete(@PathParam("id") id: UUID): Uni<Void> {
        return homeworkRepository.findById(id).flatMap {
                homework ->
            userRepository.findBy(identity.principal.name, "email").collect().asList().onItem()
                .transform { it[0] }
                .flatMap {
                        user ->
                    if (user.groups.map { it.id }
                            .contains(homework.group.id) || user.type == UserType.PROFESSOR.ordinal) {
                        homeworkRepository.delete(homework)
                    } else throw UnauthorizedGroupTarget()
                }
        }
    }
}

@Path("/homeworks")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
class HomeworksResource {
    @Inject
    lateinit var homeworkRepository: HomeworkRepository

    @Inject
    lateinit var groupRepository: GroupRepository

    @GET
    @AccountExist
    @NeedToBeInGroups(ADMIN)
    @Produces(MediaType.APPLICATION_JSON)
    fun getAll(): Multi<HomeworkEntity> {
        return homeworkRepository.getAll()
    }

    @POST
    @AccountExist
    @Produces(MediaType.APPLICATION_JSON)
    fun getFromTo(json: JsonNode): Uni<List<HomeworkEntity>> {
        if (!json.hasNonNull("group") || !json.get("group").isInt || !json.hasNonNull(
                "start"
            ) || !json.get("start").isTextual || !json.hasNonNull("end") || !json.get("end").isTextual
        ) throw BadRequestException()
        val start = LocalDate.parse(json.get("start").asText())
        val end = LocalDate.parse(json.get("end").asText())
        return wrapperRetrieveHomeworksForGroupBetweenDate(json.get("group").asInt(), start, end).collect().asList()
    }

    @CustomSql(
        """
        select id, title, content, date, "group", type from homeworks
        where "group" = $1 and date >= $2 and date <= $3
    """, entity = HomeworkEntity::class
    )
    fun wrapperRetrieveHomeworksForGroupBetweenDate(
        group: Int,
        start: LocalDate,
        end: LocalDate
    ): Multi<HomeworkEntity> {
        return homeworkRepository.wrapperRetrieveHomeworksForGroupBetweenDate(group, start, end)
    }
}