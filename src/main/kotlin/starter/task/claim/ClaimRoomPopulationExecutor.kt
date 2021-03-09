package starter.task.claim

import screeps.api.Game
import screeps.api.Memory
import screeps.api.get
import screeps.api.values
import screeps.utils.unsafe.jsObject
import starter.createTask
import starter.memory.TaskMemory
import starter.memory.isDone
import starter.memory.room
import starter.memory.tasks
import starter.memory.toRoom
import starter.memory.type
import starter.memory.upgradeBlockedTimer
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 09.03.2020
 */
class ClaimRoomPopulationExecutor(val task: TaskMemory) {
    fun execute() {
        val toRoom = Game.rooms.get(task.toRoom)
        if (toRoom == null) {
            task.upgradeBlockedTimer -= 1
        } else {
            val controller = toRoom.controller
            if (controller == null) {
                task.upgradeBlockedTimer -= 1
            } else {
                if (controller.my) {
                    task.isDone = true
                    return
                }

                task.upgradeBlockedTimer = controller.upgradeBlocked
            }
        }

        if (task.upgradeBlockedTimer <= 0) {
            val count = Memory.tasks.values.filter { taskMemory ->
                taskMemory.type == TaskType.CLAIM_ROOM.code && taskMemory.room == task.room && taskMemory.toRoom == task.toRoom
            }.count()

            if (count == 0) {
                createTask("${task.room}|${task.toRoom}|${TaskType.CLAIM_ROOM.code}", jsObject {
                    type = TaskType.CLAIM_ROOM.code
                    this.room = task.room
                    this.toRoom = task.toRoom
                })
            }
        }
    }
}