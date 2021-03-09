package starter.task.power

import screeps.api.Game
import screeps.api.RESOURCE_ENERGY
import screeps.api.RESOURCE_POWER
import screeps.api.get
import screeps.api.structures.StructurePowerSpawn
import screeps.api.structures.StructureStorage
import screeps.utils.unsafe.jsObject
import starter.extension.powerSpawn
import starter.getUsed
import starter.isLocked
import starter.lock
import starter.memory.TaskMemory
import starter.memory.TransferTask
import starter.memory.checkLinkedTasks
import starter.memory.createLinkedTask
import starter.memory.fromId
import starter.memory.initiator
import starter.memory.isDone
import starter.memory.isPrepareDone
import starter.memory.linkedTaskIds
import starter.memory.resource
import starter.memory.room
import starter.memory.toId
import starter.memory.transferTask
import starter.memory.value
import starter.task.TaskType
import starter.unlock
import kotlin.math.min

/**
 *
 *
 * @author zakharchuk
 * @since 12.06.2020
 */
class PowerSpawnFillingExecutor(val task: TaskMemory) {
    fun execute() {
        val room = Game.rooms[task.room]!!
        if (!task.isPrepareDone) {
            val id = room.powerSpawn()?.id ?: return
            if (isLocked(id)) {
                console.error("Power spawn is locked: id=$id")
                return
            }
            lock(id, jsObject { initiator = "powerSpawnFillingExecutor" })
            task.isPrepareDone = true
        }

        val storage = room.storage!!
        val powerSpawn = room.powerSpawn()!!

        task.checkLinkedTasks()
        if (task.linkedTaskIds.isNullOrEmpty()) {
            val pickTask = pickTask(storage, powerSpawn)
            if (pickTask == null) {
                unlock(powerSpawn.id)
                task.isDone = true
                return
            }
            task.createLinkedTask(TaskType.LAB_TRANSFER_TASK, jsObject {
                this.room = task.room
                this.transferTask = pickTask
            })
        }
    }

    private fun pickTask(storage: StructureStorage, powerSpawn: StructurePowerSpawn): TransferTask? {
        if (powerSpawn.store.getUsed(RESOURCE_POWER) != 100) {
            val requiredAmount = 100 - powerSpawn.store.getUsed(RESOURCE_POWER)
            val transferAmount = min(requiredAmount, 500)
            val fromId = storage.id
            val toId = powerSpawn.id
            return jsObject<TransferTask> {
                this.fromId = fromId
                this.toId = toId
                this.resource = RESOURCE_POWER
                this.value = transferAmount
            }
        }

        if (powerSpawn.store.getUsed(RESOURCE_ENERGY) != 5000) {
            val requiredAmount = 5000 - powerSpawn.store.getUsed(RESOURCE_ENERGY)
            val transferAmount = min(requiredAmount, 500)
            val fromId = storage.id
            val toId = powerSpawn.id
            return jsObject<TransferTask> {
                this.fromId = fromId
                this.toId = toId
                this.resource = RESOURCE_ENERGY
                this.value = transferAmount
            }
        }

        return null
    }
}