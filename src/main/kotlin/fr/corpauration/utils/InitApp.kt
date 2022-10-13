package fr.corpauration.utils

import fr.corpauration.user.UserRepository
import io.micrometer.core.instrument.MeterRegistry
import io.quarkus.runtime.StartupEvent
import io.smallrye.mutiny.coroutines.awaitSuspending
import kotlinx.coroutines.runBlocking
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.event.Observes
import javax.inject.Inject

@ApplicationScoped
class InitApp {
    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var registry: MeterRegistry

    fun onStart(@Observes ev: StartupEvent?) {
        runBlocking {
            userRepository.getIds().collect().asList().onItem().transform {
                registry.counter("cyrel_backend_registered_users").increment(
                    it.size.toDouble()
                )
            }.awaitSuspending()
        }
    }
}