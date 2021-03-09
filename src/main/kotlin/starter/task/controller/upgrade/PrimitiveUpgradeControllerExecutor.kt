package starter.task.controller.upgrade

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.Room
import starter.ContainerType
import starter.calculatePrimitiveBody
import starter.memory.TaskMemory
import starter.withdraw

/**
 *
 *
 * @author zakharchuk
 * @since 03.03.2020
 */
class PrimitiveUpgradeControllerExecutor(task: TaskMemory) : AbstractUpgradeControllerExecutor(task) {
    override fun withdrawEnergy(creep: Creep) {
        withdraw(task, creep, ContainerType.REQUESTER)
    }

    override fun getCreepBody(room: Room): Array<BodyPartConstant> {
        return room.calculatePrimitiveBody()
    }
}