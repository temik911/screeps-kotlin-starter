package starter.task.controller.upgrade

import screeps.api.Creep
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.RESOURCE_ENERGY
import starter.memory.TaskMemory
import starter.memory.status
import starter.task.ExecutorWithExclusiveCreep

/**
 *
 *
 * @author zakharchuk
 * @since 29.02.2020
 */
abstract class AbstractUpgradeControllerExecutor(task: TaskMemory): ExecutorWithExclusiveCreep(task) {

    override fun executeInternal(creep: Creep) {
        when (task.status) {
            "upgrade" -> {
                val controller = creep.room.controller ?: return

                if (creep.upgradeController(controller) == ERR_NOT_IN_RANGE) {
                    creep.moveTo(controller.pos)
                }

                if (creep.store.getUsedCapacity() == 0) {
                    task.status = "withdraw"
                }
            }
            "withdraw" -> {
                withdrawEnergy(creep)
                if (creep.store.getFreeCapacity(RESOURCE_ENERGY) ?: 0 == 0) {
                    task.status = "upgrade"
                }
            }
        }
    }

    override fun prepare(task: TaskMemory, creep: Creep): Boolean {
        if (task.status == "undefined") {
            task.status = "withdraw"
        }
        return true
    }

    abstract fun withdrawEnergy(creep: Creep)

}