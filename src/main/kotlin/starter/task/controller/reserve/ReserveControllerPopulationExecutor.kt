package starter.task.controller.reserve

import screeps.api.Game
import screeps.api.Room
import screeps.api.RoomMemory
import screeps.api.get
import screeps.utils.unsafe.jsObject
import starter.memory.TaskMemory
import starter.memory.checkLinkedTasks
import starter.memory.controllerReservation
import starter.memory.createLinkedTask
import starter.memory.getRoomMemory
import starter.memory.linkedTaskIds
import starter.memory.room
import starter.memory.sleepUntil
import starter.memory.toRoom
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 09.03.2020
 */
class ReserveControllerPopulationExecutor(val task: TaskMemory) {
    fun execute() {
        val roomMemory = getRoomMemory(task.toRoom)
        val toRoom = Game.rooms.get(task.toRoom)
        updateReservationTimer(roomMemory, toRoom)

        if (roomMemory.sleepUntil > Game.time) {
            return
        }

        if (roomMemory.controllerReservation < 2500) {
            createTasks(task.room, task.toRoom)
        }
    }

    private fun updateReservationTimer(roomMemory: RoomMemory, toRoom: Room?) {
        if (toRoom == null) {
            roomMemory.controllerReservation -= 1
        } else {
            val controller = toRoom.controller!!
            roomMemory.controllerReservation = controller.reservation?.ticksToEnd ?: 0
        }
    }

    fun createTasks(room: String, toRoom: String) {
        task.checkLinkedTasks()
        if (task.linkedTaskIds.isNullOrEmpty()) {
            task.createLinkedTask(TaskType.RESERVE_CONTROLLER, jsObject {
                this.room = room
                this.toRoom = toRoom
            })
        }
    }
}