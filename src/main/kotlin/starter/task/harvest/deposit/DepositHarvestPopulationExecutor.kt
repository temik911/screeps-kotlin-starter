package starter.task.harvest.deposit

import screeps.api.Deposit
import screeps.api.Game
import screeps.utils.unsafe.jsObject
import starter.createTask
import starter.memory.TaskMemory
import starter.memory.checkLinkedTasks
import starter.memory.harvestTask
import starter.memory.isDone
import starter.memory.lastCooldown
import starter.memory.linkedTaskIds
import starter.memory.room
import starter.memory.sourceId
import starter.memory.targetPosition
import starter.memory.type
import starter.task.TaskType
import starter.unlock

/**
 *
 *
 * @author zakharchuk
 * @since 27.04.2020
 */
class DepositHarvestPopulationExecutor(val task: TaskMemory) {

    fun execute() {
        val harvestTask = task.harvestTask ?: return
        val source = Game.getObjectById<Deposit>(harvestTask.sourceId!!)
        if (source != null) {
            harvestTask.lastCooldown = source.lastCooldown
        }
        if (harvestTask.lastCooldown > 50) {
            task.isDone = true
            unlock(source!!.id)
            return
        }
        checkLinkedTasks(task)
        if (task.linkedTaskIds.isNullOrEmpty()) {
            val taskId = createTask("${task.room}|${harvestTask.targetPosition!!.roomName}|${TaskType.DEPOSIT_HARVEST.code}", jsObject {
                this.room = task.room
                this.type = TaskType.DEPOSIT_HARVEST.code
                this.harvestTask = task.harvestTask
            })
            val linkedTaskIds = task.linkedTaskIds.toMutableList()
            linkedTaskIds.add(taskId)
            task.linkedTaskIds = linkedTaskIds.toTypedArray()
        }

    }
}