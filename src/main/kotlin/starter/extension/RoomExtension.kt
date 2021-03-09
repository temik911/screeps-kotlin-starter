package starter.extension

import screeps.api.BodyPartConstant
import screeps.api.CreepMemory
import screeps.api.FIND_MY_STRUCTURES
import screeps.api.Game
import screeps.api.ResourceConstant
import screeps.api.Room
import screeps.api.STRUCTURE_LINK
import screeps.api.options
import screeps.api.structures.StructureFactory
import screeps.api.structures.StructureLink
import screeps.api.structures.StructurePowerSpawn
import screeps.utils.unsafe.jsObject
import starter.getUsed
import starter.memory.body
import starter.memory.getRoomMemory
import starter.memory.memory
import starter.memory.powerSpawn
import starter.memory.rampartHitsMin
import starter.memory.requestId
import starter.memory.reservedRooms
import starter.memory.spawnRequests
import starter.memory.storageLink
import starter.memory.structures

/**
 *
 *
 * @author zakharchuk
 * @since 15.05.2020
 */

fun Room.store(resourceConstant: ResourceConstant): Int {
    val storageStore = storage?.store?.getUsed(resourceConstant) ?: 0
    val terminalStore = terminal?.store?.getUsed(resourceConstant) ?: 0
    return storageStore + terminalStore
}

fun Room.factory(): StructureFactory? {
    return find(FIND_MY_STRUCTURES, options {
        filter = { it.structureType == screeps.api.STRUCTURE_FACTORY }
    }).map { it.unsafeCast<StructureFactory>() }.firstOrNull()
}

fun Room.powerSpawn(): StructurePowerSpawn? {
    if (memory.structures.powerSpawn == null) {
        val structure = find(FIND_MY_STRUCTURES, options {
            filter = { it.structureType == screeps.api.STRUCTURE_POWER_SPAWN }
        }).firstOrNull() ?: return null
        memory.structures.powerSpawn = structure.id
    }

    val powerSpawn = Game.getObjectById<StructurePowerSpawn>(memory.structures.powerSpawn!!)
    if (powerSpawn == null) {
        memory.structures.powerSpawn = null
        return null
    }
    return powerSpawn
}

fun Room.storageLink(): StructureLink? {
    if (memory.structures.storageLink == null) {
        storage ?: return null
        val structure = storage!!.pos.findInRange(FIND_MY_STRUCTURES, 2, options {
            filter = { it.structureType == STRUCTURE_LINK }
        }).firstOrNull() ?: return null
        memory.structures.storageLink = structure.id
    }

    val storageLink = Game.getObjectById<StructureLink>(memory.structures.storageLink!!)
    if (storageLink == null) {
        memory.structures.storageLink = null
        return null
    }
    return storageLink
}

fun Room.isHighway(): Boolean {
    return isHighway(name)
}

fun Room.isSk(): Boolean {
    return isSk(name)
}

fun isHighway(roomName: String): Boolean {
    val i = roomName.substring(1, 3).toInt() % 10
    val j = roomName.substring(4, 6).toInt() % 10
    return (i == 0) || (j == 0)
}

fun isSk(roomName: String): Boolean {
    val i = roomName.substring(1, 3).toInt() % 10
    val j = roomName.substring(4, 6).toInt() % 10
    return (i in 4..6) && (j in 4..6)
}

fun toLink(name: String): String {
    return "<a href=\"https://screeps.com/a/#!/room/${Game.shard.name}/${name}\">${name}</a>"
}

fun Room.statistic() {
    val tokens = mutableListOf<String>()
    tokens.add("Room ${toLink(name)}.")
    val progressPercent = 100.0 * controller!!.progress / controller!!.progressTotal
    tokens.add("Level: ${controller!!.level} (${progressPercent.toInt()}%).")
    tokens.add("Rampart hits: ${getRoomMemory(name).rampartHitsMin}.")
    tokens.add("Spawn requests size: ${memory.spawnRequests.size}.")
    val reservedRooms = memory.reservedRooms
    tokens.add(if (reservedRooms.isEmpty()) "No reserved rooms." else "Reserved rooms(${reservedRooms.size}): ${reservedRooms.joinToString { toLink(it) }}.")
    console.log(tokens.joinToString(separator = " "))
}

fun Room.requestCreep(creepBody: Array<BodyPartConstant>, creepMemory: CreepMemory, priority: Boolean = false) {
    val spawnRequests = memory.spawnRequests.toMutableList()
    if (priority) {
        spawnRequests.add(0, jsObject {
            body = creepBody
            memory = creepMemory
        })
    } else {
        spawnRequests.add(jsObject {
            body = creepBody
            memory = creepMemory
        })
    }
    memory.spawnRequests = spawnRequests.toTypedArray()
}

fun Room.deleteRequest(requestId: String) {
    val spawnRequest = memory.spawnRequests.toMutableList().filter { it.memory.requestId != requestId }.toTypedArray()
    memory.spawnRequests = spawnRequest
}