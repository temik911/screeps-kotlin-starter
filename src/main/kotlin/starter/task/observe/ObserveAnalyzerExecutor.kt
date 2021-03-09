package starter.task.observe

import screeps.api.FIND_DEPOSITS
import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.RESOURCE_POWER
import screeps.api.STRUCTURE_POWER_BANK
import screeps.api.get
import screeps.api.options
import screeps.api.structures.StructurePowerBank
import screeps.utils.unsafe.jsObject
import starter.createTask
import starter.isLocked
import starter.lock
import starter.memory.HarvestTask
import starter.memory.TaskMemory
import starter.memory.harvestTask
import starter.memory.initiator
import starter.memory.isDone
import starter.memory.resourceType
import starter.memory.room
import starter.memory.sleepUntil
import starter.memory.sourceId
import starter.memory.targetPosition
import starter.memory.toRoom
import starter.memory.type
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 28.04.2020
 */
class ObserveAnalyzerExecutor(val task: TaskMemory) {
    fun execute() {
        if (task.sleepUntil > Game.time) {
            return
        }

        val room = Game.rooms.get(task.toRoom)
        if (room == null) {
            task.isDone = true
            return
        }

        val deposits = room.find(FIND_DEPOSITS, options { filter = { !isLocked(it.id) && it.lastCooldown < 50 } })
        deposits.forEach {
            lock(it.id, jsObject { this.initiator = task.type })
            createTask("${task.room}|${task.toRoom}|${TaskType.DEPOSIT_HARVEST_POPULATION.code}", jsObject {
                this.room = task.room
                this.type = TaskType.DEPOSIT_HARVEST_POPULATION.code
                this.harvestTask = jsObject<HarvestTask> {
                    targetPosition = it.pos
                    sourceId = it.id
                    resourceType = it.depositType
                }
            })
        }

        val powerBanks = room.find(FIND_STRUCTURES, options {
            filter = { it.structureType == STRUCTURE_POWER_BANK && !isLocked(it.id) && it.unsafeCast<StructurePowerBank>().power <= 3000 }
        })
        powerBanks.map { it.unsafeCast<StructurePowerBank>() }
                .forEach {
                    lock(it.id, jsObject { this.initiator = task.type })
                    createTask("${task.room}|${task.toRoom}|${TaskType.POWER_BANK_HARVEST_POPULATION.code}", jsObject {
                        this.room = task.room
                        this.type = TaskType.POWER_BANK_HARVEST_POPULATION.code
                        this.harvestTask = jsObject<HarvestTask> {
                            targetPosition = it.pos
                            sourceId = it.id
                            resourceType = RESOURCE_POWER
                        }
                    })
                }

        task.isDone = true
    }
}