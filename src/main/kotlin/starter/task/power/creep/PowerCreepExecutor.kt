package starter.task.power.creep

import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.FIND_MY_STRUCTURES
import screeps.api.Game
import screeps.api.LOOK_STRUCTURES
import screeps.api.RESOURCE_OPS
import screeps.api.STRUCTURE_POWER_SPAWN
import screeps.api.get
import screeps.api.options
import screeps.api.structures.StructurePowerSpawn
import starter.extension.generateOps
import starter.extension.moveToWithSwap
import starter.extension.powerSpawn
import starter.getUsed
import starter.memory.TaskMemory
import starter.memory.isFree

/**
 *
 *
 * @author zakharchuk
 * @since 03.05.2020
 */
class PowerCreepExecutor(val task: TaskMemory) {
    fun execute(name: String) {
        val powerCreep = Game.powerCreeps.get(name) ?: return
        if (!powerCreep.memory.isFree) {
            return
        }

        if (powerCreep.ticksToLive!! < 1000) {
            val powerSpawn = powerCreep.room.find(FIND_MY_STRUCTURES, options { filter = { it.structureType == STRUCTURE_POWER_SPAWN } })
                    .firstOrNull()?.unsafeCast<StructurePowerSpawn>() ?: return
            if (powerCreep.renew(powerSpawn) == ERR_NOT_IN_RANGE) {
                powerCreep.moveToWithSwap(powerSpawn)
            }
        } else if (powerCreep.store.getUsed(RESOURCE_OPS) >= 400) {
            val storage = powerCreep.room.storage!!
            if (powerCreep.transfer(storage, RESOURCE_OPS, 300) == ERR_NOT_IN_RANGE) {
                powerCreep.moveToWithSwap(storage)
            }
        }

        val controller = powerCreep.room.controller!!
        if (powerCreep.pos.lookFor(LOOK_STRUCTURES)?.firstOrNull { it.structureType == STRUCTURE_POWER_SPAWN } != null) {
            powerCreep.moveTo(controller)
            return
        }

        if (!controller.isPowerEnabled) {
            if (powerCreep.enableRoom(controller) == ERR_NOT_IN_RANGE) {
                powerCreep.moveToWithSwap(controller)
            }
            return
        }

        if (powerCreep.generateOps()) {
            return
        }

        val powerSpawn = powerCreep.room.powerSpawn() ?: return
        if (!powerCreep.pos.isNearTo(powerSpawn)) {
            powerCreep.moveTo(powerSpawn)
        }
    }
}
