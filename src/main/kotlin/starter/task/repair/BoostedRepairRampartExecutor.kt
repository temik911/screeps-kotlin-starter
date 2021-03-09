package starter.task.repair

import screeps.api.BODYPART_COST
import screeps.api.BodyPartConstant
import screeps.api.CARRY
import screeps.api.Creep
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.MOVE
import screeps.api.RESOURCE_CATALYZED_KEANIUM_ACID
import screeps.api.RESOURCE_CATALYZED_LEMERGIUM_ACID
import screeps.api.RESOURCE_CATALYZED_ZYNTHIUM_ALKALIDE
import screeps.api.RESOURCE_ENERGY
import screeps.api.Room
import screeps.api.STRUCTURE_RAMPART
import screeps.api.WORK
import screeps.api.get
import screeps.api.options
import screeps.api.structures.Structure
import screeps.utils.unsafe.jsObject
import starter.calculateBoostedRepairRampartBody
import starter.createTask
import starter.extension.moveToWithSwap
import starter.getUsed
import starter.memory.BoostCreepTask
import starter.memory.TaskMemory
import starter.memory.amount
import starter.memory.boost
import starter.memory.boostCreepTask
import starter.memory.checkLinkedTasks
import starter.memory.creepId
import starter.memory.getRoomMemory
import starter.memory.linkedTaskIds
import starter.memory.rampartHitsMin
import starter.memory.room
import starter.memory.status
import starter.memory.targetId
import starter.memory.targetRequiredHits
import starter.memory.type
import starter.task.ExecutorWithExclusiveCreep
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 22.04.2020
 */
class BoostedRepairRampartExecutor(task: TaskMemory) : ExecutorWithExclusiveCreep(task) {
    companion object {
        fun isRoomApplicable(room: Room): Boolean {
            if (room.energyCapacityAvailable < room.calculateBoostedRepairRampartBody().sumBy { BODYPART_COST[it]!! }) {
                return false
            }
            val controller = room.controller ?: return false
            if (!controller.my || controller.level != 8) {
                return false
            }
            val storage = room.storage ?: return false
            return storage.store.getUsed(RESOURCE_CATALYZED_KEANIUM_ACID) >= 300
                    && storage.store.getUsed(RESOURCE_CATALYZED_LEMERGIUM_ACID) >= 900
                    && storage.store.getUsed(RESOURCE_CATALYZED_ZYNTHIUM_ALKALIDE) >= 300
                    && storage.store.getUsed(RESOURCE_ENERGY) >= 75000
        }
    }

    override fun getCreepBody(room: Room): Array<BodyPartConstant> = room.calculateBoostedRepairRampartBody()

    override fun prepare(task: TaskMemory, creep: Creep): Boolean {
        if (task.status == "undefined") {
            task.status = "withdraw"
        }

        checkLinkedTasks(task)
        if (creep.body.none { it.boost == null }) {
            return true
        }
        if (task.linkedTaskIds.isNullOrEmpty()) {
            val unBoostedPart = creep.body.firstOrNull { it.boost == null } ?: return false
            val amount = creep.body.count { it.type == unBoostedPart.type }
            val boost = when (unBoostedPart.type) {
                CARRY -> RESOURCE_CATALYZED_KEANIUM_ACID
                WORK -> RESOURCE_CATALYZED_LEMERGIUM_ACID
                MOVE -> RESOURCE_CATALYZED_ZYNTHIUM_ALKALIDE
                else -> throw IllegalArgumentException("Unexpected body part: ${unBoostedPart.type}")
            }
            val boostCreepTask = jsObject<BoostCreepTask> {
                this.creepId = creep.id
                this.boost = boost
                this.amount = amount
            }

            val taskId = createTask("${task.room}|${TaskType.BOOST_CREEP.code}", jsObject {
                this.type = TaskType.BOOST_CREEP.code
                this.room = task.room
                this.boostCreepTask = boostCreepTask
                this.creepId = creep.id
            })
            val linkedTaskIds = task.linkedTaskIds.toMutableList()
            linkedTaskIds.add(taskId)
            task.linkedTaskIds = linkedTaskIds.toTypedArray()
        }

        return false
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

                    task.targetId = target.id
                    task.targetRequiredHits = target.hits + 180000
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