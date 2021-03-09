package starter.task.lowlevel

import screeps.api.FIND_MY_CONSTRUCTION_SITES
import screeps.api.Game
import screeps.api.get
import screeps.utils.unsafe.jsObject
import starter.memory.TaskMemory
import starter.memory.checkLinkedTasks
import starter.memory.createLinkedTask
import starter.memory.isDone
import starter.memory.linkedTaskIds
import starter.memory.room
import starter.memory.toRoom
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 10.03.2020
 */
class LowLevelBuilderPopulationExecutor(val task: TaskMemory) {
    fun execute() {
        val toRoom = Game.rooms.get(task.toRoom) ?: return
        val controller = toRoom.controller ?: return
        if (controller.level == 4 && (controller.progress.toDouble() / controller.progressTotal * 100) > 5.0) {
            if (toRoom.find(FIND_MY_CONSTRUCTION_SITES).isNullOrEmpty()) {
                task.isDone = true
                return
            }
        }

        task.checkLinkedTasks()
        if (task.linkedTaskIds.size < 4) {
            task.createLinkedTask(TaskType.LOW_LEVEL_BUILDER, jsObject {
                this.room = task.room
                this.toRoom = task.toRoom
            })
        }
    }
}