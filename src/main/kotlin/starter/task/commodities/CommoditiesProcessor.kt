package starter.task.commodities

import screeps.api.DepositResourceConstant
import screeps.api.Game
import screeps.api.RESOURCE_FIXTURES
import screeps.api.RESOURCE_FRAME
import screeps.api.RESOURCE_HYDRAULICS
import screeps.api.RESOURCE_MACHINE
import screeps.api.RESOURCE_METAL
import screeps.api.RESOURCE_TUBE
import screeps.api.ResourceConstant
import screeps.utils.unsafe.jsObject
import starter.extension.factory
import starter.extension.myRooms
import starter.memory.FactoryReactionTask
import starter.memory.TaskMemory
import starter.memory.checkLinkedTasks
import starter.memory.createLinkedTask
import starter.memory.factoryReactionTask
import starter.memory.linkedTaskIds
import starter.memory.requestedAmount
import starter.memory.resource
import starter.memory.sleepUntil
import starter.task.TaskType
import kotlin.math.max

/**
 *
 *
 * @author zakharchuk
 * @since 30.04.2020
 */
class CommoditiesProcessor(val task: TaskMemory) {
    fun execute() {
        if (task.sleepUntil > Game.time) {
            return
        }

        task.checkLinkedTasks()
        if (task.linkedTaskIds.isNullOrEmpty()) {
            val resourceToProduce = getResourceToProduce(RESOURCE_METAL)
            if (resourceToProduce != null) {
                task.createLinkedTask(TaskType.PRODUCE_COMMODITIES, jsObject {
                    this.factoryReactionTask = jsObject<FactoryReactionTask> {
                        this.resource = resourceToProduce
                        this.requestedAmount = 8
                    }
                })
                return
            }
        }

        task.sleepUntil = Game.time + 100
    }

    private fun getResourceToProduce(resource: DepositResourceConstant): ResourceConstant? {
        var maxLevelFactory = 0
        Game.myRooms().forEach {
            maxLevelFactory = max(maxLevelFactory, it.factory()?.level ?: 0)
        }

        return getResourceByLevel(maxLevelFactory)
    }

    private fun getResourceByLevel(level: Int): ResourceConstant? {
        return when (level) {
            1 -> RESOURCE_TUBE
            2 -> RESOURCE_FIXTURES
            3 -> RESOURCE_FRAME
            4 -> RESOURCE_HYDRAULICS
            5 -> RESOURCE_MACHINE
            else -> null
        }
    }
}