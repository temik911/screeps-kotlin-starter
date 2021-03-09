package starter.task.harvest.power

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.FIND_DROPPED_RESOURCES
import screeps.api.FIND_RUINS
import screeps.api.Game
import screeps.api.OK
import screeps.api.RESOURCE_POWER
import screeps.api.Room
import screeps.api.RoomPosition
import screeps.api.get
import screeps.api.options
import starter.Role
import starter.calculateTransfer
import starter.extension.moveToWithSwap
import starter.getUsed
import starter.memory.TaskMemory
import starter.memory.harvestTask
import starter.memory.isDone
import starter.memory.room
import starter.memory.status
import starter.memory.targetPosition
import starter.task.ExecutorWithPoolOfCreep

/**
 *
 *
 * @author zakharchuk
 * @since 28.04.2020
 */
class PowerBankTransferExecutor(task: TaskMemory) : ExecutorWithPoolOfCreep(task) {
    override fun getCreepBody(room: Room): Array<BodyPartConstant> = room.calculateTransfer()

    override fun getCreepRole(): Role = Role.TRANSFER

    override fun getTimeOut(): Int = 50

    override fun getTtl(): Int = 1000

    override fun prepare(task: TaskMemory, creep: Creep): Boolean {
        if (task.status == "undefined" ) {
            task.status = "wait"
        }

        val harvestTask = task.harvestTask ?: return false

        val targetPosition = harvestTask.targetPosition ?: return false
        val pos = RoomPosition(targetPosition.x, targetPosition.y, targetPosition.roomName)
        if (creep.pos.getRangeTo(pos) > 5) {
            creep.moveToWithSwap(pos)
            return false
        }

        return true
    }

    override fun executeInternal(creep: Creep) {
        when (task.status) {
            "wait" -> {
                val ruin = creep.room.find(FIND_RUINS, options {
                    filter = {
                        it.store.getUsed(RESOURCE_POWER) != 0
                    }
                }).firstOrNull()

                if (ruin != null) {
                    when (creep.withdraw(ruin, RESOURCE_POWER)) {
                        OK -> task.status = "goToHome"
                        ERR_NOT_IN_RANGE -> creep.moveToWithSwap(ruin)
                    }
                    return
                }

                val droppedResource = creep.room.find(FIND_DROPPED_RESOURCES, options {
                    filter = {
                        it.resourceType == RESOURCE_POWER
                    }
                }).firstOrNull()

                if (droppedResource != null) {
                    when (creep.pickup(droppedResource)) {
                        OK -> task.status = "goToHome"
                        ERR_NOT_IN_RANGE -> creep.moveToWithSwap(droppedResource)
                    }
                    return
                }
            }
            "goToHome" -> {
                val room = Game.rooms.get(task.room)!!
                val storage = room.storage!!
                when (creep.transfer(storage, RESOURCE_POWER)) {
                    ERR_NOT_IN_RANGE -> creep.moveToWithSwap(storage)
                    OK -> task.isDone = true
                }
            }
        }
    }
}