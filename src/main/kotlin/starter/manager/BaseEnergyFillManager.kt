package starter.manager

import screeps.api.FIND_MY_CREEPS
import screeps.api.RESOURCE_ENERGY
import screeps.api.Room
import screeps.utils.unsafe.jsObject
import starter.getUsed
import starter.memory.createLinkedTask
import starter.memory.getTasks
import starter.memory.room
import starter.memory.type
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 21.06.2020
 */
class BaseEnergyFillManager(val room: Room) {
    fun execute() {
        val creeps = room.find(FIND_MY_CREEPS)
        val storage = room.storage
        if (storage != null && storage.store.getUsed(RESOURCE_ENERGY) > 0 && !creeps.isNullOrEmpty()) {
            val tasksCount = room.memory.getTasks { it.type == TaskType.BASE_ENERGY_SUPPORT.code }.count()
            if (tasksCount < 2) {
                room.memory.createLinkedTask(TaskType.BASE_ENERGY_SUPPORT, jsObject {
                    this.room = this@BaseEnergyFillManager.room.name
                })
            }
        } else {
            val tasksCount = room.memory.getTasks { it.type == TaskType.PRIMITIVE_BASE_ENERGY_SUPPORT.code }.count()
            if (tasksCount < 4) {
                room.memory.createLinkedTask(TaskType.PRIMITIVE_BASE_ENERGY_SUPPORT, jsObject {
                    this.room = this@BaseEnergyFillManager.room.name
                })
            }
        }
    }
}