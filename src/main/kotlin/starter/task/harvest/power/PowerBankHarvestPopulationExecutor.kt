package starter.task.harvest.power

import screeps.api.Game
import screeps.api.Memory
import screeps.api.TERRAIN_MASK_WALL
import screeps.api.get
import screeps.api.structures.StructurePowerBank
import screeps.utils.unsafe.jsObject
import starter.createTask
import starter.memory.HarvestTask
import starter.memory.TaskMemory
import starter.memory.checkLinkedTasks
import starter.memory.expectedDamage
import starter.memory.harvestTask
import starter.memory.isDone
import starter.memory.linkedTaskIds
import starter.memory.room
import starter.memory.sourceId
import starter.memory.targetPosition
import starter.memory.tasks
import starter.memory.type
import starter.task.TaskType
import starter.unlock

/**
 *
 *
 * @author zakharchuk
 * @since 28.04.2020
 */
class PowerBankHarvestPopulationExecutor(val task: TaskMemory) {
    fun execute() {
        val harvestTask = task.harvestTask ?: return
        val toRoom = Game.rooms.get(harvestTask.targetPosition!!.roomName)
        val source = Game.getObjectById<StructurePowerBank>(harvestTask.sourceId!!)

        if (toRoom != null && source == null) {
            unlock(harvestTask.sourceId!!)
            task.isDone = true
            return
        }

        if (source != null) {
            checkTransferTasks(source, task, harvestTask)
        }

        checkAttackTask(source, task, harvestTask)
    }

    private fun checkTransferTasks(source: StructurePowerBank, task: TaskMemory, harvestTask: HarvestTask) {
        val harvestCount = task.linkedTaskIds
                .mapNotNull { Memory.tasks[it] }
                .filter { it.type == TaskType.POWER_BANK_HARVEST.code }
                .count()
        if (harvestCount * 600 * 300 < source.hits) {
            return
        }

        val count = source.power / 1600 + 1
        val transferCount = task.linkedTaskIds
                .mapNotNull { Memory.tasks[it] }
                .filter { it.type == TaskType.POWER_BANK_TRANSFER.code }
                .count()

        if (transferCount < count) {
            val taskId = createTask("${task.room}|${harvestTask.targetPosition!!.roomName}|${TaskType.POWER_BANK_TRANSFER.code}", jsObject {
                this.room = task.room
                this.type = TaskType.POWER_BANK_TRANSFER.code
                this.harvestTask = task.harvestTask
            })
            val linkedTaskIds = task.linkedTaskIds.toMutableList()
            linkedTaskIds.add(taskId)
            task.linkedTaskIds = linkedTaskIds.toTypedArray()
        }
    }

    private fun checkAttackTask(source: StructurePowerBank?, task: TaskMemory, harvestTask: HarvestTask) {
        checkLinkedTasks(task)
        val powerBankHarvestTasks = task.linkedTaskIds
                .mapNotNull { Memory.tasks[it] }
                .filter { it.type == TaskType.POWER_BANK_HARVEST.code }
        val count = powerBankHarvestTasks.count()
        if (source == null) {
            if (count == 0) {
                val taskId = createTask("${task.room}|${harvestTask.targetPosition!!.roomName}|${TaskType.POWER_BANK_HARVEST.code}", jsObject {
                    this.room = task.room
                    this.type = TaskType.POWER_BANK_HARVEST.code
                    this.harvestTask = task.harvestTask
                    this.expectedDamage = 600 * 1500
                })
                val linkedTaskIds = task.linkedTaskIds.toMutableList()
                linkedTaskIds.add(taskId)
                task.linkedTaskIds = linkedTaskIds.toTypedArray()
            }
        } else {
            if (countAvailableSpots(source) > count) {
                val expectedDamage = powerBankHarvestTasks
                        .sumBy { it.expectedDamage }
                if (expectedDamage < source.hits) {
                    val taskId = createTask("${task.room}|${harvestTask.targetPosition!!.roomName}|${TaskType.POWER_BANK_HARVEST.code}", jsObject {
                        this.room = task.room
                        this.type = TaskType.POWER_BANK_HARVEST.code
                        this.harvestTask = task.harvestTask
                        this.expectedDamage = 600 * 1500
                    })
                    val linkedTaskIds = task.linkedTaskIds.toMutableList()
                    linkedTaskIds.add(taskId)
                    task.linkedTaskIds = linkedTaskIds.toTypedArray()
                }
            }
        }
    }

    private fun countAvailableSpots(source: StructurePowerBank): Int {
        val room = source.room
        val terrain = room.getTerrain()
        var count = 0
        for (x in source.pos.x - 1..source.pos.x + 1) {
            for (y in source.pos.y - 1..source.pos.y + 1) {
                if (x in 1..48 && y in 1..48 && terrain.get(x, y) != TERRAIN_MASK_WALL) {
                    count++
                }
            }
        }
        return count
    }
}