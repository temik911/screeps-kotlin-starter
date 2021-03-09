package starter.task.defend

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.Room
import screeps.api.RoomPosition
import screeps.api.STRUCTURE_INVADER_CORE
import screeps.api.get
import screeps.api.options
import screeps.api.structures.StructureInvaderCore
import starter.calculateInvaderCoreDestroyerBody
import starter.extension.moveToWithSwap
import starter.memory.TaskMemory
import starter.memory.targetId
import starter.memory.toRoom
import starter.task.ExecutorWithExclusiveCreep

/**
 *
 *
 * @author zakharchuk
 * @since 31.03.2020
 */
class InvaderCoreDestroyerExecutor(task: TaskMemory): ExecutorWithExclusiveCreep(task) {
    override fun getCreepBody(room: Room): Array<BodyPartConstant> = room.calculateInvaderCoreDestroyerBody()

    override fun prepare(task: TaskMemory, creep: Creep): Boolean {
        if (task.targetId == "undefined") {
            val toRoom = Game.rooms.get(task.toRoom)
            if (toRoom != null) {
                val invaderCore = toRoom.find(FIND_STRUCTURES, options {
                    filter = {
                        it.structureType == STRUCTURE_INVADER_CORE
                    }
                }).firstOrNull()
                if (invaderCore != null) {
                    task.targetId = invaderCore.id
                }
            }
        }
        val invaderCore = Game.getObjectById<StructureInvaderCore>(task.targetId)
        if (invaderCore != null) {
            creep.moveToWithSwap(invaderCore)
        } else {
            creep.moveToWithSwap(RoomPosition(25, 25, task.toRoom))
        }
        return creep.room.name == task.toRoom && creep.pos.x in 1..48 && creep.pos.y in 1..48 && task.targetId != "undefined"
    }

    override fun executeInternal(creep: Creep) {
        val invaderCore = Game.getObjectById<StructureInvaderCore>(task.targetId)
        if (invaderCore != null) {
            if (creep.hits < creep.hitsMax) {
                creep.heal(creep)
            } else {
                val attack = creep.attack(invaderCore)
                if (attack == ERR_NOT_IN_RANGE) {
                    creep.moveToWithSwap(invaderCore)
                }
            }
        }
    }
}