package starter.task.build

import screeps.api.FIND_MY_CONSTRUCTION_SITES
import screeps.api.Game
import screeps.api.Room
import screeps.api.get
import screeps.utils.unsafe.jsObject
import starter.memory.TaskMemory
import starter.memory.checkLinkedTasks
import starter.memory.createLinkedTask
import starter.memory.getRoomMemory
import starter.memory.isExternalRoom
import starter.memory.isSkRoom
import starter.memory.linkedTaskIds
import starter.memory.room
import starter.memory.sleepUntil
import starter.memory.toRoom
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 05.03.2020
 */
class BuildPopulationExecutor(val task: TaskMemory) {
    fun execute() {
        if (task.sleepUntil > Game.time) {
            return
        }

        val room = Game.rooms.get(task.room) ?: return
        if (task.isExternalRoom || task.isSkRoom) {
            if (getRoomMemory(task.toRoom).sleepUntil > Game.time) {
                return
            }
            val toRoom = Game.rooms.get(task.toRoom) ?: return
            createTasks(room, toRoom)
        } else {
            createTasks(room, room)
        }
    }

    fun createTasks(room: Room, toRoom: Room) {
        task.checkLinkedTasks()
        if (task.linkedTaskIds.isNullOrEmpty()) {
            val target = toRoom.find(FIND_MY_CONSTRUCTION_SITES).firstOrNull()
            if (target != null) {
                task.createLinkedTask(TaskType.BUILD, jsObject {
                    this.room = room.name
                    this.toRoom = toRoom.name
                    this.isSkRoom = task.isSkRoom
                })
            } else {
                task.sleepUntil = Game.time + 50
            }
        }
    }
}