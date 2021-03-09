package starter.task.harvest

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.Memory
import screeps.api.Room
import screeps.api.RoomPosition
import screeps.api.STRUCTURE_CONTAINER
import screeps.api.Source
import screeps.api.WORK
import screeps.api.get
import screeps.api.options
import screeps.api.set
import screeps.api.structures.Structure
import screeps.api.structures.StructureContainer
import screeps.utils.unsafe.jsObject
import starter.memory.TaskMemory
import starter.calculateHarvestBody
import starter.memory.containerId
import starter.memory.containers
import starter.memory.isProvider
import starter.memory.status

/**
 *
 *
 * @author zakharchuk
 * @since 15.03.2020
 */
class PrimitiveHarvestExecutor(task: TaskMemory): AbstractHarvestExecutor<Source>(task) {
    override fun harvest(creep: Creep, source: Source) {
        if (task.containerId == "undefined") {
            val findContainers = findContainer(source.pos)
            if (findContainers.isNullOrEmpty()) {
                // ищем контейнер
                return
            } else {
                val id = findContainers[0].id
                task.containerId = id
                if (Memory.containers.get(id) == null) {
                    Memory.containers.set(id, jsObject {
                        isProvider = true
                    })
                }
            }
        }

        val container = Game.getObjectById<StructureContainer>(task.containerId) ?: return
        if (!creep.pos.isEqualTo(container.pos)) {
            creep.moveTo(container.pos)
            return
        }

        when (task.status) {
            "repair" -> {
                if (creep.store.getUsedCapacity() < creep.body.sumBy { if (it.type == WORK) 1 else 0 }) {
                    creep.harvest(source)
                } else {
                    creep.repair(container)
                }
                if (container.hits > container.hitsMax * 0.9) {
                    task.status = "harvest"
                    return
                }
            }
            "harvest" -> {
                if (container.store.getFreeCapacity() > 0) {
                    creep.harvest(source)
                } else {
                    task.status = "repair"
                }

                if (source.energy == 0 || container.hits < container.hitsMax * 0.75) {
                    task.status = "repair"
                }
            }
        }
    }

    private fun findContainer(pos: RoomPosition): Array<Structure> {
        return pos.findInRange(FIND_STRUCTURES, 1, options {
            filter = {
                it.structureType == STRUCTURE_CONTAINER
            }
        })
    }

    override fun getCreepBody(room: Room): Array<BodyPartConstant> {
        return room.calculateHarvestBody()
    }

}