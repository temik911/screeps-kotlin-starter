package starter.task.harvest

import screeps.api.BodyPartConstant
import screeps.api.CARRY
import screeps.api.Creep
import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.LOOK_STRUCTURES
import screeps.api.Memory
import screeps.api.Mineral
import screeps.api.RESOURCE_ENERGY
import screeps.api.Room
import screeps.api.RoomPosition
import screeps.api.STRUCTURE_CONTAINER
import screeps.api.STRUCTURE_EXTRACTOR
import screeps.api.WORK
import screeps.api.get
import screeps.api.options
import screeps.api.set
import screeps.api.structures.Structure
import screeps.api.structures.StructureContainer
import screeps.api.structures.StructureExtractor
import screeps.api.values
import screeps.utils.unsafe.jsObject
import starter.calculateMineralHarvestBody
import starter.calculateTransfer
import starter.createTask
import starter.memory.TaskMemory
import starter.memory.containerId
import starter.memory.containers
import starter.memory.creepTtl
import starter.memory.extractorId
import starter.memory.getSourceMemory
import starter.memory.isProvider
import starter.memory.resource
import starter.memory.room
import starter.memory.status
import starter.memory.tasks
import starter.memory.ticks
import starter.memory.times
import starter.memory.toRoom
import starter.memory.type
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 06.03.2020
 */
open class MineralHarvestExecutor(task: TaskMemory): AbstractHarvestExecutor<Mineral>(task) {
    override fun harvest(creep: Creep, source: Mineral) {
        if (task.containerId == "undefined") {
            val findContainers = findContainer(source.pos)
            if (findContainers.isNullOrEmpty()) {
                // ищем контейнер
                return
            } else {
                val id = findContainers[0].id
                task.containerId = id
                if (Memory.containers.get(id) == null) {
                    Memory.containers.set(id, jsObject {
                        isProvider = true
                    })
                }
            }
        }

        if (task.extractorId == "undefined") {
            val structures = source.pos.lookFor(LOOK_STRUCTURES)

            if (structures.isNullOrEmpty()) {
                return
            } else {
                val extractor = structures.filter { structure -> structure.structureType == STRUCTURE_EXTRACTOR }.firstOrNull()
                if (extractor == null) {
                    return
                } else {
                    task.extractorId = extractor.id
                }
            }
        }

        val container = Game.getObjectById<StructureContainer>(task.containerId) ?: return
        if (!creep.pos.isEqualTo(container.pos)) {
            creep.moveTo(container.pos)
            return
        }

        val extractor = Game.getObjectById<StructureExtractor>(task.extractorId) ?: return
        if (extractor.cooldown != 0) {
            return
        }

        if (container.store.getUsedCapacity(RESOURCE_ENERGY)?:0 > 0) {
            val createdTasks = Memory.tasks.values.filter {
                it.type == TaskType.TRANSFER.code && it.containerId == container.id
                        && it.status == "withdraw" && it.resource == RESOURCE_ENERGY
            }.size
            if (createdTasks < 1) {
                createTask("${task.room}|${(if (task.toRoom != "undefined") task.toRoom else task.room)}|${TaskType.TRANSFER.code}", jsObject {
                    type = TaskType.TRANSFER.code
                    this.room = task.room
                    containerId = container.id
                    status = "withdraw"
                    resource = RESOURCE_ENERGY
                })
            }
        }

        when (task.status) {
            "harvest" -> {
                val harvest = creep.body.sumBy { if (it.type == WORK) 2 else 0 }

                if (container.store.getFreeCapacity(source.mineralType) ?: 0 >= harvest) {
                    creep.harvest(source)
                }

                val room = Game.rooms.get(task.room)!!
                val carryCreep = room.calculateTransfer().sumBy { if (it == CARRY) 50 else 0 }
                val createdTasks = Memory.tasks.values.filter {
                    it.type == TaskType.TRANSFER.code && it.containerId == container.id
                            && it.status == "withdraw"
                }.size
                val alreadyToTransfer = carryCreep * createdTasks
                val sourceMemory = getSourceMemory(source.id)
                val timeToHarvest = if (sourceMemory.times == 0) 0 else (sourceMemory.ticks / sourceMemory.times / 5)
                val store = if ((container.store.getUsedCapacity(source.mineralType) ?: 0) + harvest * timeToHarvest >= 2000) {
                    2000
                } else {
                    (container.store.getUsedCapacity(source.mineralType) ?: 0) + harvest * timeToHarvest
                }
                if (store - alreadyToTransfer > carryCreep) {
                    createTask("${task.room}|${(if (task.toRoom != "undefined") task.toRoom else task.room)}|${TaskType.TRANSFER.code}", jsObject {
                        type = TaskType.TRANSFER.code
                        this.room = task.room
                        containerId = container.id
                        status = "withdraw"
                        creepTtl = if (sourceMemory.times == 0) 0 else (3 * sourceMemory.ticks / sourceMemory.times)
                        resource = source.mineralType
                    })
                }
            }
        }
    }

    private fun findContainer(pos: RoomPosition): Array<Structure> {
        return pos.findInRange(FIND_STRUCTURES, 1, options {
            filter = {
                it.structureType == STRUCTURE_CONTAINER
            }
        })
    }

    override fun getCreepBody(room: Room): Array<BodyPartConstant> {
        return room.calculateMineralHarvestBody()
    }

}