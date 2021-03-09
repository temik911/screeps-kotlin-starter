package starter.task.harvest

import screeps.api.FIND_MINERALS
import screeps.api.FIND_MY_STRUCTURES
import screeps.api.FIND_SOURCES
import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.Identifiable
import screeps.api.Memory
import screeps.api.Room
import screeps.api.RoomObject
import screeps.api.STRUCTURE_CONTAINER
import screeps.api.STRUCTURE_LINK
import screeps.api.Source
import screeps.api.get
import screeps.api.options
import screeps.api.structures.StructureContainer
import screeps.api.structures.StructureLink
import screeps.api.structures.StructureStorage
import screeps.utils.unsafe.jsObject
import starter.createTask
import starter.memory.TaskMemory
import starter.memory.checkLinkedTasks
import starter.memory.containerId
import starter.memory.getRoomMemory
import starter.memory.getSourceMemory
import starter.memory.isExternalRoom
import starter.memory.isSkRoom
import starter.memory.linkId
import starter.memory.linkedTaskIds
import starter.memory.newTaskCreated
import starter.memory.room
import starter.memory.sleepUntil
import starter.memory.sourceId
import starter.memory.startTime
import starter.memory.storageLinkId
import starter.memory.tasks
import starter.memory.toRoom
import starter.memory.type
import starter.task.TaskType
import starter.task.harvest.sk.SkHarvestExecutor
import starter.task.harvest.sk.SkMineralHarvestExecutor

/**
 *
 *
 * @author zakharchuk
 * @since 04.03.2020
 */
class HarvestPopulationExecutor(val task: TaskMemory) {
    fun execute() {
        task.startTime = Game.time
        val room = Game.rooms.get(task.room) ?: return
        if (task.isExternalRoom) {
            if (getRoomMemory(task.toRoom).sleepUntil > Game.time) {
                return
            }
            val toRoom = Game.rooms.get(task.toRoom) ?: return
            toRoom.find(FIND_SOURCES).forEach { source ->
                createHarvestTask(room, source, TaskType.HARVEST)
            }
        } else if (task.isSkRoom) {
            if (getRoomMemory(task.toRoom).sleepUntil > Game.time) {
                return
            }
            val toRoom = Game.rooms.get(task.toRoom) ?: return
            toRoom.find(FIND_SOURCES).forEach { source ->
                createHarvestTask(room, source, TaskType.SK_HARVEST)
            }
            toRoom.find(FIND_MINERALS).forEach { mineral ->
                if (mineral.mineralAmount > 0) {
                    createHarvestTask(room, mineral, TaskType.SK_MINERAL_HARVEST)
                }
            }
        } else {
            if (room.storage != null) {
                val storage = room.storage!!
                val storageLink = getStorageLink(task, storage)
                room.find(FIND_SOURCES).forEach { source ->
                    if (storageLink == null) {
                        createHarvestTask(room, source, TaskType.HARVEST)
                    } else {
                        createLinkTask(room, source)
                    }
                }
                room.find(FIND_MINERALS).forEach { mineral ->
                    if (mineral.mineralAmount > 0) {
                        createHarvestTask(room, mineral, TaskType.MINERAL_HARVEST)
                    }
                }
            } else {
                room.find(FIND_SOURCES).forEach { source ->
                    createHarvestTask(room, source, TaskType.PRIMITIVE_HARVESTER)
                }
            }
        }
    }

    fun createLinkTask(room: Room, source: Source) {
        val link = getSourceLink(source)
        if (link == null) {
            createHarvestTask(room, source, TaskType.HARVEST)
        } else {
            createTask(room, source, TaskType.LINK_HARVEST)
        }
    }

    fun <T> createHarvestTask(room: Room, source: T, taskType: TaskType) where T : RoomObject, T : Identifiable {
        getSourceContainer(source) ?: return
        createTask(room, source, taskType)
    }

    fun createTask(room: Room, source: Identifiable, type: TaskType) {
        checkLinkedTasks(task)
        val tasks = task.linkedTaskIds.map { Memory.tasks[it]!! }
                .filter { taskMemory ->
                    taskMemory.type == type.code && taskMemory.sourceId == source.id
                            && taskMemory.room == room.name
                }

        when (tasks.size) {
            0 -> {
                val taskId = createTask("${room.name}|${type.code}", jsObject {
                    this.type = type.code
                    this.room = room.name
                    sourceId = source.id
                })
                val linkedTaskIds = task.linkedTaskIds.toMutableList()
                linkedTaskIds.add(taskId)
                task.linkedTaskIds = linkedTaskIds.toTypedArray()
            }
            1 -> {
                val harvestTask = tasks[0]
                if (!harvestTask.newTaskCreated && isNewTaskNeeded(harvestTask, type)) {
                    val taskId = createTask("${room.name}|${type.code}", jsObject {
                        this.type = type.code
                        this.room = room.name
                        sourceId = source.id
                    })
                    val linkedTaskIds = task.linkedTaskIds.toMutableList()
                    linkedTaskIds.add(taskId)
                    task.linkedTaskIds = linkedTaskIds.toTypedArray()
                    harvestTask.newTaskCreated = true
                }
            }
        }
    }

    private fun isNewTaskNeeded(harvestTask: TaskMemory, type: TaskType): Boolean {
        return when (type) {
            TaskType.PRIMITIVE_HARVESTER -> PrimitiveHarvestExecutor(harvestTask).isNewTaskNeeded()
            TaskType.HARVEST -> HarvestExecutor(harvestTask).isNewTaskNeeded()
            TaskType.LINK_HARVEST -> LinkHarvestExecutor(harvestTask).isNewTaskNeeded()
            TaskType.MINERAL_HARVEST -> MineralHarvestExecutor(harvestTask).isNewTaskNeeded()
            TaskType.SK_HARVEST -> SkHarvestExecutor(harvestTask).isNewTaskNeeded()
            TaskType.SK_MINERAL_HARVEST -> SkMineralHarvestExecutor(harvestTask).isNewTaskNeeded()
            else -> false
        }
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

    fun getSourceLink(source: Source): StructureLink? {
        val sourceMemory = getSourceMemory(source.id)
        if (sourceMemory.linkId == null) {
            val sourceLink = source.pos.findInRange(FIND_MY_STRUCTURES, 2, options {
                filter = { it.structureType == STRUCTURE_LINK }
            }).firstOrNull() ?: return null

            sourceMemory.linkId = sourceLink.id
        }

        val sourceLink = Game.getObjectById<StructureLink>(sourceMemory.linkId)
        if (sourceLink == null) {
            sourceMemory.linkId = null
        }
        return sourceLink
    }

    fun <T> getSourceContainer(source: T): StructureContainer? where T : RoomObject, T : Identifiable {
        val sourceMemory = getSourceMemory(source.id)
        if (sourceMemory.containerId == null) {
            val container = source.pos.findInRange(FIND_STRUCTURES, 1, options {
                filter = { it.structureType == STRUCTURE_CONTAINER }
            }).firstOrNull() ?: return null

            sourceMemory.containerId = container.id
        }

        val container = Game.getObjectById<StructureContainer>(sourceMemory.containerId)
        if (container == null) {
            sourceMemory.containerId = null
        }
        return container
    }

}