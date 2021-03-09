package starter.task.controller.reserve

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.Game
import screeps.api.Room
import screeps.api.RoomPosition
import screeps.api.get
import starter.calculateReserveController
import starter.memory.TaskMemory
import starter.memory.targetPosition
import starter.memory.toRoom
import starter.task.ExecutorWithExclusiveCreep

/**
 *
 *
 * @author zakharchuk
 * @since 28.02.2020
 */
class ReserveControllerExecutor(task: TaskMemory) : ExecutorWithExclusiveCreep(task) {

    override fun getCreepBody(room: Room): Array<BodyPartConstant> = room.calculateReserveController()

    override fun executeInternal(creep: Creep) {
        val toRoom = Game.rooms.get(task.toRoom)
        if (toRoom == null) {
            moveToTargetPosition(creep)
        } else {
            val controller = toRoom.controller
            if (controller == null) {
                moveToTargetPosition(creep)
            } else if (creep.reserveController(controller) == ERR_NOT_IN_RANGE) {
                creep.moveTo(controller)
                if (task.targetPosition == null) {
                    task.targetPosition = controller.pos
                }
            }
        }
    }

    private fun moveToTargetPosition(creep: Creep) {
        if (task.targetPosition != null) {
            val position = task.targetPosition!!
            val targetPosition = RoomPosition(position.x, position.y, position.roomName)
            creep.moveTo(targetPosition)
        } else {
            creep.moveTo(RoomPosition(25, 25, task.toRoom))
        }
    }
}