package fr.corpauration.rooms

import fr.corpauration.group.GroupRepository
import fr.corpauration.utils.AccountExist
import fr.corpauration.utils.CustomSql
import fr.corpauration.utils.RepositoryGenerator
import io.quarkus.security.Authenticated
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import java.time.LocalDateTime
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/room")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
class RoomResource {
    @Inject
    @RepositoryGenerator(
        table = "rooms", id = String::class, entity = RoomEntity::class
    )
    lateinit var roomRepository: RoomRepository

    @GET
    @Path("/{id}")
    @AccountExist
    @Produces(MediaType.APPLICATION_JSON)
    fun get(@PathParam("id") id: String): Uni<RoomEntity> {
        return roomRepository.findById(id)
    }
}

@Path("/rooms")
@Authenticated
class RoomsResource {
    @Inject
    lateinit var roomRepository: RoomRepository

    @Inject
    lateinit var groupRepository: GroupRepository

    @GET
    @Path("/free")
    @AccountExist
    @Produces(MediaType.APPLICATION_JSON)
    fun getFree(@QueryParam("date") date: LocalDateTime?): Multi<RoomEntity> =
        wrapperRetrieveFreeRooms(date ?: LocalDateTime.now())

    @CustomSql(
        """
select distinct r.id, r.name, r.capacity, r.computers
from rooms as r
         left join (select ra.id
                    from rooms_courses as ra
                             join courses as c on c.id = ra.ref
                    where $1 between c.start
                              and c."end") as a on r.id = a.id
where a.id is null;
    """, entity = RoomEntity::class
    )
    fun wrapperRetrieveFreeRooms(time: LocalDateTime): Multi<RoomEntity> {
        return roomRepository.wrapperRetrieveFreeRooms(time)
    }
}