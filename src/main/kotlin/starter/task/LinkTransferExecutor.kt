package starter.task

import screeps.api.FIND_MY_STRUCTURES
import screeps.api.Game
import screeps.api.OK
import screeps.api.RESOURCE_ENERGY
import screeps.api.RoomPosition
import screeps.api.STRUCTURE_LINK
import screeps.api.options
import screeps.api.structures.Structure
import screeps.api.structures.StructureLink
import starter.memory.TaskMemory
import starter.memory.isDone
import starter.memory.linkId

/**
 *
 *
 * @author zakharchuk
 * @since 14.02.2020
 */
class LinkTransferExecutor(val task: TaskMemory) {

    fun execute() {
        val link = Game.getObjectById<StructureLink>(task.linkId)

        if (link == null) {
            task.isDone = true
            return
        }

        if (link.cooldown != 0) {
            return
        }

        var target: StructureLink? = null
        val room = link.room

        if (room.storage != null) {
            if (room.storage!!.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0 < 5000) {
                val storageLinks = findLinks(room.storage!!.pos)
                if (!storageLinks.isNullOrEmpty()) {
                    target = storageLinks[0].unsafeCast<StructureLink>()
                }
            }
        }

        if (target == null) {
            val controllerLinks = findLinks(room.controller!!.pos)

            if (controllerLinks.isNullOrEmpty()) {
                val storageLinks = findLinks(room.storage!!.pos)
                if (!storageLinks.isNullOrEmpty()) {
                    target = storageLinks[0].unsafeCast<StructureLink>()
                }
            } else {
                target = controllerLinks[0].unsafeCast<StructureLink>()
            }
        }

        if (target == null) {
            return
        }

        if (link.transferEnergy(target) == OK) {
            task.isDone = true
        }
    }

    private fun findLinks(pos: RoomPosition): Array<Structure> {
        return pos.findInRange(FIND_MY_STRUCTURES, 2, options {
            filter = {
                it.structureType == STRUCTURE_LINK && it.unsafeCast<StructureLink>().store.getFreeCapacity(RESOURCE_ENERGY) ?: 0 >= 400
            }
        })
    }
}