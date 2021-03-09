package starter.task.defend

import screeps.api.Creep
import screeps.api.FIND_HOSTILE_CREEPS
import screeps.api.FIND_MY_CREEPS
import screeps.api.FIND_MY_STRUCTURES
import screeps.api.FilterOption
import screeps.api.Game
import screeps.api.STRUCTURE_RAMPART
import screeps.api.get
import screeps.api.options
import screeps.api.structures.StructureTower
import starter.memory.TaskMemory
import starter.memory.room
import starter.memory.sleepUntil

/**
 *
 *
 * @author zakharchuk
 * @since 05.03.2020
 */
class TowerDefendExecutor(val task: TaskMemory) {
    fun execute() {
        if (task.sleepUntil > Game.time) {
            return
        }

        val room = Game.rooms.get(task.room)!!
        val towers = room.find(FIND_MY_STRUCTURES, options {
            filter = {
                it.structureType == screeps.api.STRUCTURE_TOWER
                        && it.unsafeCast<StructureTower>().store.getUsedCapacity(screeps.api.RESOURCE_ENERGY) ?: 0 > 0
            }
        }).map { it.unsafeCast<StructureTower>() }


        var isActive = false

        for (tower in towers) {
            val hostileCreep: Creep? = tower.pos.findClosestByRange(FIND_HOSTILE_CREEPS, options<FilterOption<Creep>> {
                filter = {
                    it.owner.username != "FeTiD"
                }
            })
            if (hostileCreep != null) {
                tower.attack(hostileCreep)
                isActive = true
                continue
            }

            val creeps = tower.room.find(FIND_MY_CREEPS, options {
                filter = {
                    it.hits < it.hitsMax
                }
            })

            if (!creeps.isNullOrEmpty()) {
                tower.heal(creeps[0])
                isActive = true
            } else {
                val rampart = room.find(FIND_MY_STRUCTURES, options {
                    filter = {
                        it.structureType == STRUCTURE_RAMPART
                    }
                }).firstOrNull { structure -> structure.hits < 1000 } ?: return

                tower.repair(rampart)
                isActive = true
            }
        }

        if (!isActive) {
            task.sleepUntil = Game.time + 10
        }
    }
}