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
 * @since 18.05.2020
 */

val Memory.stats: MutableRecord<String, Any> by memory { jsObject<MutableRecord<String, Any>> { } }

external interface StatsMemory : MemoryMarker

val StatsMemory.cpu: CpuStats? by memory()

external interface CpuStats: MemoryMarker

val CpuStats.getUsed: Int? by memory()
val CpuStats.bucket: Int? by memory()

