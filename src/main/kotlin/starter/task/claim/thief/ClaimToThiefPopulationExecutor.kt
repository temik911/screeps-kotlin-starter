package starter.task.claim.thief

import screeps.api.FIND_CONSTRUCTION_SITES
import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.Memory
import screeps.api.Room
import screeps.api.STRUCTURE_CONTROLLER
import screeps.api.STRUCTURE_STORAGE
import screeps.api.STRUCTURE_TERMINAL
import screeps.api.StoreOwner
import screeps.api.entries
import screeps.api.get
import screeps.api.options
import screeps.api.values
import screeps.utils.unsafe.jsObject
import starter.createTask
import starter.memory.TaskMemory
import starter.memory.ThiefTask
import starter.memory.checkLinkedTasks
import starter.memory.isDone
import starter.memory.isPrepareDone
import starter.memory.linkedTaskIds
import starter.memory.room
import starter.memory.storageId
import starter.memory.tasks
import starter.memory.terminalId
import starter.memory.thiefTask
import starter.memory.toRoom
import starter.memory.type
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 12.04.2020
 */
class ClaimToThiefPopulationExecutor(val task: TaskMemory) {
    fun execute() {
        val toRoom = Game.rooms.get(task.toRoom)
        if (!task.isPrepareDone) {
            if (toRoom == null || !toRoom.controller!!.my) {
                claimRoom()
                return
            }

            if (!clearRoom(toRoom)) {
                return
            }

            if (task.thiefTask == null) {
                val thiefTask = getThiefTask(toRoom)
                task.thiefTask = thiefTask
                return
            }

            toRoom.controller!!.unclaim()
            task.isPrepareDone = true
            return
        }

        val thiefTask = task.thiefTask!!
        if (toRoom != null) {
            if (isEmpty(thiefTask.storageId) && isEmpty(thiefTask.terminalId)) {
                createTask("${task.room}|${task.toRoom}|${TaskType.SQUAD_ATTACK.code}", jsObject {
                    this.type = TaskType.SQUAD_ATTACK.code
                    this.room = task.room
                    this.toRoom = task.toRoom
                })

                task.isDone = true
                return
            }
        }

        checkLinkedTasks(task)
        if (task.linkedTaskIds.size < 3) {
            val taskId = createTask("${task.room}|${task.toRoom}|${TaskType.THIEF.code}", jsObject {
                type = TaskType.THIEF.code
                this.room = task.room
                this.toRoom = task.toRoom
                this.thiefTask = task.thiefTask
            })
            val linkedTaskIds = task.linkedTaskIds.toMutableList()
            linkedTaskIds.add(taskId)
            task.linkedTaskIds = linkedTaskIds.toTypedArray()
        }
    }

    private fun isEmpty(storeOwnerId: String?): Boolean {
        val storeOwner = Game.getObjectById<StoreOwner>(storeOwnerId) ?: return true
        return storeOwner.store.entries.isNullOrEmpty()
    }

    private fun getThiefTask(toRoom: Room): ThiefTask {
        val structures = toRoom.find(FIND_STRUCTURES)
        return jsObject {
            this.storageId = structures.firstOrNull { it.structureType == STRUCTURE_STORAGE }?.id
            this.terminalId = structures.firstOrNull { it.structureType == STRUCTURE_TERMINAL }?.id
        }
    }

    private fun clearRoom(toRoom: Room): Boolean {
        val structures = toRoom.find(FIND_STRUCTURES, options {
            filter = {
                it.structureType != STRUCTURE_STORAGE && it.structureType != STRUCTURE_TERMINAL && it.structureType != STRUCTURE_CONTROLLER
            }
        })
        val constructionSites = toRoom.find(FIND_CONSTRUCTION_SITES)
        if (structures.isNullOrEmpty() && constructionSites.isNullOrEmpty()) {
            return true
        }
        structures.forEach { it.destroy() }
        constructionSites.forEach { it.remove() }

        return false
    }

    private fun claimRoom() {
        val count = Memory.tasks.values.filter { taskMemory ->
            taskMemory.type == TaskType.CLAIM_TO_THIEF.code && taskMemory.room == task.room && taskMemory.toRoom == task.toRoom
        }.count()

        if (count == 0) {
            createTask("${task.room}|${task.toRoom}|${TaskType.CLAIM_TO_THIEF.code}", jsObject {
                type = TaskType.CLAIM_TO_THIEF.code
                this.room = task.room
                this.toRoom = task.toRoom
            })
        }
    }
}