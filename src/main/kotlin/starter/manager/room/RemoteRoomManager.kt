package starter.manager.room

import screeps.api.COSTMATRIX_FALSE
import screeps.api.CostMatrix
import screeps.api.FIND_HOSTILE_CREEPS
import screeps.api.FIND_MY_CONSTRUCTION_SITES
import screeps.api.FIND_SOURCES
import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.LOOK_CONSTRUCTION_SITES
import screeps.api.LOOK_STRUCTURES
import screeps.api.PathFinder
import screeps.api.Room
import screeps.api.RoomMemory
import screeps.api.RoomPosition
import screeps.api.STRUCTURE_CONTAINER
import screeps.api.STRUCTURE_INVADER_CORE
import screeps.api.STRUCTURE_RAMPART
import screeps.api.STRUCTURE_ROAD
import screeps.api.get
import screeps.api.options
import screeps.utils.unsafe.jsObject
import starter.extension.isHighway
import starter.extension.isSk
import starter.externalPlan
import starter.memory.TaskMemory
import starter.memory.builder
import starter.memory.checkLinkedTasks
import starter.memory.common
import starter.memory.controllerReservation
import starter.memory.createLinkedTask
import starter.memory.defender
import starter.memory.getContainer
import starter.memory.getRoomMemory
import starter.memory.getTasks
import starter.memory.newTaskCreated
import starter.memory.roadsPlanner
import starter.memory.room
import starter.memory.roomPlan
import starter.memory.roomPlanner
import starter.memory.sleepUntilTimes
import starter.memory.sourceId
import starter.memory.toRoom
import starter.memory.type
import starter.task.TaskType
import starter.task.harvest.HarvestExecutor

/**
 *
 *
 * @author zakharchuk
 * @since 26.05.2020
 */
class RemoteRoomManager(val fromRoom: Room, val toRoomName: String) {
    fun manage() {
        val roomMemory = getRoomMemory(toRoomName)
        roomMemory.checkLinkedTasks()
        val toRoom = Game.rooms[toRoomName]
        updateReservationTimer(roomMemory, toRoom)
        defend(roomMemory, toRoom)

        if (roomMemory.sleepUntilTimes.common > Game.time) {
            return
        }

        reserveController(roomMemory)
        harvest(roomMemory, toRoom)
        build(roomMemory, toRoom)
        roomPlan(roomMemory, toRoom)
        roadsPlan(roomMemory, toRoom)
    }

    private fun roadsPlan(roomMemory: RoomMemory, toRoom: Room?) {
        if (toRoom == null || roomMemory.sleepUntilTimes.roadsPlanner > Game.time) {
            return
        }

        val storage = fromRoom.storage ?: return
        val paths = mutableListOf<RoomPosition>()
        toRoom.find(FIND_STRUCTURES, options { filter = { it.structureType == STRUCTURE_CONTAINER } })
                .forEach {
                    val path = PathFinder.search(storage.pos, jsObject<PathFinder.GoalWithRange> {
                        pos = it.pos
                        range = 1
                    }, options {
                        plainCost = 2
                        swampCost = 5
                        maxOps = 25000
                        roomCallback = { roomName -> callBack(roomName, paths) }
                    })
                    if (path.incomplete) {
                        console.log("incomplete path")
                        return
                    }
                    paths.addAll(path.path)
                }

        paths.forEach {
            val roomToBuild = Game.rooms[it.roomName] ?: return@forEach
            val structures = roomToBuild.lookForAt(LOOK_STRUCTURES, it.x, it.y)
            if (structures.isNullOrEmpty()) {
                val constructionSites = roomToBuild.lookForAt(LOOK_CONSTRUCTION_SITES, it.x, it.y)
                if (constructionSites.isNullOrEmpty()) {
                    roomToBuild.createConstructionSite(it.x, it.y, STRUCTURE_ROAD)
                }
            }
        }

        roomMemory.sleepUntilTimes.roadsPlanner = Game.time + 300
    }

    private fun callBack(roomName: String, paths: MutableList<RoomPosition>): CostMatrix {
        val room = Game.rooms[roomName]
        if (room == null || room.isSk() || room.isHighway()) {
            return COSTMATRIX_FALSE
        }
        val costMatrix = PathFinder.CostMatrix()
        room.find(FIND_STRUCTURES).forEach { structure ->
            if (structure.structureType == STRUCTURE_ROAD) {
                costMatrix.set(structure.pos.x, structure.pos.y, 1)
            } else if (structure.structureType == STRUCTURE_RAMPART) {
                // do nothing
            } else {
                costMatrix.set(structure.pos.x, structure.pos.y, 255)
            }
        }
        room.find(FIND_MY_CONSTRUCTION_SITES).forEach { constructionSite ->
            if (constructionSite.structureType == STRUCTURE_ROAD) {
                costMatrix.set(constructionSite.pos.x, constructionSite.pos.y, 1)
            } else if (constructionSite.structureType == STRUCTURE_RAMPART) {
                // do nothing
            } else {
                costMatrix.set(constructionSite.pos.x, constructionSite.pos.y, 255)
            }
        }
        paths.filter { it.roomName == roomName }
                .forEach { costMatrix.set(it.x, it.y, 1) }

        return costMatrix
    }

    private fun roomPlan(roomMemory: RoomMemory, toRoom: Room?) {
        if (toRoom == null || roomMemory.sleepUntilTimes.roomPlanner > Game.time) {
            return
        }

        if (roomMemory.roomPlan.isNullOrEmpty()) {
            roomMemory.roomPlan = toRoom.externalPlan()!!
        }

        var isChanged = false
        roomMemory.roomPlan.forEach {
            val structures = toRoom.lookForAt(LOOK_STRUCTURES, it.first, it.second)
            if (structures.isNullOrEmpty()) {
                val constructionSites = toRoom.lookForAt(LOOK_CONSTRUCTION_SITES, it.first, it.second)
                if (constructionSites.isNullOrEmpty()) {
                    toRoom.createConstructionSite(it.first, it.second, it.third)
                    isChanged = true
                } else {
                    if (constructionSites[0].structureType != it.third) {
                        constructionSites[0].remove()
                        toRoom.createConstructionSite(it.first, it.second, it.third)
                        isChanged = true
                    }
                }
            } else {
                val structure = structures.firstOrNull { structure -> structure.structureType == it.third }
                if (structure == null) {
                    structures.forEach { it.destroy() }
                    toRoom.createConstructionSite(it.first, it.second, it.third)
                    isChanged = true
                }
            }
        }

        if (!isChanged) {
            roomMemory.sleepUntilTimes.roomPlanner = Game.time + 50
        }
    }

    private fun defend(roomMemory: RoomMemory, toRoom: Room?) {
        if (toRoom == null || roomMemory.sleepUntilTimes.defender > Game.time) {
            return
        }


        val target = toRoom.find(FIND_HOSTILE_CREEPS, options { filter = { it.owner.username == "Invader" } }).firstOrNull()
        if (target != null) {
            roomMemory.sleepUntilTimes.common = Game.time + target.ticksToLive
            if (roomMemory.getTasks { it.type == TaskType.DEFEND.code }.isNullOrEmpty()) {
                createInternalTask(roomMemory, TaskType.DEFEND)
            }
        } else {
            roomMemory.sleepUntilTimes.common = 0
        }

        val invaderCore = toRoom.find(FIND_STRUCTURES, options { filter = { it.structureType == STRUCTURE_INVADER_CORE } }).firstOrNull()
        if (invaderCore != null) {
            if (roomMemory.getTasks { it.type == TaskType.INVADER_CODE_DESTROYER.code }.isNullOrEmpty()) {
                createInternalTask(roomMemory, TaskType.INVADER_CODE_DESTROYER)
            }
        }

        if (target == null && invaderCore == null) {
            roomMemory.sleepUntilTimes.defender = Game.time + 15
        }
    }

    private fun build(roomMemory: RoomMemory, toRoom: Room?) {
        if (toRoom == null || roomMemory.sleepUntilTimes.builder > Game.time) {
            return
        }

        if (roomMemory.getTasks { it.type == TaskType.BUILD.code }.isEmpty()) {
            if (toRoom.find(FIND_MY_CONSTRUCTION_SITES).isNotEmpty()) {
                createInternalTask(roomMemory, TaskType.BUILD, jsObject { })
            } else {
                roomMemory.sleepUntilTimes.builder = Game.time + 50
            }
        }
    }

    private fun harvest(roomMemory: RoomMemory, toRoom: Room?) {
        toRoom?.find(FIND_SOURCES)?.forEach { source ->
            val container = getContainer(source) ?: return@forEach
            val tasks = roomMemory.getTasks { it.type == TaskType.HARVEST.code && it.sourceId == source.id }
            when (tasks.count()) {
                0 -> {
                    createInternalTask(roomMemory, TaskType.HARVEST, jsObject { this.sourceId = source.id })
                }
                1 -> {
                    val harvestTask = tasks[0]
                    if (!harvestTask.newTaskCreated && HarvestExecutor(harvestTask).isNewTaskNeeded()) {
                        createInternalTask(roomMemory, TaskType.HARVEST, jsObject { this.sourceId = source.id })
                        harvestTask.newTaskCreated = true
                    }
                }
            }
        }
    }

    private fun reserveController(roomMemory: RoomMemory) {
        val count = roomMemory.getTasks { it.type == TaskType.RESERVE_CONTROLLER.code }.count()
        if (roomMemory.controllerReservation < 2500 && count < 1) {
            createInternalTask(roomMemory, TaskType.RESERVE_CONTROLLER)
        }
    }

    private fun updateReservationTimer(roomMemory: RoomMemory, toRoom: Room?) {
        if (toRoom == null) {
            roomMemory.controllerReservation -= 1
        } else {
            val controller = toRoom.controller!!
            roomMemory.controllerReservation = controller.reservation?.ticksToEnd ?: 0
        }
    }

    private fun createInternalTask(roomMemory: RoomMemory, taskType: TaskType, taskMemory: TaskMemory = jsObject { }) {
        taskMemory.room = fromRoom.name
        taskMemory.toRoom = toRoomName
        roomMemory.createLinkedTask(taskType, taskMemory)
    }
}