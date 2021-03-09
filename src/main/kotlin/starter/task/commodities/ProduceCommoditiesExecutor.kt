package starter.task.commodities

import screeps.api.COMMODITIES
import screeps.api.FIND_MY_STRUCTURES
import screeps.api.Game
import screeps.api.Memory
import screeps.api.PWR_OPERATE_FACTORY
import screeps.api.RESOURCE_ENERGY
import screeps.api.RESOURCE_METAL
import screeps.api.ResourceConstant
import screeps.api.Room
import screeps.api.STRUCTURE_FACTORY
import screeps.api.component1
import screeps.api.component2
import screeps.api.entries
import screeps.api.get
import screeps.api.options
import screeps.api.structures.StructureFactory
import screeps.utils.unsafe.jsObject
import starter.ALL_COMPOUNDS_WITH_MINERALS
import starter.extension.myRooms
import starter.extension.toLink
import starter.getUsed
import starter.isLocked
import starter.lock
import starter.memory.FactoryReactionTask
import starter.memory.TaskMemory
import starter.memory.TransferTask
import starter.memory.amount
import starter.memory.checkLinkedTasks
import starter.memory.createLinkedTask
import starter.memory.factoryId
import starter.memory.factoryReactionTask
import starter.memory.fromId
import starter.memory.initiator
import starter.memory.isDone
import starter.memory.isPrepareDone
import starter.memory.linkedTaskIds
import starter.memory.requestedAmount
import starter.memory.resource
import starter.memory.room
import starter.memory.status
import starter.memory.tasks
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
 * @since 03.05.2020
 */
class ProduceCommoditiesExecutor(val task: TaskMemory) {
    fun execute() {
        val factoryReactionTask = task.factoryReactionTask ?: return
        if (!task.isPrepareDone) {
            val recipe = COMMODITIES.get(factoryReactionTask.resource!!) ?: return
            val level = recipe.level ?: 0
            if (level > 0) {
                val maxTimes = 950 / recipe.cooldown
                factoryReactionTask.amount = maxTimes * recipe.amount
            } else {
                factoryReactionTask.amount = factoryReactionTask.requestedAmount
            }
            val factory = Game.myRooms().mapNotNull {
                it.find(FIND_MY_STRUCTURES, options {
                    filter = { it.structureType == STRUCTURE_FACTORY && it.unsafeCast<StructureFactory>().level?:0 == level && !isLocked(it.id) }
                }).firstOrNull()?.unsafeCast<StructureFactory>()
            }.firstOrNull()
            if (factory != null) {
                task.room = factory.room.name
                task.factoryId = factory.id
                lock(factory.id, jsObject {
                    initiator = "produceCommodities|${factoryReactionTask.resource}"
                })
                task.isPrepareDone = true
            }
            return
        }

        console.log("Produce commodities - ${factoryReactionTask.resource} - ${factoryReactionTask.requestedAmount} - ${factoryReactionTask.amount} - ${toLink(task.room)} - ${task.status}")

        if (task.status == "undefined") {
            task.status = "filling"
        }

        val resource = factoryReactionTask.resource!!
        val amount = factoryReactionTask.amount!!
        val recipe = COMMODITIES.get(resource) ?: return
        val level = recipe.level ?: 0
        val factory = Game.getObjectById<StructureFactory>(task.factoryId)
        if (factory == null) {
            task.isDone = true
            unlock(task.factoryId!!)
            return
        }
        val storage = factory.room.storage!!
        val terminal = factory.room.terminal!!

        when (task.status) {
            "filling" -> {
                task.checkLinkedTasks()
                if (isTransferTaskExists(task)) {
                    return
                }

                val moded = amount / recipe.amount * recipe.amount
                val required = if (moded < amount) amount + recipe.amount else moded
                val times = required / recipe.amount
                val allNeeded = recipe.components.entries.filter {
                    val requiredAmount = it.component2() * times - factory.store.getUsed(it.component1())
                    requiredAmount > 0
                }
                if (allNeeded.isNullOrEmpty()) {
                    task.status = "process"
                    return
                }

                val needed = allNeeded.firstOrNull {
                    isNotRequested(it.component1())
                }?: return

                val amountToTransfer = needed.component2() * times - factory.store.getUsed(needed.component1())
                val transferTask = getTransferTask(storage, factory, needed.component1(), amountToTransfer)?: getTransferTask(terminal, factory, needed.component1(), amountToTransfer)

                if (transferTask != null) {
                    task.createLinkedTask(TaskType.LAB_TRANSFER_TASK, jsObject {
                        this.type = TaskType.LAB_TRANSFER_TASK.code
                        this.room = task.room
                        this.transferTask = transferTask
                    })
                    return
                }

                if (!getResource(factory.room, needed.component1(), amountToTransfer)) {
                    if (ALL_COMPOUNDS_WITH_MINERALS.contains(needed.component1())
                            || needed.component1() == RESOURCE_ENERGY
                            || needed.component1() == RESOURCE_METAL) {
                        return
                    }
                    task.createLinkedTask(TaskType.PRODUCE_COMMODITIES, jsObject {
                        this.factoryReactionTask = jsObject<FactoryReactionTask> {
                            this.resource = needed.component1()
                            this.requestedAmount = amountToTransfer
                        }
                    })
                }
            }
            "process" -> {
                if (level != 0) {
                    task.checkLinkedTasks()
                    val factoryEffectLevel = factory.effects?.firstOrNull { it.effect == PWR_OPERATE_FACTORY }?.level
                    if (factoryEffectLevel == null) {
                        if (task.linkedTaskIds.isNullOrEmpty()) {
                            task.createLinkedTask(TaskType.POWER_OPERATE_FACTORY, jsObject {
                                this.room = task.room
                                this.factoryId = task.factoryId
                            })
                        }
                        return
                    }
                }

                val notEnough = recipe.components.entries.firstOrNull {
                    factory.store.getUsed(it.component1()) < recipe.components[it.component1()]!!
                }
                if (notEnough != null) {
                    task.status = "withdraw"
                    return
                }

                if (factory.cooldown != 0) {
                    return
                }
                factory.produce(resource)
            }
            "withdraw" -> {
                task.checkLinkedTasks()
                if (task.linkedTaskIds.isNullOrEmpty()) {
                    val transferTask = getTransferTask(factory, terminal, factoryReactionTask.resource!!, factory.store.getUsed(factoryReactionTask.resource!!))
                    if (transferTask == null) {
                        task.isDone = true
                        unlock(task.factoryId!!)
                        return
                    }

                    task.createLinkedTask(TaskType.LAB_TRANSFER_TASK, jsObject {
                        this.type = TaskType.LAB_TRANSFER_TASK.code
                        this.room = task.room
                        this.transferTask = transferTask
                    })
                    return
                }
            }
        }
    }

    private fun isTransferTaskExists(task: TaskMemory): Boolean {
        return task.linkedTaskIds.mapNotNull { Memory.tasks.get(it) }
                .firstOrNull { it.type == TaskType.LAB_TRANSFER_TASK.code } != null
    }

    private fun getResource(room: Room, resource: ResourceConstant, amount: Int): Boolean {
        val roomWithResource = Game.myRooms().firstOrNull {
            isEnoughToSend(it, resource)
        }?: return false

        val terminal = roomWithResource.terminal!!
        if (terminal.cooldown != 0) {
            return true
        }

        terminal.send(resource, min(terminal.store.getUsed(resource), amount), room.name)
        return true
    }

    private fun isEnoughToSend(room: Room, resource: ResourceConstant): Boolean {
        val terminal = room.terminal ?: return false
        return terminal.store.getUsed(resource) > 0
    }

    private fun isNotRequested(resource: ResourceConstant): Boolean {
        return task.linkedTaskIds.mapNotNull { Memory.tasks.get(it) }
                .filter { it.type == TaskType.PRODUCE_COMMODITIES.code }
                .firstOrNull { it.factoryReactionTask!!.resource == resource } == null
    }

    private fun <Z> getTransferTask(from: Z, to: Z, resource: ResourceConstant, maxAmount: Int): TransferTask? where Z: screeps.api.StoreOwner {
        val used = from.store.getUsed(resource)
        if (used == 0) {
            return null
        }

        return jsObject<TransferTask> {
            this.fromId = from.id
            this.toId = to.id
            this.resource = resource
            this.value = arrayOf(maxAmount, used, 500).min()
        }
    }
}