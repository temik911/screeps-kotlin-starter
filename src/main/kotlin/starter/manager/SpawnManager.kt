package starter.manager

import screeps.api.BODYPART_COST
import screeps.api.FIND_MY_SPAWNS
import screeps.api.OK
import screeps.api.Room
import screeps.api.get
import screeps.api.options
import starter.UniqueIdGenerator
import starter.memory.body
import starter.memory.memory
import starter.memory.spawnRequests

/**
 *
 *
 * @author zakharchuk
 * @since 13.06.2020
 */
class SpawnManager(val room: Room) {

    public fun trySpawn() {
        try {
            var spawnRequests = room.memory.spawnRequests
            if (!spawnRequests.isNullOrEmpty()) {
                val spawnRequest = spawnRequests.first()

                val cost = spawnRequest.body.sumBy { BODYPART_COST[it]!! }
                if (cost > room.energyAvailable) {
                    return
                }

                val spawn = room.find(FIND_MY_SPAWNS).firstOrNull { it.spawning == null } ?: return
                val name = UniqueIdGenerator.generateUniqueId()
                val dryCode = spawn.spawnCreep(spawnRequest.body, name, options {
                    memory = spawnRequest.memory
                    dryRun = true
                })
                when (dryCode) {
                    OK -> {
                        val createCode = spawn.spawnCreep(spawnRequest.body, name, options {
                            memory = spawnRequest.memory
                        })
                        when (createCode) {
                            OK -> {
                                spawnRequests = spawnRequests.drop(1).toTypedArray()
                            }
                        }
                    }
                }
                room.memory.spawnRequests = spawnRequests
            }
        } catch (e: Throwable) {
            console.log("exception: $e")
        }
    }
}