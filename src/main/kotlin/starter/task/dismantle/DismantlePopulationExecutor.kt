package starter.task.dismantle

import screeps.api.Game
import screeps.api.LOOK_STRUCTURES
import screeps.api.RoomPosition
import screeps.api.get
import screeps.utils.unsafe.jsObject
import starter.memory.DismantleTask
import starter.memory.TaskMemory
import starter.memory.checkLinkedTasks
import starter.memory.createLinkedTask
import starter.memory.dismantleTask
import starter.memory.isDone
import starter.memory.linkedTaskIds
import starter.memory.room
import starter.memory.targetPosition
import starter.memory.type
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 01.04.2020
 */
class DismantlePopulationExecutor(val task: TaskMemory) {
    fun execute() {
        val dismantleTask = task.dismantleTask!!
        if (dismantleTask.type == "target") {
            val position = task.dismantleTask!!.targetPosition!!
            val targetPosition = RoomPosition(position.x, position.y, position.roomName)
            val room = Game.rooms[targetPosition.roomName]
            if (room == null) {
                createDismantleTaskIfNeeded(dismantleTask, task.room)
            } else {
                if (targetPosition.lookFor(LOOK_STRUCTURES).isNullOrEmpty()) {
                    task.isDone = true
                    return
                }
                createDismantleTaskIfNeeded(dismantleTask, task.room)
            }
        }
    }

    private fun createDismantleTaskIfNeeded(dismantleTask: DismantleTask, roomName: String) {
        task.checkLinkedTasks()
        if (task.linkedTaskIds.isNullOrEmpty()) {
            task.createLinkedTask(TaskType.DISMANTLE_TARGET, jsObject {
                this.room = roomName
                this.dismantleTask = dismantleTask
            })
        }
    }
}