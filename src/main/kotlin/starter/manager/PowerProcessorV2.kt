package starter.manager

import screeps.api.Game
import screeps.api.RESOURCE_ENERGY
import screeps.api.RESOURCE_POWER
import screeps.api.Room
import screeps.utils.unsafe.jsObject
import starter.extension.powerSpawn
import starter.getUsed
import starter.isLocked
import starter.memory.createLinkedTask
import starter.memory.getTasks
import starter.memory.powerProcessor
import starter.memory.room
import starter.memory.sleepUntilTimes
import starter.memory.type
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 12.06.2020
 */
class PowerProcessorV2(val room: Room) {
    fun execute() {
        if (room.controller!!.level != 8) {
            return
        }

        if (room.memory.sleepUntilTimes.powerProcessor > Game.time) {
            return
        }

        val powerSpawn = room.powerSpawn()
        if (powerSpawn == null) {
            room.memory.sleepUntilTimes.powerProcessor = Game.time + 1000
            return
        }

        if (isLocked(powerSpawn.id)) {
            return
        }

        if (room.memory.getTasks { it.type == TaskType.POWER_SPAWN_FILLING.code }.isEmpty()) {
            if (powerSpawn.store.getUsed(RESOURCE_POWER) >= 1 && powerSpawn.store.getUsed(RESOURCE_ENERGY) >= 50) {
                powerSpawn.processPower()
            } else {
                val energy = room.storage?.store?.getUsed(RESOURCE_ENERGY) ?: 0
                val power = room.storage?.store?.getUsed(RESOURCE_POWER) ?: 0
                if (energy >= 75000 && power >= 100) {
                    room.memory.createLinkedTask(TaskType.POWER_SPAWN_FILLING, jsObject { this.room = this@PowerProcessorV2.room.name })
                } else {
                    room.memory.sleepUntilTimes.powerProcessor = Game.time + 50
                }
            }
        }
    }
}