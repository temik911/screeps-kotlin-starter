package starter.memory

import screeps.api.Memory
import screeps.api.MemoryMarker
import screeps.api.MutableRecord
import screeps.utils.memory.memory
import screeps.utils.unsafe.jsObject

/**
 *
 *
 * @author zakharchuk
 * @since 31.03.2020
 */

val Memory.locks: MutableRecord<String, LockMemory> by memory { jsObject<MutableRecord<String, LockMemory>> { } }

external interface LockMemory : MemoryMarker

var LockMemory.initiator: String? by memory()
var LockMemory.time: Int by memory { 0 }