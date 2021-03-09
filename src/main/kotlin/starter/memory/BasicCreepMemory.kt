package starter.memory

import screeps.api.CreepMemory
import screeps.api.MemoryMarker
import screeps.api.PowerCreepMemory
import screeps.utils.memory.memory
import starter.Role


var CreepMemory.building: Boolean by memory { false }
var CreepMemory.pause: Int by memory { 0 }
var CreepMemory.role by memory(Role.UNASSIGNED)
var CreepMemory.isFree: Boolean by memory { true }
var CreepMemory.requestId: String by memory { "undefined" }
var CreepMemory.uniqueId: String? by memory()

var MemoryMarker._move: CreepMoveMemory? by memory()

external interface CreepMoveMemory : MemoryMarker

var CreepMoveMemory.path: String? by memory()
var CreepMoveMemory.time: Int? by memory()

var PowerCreepMemory.isFree: Boolean by memory { true }