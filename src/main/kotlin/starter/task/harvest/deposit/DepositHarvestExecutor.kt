package starter.task.harvest.deposit

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.Deposit
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.Game
import screeps.api.OK
import screeps.api.Room
import screeps.api.RoomPosition
import screeps.api.get
import starter.Role
import starter.calculateDepositHarvestBody
import starter.extension.moveToWithSwap
import starter.memory.TaskMemory
import starter.memory.harvestTask
import starter.memory.isDone
import starter.memory.resourceType
import starter.memory.room
import starter.memory.sourceId
import starter.memory.status
import starter.memory.targetPosition
import starter.memory.ticks
import starter.task.ExecutorWithPoolOfCreep

/**
 *
 *
 * @author zakharchuk
 * @since 26.04.2020
 */
class DepositHarvestExecutor(task: TaskMemory) : ExecutorWithPoolOfCreep(task) {

    override fun getCreepBody(room: Room): Array<BodyPartConstant> = room.calculateDepositHarvestBody()

    override fun getCreepRole(): Role  = Role.DEPOSIT_HARVEST

    override fun getTimeOut(): Int = 10

    override fun getTtl(): Int = 100

    override fun prepare(task: TaskMemory, creep: Creep): Boolean {
        if (task.status == "undefined") {
            task.status = "harvest"
        }

        val harvestTask = task.harvestTask ?: return false
        val source = Game.getObjectById<Deposit>(harvestTask.sourceId!!)

        if (source == null) {
            val targetPosition = harvestTask.targetPosition ?: return false
            creep.moveToWithSwap(RoomPosition(targetPosition.x, targetPosition.y, targetPosition.roomName))
        } else {
            creep.moveToWithSwap(source)
            if (creep.pos.isNearTo(source)) {
                return true
            }
        }
        task.ticks += 1
        return false
    }

    override fun executeInternal(creep: Creep) {
        val harvestTask = task.harvestTask ?: return

        when (task.status) {
            "harvest" -> {
                val source = Game.getObjectById<Deposit>(harvestTask.sourceId!!) ?: return
                if (!creep.pos.isNearTo(source)) {
                    creep.moveToWithSwap(source)
                } else {
                    if (source.cooldown == 0) {
                        creep.harvest(source)
                    }
                }

                if (creep.store.getFreeCapacity() == 0 || creep.ticksToLive <= task.ticks * 2.5) {
                    task.status = "goToHome"
                }
            }
            "goToHome" -> {
                val room = Game.rooms.get(task.room)!!
                val storage = room.storage!!
                when (creep.transfer(storage, harvestTask.resourceType!!)) {
                    ERR_NOT_IN_RANGE -> {
                        creep.moveToWithSwap(storage)
                    }
                    OK -> {
                        task.isDone = true
                    }
                }
            }
        }
    }

}