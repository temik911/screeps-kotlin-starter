package starter.task.build

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.ERR_INVALID_TARGET
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.FIND_CONSTRUCTION_SITES
import screeps.api.Room
import screeps.api.RoomPosition
import starter.ContainerType
import starter.calculateBuilderBody
import starter.extension.moveToWithSwap
import starter.memory.TaskMemory
import starter.memory.status
import starter.memory.toRoom
import starter.task.ExecutorWithExclusiveCreep
import starter.withdraw

/**
 *
 *
 * @author zakharchuk
 * @since 05.03.2020
 */
class BuildExecutor(task: TaskMemory): ExecutorWithExclusiveCreep(task) {

    override fun getCreepBody(room: Room): Array<BodyPartConstant> = room.calculateBuilderBody()

    override fun prepare(task: TaskMemory, creep: Creep): Boolean {
        if (task.status == "undefined") {
            task.status = "withdraw"
        }

        if (creep.room.name == task.toRoom && creep.pos.x in 1..48 && creep.pos.y in 1..48) {
            return true
        }
        creep.moveTo(RoomPosition(25, 25, task.toRoom))
        return false
    }

    override fun executeInternal(creep: Creep) {
        when (task.status) {
            "build" -> {
                val target = creep.pos.findClosestByRange(FIND_CONSTRUCTION_SITES)
                if (target != null) {
                    val result = creep.build(target)
                    when (result) {
                        ERR_NOT_IN_RANGE -> creep.moveToWithSwap(target.pos, maxRooms = 1)
                        ERR_INVALID_TARGET -> creep.moveToWithSwap(creep.room.controller!!)
                    }
                }

                if (creep.store.getUsedCapacity() == 0) {
                    task.status = "withdraw"
                }
            }
            "withdraw" -> {
                withdraw(task, creep, ContainerType.ALL)

                if (creep.store.getFreeCapacity() == 0) {
                    task.status = "build"
                    return
                }
            }
        }
    }

}