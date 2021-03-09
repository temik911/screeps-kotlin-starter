package starter.task.harvest

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.FIND_MY_STRUCTURES
import screeps.api.Game
import screeps.api.Memory
import screeps.api.RESOURCE_ENERGY
import screeps.api.Room
import screeps.api.RoomPosition
import screeps.api.STRUCTURE_LINK
import screeps.api.Source
import screeps.api.WORK
import screeps.api.options
import screeps.api.structures.Structure
import screeps.api.structures.StructureLink
import screeps.api.values
import screeps.utils.unsafe.jsObject
import starter.calculateHarvestBody
import starter.createTask
import starter.memory.TaskMemory
import starter.memory.linkId
import starter.memory.tasks
import starter.memory.type
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 04.03.2020
 */
class LinkHarvestExecutor(task: TaskMemory) : AbstractHarvestExecutor<Source>(task) {
    override fun harvest(creep: Creep, source: Source) {
        if (task.linkId == "undefined") {
            val findLinks = findLinks(source.pos)
            if (findLinks.isNullOrEmpty()) {
                // ищем контейнер
                return
            } else {
                task.linkId = findLinks[0].id
            }
        }

        val link = Game.getObjectById<StructureLink>(task.linkId)?: return

        if (creep.body.sumBy { if (it.type == WORK) 2 else 0 } > creep.store.getFreeCapacity(RESOURCE_ENERGY)?:0) {
            if (creep.transfer(link, RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
                creep.moveTo(link)
            }
        } else {
            if (creep.harvest(source) == ERR_NOT_IN_RANGE) {
                creep.moveTo(source)
            }
        }

        if (link.store.getUsedCapacity(RESOURCE_ENERGY)?:0 > (link.store.getCapacity(RESOURCE_ENERGY)?:0) / 2) {
            if (Memory.tasks.values.filter { taskMemory -> taskMemory.type == TaskType.LINK_TRANSFER.code && taskMemory.linkId == link.id }.isNullOrEmpty()) {
                createTask("${link.room.name}|${TaskType.LINK_TRANSFER.code}", jsObject {
                    type = TaskType.LINK_TRANSFER.code
                    linkId = link.id
                })
            }
        }
    }

    override fun getCreepBody(room: Room): Array<BodyPartConstant> {
        return room.calculateHarvestBody()
    }

    private fun findLinks(pos: RoomPosition): Array<Structure> {
        return pos.findInRange(FIND_MY_STRUCTURES, 2, options {
            filter = {
                it.structureType == STRUCTURE_LINK
            }
        })
    }

}