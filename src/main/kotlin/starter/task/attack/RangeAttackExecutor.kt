package starter.task.attack

import screeps.api.COSTMATRIX_FALSE
import screeps.api.CostMatrix
import screeps.api.Creep
import screeps.api.FIND_EXIT
import screeps.api.FIND_HOSTILE_CREEPS
import screeps.api.FIND_HOSTILE_STRUCTURES
import screeps.api.FIND_MY_CREEPS
import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.LOOK_STRUCTURES
import screeps.api.PathFinder
import screeps.api.RANGED_ATTACK
import screeps.api.RESOURCE_ENERGY
import screeps.api.Room
import screeps.api.RoomPosition
import screeps.api.STRUCTURE_CONTROLLER
import screeps.api.STRUCTURE_RAMPART
import screeps.api.STRUCTURE_ROAD
import screeps.api.STRUCTURE_TOWER
import screeps.api.STRUCTURE_WALL
import screeps.api.get
import screeps.api.options
import screeps.api.structures.StructureTower
import starter.calculateRangeAttackBody
import starter.memory.TaskMemory
import starter.memory.lastTimeDamage
import starter.memory.status
import starter.memory.targetPosition
import starter.memory.toRoom
import starter.task.ExecutorWithExclusiveCreep
import starter.walker.walkTo

/**
 *
 *
 * @author zakharchuk
 * @since 09.05.2020
 */
open class RangeAttackExecutor(task: TaskMemory): ExecutorWithExclusiveCreep(task) {

    override fun getCreepBody(room: Room) = room.calculateRangeAttackBody()

    override fun prepare(task: TaskMemory, creep: Creep): Boolean {
        if (task.status == "undefined") {
            task.status = "prepareWithdrawEnergy"
        }
        return true
    }

    override fun executeInternal(creep: Creep) {
        when (task.status) {
            "prepareWithdrawEnergy" -> {
                if (creep.pos.roomName == task.toRoom) {
                    val position = creep.pos.findClosestByPath(FIND_EXIT)
                    if (position != null) {
                        creep.moveTo(position)
                    }
                } else if (!(creep.pos.x in 1..48 && creep.pos.y in 1..48)) {
                    val controller = creep.room.controller
                    if (controller != null) {
                        creep.moveTo(controller)
                    } else {
                        creep.moveTo(creep.room.getPositionAt(25, 25)!!)
                    }
                }

                if (creep.hits == creep.hitsMax) {
                    task.lastTimeDamage = Game.time
                    task.status = "withdrawEnergy"
                }
                heal(creep)
            }
            "withdrawEnergy" -> {
                if (!moveToRoom(creep, task.toRoom)) {
                    task.lastTimeDamage = Game.time
                    heal(creep)
                    return
                }

                if (creep.hits < creep.hitsMax * 0.75) {
                    task.status = "prepareWithdrawEnergy"
                    val position = creep.pos.findClosestByPath(FIND_EXIT)
                    if (position != null) {
                        creep.moveTo(position)
                    }
                    heal(creep)
                    return
                }

                creep.rangedMassAttack()
                heal(creep)

                if (creep.room.name == task.toRoom) {
                    val towers = creep.room.find(FIND_HOSTILE_STRUCTURES, options {
                        filter = {
                            it.structureType == STRUCTURE_TOWER && it.unsafeCast<StructureTower>().store.getUsedCapacity(RESOURCE_ENERGY) ?: 0 > 0
                        }
                    })

                    if (towers.isNullOrEmpty()) {
                        task.status = "attack"
                    }

                    if (creep.hits < creep.hitsMax) {
                        task.lastTimeDamage = Game.time
                    }

                    if (task.lastTimeDamage + 10 < Game.time) {
                        task.status = "attack"
                    }

                }
            }
            "attack" -> {
                if (creep.hits < creep.hitsMax * 0.95) {
                    task.status = "prepareWithdrawEnergy"
                    val position = creep.pos.findClosestByPath(FIND_EXIT)
                    if (position != null) {
                        creep.moveTo(position)
                    }
                    heal(creep)
                    return
                }

                creep.rangedMassAttack()
                heal(creep)

                val find = creep.room.find(FIND_HOSTILE_STRUCTURES, options {
                    filter = {
                        it.structureType == STRUCTURE_TOWER && it.pos.lookFor(LOOK_STRUCTURES)?.firstOrNull { it.structureType == STRUCTURE_RAMPART } == null
                    }
                })

                if (!find.isNullOrEmpty()) {
                    val structure = find[0]
                    val path = PathFinder.search(creep.pos, structure.pos, options {
                        plainCost = 1
                        swampCost = 5
                        roomCallback = { roomName -> callback(roomName, creep) }
                    })

                    val target = path.path.get(0)
                    val lookFor = target.lookFor(LOOK_STRUCTURES)?.firstOrNull { it.structureType == STRUCTURE_WALL }
                    if (lookFor != null) {
                        creep.cancelOrder("rangedMassAttack")
                        creep.rangedAttack(lookFor)
                    }
                    creep.moveTo(target)
                } else {
                    val findSpawns = creep.room.find(FIND_HOSTILE_STRUCTURES, options {
                        filter = {
                            it.structureType == screeps.api.STRUCTURE_SPAWN
                        }
                    })
                    if (!findSpawns.isNullOrEmpty()) {
                        creep.moveTo(findSpawns[0])
                    } else {
                        val otherStructures = creep.pos.findClosestByPath(FIND_HOSTILE_STRUCTURES, options {
                            filter = {
                                it.structureType != STRUCTURE_CONTROLLER
                                        && it.pos.lookFor(LOOK_STRUCTURES)?.firstOrNull { it.structureType == STRUCTURE_RAMPART } == null
                            }
                        })
                        if (otherStructures != null) {
                            creep.moveTo(otherStructures)
                        }
                    }
                }
            }
        }
    }

    private fun moveToRoom(creep: Creep, toRoom: String): Boolean {
        val room = Game.rooms.get(toRoom)
        if (room == null) {
//            creep.walkTo(RoomPosition(25, 25, toRoom), false, listOf("E53S57"))
//            creep.walkTo(RoomPosition(25, 25, "E53S58"), false, emptyList())
            creep.moveTo(RoomPosition(25, 25, toRoom))
        } else {
            if (task.targetPosition == null) {
                task.targetPosition = room.controller?.pos
            }
            if (creep.room.name == task.toRoom && creep.pos.x in 1..48 && creep.pos.y in 1..48) {
                return true
            } else {
                if (task.targetPosition != null) {
                    val position = task.targetPosition!!
                    val targetPosition = RoomPosition(position.x, position.y, position.roomName)
                    creep.moveTo(targetPosition, options { maxRooms = 1 })
                } else {
                    creep.moveTo(RoomPosition(25, 25, task.toRoom), options { maxRooms = 1 })
                }
            }
        }
        return false
    }

    private fun rangeAttack(creep: Creep) {
        val inRange = creep.pos.findInRange(FIND_HOSTILE_CREEPS, 3)
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
    }

    private fun heal(attackCreep: Creep) {
        if (attackCreep.hits < attackCreep.hitsMax) {
            attackCreep.heal(attackCreep)
        } else {
            val myCreeps = attackCreep.pos.findInRange(FIND_MY_CREEPS, 3, options {
                filter = {
                    it.hits < it.hitsMax
                }
            })
            if (!myCreeps.isNullOrEmpty()) {
                val target = myCreeps.minBy { it.hits }
                if (target != null) {
                    if (target.pos.isNearTo(attackCreep.pos)) {
                        attackCreep.heal(target)
                    } else {
                        attackCreep.rangedHeal(target)
                    }
                }
            } else {
                attackCreep.heal(attackCreep)
            }
        }
    }

    private fun callback(roomName: String, creep: Creep): CostMatrix {
        val room = Game.rooms[roomName] ?: return COSTMATRIX_FALSE
        val costMatrix = PathFinder.CostMatrix()

        val maxHits = room.find(FIND_STRUCTURES).maxBy { if (it.structureType == STRUCTURE_WALL || it.structureType == STRUCTURE_RAMPART) it.hits else 0 }?.hits ?: 10000

        room.find(FIND_STRUCTURES).forEach { structure ->
            if (structure.structureType == STRUCTURE_ROAD) {
                costMatrix.set(structure.pos.x, structure.pos.y, 1)
            } else if (structure.structureType == STRUCTURE_WALL || structure.structureType == STRUCTURE_RAMPART) {
                val percent = structure.hits * 1.0 / maxHits
                val value = (255 * percent).toInt()
                costMatrix.set(structure.pos.x, structure.pos.y, value)
            } else {
                costMatrix.set(structure.pos.x, structure.pos.y, 1)
            }
        }
        room.find(FIND_HOSTILE_STRUCTURES).forEach { structure ->
            if (structure.structureType == STRUCTURE_RAMPART) {
                val percent = structure.hits * 1.0 / maxHits
                val value = (255 * percent).toInt()
                costMatrix.set(structure.pos.x, structure.pos.y, value)
            } else {
                costMatrix.set(structure.pos.x, structure.pos.y, 1)
            }
        }
        room.find(FIND_MY_CREEPS).forEach { creep ->
            costMatrix.set(creep.pos.x, creep.pos.y, 255)
        }

        return costMatrix
    }
}