package starter.task.lab

import screeps.api.Creep
import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.OK
import screeps.api.RESOURCE_ENERGY
import screeps.api.ResourceConstant
import screeps.api.STRUCTURE_LAB
import screeps.api.get
import screeps.api.options
import screeps.api.structures.StructureLab
import screeps.utils.unsafe.jsObject
import starter.extension.moveToWithSwap
import starter.isLocked
import starter.lock
import starter.memory.TaskMemory
import starter.memory.TransferTask
import starter.memory.amount
import starter.memory.boost
import starter.memory.boostCreepTask
import starter.memory.checkLinkedTasks
import starter.memory.createLinkedTask
import starter.memory.creepId
import starter.memory.fromId
import starter.memory.initiator
import starter.memory.isDone
import starter.memory.labId
import starter.memory.linkedTaskIds
import starter.memory.resource
import starter.memory.room
import starter.memory.toId
import starter.memory.transferTask
import starter.memory.type
import starter.memory.value
import starter.task.TaskType
import starter.unlock
import kotlin.math.min

/**
 *
 *
 * @author zakharchuk
 * @since 31.03.2020
 */
class BoostCreepExecutor(val task: TaskMemory) {
    fun execute() {
        val boostCreepTask = task.boostCreepTask
        if (boostCreepTask == null) {
            task.isDone = true
            return
        }

        if (boostCreepTask.labId == null) {
            val room = Game.rooms.get(task.room)!!
            val lab = room.find(FIND_STRUCTURES, options {
                filter = {
                    it.structureType == STRUCTURE_LAB && !isLocked(it.id)
                }
            }).firstOrNull() ?: return

            lock(lab.id, jsObject {
                this.initiator = task.type
            })
            boostCreepTask.labId = lab.id
        }

        task.checkLinkedTasks()
        if (task.linkedTaskIds.isNullOrEmpty()) {
            val lab = Game.getObjectById<StructureLab>(boostCreepTask.labId) ?: return
            if (lab.mineralType != null && lab.mineralType != boostCreepTask.boost) {
                val amount = (lab.store.getUsedCapacity(lab.mineralType!!) ?: 0)
                createTransferTask(lab.id, lab.room.storage!!.id, amount, lab.mineralType!!)
                return
            }

            if (lab.store.getUsedCapacity(boostCreepTask.boost!!) ?: 0 < (30 * boostCreepTask.amount!!)) {
                val amount = (30 * boostCreepTask.amount!!) - (lab.store.getUsedCapacity(boostCreepTask.boost!!) ?: 0)
                createTransferTask(lab.room.storage!!.id, lab.id, amount, boostCreepTask.boost!!)
                return
            }

            if (lab.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0 < (20 * boostCreepTask.amount!!)) {
                val amount = (20 * boostCreepTask.amount!!) - (lab.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0)
                createTransferTask(lab.room.storage!!.id, lab.id, amount, RESOURCE_ENERGY)
                return
            }

            val boostedCreep = Game.getObjectById<Creep>(boostCreepTask.creepId!!)
            if (boostedCreep == null) {
                task.isDone = true
                unlock(lab.id)
                return
            }

            if (boostedCreep.pos.isNearTo(lab)) {
                val result = lab.boostCreep(boostedCreep)
                if (result == OK) {
                    task.isDone = true
                    unlock(lab.id)
                }
            } else {
                boostedCreep.moveToWithSwap(lab)
            }
        }
    }

    private fun createTransferTask(fromId: String, toId: String, amount: Int, resource: ResourceConstant) {
        val transferAmount = min(amount, 500)
        val transferTask = jsObject<TransferTask> {
            this.fromId = fromId
            this.toId = toId
            this.resource = resource
            this.value = transferAmount
        }
        task.createLinkedTask(TaskType.LAB_TRANSFER_TASK, jsObject {
            this.room = task.room
            this.transferTask = transferTask
        })
    }
}