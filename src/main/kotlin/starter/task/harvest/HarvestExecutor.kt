package starter.task.harvest

import screeps.api.BodyPartConstant
import screeps.api.CARRY
import screeps.api.Creep
import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.Memory
import screeps.api.RESOURCE_ENERGY
import screeps.api.Room
import screeps.api.RoomPosition
import screeps.api.STRUCTURE_CONTAINER
import screeps.api.Source
import screeps.api.WORK
import screeps.api.get
import screeps.api.options
import screeps.api.set
import screeps.api.structures.Structure
import screeps.api.structures.StructureContainer
import screeps.utils.unsafe.jsObject
import starter.calculateHarvestBody
import starter.calculateTransfer
import starter.createTask
import starter.memory.ContainerMemory
import starter.memory.TaskMemory
import starter.memory.containerId
import starter.memory.containers
import starter.memory.creepTtl
import starter.memory.getSourceMemory
import starter.memory.isDone
import starter.memory.isProvider
import starter.memory.linkedTaskIds
import starter.memory.resource
import starter.memory.room
import starter.memory.status
import starter.memory.tasks
import starter.memory.ticks
import starter.memory.times
import starter.memory.toRoom
import starter.memory.type
import starter.task.TaskType
import kotlin.math.roundToInt

/**
 *
 *
 * @author zakharchuk
 * @since 04.03.2020
 */
open class HarvestExecutor(task: TaskMemory) : AbstractHarvestExecutor<Source>(task) {
    override fun harvest(creep: Creep, source: Source) {
        if (task.containerId == "undefined") {
            val findContainers = findContainer(source.pos)
            if (findContainers.isNullOrEmpty()) {
                // ищем контейнер
                return
            } else {
                val id = findContainers[0].id
                task.containerId = id
                if (Memory.containers[id] == null) {
                    Memory.containers[id] = jsObject { isProvider = true }
                }
            }
        }

        val container = Game.getObjectById<StructureContainer>(task.containerId) ?: return
        if (!creep.pos.isEqualTo(container.pos)) {
            creep.moveTo(container.pos)
            return
        }

        when (task.status) {
            "repair" -> {
                if (creep.store.getUsedCapacity() < creep.body.sumBy { if (it.type == WORK) 1 else 0 }) {
                    creep.harvest(source)
                } else {
                    creep.repair(container)
                }
                if (container.hits > container.hitsMax * 0.9) {
                    task.status = "harvest"
                    return
                }
            }
            "harvest" -> {
                if (container.store.getFreeCapacity() > 0) {
                    creep.harvest(source)
                } else {
                    task.status = "repair"
                }

                if (source.energy == 0 || container.hits < container.hitsMax * 0.75) {
                    task.status = "repair"
                }

                val room = Game.rooms.get(task.room)!!
                val carryCreep = room.calculateTransfer().sumBy { if (it == CARRY) 50 else 0 }
                val containerMemory = Memory.containers[container.id]!!
                checkLinkedTasks(containerMemory)
                val createdTasks = containerMemory.linkedTaskIds.map { Memory.tasks[it]!! }
                        .count {
                            it.type == TaskType.TRANSFER.code && it.containerId == container.id && it.status == "withdraw"
                        }

                if (createdTasks > 2) {
                    return
                }

                val alreadyToTransfer = carryCreep * createdTasks
                val harvest = creep.body.sumBy { if (it.type == WORK) 2 else 0 }
                val sourceMemory = getSourceMemory(source.id)
                val timeToHarvest = if (sourceMemory.times == 0) 0 else (sourceMemory.ticks / sourceMemory.times)
                val store = if ((container.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0) + harvest * timeToHarvest >= 2000) {
                    2000
                } else {
                    (container.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0) + harvest * timeToHarvest
                }
                if (store - alreadyToTransfer > carryCreep) {
                    val taskId = createTask("${task.room}|${(if (task.toRoom != "undefined") task.toRoom else task.room)}|${TaskType.TRANSFER.code}", jsObject {
                        type = TaskType.TRANSFER.code
                        this.room = task.room
                        containerId = container.id
                        status = "withdraw"
                        creepTtl = (timeToHarvest * 2.5).roundToInt()
                        resource = RESOURCE_ENERGY
                    })
                    val linkedTaskIds = containerMemory.linkedTaskIds.toMutableList()
                    linkedTaskIds.add(taskId)
                    containerMemory.linkedTaskIds = linkedTaskIds.toTypedArray()
                }
            }
        }
    }

    private fun checkLinkedTasks(containerMemory: ContainerMemory) {
        val linkedTaskIds = containerMemory.linkedTaskIds.filter {
            val taskMemory = Memory.tasks[it]
            taskMemory != null && !taskMemory.isDone
        }
        containerMemory.linkedTaskIds = linkedTaskIds.toTypedArray()
    }

    private fun findContainer(pos: RoomPosition): Array<Structure> {
        return pos.findInRange(FIND_STRUCTURES, 1, options {
            filter = {
                it.structureType == STRUCTURE_CONTAINER
            }
        })
    }

    override fun getCreepBody(room: Room): Array<BodyPartConstant> {
        return room.calculateHarvestBody()
    }

}