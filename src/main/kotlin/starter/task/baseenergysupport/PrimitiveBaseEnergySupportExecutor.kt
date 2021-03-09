package starter.task.baseenergysupport

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
 * @since 04.03.2020
 */
class PrimitiveBaseEnergySupportExecutor(task: TaskMemory) : AbstractBaseEnergySupportExecutor(task) {
    override fun withdrawEnergy(creep: Creep) {
        withdraw(task, creep, ContainerType.PROVIDER, storageCapacityLimit = 0)
    }

    override fun getCreepBody(room: Room): Array<BodyPartConstant> {
        return room.calculatePrimitiveBody()
    }
}