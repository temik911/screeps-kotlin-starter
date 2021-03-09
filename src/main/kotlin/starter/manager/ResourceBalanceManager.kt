package starter.manager

import screeps.api.Game
import screeps.api.Room
import screeps.utils.unsafe.jsObject
import starter.extension.storageLink
import starter.memory.createLinkedTask
import starter.memory.getTasks
import starter.memory.resourceBalance
import starter.memory.room
import starter.memory.sleepUntilTimes
import starter.memory.type
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 14.06.2020
 */
class ResourceBalanceManager(val room: Room) {
    fun execute() {
        if (room.controller!!.level < 5) {
            return
        }

        if (room.memory.sleepUntilTimes.resourceBalance > Game.time) {
            return
        }

        if (room.storage == null || room.storageLink() == null) {
            room.memory.sleepUntilTimes.resourceBalance = Game.time + 500
            return
        }

        val tasks = room.memory.getTasks { it.type == TaskType.STORAGE_SUPPORT_V2.code }
        if (tasks.isEmpty()) {
            room.memory.createLinkedTask(TaskType.STORAGE_SUPPORT_V2, jsObject {
                this.room = this@ResourceBalanceManager.room.name
            })
            room.memory.sleepUntilTimes.resourceBalance = Game.time + 1500
        }
    }
}