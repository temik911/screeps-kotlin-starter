package starter.task.unclaim

import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.Memory
import screeps.api.RESOURCE_ENERGY
import screeps.api.STRUCTURE_CONTROLLER
import screeps.api.component2
import screeps.api.entries
import screeps.api.get
import screeps.api.keys
import screeps.api.options
import screeps.api.values
import screeps.utils.unsafe.jsObject
import starter.extension.factory
import starter.getUsed
import starter.isLocked
import starter.lock
import starter.memory.TaskMemory
import starter.memory.checkLinkedTasks
import starter.memory.createLinkedTask
import starter.memory.externalOrderId
import starter.memory.initiator
import starter.memory.isDone
import starter.memory.isPrepareDone
import starter.memory.linkedTaskIds
import starter.memory.marketPlace
import starter.memory.reservedRooms
import starter.memory.room
import starter.memory.tasks
import starter.memory.terminalId
import starter.memory.toRoom
import starter.memory.type
import starter.task.TaskType
import starter.unlockByInitiator
import kotlin.math.min

/**
 *
 *
 * @author zakharchuk
 * @since 08.05.2020
 */
class UnclaimMyRoomPopulationExecutor(val task: TaskMemory) {

    fun execute() {

        val room = Game.rooms.get(task.room)!!
        if (!task.isPrepareDone) {
            room.memory.reservedRooms = emptyArray()
            Memory.tasks.entries.forEach { task ->
                val taskMemory = task.component2()
                if (taskMemory.room == room.name) {
                    val taskType = TaskType.of(taskMemory.type) ?: return@forEach
                    if (taskType.removeOnUnclaim) {
                        taskMemory.isDone = true
                    }
                }
            }
            if (room.terminal != null) {
                val terminalId = room.terminal!!.id
                Memory.marketPlace.values.forEach { order ->
                    if (order.terminalId == terminalId) {
                        if (order.externalOrderId != null) {
                            Game.market.cancelOrder(order.externalOrderId!!)
                        }
                        order.isDone = true
                    }
                }
            }
            val factory = room.factory()
            if (factory != null) {
                if (isLocked(factory.id)) {
                    return
                }
                lock(factory.id, jsObject {
                    initiator = room.name
                })
            }
            task.isPrepareDone = true
        }

        if (room.storage != null && room.terminal != null) {
            if (room.storage!!.store.getUsedCapacity() != 0) {
                checkLinkedTasks(task);
                if (task.linkedTaskIds.isNullOrEmpty()) {
                    task.createLinkedTask(TaskType.UNCLAIM_MY_ROOM, jsObject {
                        this.room = task.room
                    })
                }
            }
        }

        if (room.terminal != null) {
            val toRoom = Game.rooms.get(task.toRoom)!!
            val toRoomTerminal = toRoom.terminal ?: return
            if (toRoomTerminal.store.getFreeCapacity() > 50000) {
                val terminal = room.terminal!!
                if (terminal.cooldown == 0) {
                    val resource = terminal.store.keys.firstOrNull { it != RESOURCE_ENERGY }
                    if (resource != null) {
                        val resourceAmount = min(terminal.store.getUsed(resource), 10000)
                        terminal.send(resource, resourceAmount, task.toRoom)
                    } else {
                        if (room.storage!!.store.keys.firstOrNull { it != RESOURCE_ENERGY } == null) {
                            val energy = min(terminal.store.getUsed(RESOURCE_ENERGY), 25000)
                            val cost = Game.market.calcTransactionCost(energy, room.name, task.toRoom)
                            if (energy > cost) {
                                terminal.send(RESOURCE_ENERGY, energy - cost, task.toRoom)
                            }
                        }
                    }
                }
            }
        }

        val isStorageEmpty = (room.storage?.store?.getUsedCapacity() ?: 0) == 0
        val isTerminalEmpty = (room.terminal?.store?.getUsedCapacity() ?: 0) < 100

        if (isStorageEmpty && isTerminalEmpty) {
            Memory.tasks.entries.forEach { task ->
                val taskMemory = task.component2()
                if (taskMemory.room == room.name) {
                    taskMemory.isDone = true
                }
            }

            room.find(FIND_STRUCTURES, options {
                filter = { it.structureType != STRUCTURE_CONTROLLER }
            }).forEach { it.destroy() }

            room.controller!!.unclaim()

            unlockByInitiator(room.name)

            task.isDone = true
        }
    }
}