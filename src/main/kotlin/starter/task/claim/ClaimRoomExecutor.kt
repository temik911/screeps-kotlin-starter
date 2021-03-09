package starter.task.claim

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.Game
import screeps.api.OK
import screeps.api.Room
import screeps.api.RoomPosition
import screeps.api.get
import screeps.utils.unsafe.jsObject
import starter.calculateClaimRoom
import starter.createTask
import starter.memory.TaskMemory
import starter.memory.isDone
import starter.memory.room
import starter.memory.toRoom
import starter.memory.type
import starter.task.ExecutorWithExclusiveCreep
import starter.task.TaskType
import starter.walker.walkTo

/**
 *
 *
 * @author zakharchuk
 * @since 01.03.2020
 */
class ClaimRoomExecutor(task: TaskMemory): ExecutorWithExclusiveCreep(task) {

    override fun getCreepBody(room: Room): Array<BodyPartConstant> = room.calculateClaimRoom()

    override fun executeInternal(creep: Creep) {
        val toRoom = Game.rooms.get(task.toRoom)
        val ignoreRooms = listOf("E53S57", "E52S58")
        if (toRoom == null) {
            creep.walkTo(RoomPosition(25, 25, task.toRoom), ignoreRooms = ignoreRooms)
            return
        }

        val controller = toRoom.controller
        if (controller == null) {
            creep.walkTo(RoomPosition(25, 25, task.toRoom), ignoreRooms = ignoreRooms)
        } else {
            if (controller.owner != null) {
                if (creep.attackController(controller) == ERR_NOT_IN_RANGE) {
                    creep.walkTo(controller, ignoreRooms = listOf("E43S41", "E43S42"))
                }
            } else {
                when (creep.claimController(controller)) {
                    ERR_NOT_IN_RANGE -> creep.walkTo(controller, ignoreRooms = ignoreRooms)
                    OK -> {
                        createTask("${toRoom.name}|${TaskType.CLEAR_ROOM.code}", jsObject {
                            type = TaskType.CLEAR_ROOM.code
                            this.room = toRoom.name
                        })
                        createTask("${task.room}|${toRoom.name}|${TaskType.LOW_LEVEL_BUILDER_POPULATION.code}", jsObject {
                            type = TaskType.LOW_LEVEL_BUILDER_POPULATION.code
                            this.room = task.room
                            this.toRoom = toRoom.name
                        })
                        createTask("${toRoom.name}|${TaskType.ROOM_PLANER_V2.code}", jsObject {
                            type = TaskType.ROOM_PLANER_V2.code
                            this.room = toRoom.name
                        })
                        task.isDone = true
                    }
                }
            }
        }
    }
}