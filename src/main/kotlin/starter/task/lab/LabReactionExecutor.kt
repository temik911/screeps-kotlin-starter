package starter.task.lab

import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.Memory
import screeps.api.Room
import screeps.api.STRUCTURE_LAB
import screeps.api.get
import screeps.api.options
import screeps.api.structures.StructureLab
import screeps.api.values
import screeps.utils.unsafe.jsObject
import starter.UniqueIdGenerator
import starter.createTask
import starter.isLocked
import starter.lock
import starter.memory.LabReactionTask
import starter.memory.TaskMemory
import starter.memory.TransferTask
import starter.memory.amount
import starter.memory.externalTaskId
import starter.memory.fromId
import starter.memory.initiator
import starter.memory.internalTaskId
import starter.memory.isDone
import starter.memory.isPrepareDone
import starter.memory.labReactionTask
import starter.memory.providerLab1
import starter.memory.providerLab2
import starter.memory.resource
import starter.memory.resource1
import starter.memory.resource2
import starter.memory.room
import starter.memory.startTime
import starter.memory.status
import starter.memory.tasks
import starter.memory.toId
import starter.memory.transferTask
import starter.memory.type
import starter.memory.value
import starter.task.TaskType
import starter.unlock
import starter.unlockByInitiator
import kotlin.math.min

/**
 *
 *
 * @author zakharchuk
 * @since 30.03.2020
 */
class LabReactionExecutor(val task: TaskMemory) {
    fun execute() {
        val labReactionTask = task.labReactionTask
        if (labReactionTask == null) {
            task.isDone = true
            return
        }

        if (!task.isPrepareDone) {
            val room = Game.rooms.get(task.room)!!
            if (task.internalTaskId == null || Memory.tasks.values.filter { memory -> memory.externalTaskId == task.internalTaskId }.isNullOrEmpty()) {
                if (task.internalTaskId != null) {
                    unlockByInitiator(task.internalTaskId!!)
                }
                val pickTask = pickWithdrawTask(room)
                if (pickTask == null) {
                    task.isPrepareDone = true
                    return
                }
                val uniqueId = UniqueIdGenerator.generateUniqueId()
                createTask("${task.room}|${TaskType.LAB_TRANSFER_TASK.code}", jsObject {
                    this.type = TaskType.LAB_TRANSFER_TASK.code
                    this.room = task.room
                    this.externalTaskId = uniqueId
                    this.transferTask = pickTask
                })
                task.internalTaskId = uniqueId
                lock(pickTask.fromId!!, jsObject {
                    this.initiator = uniqueId
                })
            }
            return
        }

        if (labReactionTask.providerLab1 == null || labReactionTask.providerLab2 == null) {
            val room = Game.rooms.get(task.room)!!
            val labs = room.find(FIND_STRUCTURES, options {
                filter = {
                    it.structureType == STRUCTURE_LAB && !isLocked(it.id)
                }
            })

            val providerLabs = labs.filter {
                it.pos.findInRange(labs, 2).size == labs.size
            }

            if (providerLabs.size < 2) {
                return
            }

            labReactionTask.providerLab1 = providerLabs[0].id
            lock(providerLabs[0].id, jsObject {
                this.initiator = task.type
            })

            labReactionTask.providerLab2 = providerLabs[1].id
            lock(providerLabs[1].id, jsObject {
                this.initiator = task.type
            })
        }

        if (task.status == "undefined") {
            task.status = "filling"
        }

        when (task.status) {
            "filling" -> {
                if (task.startTime + 500 < Game.time) {
                    task.isDone = true
                    unlock(labReactionTask.providerLab1!!)
                    unlock(labReactionTask.providerLab2!!)
                    return
                }
                if (task.internalTaskId == null || Memory.tasks.values.filter { memory -> memory.externalTaskId == task.internalTaskId }.isNullOrEmpty()) {
                    val pickTask = pickTask(labReactionTask)
                    if (pickTask == null) {
                        task.status = "runReaction"
                        return
                    }
                    val uniqueId = UniqueIdGenerator.generateUniqueId()
                    createTask("${task.room}|${TaskType.LAB_TRANSFER_TASK.code}", jsObject {
                        this.type = TaskType.LAB_TRANSFER_TASK.code
                        this.room = task.room
                        this.externalTaskId = uniqueId
                        this.transferTask = pickTask
                    })
                    task.internalTaskId = uniqueId
                }
            }
            "runReaction" -> {
                val lab1 = Game.getObjectById<StructureLab>(labReactionTask.providerLab1!!)
                val lab2 = Game.getObjectById<StructureLab>(labReactionTask.providerLab2!!)

                if (lab1 == null || lab2 == null) {
                    task.isDone = true
                    return
                }

                if (lab1.store.getUsedCapacity(labReactionTask.resource1!!) ?: 0 == 0 && lab2.store.getUsedCapacity(labReactionTask.resource2!!) ?: 0 == 0) {
                    task.status = "withdraw"
                    return
                }

                lab1.room.find(FIND_STRUCTURES, options {
                    filter = {
                        it.structureType == STRUCTURE_LAB && !isLocked(it.id)
                    }
                }).filter { it.unsafeCast<StructureLab>().cooldown == 0 }
                        .forEach { it.unsafeCast<StructureLab>().runReaction(lab1, lab2) }
            }
            "withdraw" -> {
                val room = Game.rooms.get(task.room)!!
                if (task.internalTaskId == null || Memory.tasks.values.filter { memory -> memory.externalTaskId == task.internalTaskId }.isNullOrEmpty()) {
                    if (task.internalTaskId != null) {
                        unlockByInitiator(task.internalTaskId!!)
                    }
                    val pickTask = pickWithdrawTask(room)
                    if (pickTask == null) {
                        task.isDone = true
                        unlock(labReactionTask.providerLab1!!)
                        unlock(labReactionTask.providerLab2!!)
                        return
                    }
                    val uniqueId = UniqueIdGenerator.generateUniqueId()
                    createTask("${task.room}|${TaskType.LAB_TRANSFER_TASK.code}", jsObject {
                        this.type = TaskType.LAB_TRANSFER_TASK.code
                        this.room = task.room
                        this.externalTaskId = uniqueId
                        this.transferTask = pickTask
                    })
                    task.internalTaskId = uniqueId
                    lock(pickTask.fromId!!, jsObject {
                        this.initiator = uniqueId
                    })
                }
            }
        }
    }

    private fun pickWithdrawTask(room: Room): TransferTask? {
        val lab = room.find(FIND_STRUCTURES, options {
            filter = {
                it.structureType == STRUCTURE_LAB && !isLocked(it.id)
            }
        }).map { it.unsafeCast<StructureLab>() }
                .filter { it.store.getUsedCapacity() != 0 && it.mineralType != null }
                .firstOrNull { it.store.getUsedCapacity(it.mineralType!!) != 0 } ?: return null

        val transferAmount = min(lab.store.getUsedCapacity(lab.mineralType!!) ?: 0, 500)
        val fromId = lab.id
        val toId = room.storage!!.id
        return jsObject<TransferTask> {
            this.fromId = fromId
            this.toId = toId
            this.resource = lab.mineralType!!
            this.value = transferAmount
        }
    }

    private fun pickTask(labReactionTask: LabReactionTask): TransferTask? {
        val lab1 = Game.getObjectById<StructureLab>(labReactionTask.providerLab1!!)
        val lab2 = Game.getObjectById<StructureLab>(labReactionTask.providerLab2!!)
        val amount = labReactionTask.amount!!

        if (lab1 == null || lab2 == null) {
            task.isDone = true
            return null
        }

        if (lab1.store.getUsedCapacity(labReactionTask.resource1!!) ?: 0 != amount) {
            val requiredAmount = amount - (lab1.store.getUsedCapacity(labReactionTask.resource1!!) ?: 0)
            val transferAmount = min(requiredAmount, 500)
            val fromId = lab1.room.storage!!.id
            val toId = lab1.id
            return jsObject<TransferTask> {
                this.fromId = fromId
                this.toId = toId
                this.resource = labReactionTask.resource1!!
                this.value = transferAmount
            }
        }

        if (lab2.store.getUsedCapacity(labReactionTask.resource2!!) ?: 0 != amount) {
            val requiredAmount = amount - (lab2.store.getUsedCapacity(labReactionTask.resource2!!) ?: 0)
            val transferAmount = min(requiredAmount, 500)
            val fromId = lab2.room.storage!!.id
            val toId = lab2.id
            return jsObject<TransferTask> {
                this.fromId = fromId
                this.toId = toId
                this.resource = labReactionTask.resource2!!
                this.value = transferAmount
            }
        }

        return null
    }

}