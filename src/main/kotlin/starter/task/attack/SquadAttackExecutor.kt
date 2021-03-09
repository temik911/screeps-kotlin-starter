package starter.task.attack

import screeps.api.BodyPartConstant
import screeps.api.COSTMATRIX_FALSE
import screeps.api.CostMatrix
import screeps.api.Creep
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.FIND_EXIT
import screeps.api.FIND_HOSTILE_CREEPS
import screeps.api.FIND_HOSTILE_STRUCTURES
import screeps.api.FIND_MY_CREEPS
import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.LOOK_STRUCTURES
import screeps.api.PathFinder
import screeps.api.Room
import screeps.api.RoomPosition
import screeps.api.STRUCTURE_EXTRACTOR
import screeps.api.STRUCTURE_KEEPER_LAIR
import screeps.api.STRUCTURE_RAMPART
import screeps.api.STRUCTURE_ROAD
import screeps.api.STRUCTURE_SPAWN
import screeps.api.STRUCTURE_TOWER
import screeps.api.STRUCTURE_WALL
import screeps.api.get
import screeps.api.options
import screeps.api.structures.StructureTower
import starter.calculateAttackBody
import starter.memory.TaskMemory
import starter.memory.lastTimeDamage
import starter.memory.status
import starter.memory.toRoom
import starter.task.ExecutorWithExclusiveCreep

/**
 *
 *
 * @author zakharchuk
 * @since 06.03.2020
 */
open class SquadAttackExecutor(task: TaskMemory) : ExecutorWithExclusiveCreep(task) {

    override fun getCreepBody(room: Room): Array<BodyPartConstant> = room.calculateAttackBody()

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

                val hostileCreeps = creep.pos.findInRange(FIND_HOSTILE_CREEPS, 1)
                if (!hostileCreeps.isNullOrEmpty()) {
                    creep.attack(hostileCreeps[0])
                } else {
                    heal(creep)
                }

                if (creep.room.name == task.toRoom) {
                    val towers = creep.room.find(FIND_HOSTILE_STRUCTURES, options {
                        filter = {
                            it.structureType == screeps.api.STRUCTURE_TOWER && it.unsafeCast<StructureTower>().store.getUsedCapacity(screeps.api.RESOURCE_ENERGY) ?: 0 > 0
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
                if (creep.hits < creep.hitsMax * 0.9) {
                    task.status = "prepareWithdrawEnergy"
                    val position = creep.pos.findClosestByPath(FIND_EXIT)
                    if (position != null) {
                        creep.moveTo(position)
                    }
                    heal(creep)
                    return
                }

                val hostileCreeps = creep.pos.findInRange(FIND_HOSTILE_CREEPS, 1)
                if (!hostileCreeps.isNullOrEmpty()) {
                    creep.attack(hostileCreeps[0])
                    return
                }

                val find = creep.room.find(FIND_HOSTILE_STRUCTURES, options {
                    filter = {
                        it.structureType == STRUCTURE_TOWER && it.pos.lookFor(LOOK_STRUCTURES)?.firstOrNull { it.structureType == STRUCTURE_RAMPART } == null
                    }
                })

                if (!find.isNullOrEmpty()) {
                    val structure = find[0]
                    if (creep.attack(structure) == ERR_NOT_IN_RANGE) {
                        val path = PathFinder.search(creep.pos, structure.pos, options {
                            plainCost = 1
                            swampCost = 5
                            roomCallback = { roomName -> callback(roomName) }
                        })

                        val target = path.path.get(0)
                        creep.moveTo(target)
                        val lookFor = target.lookFor(LOOK_STRUCTURES)
                        if (!lookFor.isNullOrEmpty()) {
                            creep.attack(lookFor[0])
                        } else {
                            heal(creep)
                        }
                    }
                } else {
                    val target = creep.pos.findClosestByPath(FIND_HOSTILE_STRUCTURES, options {
                        filter = {
                            it.structureType == STRUCTURE_SPAWN
                        }
                    })
                    if (target != null) {
                        if (creep.attack(target) == ERR_NOT_IN_RANGE) {
                            creep.moveTo(target)
                            heal(creep)
                        }
                    } else {
                        val structure = creep.pos.findClosestByPath(FIND_STRUCTURES, options {
                            filter = {
                                it.structureType != screeps.api.STRUCTURE_CONTROLLER && it.structureType != STRUCTURE_KEEPER_LAIR &&
                                        (creep.room.controller != null || it.structureType != STRUCTURE_EXTRACTOR)
                            }
                        })
                        if (structure != null) {
                            if (creep.attack(structure) == ERR_NOT_IN_RANGE) {
                                creep.moveTo(structure)
                                heal(creep)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun moveToRoom(creep: Creep, toRoom: String): Boolean {
        val room = Game.rooms.get(toRoom)
        if (room == null) {
            creep.moveTo(RoomPosition(25, 25, toRoom))
        } else {
            if (creep.room.name == task.toRoom && creep.pos.x in 1..48 && creep.pos.y in 1..48) {
                return true
            } else {
                val controller = room.controller
                if (controller != null) {
                    creep.moveTo(controller, options {
                        reusePath = 0
                    })
                } else {
                    creep.moveTo(RoomPosition(25, 25, toRoom), options {
                        reusePath = 0
                    })
                }
            }
        }
        return false
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

    private fun callback(roomName: String): CostMatrix {
        val room = Game.rooms[roomName] ?: return COSTMATRIX_FALSE
        val costMatrix = PathFinder.CostMatrix()
        room.find(FIND_STRUCTURES).forEach { structure ->
            if (structure.structureType == STRUCTURE_ROAD) {
                costMatrix.set(structure.pos.x, structure.pos.y, 1)
            } else if (structure.structureType == STRUCTURE_WALL || structure.structureType == STRUCTURE_RAMPART) {
                if (structure.hits < 50000) {
                    costMatrix.set(structure.pos.x, structure.pos.y, 5)
                } else {
                    costMatrix.set(structure.pos.x, structure.pos.y, 50)
                }
            } else {
                costMatrix.set(structure.pos.x, structure.pos.y, 255)
            }
        }
        room.find(FIND_HOSTILE_STRUCTURES).forEach { structure ->
            if (structure.structureType == STRUCTURE_RAMPART) {
                if (structure.hitsMax < 50000) {
                    costMatrix.set(structure.pos.x, structure.pos.y, 5)
                } else {
                    costMatrix.set(structure.pos.x, structure.pos.y, 50)
                }
            } else {
                costMatrix.set(structure.pos.x, structure.pos.y, 255)
            }
        }
        room.find(FIND_MY_CREEPS).forEach { creep ->
            costMatrix.set(creep.pos.x, creep.pos.y, 255)
        }

        return costMatrix
    }
}