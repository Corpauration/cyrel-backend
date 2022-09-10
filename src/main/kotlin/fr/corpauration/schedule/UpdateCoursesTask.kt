package fr.corpauration.schedule

import fr.corpauration.cycelcat.CyCelcat
import fr.corpauration.cycelcat.resources.Course
import fr.corpauration.cycelcat.resources.SideBarEventElement
import fr.corpauration.cycelcat.resources.Student
import fr.corpauration.group.GroupRepository
import fr.corpauration.user.StudentRepository
import fr.corpauration.utils.CustomSql
import io.quarkus.arc.Arc
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import io.smallrye.mutiny.coroutines.awaitSuspending
import io.vertx.mutiny.pgclient.PgPool
import io.vertx.mutiny.sqlclient.SqlConnection
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.jboss.logging.Logger
import java.time.LocalDate
import java.time.LocalDateTime
import javax.enterprise.context.ApplicationScoped


@ApplicationScoped
class UpdateCoursesTask {
    var groupRepository: GroupRepository = Arc.container().instance(GroupRepository::class.java).get()

    var studentRepository: StudentRepository = Arc.container().instance(StudentRepository::class.java).get()

    private final val courseRepository: CourseRepository = CourseRepository()

    lateinit var tr: SqlConnection

    private val LOG: Logger = Logger.getLogger(UpdateCoursesTask::class.java)

    init {
        LOG.info("Updating courses...")
        courseRepository.client = Arc.container().instance(PgPool::class.java).get()
        val celcat = CyCelcat()
        runBlocking {
            celcat.login(System.getenv("CELCAT_USERNAME"), System.getenv("CELCAT_PASSWORD"))
            getGroupReferents().flatMap {
                GlobalScope.async {
                    Uni.join().all(it.map { updateCourses(celcat, it.first, it.second) }).andCollectFailures()
                        .awaitSuspending()
                }.asUni()
            }.awaitSuspending()
        }
        LOG.info("Done!")
    }

    fun getGroupReferents(): Uni<List<Pair<Int, Int>>> {
        return groupRepository.findBy(false, "private").collect().where {
            it.referent != null
        }.asList().flatMap {
            Uni.join().all(it.map { groupEntity ->
                studentRepository.findById(groupEntity.referent!!).map { Pair(groupEntity.id, it.student_id) }
            }).andCollectFailures()
        }
    }

    suspend fun updateCourses(celcat: CyCelcat, group: Int, referent: Int): Uni<MutableList<Void>>? {
        LOG.info("Updating courses for group $group with referent id $referent")
        val now = LocalDate.now()
        val courses = celcat.getCalendarData(
            if (now.month.value in 9..12) now.withMonth(9).withDayOfMonth(1) else now.withMonth(9).withDayOfMonth(1)
                .minusYears(1),
            if (now.month.value in 9..12) LocalDate.now().withMonth(9).withDayOfMonth(1)
                .plusYears(1) else now.withMonth(9).withDayOfMonth(1),
            Student::class,
            "month",
            referent
        )

        return courseRepository.client.withTransaction {
            tr = it
            wrapperDeleteGroupFromCourses(group)
                .flatMap {
                    Uni.join()
                        .all(courses.map { GlobalScope.async { updateEvent(celcat, it).awaitSuspending() }.asUni() })
                        .andCollectFailures()
                        .flatMap {
                            Uni.join().all(courses.map { c ->
                                GlobalScope.async {
                                    wrapperInsertGroupFromCourses(
                                        c.id,
                                        group
                                    ).awaitSuspending()
                                }.asUni()
                            })
                                .andCollectFailures()
                        }
                }
        }
    }

    suspend fun updateEvent(celcat: CyCelcat, course: Course): Uni<Void> {
        val event = celcat.getSideBarEvent(course.id)
        val id: String = course.id
        val start: LocalDateTime = LocalDateTime.parse(course.start)
        val end: LocalDateTime? = if (course.end == null) null else LocalDateTime.parse(course.end)
        var category: CourseCategory = CourseCategory.DEFAULT
        var subject: String? = null
        var teachers = ""
        var rooms = ""
//        var groups: List<GroupEntity>

        var svElem = SideBarEventElement(
            "",
            entityType = 0,
            containsHyperlinks = false,
            isNotes = false,
            isStudentSpecific = false,
            content = null,
            assignmentContext = null,
            federationId = null
        )
        for (element in event.elements) {
            if (element.label != "") svElem = element
            when (element.label) {
                "Catégorie" -> category = when (element.content) {
                    "CM" -> CourseCategory.CM
                    else -> CourseCategory.DEFAULT
                }

                "Matière" -> subject = element.content
                "Salle" -> rooms = element.content.toString()
                "Enseignant" -> teachers = element.content.toString()
                "" -> when (svElem.label) {
                    "Salle" -> rooms += ",${element.content}"
                    "Enseignant" -> teachers += ",${element.content}"
                }
            }
        }
        return wrapperUpdateCourse(id, start, end, category, subject, rooms, teachers)
    }

    @CustomSql(
        """
            DELETE FROM courses_groups
            WHERE ref = $1
    """, entity = CourseEntity::class
    )
    fun wrapperDeleteGroupFromCourses(group: Int): Uni<Void> {
        return courseRepository.wrapperDeleteGroupFromCoursesWithTransaction(group, tr)
    }

    @CustomSql(
        """
            INSERT INTO courses
                ( id
                , start
                , "end"
                , category
                , subject
                , rooms
                , teachers
                )
            VALUES ( $1, $2, $3, $4, $5, $6, $7 )
            ON CONFLICT (id) DO UPDATE
            SET ( start
                , "end"
                , category
                , subject
                , rooms
                , teachers
                ) = ( EXCLUDED.start
                    , EXCLUDED.end
                    , EXCLUDED.category
                    , EXCLUDED.subject
                    , EXCLUDED.rooms
                    , EXCLUDED.teachers
                    )
    """, entity = CourseEntity::class
    )
    fun wrapperUpdateCourse(
        id: String,
        start: LocalDateTime,
        end: LocalDateTime?,
        category: CourseCategory,
        subject: String?,
        teachers: String,
        rooms: String,
    ): Uni<Void> {
        return courseRepository.wrapperUpdateCourseWithTransaction(
            id,
            start,
            end,
            category,
            subject,
            teachers,
            rooms,
            tr
        )
    }

    @CustomSql(
        """
        INSERT INTO courses_groups (id, ref)
        VALUES ( $1, $2 )
    """, entity = CourseEntity::class
    )
    fun wrapperInsertGroupFromCourses(course: String, group: Int): Uni<Void> {
        return courseRepository.wrapperInsertGroupFromCoursesWithTransaction(course, group, tr)
    }
}