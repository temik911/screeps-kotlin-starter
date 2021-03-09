package starter.task.baseenergysupport

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.Memory
import screeps.api.RESOURCE_ENERGY
import screeps.api.Room
import screeps.api.STRUCTURE_CONTAINER
import screeps.api.get
import screeps.api.options
import screeps.api.structures.StructureContainer
import starter.calculateBaseEnergySupportBody
import starter.extension.moveToWithSwap
import starter.memory.TaskMemory
import starter.memory.containerId
import starter.memory.containers
import starter.memory.isProvider
import starter.memory.status

/**
 *
 *
 * @author zakharchuk
 * @since 04.03.2020
 */
class BaseEnergySupportExecutor(task: TaskMemory) : AbstractBaseEnergySupportExecutor(task) {
    override fun withdrawEnergy(creep: Creep) {
        if (creep.store.getUsedCapacity() > 0) {
            task.status = "transfer"
            task.containerId = "undefined"
        }

        if (creep.room.storage != null && creep.room.storage!!.store.getUsedCapacity(RESOURCE_ENERGY)?:0 > 0) {
            if (creep.withdraw(creep.room.storage!!, RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
                creep.moveToWithSwap(creep.room.storage!!)
            }
        } else {
            if (task.containerId == "undefined" || Game.time % 25 == 0) {
                val container = creep.room.find(FIND_STRUCTURES, options {
                    filter = {
                        it.structureType == STRUCTURE_CONTAINER
                                && it.unsafeCast<StructureContainer>().store.getUsedCapacity(RESOURCE_ENERGY) ?: 0 > creep.store.getFreeCapacity(RESOURCE_ENERGY) ?: 0
                                && if (Memory.containers[it.id] != null) Memory.containers[it.id]!!.isProvider else false
                    }
                }).map { structure -> structure.unsafeCast<StructureContainer>() }
                        .sortedWith(Comparator { a, b -> if (a.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0 > b.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0) 1 else -1 })
                        .lastOrNull()

                if (container == null) {
                    return
                }

                task.containerId = container.id
            }

            val container = Game.getObjectById<StructureContainer>(task.containerId)

            if (container == null) {
                task.containerId = "undefined"
                return
            }

            if (creep.withdraw(container, RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
                creep.moveToWithSwap(container.pos)
            }

            if (creep.store.getFreeCapacity(RESOURCE_ENERGY) ?: 0 == 0) {
                task.containerId = "undefined"
            }
        }
    }

    override fun getCreepBody(room: Room): Array<BodyPartConstant> {
        return room.calculateBaseEnergySupportBody()
    }
}