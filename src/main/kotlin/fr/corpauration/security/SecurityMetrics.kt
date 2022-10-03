package fr.corpauration.security

import java.time.LocalDateTime
import javax.enterprise.context.ApplicationScoped
import kotlin.properties.Delegates

@ApplicationScoped
class SecurityMetrics {
    var count by Delegates.notNull<Int>()
    lateinit var time: LocalDateTime

    init {
        count = 0;
        time = LocalDateTime.now()
    }
}