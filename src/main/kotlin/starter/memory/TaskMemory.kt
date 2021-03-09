package starter.memory

import screeps.api.BodyPartConstant
import screeps.api.Memory
import screeps.api.MemoryMarker
import screeps.api.MutableRecord
import screeps.api.RESOURCE_ENERGY
import screeps.api.ResourceConstant
import screeps.api.RoomPosition
import screeps.api.get
import screeps.utils.memory.memory
import screeps.utils.unsafe.jsObject
import starter.createTask
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 29.03.2020
 */

val Memory.tasks: MutableRecord<String, TaskMemory> by memory { jsObject<MutableRecord<String, TaskMemory>> { } }

external interface TaskMemory : MemoryMarker

var TaskMemory.type: String by memory { "undefined" }
var TaskMemory.withdrawType: String by memory { "container" }
var TaskMemory.room: String by memory { "undefined" }
var TaskMemory.creepId: String by memory { "undefined" }
var TaskMemory.creepIds: Array<String> by memory { js("[]") }
var TaskMemory.sourceId: String by memory { "undefined" }
var TaskMemory.extractorId: String by memory { "undefined" }
var TaskMemory.containerId: String by memory { "undefined" }
var TaskMemory.targetId: String by memory { "undefined" }
var TaskMemory.targetRequiredHits: Int by memory { 0 }
var TaskMemory.status: String by memory { "undefined" }
var TaskMemory.toRoom: String by memory { "undefined" }
var TaskMemory.isDone: Boolean by memory { false }
var TaskMemory.linkId: String by memory { "undefined" }
var TaskMemory.isPrepare: Boolean by memory { true }
var TaskMemory.isPrepareDone: Boolean by memory { false }
var TaskMemory.ticks: Int by memory { 0 }
var TaskMemory.newTaskCreated: Boolean by memory { false }
var TaskMemory.startTime: Int by memory { 0 }
var TaskMemory.creepTtl: Int by memory { 0 }
var TaskMemory.storageLinkId: String by memory { "undefined" }
var TaskMemory.controllerLinkId: String by memory { "undefined" }
var TaskMemory.terminalId: String? by memory()
var TaskMemory.currentLevel: Int by memory { 0 }
var TaskMemory.resource: ResourceConstant by memory { RESOURCE_ENERGY }
var TaskMemory.creepRequestId: String by memory { "undefined" }
var TaskMemory.position: RoomPosition by memory { RoomPosition(25, 25, "E9S21") }
var TaskMemory.toPosition: RoomPosition by memory { RoomPosition(25, 25, "E9S21") }
var TaskMemory.isExternalRoom: Boolean by memory { false }
var TaskMemory.isSkRoom: Boolean by memory { false }
var TaskMemory.upgradeBlockedTimer: Int by memory { 0 }
var TaskMemory.lastTimeDamage: Int by memory { 0 }
var TaskMemory.lastHealId: String? by memory()
var TaskMemory.transferTask: TransferTask? by memory()
var TaskMemory.createCreepTask: CreateCreepTask? by memory()
var TaskMemory.createCreepTasks: Array<CreateCreepTask> by memory { js("[]") }
var TaskMemory.labReactionTask: LabReactionTask? by memory()
var TaskMemory.factoryReactionTask: FactoryReactionTask? by memory()
var TaskMemory.internalTaskId: String? by memory()
var TaskMemory.externalTaskId: String? by memory()
var TaskMemory.boostCreepTask: BoostCreepTask? by memory()
var TaskMemory.dismantleTask: DismantleTask? by memory()
var TaskMemory.keeperLairId: String? by memory()
var TaskMemory.sleepUntil: Int by memory { 0 }
var TaskMemory.linkedTaskIds: Array<String> by memory { js("[]") }
var TaskMemory.thiefTask: ThiefTask? by memory()
var TaskMemory.harvestTask: HarvestTask? by memory()
var TaskMemory.observerRooms: Array<String> by memory { js("[]") }
var TaskMemory.observerId: String? by memory()
var TaskMemory.powerSpawn: String? by memory()
var TaskMemory.factoryId: String? by memory()
var TaskMemory.expectedDamage: Int by memory { 0 }
var TaskMemory.targetPosition: RoomPosition? by memory()
var TaskMemory.distance: Int? by memory()

external interface BoostCreepTask : MemoryMarker

var BoostCreepTask.creepId: String? by memory()
var BoostCreepTask.boost: ResourceConstant? by memory()
var BoostCreepTask.bodyPart: BodyPartConstant? by memory()
var BoostCreepTask.amount: Int? by memory()
var BoostCreepTask.labId: String? by memory()

external interface TransferTask : MemoryMarker

var TransferTask.fromId: String? by memory()
var TransferTask.toId: String? by memory()
var TransferTask.resource: ResourceConstant? by memory()
var TransferTask.value: Int? by memory()

external interface CreateCreepTask : MemoryMarker

var CreateCreepTask.uniqueId: String? by memory()

external interface LabReactionTask : MemoryMarker

var LabReactionTask.providerLab1: String? by memory()
var LabReactionTask.resource1: ResourceConstant? by memory()
var LabReactionTask.providerLab2: String? by memory()
var LabReactionTask.resource2: ResourceConstant? by memory()
var LabReactionTask.amount: Int? by memory()

external interface FactoryReactionTask : MemoryMarker

var FactoryReactionTask.resource: ResourceConstant? by memory()
var FactoryReactionTask.amount: Int? by memory()
var FactoryReactionTask.requestedAmount: Int? by memory()

external interface DismantleTask : MemoryMarker

var DismantleTask.type: String? by memory()
var DismantleTask.targetPosition: RoomPosition? by memory()

external interface ThiefTask : MemoryMarker

var ThiefTask.storageId: String? by memory()
var ThiefTask.terminalId: String? by memory()

external interface HarvestTask : MemoryMarker

var HarvestTask.targetPosition: RoomPosition? by memory()
var HarvestTask.sourceId: String? by memory()
var HarvestTask.resourceType: ResourceConstant? by memory()
var HarvestTask.lastCooldown: Int by memory { 0 }

fun checkLinkedTasks(task: TaskMemory) {
    task.checkLinkedTasks()
}

fun TaskMemory.checkLinkedTasks() {
    val linkedTaskIds = this.linkedTaskIds.filter {
        val taskMemory = Memory.tasks[it]
        taskMemory != null && !taskMemory.isDone
    }
    this.linkedTaskIds = linkedTaskIds.toTypedArray()
}

fun TaskMemory.createLinkedTask(taskType: TaskType, taskMemory: TaskMemory) {
    taskMemory.type = taskType.code
    val taskName = "${taskMemory.room}|" + (if (taskMemory.toRoom != "undefined") "${taskMemory.toRoom}|" else "") + taskType.code
    val taskId = createTask(taskName, taskMemory)
    val linkedTaskIds = this.linkedTaskIds.toMutableList()
    linkedTaskIds.add(taskId)
    this.linkedTaskIds = linkedTaskIds.toTypedArray()
}

fun TaskMemory.calculateTasksCount(): Map<TaskType, Int> {
    this.checkLinkedTasks()
    val tasksCount = mutableMapOf<TaskType, Int>()
    this.linkedTaskIds.map { Memory.tasks[it] }
            .mapNotNull { TaskType.of(it!!.type) }
            .forEach {
                val current = if (tasksCount[it] == null) 0 else tasksCount[it]!!
                tasksCount[it] = current + 1
            }
    return tasksCount
}