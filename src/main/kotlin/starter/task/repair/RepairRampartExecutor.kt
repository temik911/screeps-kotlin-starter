package starter.task.repair

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.RESOURCE_ENERGY
import screeps.api.Room
import screeps.api.STRUCTURE_RAMPART
import screeps.api.options
import screeps.api.structures.Structure
import starter.calculateRepairRampartBody
import starter.extension.moveToWithSwap
import starter.memory.TaskMemory
import starter.memory.getRoomMemory
import starter.memory.rampartHitsMin
import starter.memory.status
import starter.memory.targetId
import starter.memory.targetRequiredHits
import starter.task.ExecutorWithExclusiveCreep

/**
 *
 *
 * @author zakharchuk
 * @since 26.03.2020
 */
class RepairRampartExecutor(task: TaskMemory): ExecutorWithExclusiveCreep(task) {

    override fun getCreepBody(room: Room): Array<BodyPartConstant> = room.calculateRepairRampartBody()

    override fun prepare(task: TaskMemory, creep: Creep): Boolean {
        if (task.status == "undefined") {
            task.status = "withdraw"
        }
        return true
    }

    override fun executeInternal(creep: Creep) {
        when (task.status) {
            "repair" -> {
                if (task.targetId == "undefined") {
                    val target = creep.room.find(FIND_STRUCTURES, options {
                        filter = {
                            it.structureType == STRUCTURE_RAMPART
                        }
                    }).minBy { it.hits } ?: return

                    val hitsToRepair = if (creep.room.controller!!.level == 8) 90000 else 45000

                    task.targetId = target.id
                    task.targetRequiredHits = target.hits + hitsToRepair
                    getRoomMemory(creep.room.name).rampartHitsMin = target.hits
                }

                val target = Game.getObjectById<Structure>(task.targetId)

                if (target == null || target.hits > task.targetRequiredHits) {
                    task.targetId = "undefined"
                    task.targetRequiredHits = 0
                    return
                }

                if (creep.repair(target) == ERR_NOT_IN_RANGE) {
                    creep.moveToWithSwap(target)
                }

                if (creep.store.getUsedCapacity() == 0) {
                    task.status = "withdraw"
                }
            }
            "withdraw" -> {
                if (creep.room.storage != null && creep.room.storage!!.store.getUsedCapacity(RESOURCE_ENERGY)?:0 > 5000) {
                    if (creep.withdraw(creep.room.storage!!, RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
                        creep.moveToWithSwap(creep.room.storage!!)
                    }
                }
                if (creep.store.getFreeCapacity(RESOURCE_ENERGY) ?: 0 == 0) {
                    task.status = "repair"
                }
            }
        }
    }
}