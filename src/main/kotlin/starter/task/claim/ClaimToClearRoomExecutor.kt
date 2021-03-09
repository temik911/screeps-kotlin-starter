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

/**
 *
 *
 * @author zakharchuk
 * @since 09.03.2020
 */
class ClaimToClearRoomExecutor(task: TaskMemory): ExecutorWithExclusiveCreep(task) {

    override fun getCreepBody(room: Room): Array<BodyPartConstant> = room.calculateClaimRoom()

    override fun executeInternal(creep: Creep) {
        val toRoom = Game.rooms.get(task.toRoom)
        if (toRoom == null) {
            creep.moveTo(RoomPosition(25, 25, task.toRoom))
            return
        }

        val controller = toRoom.controller
        if (controller == null) {
            creep.moveTo(RoomPosition(25, 25, toRoom.name))
        } else {
            when (creep.claimController(controller)) {
                ERR_NOT_IN_RANGE -> creep.moveTo(controller)
                OK -> {
                    createTask("${toRoom.name}|${TaskType.CLEAR_ROOM.code}", jsObject {
                        type = TaskType.CLEAR_ROOM.code
                        this.room = toRoom.name
                    })
                    createTask("${toRoom.name}|${TaskType.UN_CLAIM_ROOM.code}", jsObject {
                        type = TaskType.UN_CLAIM_ROOM.code
                        this.room = toRoom.name
                    })
                    task.isDone = true
                }
            }
        }
    }

}