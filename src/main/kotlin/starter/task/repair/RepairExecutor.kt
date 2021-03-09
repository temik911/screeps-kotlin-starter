package starter.task.repair

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.RESOURCE_ENERGY
import screeps.api.Room
import screeps.api.RoomObject
import screeps.api.RoomPosition
import screeps.api.STRUCTURE_CONTAINER
import screeps.api.STRUCTURE_ROAD
import screeps.api.options
import screeps.api.structures.Structure
import starter.ContainerType
import starter.calculateRepairBody
import starter.extension.moveToWithSwap
import starter.memory.TaskMemory
import starter.memory.status
import starter.memory.targetId
import starter.memory.toRoom
import starter.task.ExecutorWithExclusiveCreep
import starter.withdraw

/**
 *
 *
 * @author zakharchuk
 * @since 05.03.2020
 */
class RepairExecutor(task: TaskMemory): ExecutorWithExclusiveCreep(task) {

    override fun getCreepBody(room: Room): Array<BodyPartConstant> = room.calculateRepairBody()

    override fun prepare(task: TaskMemory, creep: Creep): Boolean {
        if (task.status == "undefined") {
            task.status = "withdraw"
        }

        if (creep.room.name == task.toRoom && creep.pos.x in 1..48 && creep.pos.y in 1..48) {
            return true
        }
        creep.moveToWithSwap(RoomPosition(25, 25, task.toRoom))
        return false
    }

    override fun executeInternal(creep: Creep) {
        when (task.status) {
            "repair" -> {
                if (task.targetId == "undefined" || Game.time % 100 == 0) {
                    val targets = creep.room.find(FIND_STRUCTURES)
                            .filter { (it.structureType == STRUCTURE_ROAD || it.structureType == STRUCTURE_CONTAINER) }
                            .filter { it.hits < it.hitsMax * 0.75 }

                    if (targets.isNotEmpty()) {
                        val target = creep.pos.findClosestByPath<Structure>(targets.map { it.unsafeCast<RoomObject>() }.toTypedArray())
                                ?: return
                        task.targetId = target.id
                    }
                }

                val target = Game.getObjectById<Structure>(task.targetId)

                if (target == null || target.hits > target.hitsMax * 0.9) {
                    task.targetId = "undefined"
                    return
                }

                if (creep.repair(target) == ERR_NOT_IN_RANGE) {
                    creep.moveTo(target, options {
                        maxRooms = 1
                    })
                }

                if (creep.store.getUsedCapacity() == 0) {
                    task.status = "withdraw"
                }
            }
            "withdraw" -> {
                withdraw(task, creep, ContainerType.ALL)
                if (creep.store.getFreeCapacity(RESOURCE_ENERGY) ?: 0 == 0) {
                    task.status = "repair"
                }
            }
        }
    }
}