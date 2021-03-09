package starter.task.remote

import screeps.api.Creep
import screeps.api.FIND_HOSTILE_STRUCTURES
import screeps.api.FIND_SOURCES
import screeps.api.Game
import screeps.api.Memory
import screeps.api.Room
import screeps.api.RoomPosition
import screeps.api.STRUCTURE_CONTROLLER
import screeps.api.STRUCTURE_INVADER_CORE
import screeps.api.get
import screeps.api.options
import screeps.api.values
import starter.calculateRemoteRoomAnalyzer
import starter.extension.moveToWithSwap
import starter.memory.TaskMemory
import starter.memory.distance
import starter.memory.isDone
import starter.memory.room
import starter.memory.tasks
import starter.memory.toRoom
import starter.memory.type
import starter.reserveRoom
import starter.task.ExecutorWithExclusiveCreep
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 08.05.2020
 */
class RemoteRoomAnalyzerExecutor(task: TaskMemory) : ExecutorWithExclusiveCreep(task) {

    override fun getCreepBody(room: Room) = room.calculateRemoteRoomAnalyzer()

    override fun executeInternal(creep: Creep) {
        val toRoom = Game.rooms.get(task.toRoom)
        if (toRoom == null) {
            creep.moveToWithSwap(RoomPosition(25, 25, task.toRoom))
            return
        }

        if (toRoom.controller == null) {
            creep.suicide()
            task.isDone = true
            return
        }
        val controller = toRoom.controller!!

        if (controller.owner != null) {
            creep.suicide()
            task.isDone = true
            return
        }

        if (controller.reservation != null && controller.reservation!!.username != "Invader") {
            creep.suicide()
            task.isDone = true
            return
        }

        val structures = toRoom.find(FIND_HOSTILE_STRUCTURES, options { filter = { it.structureType != STRUCTURE_CONTROLLER && it.structureType != STRUCTURE_INVADER_CORE } })
        if (!structures.isNullOrEmpty()) {
            creep.suicide()
            task.isDone = true
            return
        }

        val alreadyReserved = Memory.tasks.values.firstOrNull {
            it.toRoom == task.toRoom && it.type == TaskType.RESERVE_CONTROLLER_POPULATION.code
        } != null
        if (alreadyReserved) {
            creep.suicide()
            task.isDone = true
            return
        }

        val distance = task.distance ?: 0
        if (distance > 1 && toRoom.find(FIND_SOURCES).count() < 2) {
            creep.suicide()
            task.isDone = true
            return
        }

        reserveRoom(task.room, task.toRoom)
        creep.suicide()
        task.isDone = true
    }
}