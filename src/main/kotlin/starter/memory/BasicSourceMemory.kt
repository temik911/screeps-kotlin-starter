package starter.memory

import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.Identifiable
import screeps.api.Memory
import screeps.api.MemoryMarker
import screeps.api.MutableRecord
import screeps.api.RoomObject
import screeps.api.RoomPosition
import screeps.api.STRUCTURE_CONTAINER
import screeps.api.get
import screeps.api.options
import screeps.api.set
import screeps.api.structures.StructureContainer
import screeps.utils.memory.memory
import screeps.utils.unsafe.jsObject

/**
 *
 *
 * @author zakharchuk
 * @since 28.02.2020
 */

val Memory.sources: MutableRecord<String, SourceMemory> by memory { jsObject<MutableRecord<String, SourceMemory>> {

} }

external interface SourceMemory : MemoryMarker
var SourceMemory.ticks: Int by memory { 0 }
var SourceMemory.times: Int by memory { 0 }
var SourceMemory.linkId: String? by memory()
var SourceMemory.containerId: String? by memory()
var SourceMemory.position: RoomPosition? by memory()

fun getSourceMemory(sourceId: String): SourceMemory {
    if (Memory.sources[sourceId] == null) {
        Memory.sources[sourceId] = jsObject { }
    }
    return Memory.sources[sourceId]!!
}

fun <T> getContainer(source: T): StructureContainer? where T : RoomObject, T : Identifiable {
    val sourceMemory = getSourceMemory(source.id)
    if (sourceMemory.containerId == null) {
        val container = source.pos.findInRange(FIND_STRUCTURES, 1, options {
            filter = { it.structureType == STRUCTURE_CONTAINER }
        }).firstOrNull() ?: return null

        sourceMemory.containerId = container.id
    }

    val container = Game.getObjectById<StructureContainer>(sourceMemory.containerId)
    if (container == null) {
        sourceMemory.containerId = null
    }
    return container
}