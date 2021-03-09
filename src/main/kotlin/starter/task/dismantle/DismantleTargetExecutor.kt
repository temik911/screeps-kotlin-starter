package starter.task.dismantle

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.LOOK_STRUCTURES
import screeps.api.Room
import screeps.api.RoomPosition
import starter.calculateDismantleBody
import starter.extension.moveToWithSwap
import starter.memory.TaskMemory
import starter.memory.dismantleTask
import starter.memory.targetPosition
import starter.task.ExecutorWithExclusiveCreep

/**
 *
 *
 * @author zakharchuk
 * @since 01.04.2020
 */
class DismantleTargetExecutor(task:TaskMemory) : ExecutorWithExclusiveCreep(task) {
    override fun getCreepBody(room: Room): Array<BodyPartConstant> = room.calculateDismantleBody()

    override fun executeInternal(creep: Creep) {
        val position = task.dismantleTask!!.targetPosition!!
        val targetPosition = RoomPosition(position.x, position.y, position.roomName)
        val nearTo = creep.pos.isNearTo(targetPosition)
        if (!nearTo) {
            creep.moveToWithSwap(targetPosition)
        } else {
            val structure = targetPosition.lookFor(LOOK_STRUCTURES)?.firstOrNull()
            if (structure != null) {
                creep.dismantle(structure)
            }
        }
    }
}