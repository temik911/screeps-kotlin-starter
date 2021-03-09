package starter.memory

import screeps.api.Memory
import screeps.api.MemoryMarker
import screeps.api.MutableRecord
import screeps.utils.memory.memory
import screeps.utils.unsafe.jsObject

val Memory.containers: MutableRecord<String, ContainerMemory> by memory { jsObject<MutableRecord<String, ContainerMemory>> { } }

external interface ContainerMemory : MemoryMarker

var ContainerMemory.isProvider: Boolean by memory { false }
var ContainerMemory.linkedTaskIds: Array<String> by memory { js("[]") }
