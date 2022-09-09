package fr.corpauration.schedule

import io.quarkus.scheduler.Scheduled
import javax.enterprise.context.ApplicationScoped
import javax.transaction.Transactional


@ApplicationScoped
class UpdateCoursesTaskBean {
    @Transactional
    @Scheduled(every = "3h", identity = "update-courses")
    fun schedule() {
        val task = UpdateCoursesTask()
    }
}