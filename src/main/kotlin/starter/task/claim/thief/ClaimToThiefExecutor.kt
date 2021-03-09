package starter.task.claim.thief

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.Game
import screeps.api.OK
import screeps.api.Room
import screeps.api.RoomPosition
import screeps.api.get
import starter.calculateClaimToThiefRoom
import starter.memory.TaskMemory
import starter.memory.isDone
import starter.memory.toRoom
import starter.task.ExecutorWithExclusiveCreep

/**
 *
 *
 * @author zakharchuk
 * @since 12.04.2020
 */
class ClaimToThiefExecutor(task: TaskMemory): ExecutorWithExclusiveCreep(task) {

    override fun getCreepBody(room: Room): Array<BodyPartConstant> = room.calculateClaimToThiefRoom()

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
            if (controller.owner != null) {
                if (creep.attackController(controller) == ERR_NOT_IN_RANGE) {
                    creep.moveTo(controller)
                }
            } else {
                when (creep.claimController(controller)) {
                    ERR_NOT_IN_RANGE -> creep.moveTo(controller)
                    OK -> task.isDone = true
                }
            }
        }
    }

}