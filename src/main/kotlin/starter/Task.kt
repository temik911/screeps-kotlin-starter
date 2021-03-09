package starter

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.CreepMemory
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.ERR_NO_PATH
import screeps.api.FIND_MY_CREEPS
import screeps.api.FIND_SOURCES
import screeps.api.FIND_STRUCTURES
import screeps.api.FilterOption
import screeps.api.Game
import screeps.api.Memory
import screeps.api.RESOURCE_ENERGY
import screeps.api.STRUCTURE_CONTAINER
import screeps.api.STRUCTURE_KEEPER_LAIR
import screeps.api.Source
import screeps.api.WORK
import screeps.api.component1
import screeps.api.component2
import screeps.api.entries
import screeps.api.get
import screeps.api.options
import screeps.api.structures.Structure
import screeps.api.structures.StructureContainer
import screeps.api.structures.StructureKeeperLair
import screeps.utils.memory.memory
import screeps.utils.unsafe.jsObject
import starter.extension.deleteRequest
import starter.extension.moveToWithSwap
import starter.extension.requestCreep
import starter.memory.TaskMemory
import starter.memory.containerId
import starter.memory.containers
import starter.memory.creepRequestId
import starter.memory.isDone
import starter.memory.isFree
import starter.memory.isProvider
import starter.memory.isSkRoom
import starter.memory.requestId
import starter.memory.role
import starter.memory.room
import starter.memory.sourceId
import starter.memory.startTime
import starter.memory.tasks
import starter.memory.type
import starter.memory.withdrawType
import starter.task.TaskType

var Memory.timer: Int by memory { 0 }

fun executeTasks(): TaskCpuUsed {
    val taskTypeToCpuUsed = mutableMapOf<String, Double>()
    val roomToCpuUsed = mutableMapOf<String, Double>()
    Memory.tasks.entries.forEach { task ->
        val taskMemory = task.component2()
        if (taskMemory.isDone) {
            return@forEach
        }

        if (taskMemory.startTime == 0) {
            taskMemory.startTime = Game.time
        }

        val taskType = TaskType.of(taskMemory.type) ?: return@forEach

        if (taskType == TaskType.BOOSTED_RANGE_ATTACK || taskType == TaskType.RANGE_ATTACK
                || taskType == TaskType.CLAIM_ROOM_POPULATION || taskType == TaskType.SQUAD_ATTACK
                || taskType == TaskType.BOOSTED_DISMANTLE_ATTACK || taskType == TaskType.BOOSTED_DISMANTLE_ATTACK_POPULATION
                || taskType == TaskType.NUKE_SAVER) {
            console.log(task.component1())
        }

        if (taskType.isUnlimited || taskMemory.startTime + taskType.ttl >= Game.time) {
            try {
                val cpuUsed = withTimer(taskType, taskMemory)
                taskTypeToCpuUsed[cpuUsed.taskType.code] = (taskTypeToCpuUsed[cpuUsed.taskType.code] ?: 0.0) + cpuUsed.cpuUsed
                roomToCpuUsed[cpuUsed.room] = (roomToCpuUsed[cpuUsed.room] ?: 0.0) + cpuUsed.cpuUsed
            } catch (e: Throwable) {
                console.log("${task.component1()} ${e.cause} ${e.message}")
            }
        } else {
            taskMemory.isDone = true
        }
    }
    return TaskCpuUsed(taskTypeToCpuUsed, roomToCpuUsed)
}

fun peekCreep2(task: TaskMemory, creepRole: Role, creepBody: Array<BodyPartConstant>, priority: Boolean = false, timeOut: Int = 0,
               ttl: Int = 0): Creep? {
    if (task.startTime == 0) {
        task.startTime = Game.time
    }
    val room = Game.rooms[task.room]!!
    if (task.startTime + timeOut <= Game.time) {
        if (task.creepRequestId == "undefined") {
            val first = room.find(FIND_MY_CREEPS)
                    .firstOrNull { it.memory.isFree && it.memory.role == creepRole && !it.spawning && it.ticksToLive >= ttl }
            if (first == null) {
                task.creepRequestId = UniqueIdGenerator.generateUniqueId()
                val memory = jsObject<CreepMemory> {
                    role = creepRole
                    isFree = true
                    requestId = task.creepRequestId
                }
                room.requestCreep(creepBody, memory, priority)
                return null
            }
        }
    }

    val foundedCreep = room.find(FIND_MY_CREEPS)
            .firstOrNull { it.memory.isFree && it.memory.role == creepRole && !it.spawning && it.ticksToLive >= ttl }
    if (foundedCreep != null) {
        if (task.creepRequestId != "undefined") {
            room.deleteRequest(task.creepRequestId)
        }
    }
    return foundedCreep
}

fun withdraw(task: TaskMemory, creep: Creep, containerType: ContainerType, storageCapacityLimit: Int = 5000) {
    if (creep.room.storage != null
            && creep.room.storage!!.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0 > storageCapacityLimit
            && creep.room.storage!!.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0 > creep.store.getFreeCapacity(RESOURCE_ENERGY) ?: 0) {
        if (creep.withdraw(creep.room.storage!!, RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
            creep.moveToWithSwap(creep.room.storage!!.pos)
        }
    } else {
        withdrawFromContainerOrHarvest(task, creep, containerType)
    }
}

fun withdrawFromContainerOrHarvest(task: TaskMemory, creep: Creep, containerType: ContainerType) {
    when (task.withdrawType) {
        "container" -> {
            if (task.containerId == "undefined") {
                val container = creep.pos.findClosestByPath(FIND_STRUCTURES, options {
                    filter = {
                        it.structureType == STRUCTURE_CONTAINER
                                && it.unsafeCast<StructureContainer>().store.getUsedCapacity(RESOURCE_ENERGY) ?: 0 > creep.store.getFreeCapacity(RESOURCE_ENERGY) ?: 0
                                && containerType.isApplicable(it.id)
                    }
                })

                if (container == null) {
                    if (creep.body.any { it.type == WORK }) {
                        task.withdrawType = "harvest"
                    }
                    return
                }

                task.containerId = container.id
            }

            val container = Game.getObjectById<StructureContainer>(task.containerId)
            if (container == null || container.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0 < creep.store.getFreeCapacity(RESOURCE_ENERGY) ?: 0) {
                task.containerId = "undefined"
                return
            }

            if (creep.withdraw(container, RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
                if (creep.moveTo(container, options { maxRooms = 1 }) == ERR_NO_PATH) {
                    task.containerId = "undefined"
                }
            }
        }
        "harvest" -> {
            if (task.sourceId == "undefined") {
                val source = creep.pos.findClosestByPath(FIND_SOURCES, options {
                    filter = { it.energy > 0 && (if (task.isSkRoom) notNearActiveKeeperLair(it) else true) }
                })

                if (source == null) {
                    task.sourceId = "undefined"
                    task.withdrawType = "container"
                    return
                }

                task.sourceId = source.id
            }

            val source = Game.getObjectById<Source>(task.sourceId)
            if (source == null || source.energy == 0) {
                task.sourceId = "undefined"
                task.withdrawType = "container"
                return
            }

            if (creep.harvest(source) == ERR_NOT_IN_RANGE) {
                if (creep.moveTo(source, options { maxRooms = 1 }) == ERR_NO_PATH) {
                    task.sourceId = "undefined"
                    task.withdrawType = "container"
                    return
                }
            }
        }
    }

    if ((creep.store.getFreeCapacity(RESOURCE_ENERGY) ?: 0) == 0) {
        task.withdrawType = "container"
        task.containerId = "undefined"
        task.sourceId = "undefined"
    }
}

fun notNearActiveKeeperLair(source: Source): Boolean {
    val keeperLair = source.pos.findClosestByRange(FIND_STRUCTURES, options<FilterOption<Structure>> {
        filter = {
            it.structureType == STRUCTURE_KEEPER_LAIR
        }
    })?.unsafeCast<StructureKeeperLair>() ?: return true
    return keeperLair.ticksToSpawn != null && keeperLair.ticksToSpawn!! >= 50
}

enum class ContainerType {
    ALL {
        override fun isApplicable(containerId: String): Boolean = true
    },
    PROVIDER {
        override fun isApplicable(containerId: String): Boolean = Memory.containers[containerId]?.isProvider ?: false
    },
    REQUESTER {
        override fun isApplicable(containerId: String): Boolean = !PROVIDER.isApplicable(containerId)
    };

    abstract fun isApplicable(containerId: String): Boolean
}