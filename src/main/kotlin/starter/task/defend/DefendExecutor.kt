package starter.task.defend

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.FIND_HOSTILE_CREEPS
import screeps.api.FIND_MY_CREEPS
import screeps.api.RANGED_ATTACK
import screeps.api.Room
import screeps.api.RoomPosition
import screeps.api.options
import starter.calculateDefendExternalRoom
import starter.memory.TaskMemory
import starter.memory.toRoom
import starter.task.ExecutorWithExclusiveCreep

/**
 *
 *
 * @author zakharchuk
 * @since 29.02.2020
 */
class DefendExecutor(task: TaskMemory): ExecutorWithExclusiveCreep(task) {

    override fun getCreepBody(room: Room): Array<BodyPartConstant> = room.calculateDefendExternalRoom(task.toRoom)

    override fun prepare(task: TaskMemory, creep: Creep): Boolean {
        creep.moveTo(RoomPosition(25, 25, task.toRoom))
        return creep.room.name == task.toRoom && creep.pos.x in 1..48 && creep.pos.y in 1..48
    }

    override fun executeInternal(creep: Creep) {
        if (creep.room.name != task.toRoom) {
            creep.moveTo(RoomPosition(25, 25, task.toRoom))
            if (creep.room.name != task.toRoom || creep.pos.x !in 1..48 || creep.pos.y !in 1..48) {
                return
            }
        }

        val hostileCreeps = creep.room.find(FIND_HOSTILE_CREEPS, options {
            filter = {
                it.owner.username == "Invader"
            }
        })
        if (!hostileCreeps.isNullOrEmpty()) {
            val inRange = creep.pos.findInRange(FIND_HOSTILE_CREEPS, 3, options {
                filter = {
                    it.owner.username == "Invader"
                }
            })
            val massDamage = inRange.sumBy {
                when (it.pos.getRangeTo(creep)) {
                    1 -> 10
                    2 -> 4
                    3 -> 1
                    else -> 0
                }
            }
            if (massDamage > 10) {
                creep.rangedMassAttack()
            } else {
                val toAttack = inRange.maxBy { it.getActiveBodyparts(RANGED_ATTACK) }
                if (toAttack != null) {
                    creep.rangedAttack(toAttack)
                }
            }

            val toMove = hostileCreeps.minBy { it.pos.getRangeTo(creep) }
            if (toMove != null) {
                creep.moveTo(toMove, options { maxRooms = 1 })
            }
        }

        if (creep.hits < creep.hitsMax) {
            creep.heal(creep)
        } else if (hostileCreeps.isNullOrEmpty()) {
            val creeps = creep.room.find(FIND_MY_CREEPS, options {
                filter = { it.hits < it.hitsMax }
            })
            if (!creeps.isNullOrEmpty()) {
                val target = creep.pos.findClosestByPath(FIND_MY_CREEPS, options {
                    filter = { it.hits < it.hitsMax }
                }) ?: return
                if (creep.heal(target) == ERR_NOT_IN_RANGE) {
                    creep.moveTo(target)
                }
                return
            }
        }
    }
}