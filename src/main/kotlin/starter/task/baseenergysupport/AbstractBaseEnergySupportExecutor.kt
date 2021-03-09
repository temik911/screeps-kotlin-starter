package starter.task.baseenergysupport

import screeps.api.Creep
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.FIND_MY_STRUCTURES
import screeps.api.FIND_SOURCES
import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.IStructure
import screeps.api.Memory
import screeps.api.OK
import screeps.api.RESOURCE_ENERGY
import screeps.api.STRUCTURE_CONTAINER
import screeps.api.STRUCTURE_EXTENSION
import screeps.api.STRUCTURE_SPAWN
import screeps.api.STRUCTURE_TERMINAL
import screeps.api.STRUCTURE_TOWER
import screeps.api.StoreOwner
import screeps.api.get
import screeps.api.options
import screeps.api.structures.StructureContainer
import screeps.api.structures.StructureExtension
import screeps.api.structures.StructureTower
import starter.extension.moveToWithSwap
import starter.getUsed
import starter.memory.TaskMemory
import starter.memory.containers
import starter.memory.isProvider
import starter.memory.sleepUntil
import starter.memory.status
import starter.memory.targetId
import starter.task.ExecutorWithExclusiveCreep

/**
 *
 *
 * @author zakharchuk
 * @since 04.03.2020
 */
abstract class AbstractBaseEnergySupportExecutor(task: TaskMemory) : ExecutorWithExclusiveCreep(task) {

    override fun priority(): Boolean = true

    override fun prepare(task: TaskMemory, creep: Creep): Boolean {
        if (task.status == "undefined") {
            task.status = "transfer"
        }
        return true
    }

    override fun executeInternal(creep: Creep) {
        if (task.sleepUntil > Game.time) {
            return
        }

        when (task.status) {
            "transfer" -> {
                if (creep.store.getUsedCapacity() == 0) {
                    task.status = "withdraw"
                    return
                }

                if (task.targetId == "undefined") {
                    val targetId = getTargetId(creep)
                    if (targetId == null) {
                        val source = creep.pos.findInRange(creep.room.find(FIND_SOURCES), 4).firstOrNull()
                        if (source != null) {
                            creep.moveToWithSwap(creep.room.controller!!)
                        } else {
                            task.sleepUntil = Game.time + 10
                        }
                    } else {
                        task.targetId = targetId
                    }
                }

                if (task.targetId != "undefined") {
                    val target = Game.getObjectById<IStructure>(task.targetId)
                    if (target == null) {
                        task.targetId = "undefined"
                        return
                    }
                    when (creep.transfer(target, RESOURCE_ENERGY)) {
                        ERR_NOT_IN_RANGE -> creep.moveToWithSwap(target.pos)
                        OK -> task.targetId = "undefined"
                        else -> task.targetId = "undefined"
                    }
                }
            }
            "withdraw" -> {
                if (creep.store.getFreeCapacity(RESOURCE_ENERGY) ?: 0 == 0) {
                    task.status = "transfer"
                    return
                }

                withdrawEnergy(creep)
            }
        }
    }

    abstract fun withdrawEnergy(creep: Creep)

    private fun getTargetId(creep: Creep): String? {
        val spawnOrExtension = creep.pos.findClosestByPath(FIND_MY_STRUCTURES, options {
            ignoreCreeps = true
            filter = {
                (it.structureType == STRUCTURE_EXTENSION || it.structureType == STRUCTURE_SPAWN)
                        && it.unsafeCast<StructureExtension>().store.getFreeCapacity(RESOURCE_ENERGY)!! > 0
            }
        })
        if (spawnOrExtension != null) {
            return spawnOrExtension.id
        }

        val tower = creep.room.find(FIND_MY_STRUCTURES, options {
            filter = {
                it.structureType == STRUCTURE_TOWER && it.unsafeCast<StructureTower>().store.getFreeCapacity(RESOURCE_ENERGY) ?: 0 > 0
            }
        }).minBy { it.unsafeCast<StructureTower>().store.getUsed(RESOURCE_ENERGY) }
        if (tower != null) {
            return tower.id
        }

        val containers = creep.room.find(FIND_STRUCTURES, options {
            filter = {
                it.structureType == STRUCTURE_CONTAINER
                        && it.unsafeCast<StructureContainer>().store.getFreeCapacity(RESOURCE_ENERGY) ?: 0 > ((it.unsafeCast<StructureContainer>().store.getCapacity(RESOURCE_ENERGY)
                        ?: 0) / 2)
                        && it.unsafeCast<StructureContainer>().store.getFreeCapacity(RESOURCE_ENERGY) ?: 0 > creep.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0
                        && if (Memory.containers[it.id] != null) !Memory.containers[it.id]!!.isProvider else true
            }
        })

        if (containers.isNotEmpty()) {
            return containers[0].id
        }

        val terminal = creep.room.find(FIND_STRUCTURES, options {
            filter = {
                it.structureType == STRUCTURE_TERMINAL && it.unsafeCast<StoreOwner>().store.getUsedCapacity(RESOURCE_ENERGY) ?: 0 < 25000
            }
        }).firstOrNull()
        return terminal?.id
    }
}