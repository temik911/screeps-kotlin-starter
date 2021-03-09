package starter.task.defend

import screeps.api.ATTACK
import screeps.api.FIND_HOSTILE_CREEPS
import screeps.api.FIND_MY_CREEPS
import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.Memory
import screeps.api.Room
import screeps.api.STRUCTURE_INVADER_CORE
import screeps.api.get
import screeps.api.options
import screeps.api.values
import screeps.utils.unsafe.jsObject
import starter.createTask
import starter.memory.TaskMemory
import starter.memory.getRoomMemory
import starter.memory.isExternalRoom
import starter.memory.isSkRoom
import starter.memory.newTaskCreated
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
 * @since 09.03.2020
 */
class DefendPopulationExecutor(val task: TaskMemory) {
    fun execute() {
        if (task.sleepUntil > Game.time) {
            return
        }

        val room = Game.rooms.get(task.room) ?: return
        if (task.isExternalRoom) {
            val toRoom = Game.rooms.get(task.toRoom) ?: return
            check(room, toRoom)
        } else if (task.isSkRoom) {
            createSkTask(room, task.toRoom)
            createSkHealTask(room, task.toRoom)
            val toRoom = Game.rooms.get(task.toRoom) ?: return
            check(room, toRoom)
        }
    }

    private fun createSkHealTask(room: Room, toRoomName: String) {
        val toRoom = Game.rooms.get(toRoomName) ?: return
        val find = toRoom.find(FIND_MY_CREEPS, options {
            filter = {
                it.hits < it.hitsMax && it.getActiveBodyparts(ATTACK) == 0
            }
        })
        if (!find.isNullOrEmpty()) {
            val tasks = Memory.tasks.values.filter { taskMemory ->
                taskMemory.type == TaskType.SK_HEAL.code && taskMemory.room == room.name
                        && taskMemory.toRoom == toRoom.name
            }
            if (tasks.isNullOrEmpty()) {
                createTask("${room.name}|${toRoom.name}|${TaskType.SK_HEAL.code}", jsObject {
                    this.type = TaskType.SK_HEAL.code
                    this.room = room.name
                    this.toRoom = toRoom.name
                })
            }
        }
    }

    private fun createSkTask(room: Room, toRoom: String) {
        val tasks = Memory.tasks.values.filter { taskMemory ->
            taskMemory.type == TaskType.SK_DEFEND.code && taskMemory.room == room.name
                    && taskMemory.toRoom == toRoom
        }
        when (tasks.size) {
            0 -> {
                createTask("${room.name}|${toRoom}|${TaskType.SK_DEFEND.code}", jsObject {
                    this.type = TaskType.SK_DEFEND.code
                    this.room = room.name
                    this.toRoom = toRoom
                })
            }
            1 -> {
                val skDefendTask = tasks[0]
                if (!skDefendTask.newTaskCreated && SkDefendExecutor(skDefendTask).isNewTaskNeeded()) {
                    createTask("${room.name}|${toRoom}|${TaskType.SK_DEFEND.code}", jsObject {
                        this.type = TaskType.SK_DEFEND.code
                        this.room = room.name
                        this.toRoom = toRoom
                    })
                    skDefendTask.newTaskCreated = true
                }
            }
        }
    }

    fun check(room: Room, toRoom: Room) {
        val target = toRoom.find(FIND_HOSTILE_CREEPS, options {
            filter = {
                it.owner.username == "Invader"
            }
        }).firstOrNull()
        if (target != null) {
            val roomMemory = getRoomMemory(toRoom.name)
            roomMemory.sleepUntil = Game.time + target.ticksToLive
            createTaskIfNotExists(room, toRoom, TaskType.DEFEND)
        } else {
            val roomMemory = getRoomMemory(toRoom.name)
            roomMemory.sleepUntil = 0
        }

        val invaderCore = toRoom.find(FIND_STRUCTURES, options {
            filter = {
                it.structureType == STRUCTURE_INVADER_CORE
            }
        }).firstOrNull()
        if (invaderCore != null) {
            createTaskIfNotExists(room, toRoom, TaskType.INVADER_CODE_DESTROYER)
        }

        if (target == null && invaderCore == null) {
            task.sleepUntil = Game.time + 10
        }
    }

    private fun createTaskIfNotExists(room: Room, toRoom: Room, taskType: TaskType) {
        val tasks = Memory.tasks.values.filter { taskMemory ->
            taskMemory.type == taskType.code
                    && taskMemory.room == room.name
                    && taskMemory.toRoom == toRoom.name
        }
        if (tasks.isNullOrEmpty()) {
            createTask("${room.name}|${toRoom.name}|${taskType.code}", jsObject {
                this.type = taskType.code
                this.room = room.name
                this.toRoom = toRoom.name
            })
        }
    }
}