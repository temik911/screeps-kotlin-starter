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
import starter.extension.deleteRequest
import starter.extension.requestCreep
import starter.memory.TaskMemory
import starter.memory.creepId
import starter.memory.creepRequestId
import starter.memory.isDone
import starter.memory.isFree
import starter.memory.isPrepareDone
import starter.memory.requestId
import starter.memory.role
import starter.memory.room
import starter.memory.startTime

/**
 *
 *
 * @author zakharchuk
 * @since 28.03.2020
 */
abstract class ExecutorWithPoolOfCreep(val task: TaskMemory) {

    fun execute() {
        if (task.creepId == "undefined") {
            val room = Game.rooms[task.room]!!
            val creep = peekCreepFromPool(task, getCreepRole(), getCreepBody(room), timeOut = getTimeOut(), ttl = getTtl()) ?: return
            creep.memory.isFree = false
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

        if (task.isDone) {
            creep.memory.isFree = true
        }
    }

    private fun peekCreepFromPool(task: TaskMemory, creepRole: Role, creepBody: Array<BodyPartConstant>, priority: Boolean = false, timeOut: Int = 0,
                                  ttl: Int = 0): Creep? {
        if (task.startTime == 0) {
            task.startTime = Game.time
        }
        val room = Game.rooms[task.room]!!
        if (task.startTime + timeOut <= Game.time) {
            if (task.creepRequestId == "undefined") {
                val first = room.find(FIND_MY_CREEPS)
                        .firstOrNull { it.memory.isFree && it.memory.role == creepRole && !it.spawning && it.ticksToLive >= ttl }
                if (first == null) {
                    task.creepRequestId = UniqueIdGenerator.generateUniqueId()
                    val memory = jsObject<CreepMemory> {
                        role = creepRole
                        isFree = true
                        requestId = task.creepRequestId
                    }
                    room.requestCreep(creepBody, memory, priority)
                    return null
                }
            }
        }

        val foundedCreep = room.find(FIND_MY_CREEPS)
                .firstOrNull { it.memory.isFree && it.memory.role == creepRole && !it.spawning && it.ticksToLive >= ttl }
        if (foundedCreep != null) {
            if (task.creepRequestId != "undefined") {
                room.deleteRequest(task.creepRequestId)
            }
        }
        return foundedCreep
    }

    protected abstract fun getCreepBody(room: Room): Array<BodyPartConstant>

    protected abstract fun getCreepRole(): Role

    protected open fun getTimeOut(): Int = 0

    protected open fun getTtl(): Int = 0

    protected open fun prepare(task: TaskMemory, creep: Creep): Boolean = true

    protected abstract fun executeInternal(creep: Creep)

}