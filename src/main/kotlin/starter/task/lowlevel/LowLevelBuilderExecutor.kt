package starter.task.lowlevel

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.FIND_MY_CONSTRUCTION_SITES
import screeps.api.Game
import screeps.api.RESOURCE_ENERGY
import screeps.api.Room
import screeps.api.get
import screeps.api.structures.StructureController
import starter.ContainerType
import starter.calculateBuilderBody
import starter.memory.TaskMemory
import starter.memory.status
import starter.memory.toRoom
import starter.task.ExecutorWithExclusiveCreep
import starter.walker.walkTo
import starter.withdraw

/**
 *
 *
 * @author zakharchuk
 * @since 01.03.2020
 */
class LowLevelBuilderExecutor(task: TaskMemory) : ExecutorWithExclusiveCreep(task) {

    override fun getCreepBody(room: Room): Array<BodyPartConstant> = room.calculateBuilderBody()

    override fun prepare(task: TaskMemory, creep: Creep): Boolean {
        if (task.status == "undefined") {
            task.status = "harvest"
        }

        val room = Game.rooms.get(task.toRoom) ?: return false
        val controller = room.controller ?: return false
        val ignoreRooms = listOf("E53S57", "E52S58")
        creep.walkTo(controller, ignoreRooms = ignoreRooms)
        return creep.room.name == room.name && creep.pos.x in 1..48 && creep.pos.y in 1..48
    }

    override fun executeInternal(creep: Creep) {
        when (task.status) {
            "harvest" -> {
                withdraw(task, creep, ContainerType.ALL)
                if (creep.store.getFreeCapacity(RESOURCE_ENERGY) ?: 0 == 0) {
                    task.status = "work"
                }
            }
            "work" -> {
                val constructionSite = creep.pos.findClosestByPath(FIND_MY_CONSTRUCTION_SITES)
                if (constructionSite != null) {
                    if (creep.build(constructionSite) == ERR_NOT_IN_RANGE) {
                        creep.moveTo(constructionSite)
                    }
                } else {
                    val controller: StructureController = creep.room.controller ?: return
                    if (creep.upgradeController(controller) == ERR_NOT_IN_RANGE) {
                        creep.moveTo(controller)
                    }
                }

                if (creep.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0 == 0) {
                    task.status = "harvest"
                }
            }
        }
    }
}