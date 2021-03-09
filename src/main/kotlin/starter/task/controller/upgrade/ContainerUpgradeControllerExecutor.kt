package starter.task.controller.upgrade

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.FIND_STRUCTURES
import screeps.api.Memory
import screeps.api.RESOURCE_ENERGY
import screeps.api.Room
import screeps.api.STRUCTURE_CONTAINER
import screeps.api.get
import screeps.api.options
import screeps.api.structures.StructureContainer
import starter.calculateUpgradeBody
import starter.memory.containers
import starter.memory.isProvider
import starter.memory.TaskMemory

/**
 *
 *
 * @author zakharchuk
 * @since 15.03.2020
 */
class ContainerUpgradeControllerExecutor(task: TaskMemory) : AbstractUpgradeControllerExecutor(task) {
    override fun withdrawEnergy(creep: Creep) {
        val container = creep.room.find(FIND_STRUCTURES, options {
            filter = {
                it.structureType == STRUCTURE_CONTAINER && it.unsafeCast<StructureContainer>().store.getUsedCapacity(RESOURCE_ENERGY) ?: 0 > creep.store.getFreeCapacity(RESOURCE_ENERGY) ?: 0
                        && !(Memory.containers[it.id]?.isProvider ?: false)
            }
        }).map { structure -> structure.unsafeCast<StructureContainer>() }
                .sortedWith(Comparator { a, b -> if (a.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0 > b.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0) 1 else -1 })
                .lastOrNull()
                ?: return

        if (creep.withdraw(container, RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
            creep.moveTo(container.pos)
        }
    }

    override fun getCreepBody(room: Room): Array<BodyPartConstant> {
        return room.calculateUpgradeBody()
    }
}