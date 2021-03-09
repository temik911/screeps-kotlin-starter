package starter.task

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.CreepMemory
import screeps.api.FIND_MY_CREEPS
import screeps.api.Game
import screeps.api.Room
import screeps.api.get
import screeps.utils.unsafe.jsObject
import starter.Role
import starter.UniqueIdGenerator
import starter.extension.requestCreep
import starter.memory.CreateCreepTask
import starter.memory.TaskMemory
import starter.memory.createCreepTasks
import starter.memory.creepIds
import starter.memory.isDone
import starter.memory.isPrepareDone
import starter.memory.role
import starter.memory.room
import starter.memory.uniqueId

/**
 *
 *
 * @author zakharchuk
 * @since 28.03.2020
 */
abstract class ExecutorWithSeveralCreeps(val task: TaskMemory) {

    fun execute() {
        if (task.creepIds.size != getCreepsCount()) {
            val room = Game.rooms[task.room]!!
            val creepRequests = getCreepRequests(room)
            val creeps = peekCreeps(task, room, creepRequests, priority())
            if (creeps.size != creepRequests.size) {
                return
            }
            task.creepIds = creeps.map { it.id }.toTypedArray()
        }

        val creeps = task.creepIds.mapNotNull { Game.getObjectById<Creep>(it) }

        if (creeps.isNullOrEmpty()) {
            task.isDone = true
            return
        }

        if (!task.isPrepareDone) {
            val prepare = prepare(task, creeps)
            if (!prepare) {
                return
            }
            task.isPrepareDone = true
        }

        executeInternal(creeps)
    }

    private fun peekCreeps(task: TaskMemory, room: Room, creepRequests: Array<CreepRequest>, priority: Boolean): Array<Creep> {
        if (task.createCreepTasks.isNullOrEmpty()) {
            val requests = creepRequests.map {
                val uniqueId = UniqueIdGenerator.generateUniqueId()
                val memory = jsObject<CreepMemory> {
                    this.uniqueId = uniqueId
                    this.role = it.role
                }
                room.requestCreep(it.body, memory, priority)
                jsObject<CreateCreepTask> {
                    this.uniqueId = uniqueId
                }
            }.toTypedArray()
            task.createCreepTasks = requests
            return emptyArray()
        }

        return task.createCreepTasks.mapNotNull { createTask ->
            room.find(FIND_MY_CREEPS)
                    .firstOrNull { !it.spawning && it.memory.uniqueId != null && it.memory.uniqueId!! == createTask.uniqueId!! }
        }.toTypedArray()
    }

    protected open fun priority(): Boolean = false

    protected open fun prepare(task: TaskMemory, creeps: List<Creep>): Boolean = true

    protected abstract fun executeInternal(creeps: List<Creep>)

    protected abstract fun getCreepRequests(room: Room): Array<CreepRequest>

    protected abstract fun getCreepsCount(): Int

    class CreepRequest(val body: Array<BodyPartConstant>,
                       val role: Role)

}