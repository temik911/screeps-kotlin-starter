package starter.task

import screeps.api.Creep
import screeps.api.ERR_FULL
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.Game
import screeps.api.LOOK_STRUCTURES
import screeps.api.RESOURCE_ENERGY
import screeps.api.STRUCTURE_ROAD
import screeps.api.WORK
import screeps.api.get
import screeps.api.structures.StructureContainer
import starter.Role
import starter.calculateTransfer
import starter.extension.moveToWithSwap
import starter.getUsed
import starter.memory.TaskMemory
import starter.memory.containerId
import starter.memory.creepId
import starter.memory.creepTtl
import starter.memory.isDone
import starter.memory.isFree
import starter.memory.resource
import starter.memory.room
import starter.memory.status
import starter.peekCreep2

/**
 *
 *
 * @author zakharchuk
 * @since 28.02.2020
 */
class TransferExecutor(val task: TaskMemory) {

    fun execute() {
        if (task.isDone) {
            return
        }

        if (task.creepId == "undefined") {
            val room = Game.rooms[task.room]!!
            val creep = peekCreep2(task, Role.TRANSFER, room.calculateTransfer(), timeOut = 50, ttl = task.creepTtl) ?: return
            creep.memory.isFree = false
            task.creepId = creep.id
        }

        val creep = Game.getObjectById<Creep>(task.creepId)

        if (creep == null) {
            task.isDone = true
            return
        }

        when (task.status) {
            "withdraw" -> {
                if (creep.store.getUsed(task.resource) > 0) {
                    task.status = "transfer"
                    return
                }

                val container = Game.getObjectById<StructureContainer>(task.containerId)
                if (container == null) {
                    task.isDone = true
                    creep.memory.isFree = true
                    return
                }
                if (creep.withdraw(container, task.resource) == ERR_NOT_IN_RANGE) {
                    creep.moveToWithSwap(container)
                }
            }
            "transfer" -> {
                if (creep.store.getUsed(task.resource) == 0) {
                    task.isDone = true
                    creep.memory.isFree = true
                    return
                }

                val room = Game.rooms.get(task.room)!!
                val storage = room.storage!!
                if (creep.store.getUsed(RESOURCE_ENERGY) > 0.5 * (creep.store.getCapacity(RESOURCE_ENERGY) ?: 0)
                        && creep.body.firstOrNull { part -> part.type == WORK && part.hits > 0 } != null) {
                    val lookFor = creep.pos.lookFor(LOOK_STRUCTURES)
                    if (!lookFor.isNullOrEmpty()) {
                        val roads = lookFor.filter {
                            it.structureType == STRUCTURE_ROAD
                        }
                        if (!roads.isNullOrEmpty()) {
                            val road = roads[0]
                            if (road.hits < road.hitsMax * 0.9) {
                                creep.repair(road)
                                return
                            }
                        }
                    }
                }
                when (creep.transfer(storage, task.resource)) {
                    ERR_NOT_IN_RANGE -> creep.moveToWithSwap(storage)
                    ERR_FULL -> task.status = "transferToTerminal"
                }
            }
            "transferToTerminal" -> {
                if (creep.store.getUsed(task.resource) == 0) {
                    task.isDone = true
                    creep.memory.isFree = true
                    return
                }

                val room = Game.rooms.get(task.room)!!
                val terminal = room.terminal
                if (terminal == null) {
                    task.status = "transfer"
                    return
                }

                when (creep.transfer(terminal, task.resource)) {
                    ERR_NOT_IN_RANGE -> creep.moveToWithSwap(terminal)
                    ERR_FULL -> task.status = "transfer"
                }
            }
        }
    }
}