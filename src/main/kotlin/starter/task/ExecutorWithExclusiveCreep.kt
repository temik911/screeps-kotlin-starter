package starter.task

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.CreepMemory
import screeps.api.FIND_MY_CREEPS
import screeps.api.Game
import screeps.api.Room
import screeps.api.get
import screeps.utils.unsafe.jsObject
import starter.UniqueIdGenerator
import starter.extension.requestCreep
import starter.memory.CreateCreepTask
import starter.memory.TaskMemory
import starter.memory.createCreepTask
import starter.memory.creepId
import starter.memory.isDone
import starter.memory.isPrepareDone
import starter.memory.room
import starter.memory.uniqueId

/**
 *
 *
 * @author zakharchuk
 * @since 28.03.2020
 */
abstract class ExecutorWithExclusiveCreep(val task: TaskMemory) {

    fun execute() {
        if (task.creepId == "undefined") {
            val room = Game.rooms[task.room]!!
            val creep = peekExclusiveCreep(task, room, getCreepBody(room), priority()) ?: return
            task.creepId = creep.id
        }

        val creep = Game.getObjectById<Creep>(task.creepId)

        if (creep == null) {
            task.isDone = true
            return
        }

        if (!task.isPrepareDone) {
            val prepare = prepare(task, creep)
            if (!prepare) {
                return
            }
            task.isPrepareDone = true
        }

        executeInternal(creep)
    }

    private fun peekExclusiveCreep(task: TaskMemory, room: Room, creepBody: Array<BodyPartConstant>, priority: Boolean = false): Creep? {
        if (task.createCreepTask == null) {
            val uniqueId = UniqueIdGenerator.generateUniqueId()
            val memory = jsObject<CreepMemory> {
                this.uniqueId = uniqueId
            }
            room.requestCreep(creepBody, memory, priority)
            task.createCreepTask = jsObject<CreateCreepTask> {
                this.uniqueId = uniqueId
            }
            return null
        }

        val createCreepTask = task.createCreepTask!!
        return room.find(FIND_MY_CREEPS)
                .firstOrNull { !it.spawning && it.memory.uniqueId != null && it.memory.uniqueId!! == createCreepTask.uniqueId!! }
    }

    protected abstract fun getCreepBody(room: Room): Array<BodyPartConstant>

    protected open fun priority(): Boolean = false

    protected open fun prepare(task: TaskMemory, creep: Creep): Boolean = true

    protected abstract fun executeInternal(creep: Creep)

}