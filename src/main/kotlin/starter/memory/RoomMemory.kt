package starter.memory

import screeps.api.BodyPartConstant
import screeps.api.BuildableStructureConstant
import screeps.api.CreepMemory
import screeps.api.Memory
import screeps.api.MemoryMarker
import screeps.api.RoomMemory
import screeps.api.get
import screeps.api.set
import screeps.utils.memory.memory
import screeps.utils.unsafe.jsObject
import starter.createTask
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 12.04.2020
 */

var RoomMemory.sleepUntil: Int by memory { 0 }
var RoomMemory.rampartHitsMin: Int by memory { 500_000 }
var RoomMemory.controllerReservation: Int by memory { 0 }
var RoomMemory.linkedTaskIds: Array<String> by memory { js("[]") }
var RoomMemory.sleepUntilTimes: SleepUntil by memory { jsObject<SleepUntil> { } }
var RoomMemory.plainRoomPlan: Array<Triple<Int, Int, String>> by memory { js("[]") }
var RoomMemory.roomPlan: Array<Triple<Int, Int, BuildableStructureConstant>> by memory { js("[]") }
var RoomMemory.reservedRooms: Array<String> by memory { js("[]") }
var RoomMemory.structures: Structures by memory { jsObject<Structures> { } }
var RoomMemory.spawnRequests: Array<SpawnRequest> by memory { js("[]") }

external interface SpawnRequest : MemoryMarker

var SpawnRequest.body: Array<BodyPartConstant> by memory { js("[]") }
var SpawnRequest.memory: CreepMemory by memory { jsObject<CreepMemory> { } }

external interface Structures : MemoryMarker

var Structures.powerSpawn: String? by memory()
var Structures.storageLink: String? by memory()

external interface SleepUntil : MemoryMarker

var SleepUntil.common: Int by memory { 0 }
var SleepUntil.builder: Int by memory { 0 }
var SleepUntil.defender: Int by memory { 0 }
var SleepUntil.roomPlanner: Int by memory { 0 }
var SleepUntil.roadsPlanner: Int by memory { 0 }
var SleepUntil.powerProcessor: Int by memory { 0 }
var SleepUntil.labProcessor: Int by memory { 0 }
var SleepUntil.terminalManager: Int by memory { 0 }
var SleepUntil.reserveRemoteRoomsManager: Int by memory { 0 }
var SleepUntil.resourceBalance: Int by memory { 0 }
var SleepUntil.nukesCheck: Int by memory { 0 }

fun getRoomMemory(roomName: String): RoomMemory {
    if (Memory.rooms[roomName] == null) {
        Memory.rooms[roomName] = jsObject { }
    }
    return Memory.rooms[roomName]!!
}

fun RoomMemory.checkLinkedTasks() {
    val linkedTaskIds = this.linkedTaskIds.filter {
        val taskMemory = Memory.tasks[it]
        taskMemory != null && !taskMemory.isDone
    }
    this.linkedTaskIds = linkedTaskIds.toTypedArray()
}

fun RoomMemory.createLinkedTask(taskType: TaskType, taskMemory: TaskMemory) {
    taskMemory.type = taskType.code
    val taskName = "${taskMemory.room}|" + (if (taskMemory.toRoom != "undefined") "${taskMemory.toRoom}|" else "") + taskType.code
    val taskId = createTask(taskName, taskMemory)
    val linkedTaskIds = this.linkedTaskIds.toMutableList()
    linkedTaskIds.add(taskId)
    this.linkedTaskIds = linkedTaskIds.toTypedArray()
}

fun RoomMemory.getTasks(filter: (TaskMemory) -> Boolean): List<TaskMemory> {
    this.checkLinkedTasks()
    return this.linkedTaskIds.mapNotNull { Memory.tasks[it] }
            .filter(filter)
            .toList()
}