package starter.task.attack

import screeps.api.ATTACK
import screeps.api.Creep
import screeps.api.HEAL
import screeps.api.MOVE
import screeps.api.RANGED_ATTACK
import screeps.api.RESOURCE_CATALYZED_GHODIUM_ALKALIDE
import screeps.api.RESOURCE_CATALYZED_KEANIUM_ALKALIDE
import screeps.api.RESOURCE_CATALYZED_LEMERGIUM_ALKALIDE
import screeps.api.RESOURCE_CATALYZED_UTRIUM_ACID
import screeps.api.RESOURCE_CATALYZED_ZYNTHIUM_ALKALIDE
import screeps.api.Room
import screeps.api.TOUGH
import screeps.utils.unsafe.jsObject
import starter.calculateBoostedRangeAttackBody
import starter.createTask
import starter.memory.BoostCreepTask
import starter.memory.TaskMemory
import starter.memory.amount
import starter.memory.boost
import starter.memory.boostCreepTask
import starter.memory.checkLinkedTasks
import starter.memory.creepId
import starter.memory.linkedTaskIds
import starter.memory.room
import starter.memory.type
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 09.05.2020
 */
class BoostedRangeAttackExecutor(task: TaskMemory): RangeAttackExecutor(task) {

    override fun getCreepBody(room: Room) = room.calculateBoostedRangeAttackBody()

    override fun prepare(task: TaskMemory, creep: Creep): Boolean {
        if (!super.prepare(task, creep)) {
            return false
        }

        checkLinkedTasks(task)
        if (creep.body.none { it.boost == null }) {
            return true
        }
        if (task.linkedTaskIds.isNullOrEmpty()) {
            val unBoostedPart = creep.body.firstOrNull { it.boost == null } ?: return false
            val amount = creep.body.count { it.type == unBoostedPart.type }
            val boost = when (unBoostedPart.type) {
                HEAL -> RESOURCE_CATALYZED_LEMERGIUM_ALKALIDE
                ATTACK -> RESOURCE_CATALYZED_UTRIUM_ACID
                RANGED_ATTACK -> RESOURCE_CATALYZED_KEANIUM_ALKALIDE
                MOVE -> RESOURCE_CATALYZED_ZYNTHIUM_ALKALIDE
                TOUGH -> RESOURCE_CATALYZED_GHODIUM_ALKALIDE
                else -> throw IllegalArgumentException("Unexpected body part: ${unBoostedPart.type}")
            }
            val boostCreepTask = jsObject<BoostCreepTask> {
                this.creepId = creep.id
                this.boost = boost
                this.amount = amount
            }

            val taskId = createTask("${task.room}|${TaskType.BOOST_CREEP.code}", jsObject {
                this.type = TaskType.BOOST_CREEP.code
                this.room = task.room
                this.boostCreepTask = boostCreepTask
            })
            val linkedTaskIds = task.linkedTaskIds.toMutableList()
            linkedTaskIds.add(taskId)
            task.linkedTaskIds = linkedTaskIds.toTypedArray()
        }

        return false
    }
}