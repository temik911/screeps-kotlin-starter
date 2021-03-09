package starter.task.controller.upgrade

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.Game
import screeps.api.RESOURCE_ENERGY
import screeps.api.Room
import screeps.api.structures.StructureLink
import starter.calculateLinkUpgradeBody
import starter.extension.moveToWithSwap
import starter.memory.TaskMemory
import starter.memory.linkId

/**
 *
 *
 * @author zakharchuk
 * @since 29.02.2020
 */
open class LinkUpgradeControllerExecutor(task: TaskMemory) : AbstractUpgradeControllerExecutor(task) {
    override fun withdrawEnergy(creep: Creep) {
        val link = Game.getObjectById<StructureLink>(task.linkId) ?: return
        if (creep.withdraw(link, RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
            creep.moveToWithSwap(link)
        }
    }

    override fun getCreepBody(room: Room): Array<BodyPartConstant> {
        return room.calculateLinkUpgradeBody()
    }
}