package starter.task.defend

import screeps.api.ATTACK
import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.FIND_MY_CREEPS
import screeps.api.Game
import screeps.api.Room
import screeps.api.RoomPosition
import screeps.api.get
import screeps.api.options
import starter.calculateHealSkRoom
import starter.extension.moveToWithSwap
import starter.memory.TaskMemory
import starter.memory.room
import starter.memory.toRoom
import starter.task.ExecutorWithExclusiveCreep

/**
 *
 *
 * @author zakharchuk
 * @since 05.04.2020
 */
class SkHealExecutor(task: TaskMemory) : ExecutorWithExclusiveCreep(task) {

    override fun getCreepBody(room: Room): Array<BodyPartConstant> = room.calculateHealSkRoom()

    override fun prepare(task: TaskMemory, creep: Creep): Boolean {
        creep.moveToWithSwap(RoomPosition(25, 25, task.toRoom))
        return creep.room.name == task.toRoom && creep.pos.x in 1..48 && creep.pos.y in 1..48
    }

    override fun executeInternal(creep: Creep) {
        if (creep.room.name != task.toRoom) {
            creep.moveToWithSwap(RoomPosition(25, 25, task.toRoom))
            return
        }

        if (creep.hits < creep.hitsMax) {
            val room = Game.rooms.get(task.room)!!
            creep.moveTo(room.storage!!, options {
                maxRooms = 1
            })
            creep.heal(creep)
            return
        }

        val healedCreep = creep.pos.findClosestByPath(FIND_MY_CREEPS, options {
            filter = {
                it.hits < it.hitsMax && it.getActiveBodyparts(ATTACK) == 0
            }
        })

        if (healedCreep != null) {
            if (!creep.pos.isNearTo(healedCreep)) {
                creep.rangedHeal(healedCreep)
                creep.moveTo(healedCreep, options {
                    maxRooms = 1
                })
            } else {
                creep.heal(healedCreep)
            }
        } else {
            val attackCreep = creep.pos.findClosestByPath(FIND_MY_CREEPS, options {
                filter = {
                    it.getActiveBodyparts(ATTACK) != 0 && it.hits < it.hitsMax
                }
            }) ?: return
            if (!creep.pos.isNearTo(attackCreep)) {
                creep.rangedHeal(attackCreep)
                creep.moveTo(attackCreep, options {
                    maxRooms = 1
                })
            } else {
                creep.heal(attackCreep)
            }

        }
    }
}