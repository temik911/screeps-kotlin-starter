package starter.task.unclaim

import screeps.api.Creep
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.Game
import screeps.api.RESOURCE_ENERGY
import screeps.api.Room
import screeps.api.get
import screeps.api.keys
import starter.calculateBaseEnergySupportBody
import starter.extension.moveToWithSwap
import starter.memory.TaskMemory
import starter.memory.room
import starter.task.ExecutorWithExclusiveCreep

/**
 *
 *
 * @author zakharchuk
 * @since 08.05.2020
 */
class UnclaimMyRoomExecutor(task: TaskMemory) : ExecutorWithExclusiveCreep(task) {

    override fun getCreepBody(room: Room) = room.calculateBaseEnergySupportBody()

    override fun priority() = true

    override fun executeInternal(creep: Creep) {
        val room = Game.rooms.get(task.room)!!
        val storage = room.storage ?: return
        val terminal = room.terminal ?: return

        if (creep.store.getUsedCapacity() > 0) {
            if (creep.transfer(terminal, creep.store.keys.first()) == ERR_NOT_IN_RANGE) {
                creep.moveToWithSwap(terminal)
            }
            return
        }

        if (storage.store.getUsedCapacity() == 0) {
            return
        }

        val resource = storage.store.keys.firstOrNull { it != RESOURCE_ENERGY } ?: RESOURCE_ENERGY
        if (creep.withdraw(storage, resource) == ERR_NOT_IN_RANGE) {
            creep.moveToWithSwap(storage)
        }
    }
}