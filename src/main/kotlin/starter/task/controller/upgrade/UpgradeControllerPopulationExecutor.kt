package starter.task.controller.upgrade

import screeps.api.FIND_MY_STRUCTURES
import screeps.api.Game
import screeps.api.Memory
import screeps.api.RESOURCE_CATALYZED_GHODIUM_ACID
import screeps.api.RESOURCE_ENERGY
import screeps.api.Room
import screeps.api.WORK
import screeps.api.get
import screeps.api.options
import screeps.api.structures.StructureContainer
import screeps.api.structures.StructureController
import screeps.api.structures.StructureLink
import screeps.api.structures.StructureStorage
import screeps.api.values
import screeps.utils.unsafe.jsObject
import starter.calculateLinkUpgradeBody
import starter.createTask
import starter.memory.TaskMemory
import starter.memory.checkLinkedTasks
import starter.memory.containerId
import starter.memory.controllerLinkId
import starter.memory.linkId
import starter.memory.linkedTaskIds
import starter.memory.room
import starter.memory.storageLinkId
import starter.memory.tasks
import starter.memory.type
import starter.task.TaskType
import kotlin.math.max
import kotlin.math.roundToInt

/**
 *
 *
 * @author zakharchuk
 * @since 13.02.2020
 */
class UpgradeControllerPopulationExecutor(private val task: TaskMemory) {

    fun execute() {
        val room = Game.rooms.get(task.room)!!
        val controller = room.controller!!
        if (room.storage != null) {
            val storage = room.storage!!

            val controllerLink = getControllerLink(task, controller)
            val storageLink = getStorageLink(task, storage)

            val numberOfTasks = getNumberOfTasks(controller, controllerLink, storageLink, storage, room)

            if (numberOfTasks == 0) {
                return
            }

            checkLinkedTasks(task)
            val tasks = task.linkedTaskIds.count {
                val taskMemory = Memory.tasks[it]!!
                taskMemory.room == room.name
                        && (taskMemory.type == TaskType.UPGRADE_CONTROLLER.code || taskMemory.type == TaskType.LINK_UPGRADE_CONTROLLER.code || taskMemory.type == TaskType.BOOSTED_LINK_UPGRADE_CONTROLLER.code)
            }

            (tasks until numberOfTasks).forEach { _ ->
                if (controllerLink == null) {
                    val taskId = createTask("${room.name}|${TaskType.UPGRADE_CONTROLLER.code}", jsObject {
                        type = TaskType.UPGRADE_CONTROLLER.code
                        this.room = room.name
                    })
                    val linkedTaskIds = task.linkedTaskIds.toMutableList()
                    linkedTaskIds.add(taskId)
                    task.linkedTaskIds = linkedTaskIds.toTypedArray()
                } else {
                    val toCreate = numberOfTasks - tasks
                    val acidNeeded = room.calculateLinkUpgradeBody().sumBy { if (it == WORK) 30 else 0 }
                    val boostAmount = if (controller.level == 8) 50000 else (toCreate * acidNeeded + 1000)
                    if (storage.store.getUsedCapacity(RESOURCE_CATALYZED_GHODIUM_ACID) ?: 0 < boostAmount) {
                        createTask(task, TaskType.LINK_UPGRADE_CONTROLLER, controllerLink, room)
                    } else {
                        createTask(task, TaskType.BOOSTED_LINK_UPGRADE_CONTROLLER, controllerLink, room)
                    }
                }
            }
        } else {
            val container = getControllerContainer(task, controller)

            if (container == null) {
                createNoStorageUpgradeTasks(TaskType.PRIMITIVE_UPDATE_CONTROLLER, room)
            } else {
                createNoStorageUpgradeTasks(TaskType.CONTAINER_UPGRADE_CONTROLLER, room)
            }
        }
    }

    fun createTask(task: TaskMemory, taskType: TaskType, controllerLink: StructureLink, room: Room) {
        val taskId = createTask("${room.name}|${taskType.code}", jsObject {
            type = taskType.code
            this.room = room.name
            linkId = controllerLink.id
        })
        val linkedTaskIds = task.linkedTaskIds.toMutableList()
        linkedTaskIds.add(taskId)
        task.linkedTaskIds = linkedTaskIds.toTypedArray()
    }

    fun createNoStorageUpgradeTasks(type: TaskType, room: Room) {
        val tasks = Memory.tasks.values.filter { taskMemory ->
            taskMemory.type == type.code && taskMemory.room == room.name
        }

        (tasks.size..1).forEach { _ ->
            val taskId = createTask("${room.name}|${type.code}", jsObject {
                this.type = type.code
                this.room = room.name
            })
            val linkedTaskIds = task.linkedTaskIds.toMutableList()
            linkedTaskIds.add(taskId)
            task.linkedTaskIds = linkedTaskIds.toTypedArray()
        }
    }

    fun getControllerLink(task: TaskMemory, controller: StructureController): StructureLink? {
        if (task.controllerLinkId == "undefined") {
            val controllerLink = controller.pos.findInRange(FIND_MY_STRUCTURES, 2, options {
                filter = { it.structureType == screeps.api.STRUCTURE_LINK }
            }).firstOrNull() ?: return null

            task.controllerLinkId = controllerLink.id
        }

        val controllerLink = Game.getObjectById<StructureLink>(task.controllerLinkId)
        if (controllerLink == null) {
            task.controllerLinkId = "undefined"
        }
        return controllerLink
    }

    fun getControllerContainer(task: TaskMemory, controller: StructureController): StructureContainer? {
        if (task.containerId == "undefined") {
            val container = controller.pos.findInRange(FIND_MY_STRUCTURES, 1, options {
                filter = { it.structureType == screeps.api.STRUCTURE_CONTAINER }
            }).firstOrNull() ?: return null

            task.containerId = container.id
        }

        val controllerContainer = Game.getObjectById<StructureContainer>(task.containerId)
        if (controllerContainer == null) {
            task.containerId = "undefined"
        }
        return controllerContainer
    }

    fun getStorageLink(task: TaskMemory, storage: StructureStorage): StructureLink? {
        if (task.storageLinkId == "undefined") {
            val storageLink = storage.pos.findInRange(FIND_MY_STRUCTURES, 2, options {
                filter = { it.structureType == screeps.api.STRUCTURE_LINK }
            }).firstOrNull() ?: return null

            task.storageLinkId = storageLink.id
        }

        val storageLink = Game.getObjectById<StructureLink>(task.storageLinkId)
        if (storageLink == null) {
            task.storageLinkId = "undefined"
        }
        return storageLink
    }

    fun getNumberOfTasks(controller: StructureController, controllerLink: StructureLink?, storageLink: StructureLink?, storage: StructureStorage, room: Room): Int {
        val numberOfTasks: Int
        if (controller.level == 8) {
            numberOfTasks = 1
        } else if (controllerLink != null && storageLink != null) {
            val usedCapacity = storage.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0
            if (usedCapacity < 5000) {
                numberOfTasks = 0
            } else if (usedCapacity < 10000) {
                numberOfTasks = 1
            } else {
                val perTick = 800.0 / controllerLink.pos.getRangeTo(storageLink)
                val work = room.calculateLinkUpgradeBody().sumBy { if (it == WORK) 1 else 0 }
                val maxCreeps = (perTick / work).roundToInt()
                val requiredCreeps = max(((usedCapacity - 10000).toDouble() / (1500 * work)).roundToInt(), 1)
                numberOfTasks = arrayListOf(requiredCreeps, maxCreeps, 8).min()!!
            }
        } else {
            val usedCapacity = storage.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0
            if (usedCapacity < 5000) {
                numberOfTasks = 0
            } else if (usedCapacity < 10000) {
                numberOfTasks = 1
            } else if (usedCapacity < 25000) {
                numberOfTasks = 2
            } else if (usedCapacity < 125000) {
                numberOfTasks = 4
            } else if (usedCapacity < 500000) {
                numberOfTasks = 6
            } else {
                numberOfTasks = 8
            }
        }

        return numberOfTasks
    }
}