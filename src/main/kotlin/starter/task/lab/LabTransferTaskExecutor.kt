package starter.task.lab

import screeps.api.Creep
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.Game
import screeps.api.OK
import screeps.api.Room
import screeps.api.keys
import screeps.api.structures.Structure
import starter.Role
import starter.calculateLab
import starter.extension.moveToWithSwap
import starter.memory.TaskMemory
import starter.memory.fromId
import starter.memory.isDone
import starter.memory.resource
import starter.memory.status
import starter.memory.toId
import starter.memory.transferTask
import starter.memory.value
import starter.task.ExecutorWithPoolOfCreep

/**
 *
 *
 * @author zakharchuk
 * @since 30.03.2020
 */
class LabTransferTaskExecutor(task: TaskMemory) : ExecutorWithPoolOfCreep(task) {

    override fun getCreepBody(room: Room) = room.calculateLab()

    override fun getCreepRole() = Role.LAB

    override fun getTtl() = 50

    override fun getTimeOut() = 10

    override fun executeInternal(creep: Creep) {
        if (task.transferTask == null) {
            task.isDone = true
            return
        }

        if (task.status == "cleanUp") {
            if (creep.store.getUsedCapacity() != 0) {
                if (creep.transfer(creep.room.storage!!, creep.store.keys.first()) == ERR_NOT_IN_RANGE) {
                    creep.moveToWithSwap(creep.room.storage!!)
                }
            } else {
                task.isDone = true
            }
            return
        }

        val transferTask = task.transferTask!!
        val resource = transferTask.resource!!
        val value = transferTask.value!!
        if ((creep.store.getUsedCapacity(resource) ?: 0) < value) {
            val from = Game.getObjectById<Structure>(transferTask.fromId!!)
            if (from == null) {
                task.isDone = true
                return
            }
            val withdraw = creep.withdraw(from, resource, value - (creep.store.getUsedCapacity(resource) ?: 0))
            when (withdraw) {
                OK -> {
                    // do nothing
                }
                ERR_NOT_IN_RANGE -> creep.moveToWithSwap(from)
                else -> task.status = "cleanUp"
            }
        } else {
            val to = Game.getObjectById<Structure>(transferTask.toId!!)
            if (to == null) {
                task.isDone = true
                return
            }

            when (creep.transfer(to, resource, value)) {
                OK -> {
                    task.isDone = true
                }
                ERR_NOT_IN_RANGE -> creep.moveToWithSwap(to)
                else -> task.status = "cleanUp"
            }
        }
    }
}