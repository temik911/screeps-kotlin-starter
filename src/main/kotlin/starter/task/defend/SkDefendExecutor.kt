package starter.task.defend

import screeps.api.ATTACK
import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.FIND_HOSTILE_CREEPS
import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.Room
import screeps.api.RoomPosition
import screeps.api.STRUCTURE_KEEPER_LAIR
import screeps.api.options
import screeps.api.structures.StructureKeeperLair
import starter.calculateDefendSkRoom
import starter.extension.moveToWithSwap
import starter.memory.TaskMemory
import starter.memory.creepId
import starter.memory.toRoom
import starter.task.ExecutorWithExclusiveCreep

/**
 *
 *
 * @author zakharchuk
 * @since 04.04.2020
 */
class SkDefendExecutor(task: TaskMemory) : ExecutorWithExclusiveCreep(task) {

    override fun getCreepBody(room: Room): Array<BodyPartConstant> = room.calculateDefendSkRoom()

    override fun prepare(task: TaskMemory, creep: Creep): Boolean {
        creep.moveToWithSwap(RoomPosition(25, 25, task.toRoom))
        return creep.room.name == task.toRoom && creep.pos.x in 1..48 && creep.pos.y in 1..48
    }

    override fun executeInternal(creep: Creep) {
        if (creep.room.name != task.toRoom) {
            creep.moveToWithSwap(RoomPosition(25, 25, task.toRoom))
            return
        }

        val hostileCreep = creep.pos.findClosestByPath(FIND_HOSTILE_CREEPS, options {
            filter = {
                it.owner.username == "Source Keeper"
            }
        })

        if (hostileCreep != null) {
            val rangeTo = creep.pos.getRangeTo(hostileCreep.pos)
            if (rangeTo in 5..7 && creep.hits < creep.hitsMax) {
                heal(creep)
                return
            }

            if (creep.getActiveBodyparts(ATTACK) != 0) {
                if (creep.attack(hostileCreep) == ERR_NOT_IN_RANGE) {
                    creep.moveToWithSwap(hostileCreep, maxRooms = 1)
                    heal(creep)
                }
            } else {
                heal(creep)
            }
        } else {
            val keeperLair = creep.room.find(FIND_STRUCTURES, options {
                filter = {
                    it.structureType == STRUCTURE_KEEPER_LAIR && it.unsafeCast<StructureKeeperLair>().ticksToSpawn != null
                }
            }).minBy { it.unsafeCast<StructureKeeperLair>().ticksToSpawn!! }
            if (keeperLair == null) {
                heal(creep)
            } else {
                val rangeTo = creep.pos.getRangeTo(keeperLair.pos)
                if (rangeTo in 5..7 && creep.hits < creep.hitsMax) {
                    heal(creep)
                    return
                }
                creep.moveToWithSwap(keeperLair, maxRooms = 1)
                heal(creep)
            }
        }
    }

    private fun heal(creep: Creep) {
        if (creep.hits < creep.hitsMax) {
            creep.heal(creep)
        }
    }

    fun isNewTaskNeeded(): Boolean {
        if (task.creepId == "undefined") {
            return false
        }
        val creep = Game.getObjectById<Creep>(task.creepId) ?: return true
        return creep.ticksToLive <= 300
    }
}