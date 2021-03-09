package starter.task.controller.upgrade

import screeps.api.Creep
import screeps.api.Memory
import screeps.api.RESOURCE_CATALYZED_GHODIUM_ACID
import screeps.api.WORK
import screeps.api.values
import screeps.utils.unsafe.jsObject
import starter.UniqueIdGenerator
import starter.createTask
import starter.memory.BoostCreepTask
import starter.memory.TaskMemory
import starter.memory.amount
import starter.memory.boost
import starter.memory.boostCreepTask
import starter.memory.creepId
import starter.memory.externalTaskId
import starter.memory.internalTaskId
import starter.memory.room
import starter.memory.tasks
import starter.memory.type
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 31.03.2020
 */
class BoostedLinkUpgradeControllerExecutor(task: TaskMemory) : LinkUpgradeControllerExecutor(task) {
    override fun prepare(task: TaskMemory, creep: Creep): Boolean {
        val prepare = super.prepare(task, creep)
        if (!prepare) {
            return false
        }

        if (task.internalTaskId != null) {
            return Memory.tasks.values.filter { memory -> memory.externalTaskId == task.internalTaskId }.isNullOrEmpty()
        }

        val boostCreepTask = jsObject<BoostCreepTask> {
            this.creepId = creep.id
            this.boost = RESOURCE_CATALYZED_GHODIUM_ACID
            this.amount = creep.body.sumBy { if (it.type == WORK) 1 else 0 }
        }

        val uniqueId = UniqueIdGenerator.generateUniqueId()
        createTask("${task.room}|${TaskType.BOOST_CREEP.code}", jsObject {
            this.type = TaskType.BOOST_CREEP.code
            this.room = task.room
            this.externalTaskId = uniqueId
            this.boostCreepTask = boostCreepTask
        })
        task.internalTaskId = uniqueId

        return false
    }
}