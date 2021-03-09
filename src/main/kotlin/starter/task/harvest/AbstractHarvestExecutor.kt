package starter.task.harvest

import screeps.api.Creep
import screeps.api.Game
import screeps.api.Identifiable
import screeps.api.RenewableHarvestable
import screeps.api.RoomObjectNotNull
import screeps.api.RoomPosition
import screeps.api.get
import starter.extension.moveToWithSwap
import starter.memory.TaskMemory
import starter.memory.creepId
import starter.memory.getSourceMemory
import starter.memory.position
import starter.memory.room
import starter.memory.sourceId
import starter.memory.status
import starter.memory.ticks
import starter.memory.times
import starter.task.ExecutorWithExclusiveCreep

/**
 *
 *
 * @author zakharchuk
 * @since 04.03.2020
 */
abstract class AbstractHarvestExecutor<T>(task: TaskMemory) : ExecutorWithExclusiveCreep(task) where T : RoomObjectNotNull, T : RenewableHarvestable, T : Identifiable {

    override fun executeInternal(creep: Creep) {
        val source = Game.getObjectById<T>(task.sourceId) ?: return
        harvest(creep, source)
    }

    override fun prepare(task: TaskMemory, creep: Creep): Boolean {
        if (task.status == "undefined") {
            task.status = "harvest"
        }

        val sourceMemory = getSourceMemory(task.sourceId)
        if (sourceMemory.position == null) {
            val source = Game.getObjectById<T>(task.sourceId) ?: return false
            sourceMemory.position = source.pos
        }

        val sourcePosition = getSourcePosition(sourceMemory.position!!)
        if (!creep.pos.isNearTo(sourcePosition)) {
            task.ticks += 1
            creep.moveToWithSwap(sourcePosition)
            return false
        }

        sourceMemory.ticks += task.ticks
        sourceMemory.times += 1
        return true
    }

    abstract fun harvest(creep: Creep, source: T)

    fun isNewTaskNeeded(): Boolean {
        if (task.creepId == "undefined") {
            return false
        }

        val creep = Game.getObjectById<Creep>(task.creepId) ?: return true

        if (creep.ticksToLive > getPrepareTicks(task)) {
            return false
        }

        return true
    }

    private fun getPrepareTicks(task: TaskMemory): Int {
        val prepareCreep = getCreepBody(Game.rooms.get(task.room)!!).size * 3
        val sourceMemory = getSourceMemory(task.sourceId)
        return prepareCreep + if (sourceMemory.times == 0) 0 else sourceMemory.ticks / sourceMemory.times
    }

    private fun getSourcePosition(position: RoomPosition): RoomPosition {
        return RoomPosition(position.x, position.y, position.roomName)
    }
}