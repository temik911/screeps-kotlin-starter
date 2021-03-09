package starter.task.repair

import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.Memory
import screeps.api.Room
import screeps.api.STRUCTURE_CONTAINER
import screeps.api.STRUCTURE_RAMPART
import screeps.api.STRUCTURE_ROAD
import screeps.api.get
import screeps.api.options
import screeps.utils.unsafe.jsObject
import starter.memory.TaskMemory
import starter.memory.checkLinkedTasks
import starter.memory.createLinkedTask
import starter.memory.getRoomMemory
import starter.memory.linkedTaskIds
import starter.memory.room
import starter.memory.sleepUntil
import starter.memory.tasks
import starter.memory.toRoom
import starter.memory.type
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 05.03.2020
 */
class RepairPopulationExecutor(val task: TaskMemory) {
    fun execute() {
        if (getRoomMemory(task.toRoom).sleepUntil > Game.time) {
            return
        }

        if (task.sleepUntil > Game.time) {
            return
        }

        val toRoom = Game.rooms.get(task.toRoom)
        if (toRoom != null) {
            task.checkLinkedTasks()
            val tasks = task.linkedTaskIds.count {
                val taskMemory = Memory.tasks[it]!!
                taskMemory.type == TaskType.REPAIR.code && taskMemory.room == task.room && taskMemory.toRoom == toRoom.name
            }
            if (tasks == 0) {
                val target = toRoom.find(FIND_STRUCTURES)
                        .filter { (it.structureType == STRUCTURE_ROAD || it.structureType == STRUCTURE_CONTAINER) }
                        .filter { it.hits < it.hitsMax * 0.75 }
                        .firstOrNull()
                if (target != null) {
                    task.createLinkedTask(TaskType.REPAIR, jsObject {
                        this.room = task.room
                        this.toRoom = toRoom.name
                    })
                } else {
                    task.sleepUntil = Game.time + 50
                }
            }
        }

        if (task.room == task.toRoom) {
            val rampartNeededHits = 30_000_000
            val room = Game.rooms.get(task.room)!!
            if (BoostedRepairRampartExecutor.isRoomApplicable(room)) {
                if (!createRepairTaskIfNeeded(room, rampartNeededHits - 150_000, TaskType.BOOSTED_REPAIR_RAMPART)) {
                    createRepairTaskIfNeeded(room, rampartNeededHits, TaskType.REPAIR_RAMPART)
                }
            } else {
                createRepairTaskIfNeeded(room, rampartNeededHits, TaskType.REPAIR_RAMPART)
            }
        }
    }

    private fun createRepairTaskIfNeeded(room: Room, rampartHitsMax: Int, taskType: TaskType): Boolean {
        task.checkLinkedTasks()
        val tasks = task.linkedTaskIds.map { Memory.tasks[it]!! }
                .filter { taskMemory ->
                    (taskMemory.type == TaskType.REPAIR_RAMPART.code || taskMemory.type == TaskType.BOOSTED_REPAIR_RAMPART.code)
                            && taskMemory.room == room.name
                }

        if (!tasks.isNullOrEmpty()) {
            return true
        }

        val ramparts = room.find(FIND_STRUCTURES, options {
            filter = {
                it.structureType == STRUCTURE_RAMPART && it.hits < rampartHitsMax
            }
        })

        if (ramparts.isNullOrEmpty()) {
            task.sleepUntil = Game.time + 50
            return false
        }

        task.createLinkedTask(taskType, jsObject {
            this.room = room.name
        })
        return true
    }
}