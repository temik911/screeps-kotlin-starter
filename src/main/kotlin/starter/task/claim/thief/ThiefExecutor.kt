package starter.task.claim.thief

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.Game
import screeps.api.OK
import screeps.api.Room
import screeps.api.RoomPosition
import screeps.api.StoreOwner
import screeps.api.component1
import screeps.api.entries
import screeps.api.get
import screeps.api.structures.StructureStorage
import screeps.api.structures.StructureTerminal
import starter.calculateTransfer
import starter.extension.moveToWithSwap
import starter.memory.TaskMemory
import starter.memory.isDone
import starter.memory.room
import starter.memory.status
import starter.memory.storageId
import starter.memory.terminalId
import starter.memory.thiefTask
import starter.memory.toRoom
import starter.task.ExecutorWithExclusiveCreep

/**
 *
 *
 * @author zakharchuk
 * @since 12.04.2020
 */
class ThiefExecutor(task: TaskMemory) : ExecutorWithExclusiveCreep(task) {
    override fun getCreepBody(room: Room): Array<BodyPartConstant> = room.calculateTransfer()

    override fun prepare(task: TaskMemory, creep: Creep): Boolean {
        if (task.status == "undefined") {
            task.status = "withdraw"
        }
        return true
    }

    override fun executeInternal(creep: Creep) {
        when (task.status) {
            "withdraw" -> {
                val toRoom = Game.rooms.get(task.toRoom)
                if (toRoom == null) {
                    creep.moveToWithSwap(RoomPosition(25, 25, task.toRoom))
                } else {
                    val thiefTask = task.thiefTask!!
                    if (!isEmpty(thiefTask.storageId)) {
                        val storage = Game.getObjectById<StructureStorage>(thiefTask.storageId)!!
                        when (creep.withdraw(storage, storage.store.entries.first().component1())) {
                            OK -> task.status = "transfer"
                            ERR_NOT_IN_RANGE -> creep.moveToWithSwap(storage)
                        }
                    } else if (!isEmpty(thiefTask.terminalId)) {
                        val terminal = Game.getObjectById<StructureTerminal>(thiefTask.terminalId)!!
                        when (creep.withdraw(terminal, terminal.store.entries.first().component1())) {
                            OK -> task.status = "transfer"
                            ERR_NOT_IN_RANGE -> creep.moveToWithSwap(terminal)
                        }
                    } else {
                        task.isDone = true
                    }
                }
            }
            "transfer" -> {
                val room = Game.rooms.get(task.room)!!
                val storage = room.storage!!
                when (creep.transfer(storage, creep.store.entries.first().component1())) {
                    OK -> task.status = "withdraw"
                    ERR_NOT_IN_RANGE -> creep.moveToWithSwap(storage)
                }
            }
        }
    }

    private fun isEmpty(storeOwnerId: String?): Boolean {
        val storeOwner = Game.getObjectById<StoreOwner>(storeOwnerId) ?: return true
        return storeOwner.store.entries.isNullOrEmpty()
    }

}