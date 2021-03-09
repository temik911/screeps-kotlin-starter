package starter.task.nuke

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.FIND_NUKES
import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.LOOK_STRUCTURES
import screeps.api.RESOURCE_ENERGY
import screeps.api.Room
import screeps.api.RoomPosition
import screeps.api.STRUCTURE_RAMPART
import screeps.api.get
import screeps.api.options
import screeps.api.structures.Structure
import screeps.api.structures.StructureRampart
import starter.calculateRepairRampartBody
import starter.extension.moveToWithSwap
import starter.memory.TaskMemory
import starter.memory.getRoomMemory
import starter.memory.rampartHitsMin
import starter.memory.room
import starter.memory.status
import starter.memory.targetId
import starter.memory.targetRequiredHits
import starter.task.ExecutorWithExclusiveCreep

/**
 *
 *
 * @author zakharchuk
 * @since 14.10.2020
 */
class NukeSaverExecutor(task: TaskMemory): ExecutorWithExclusiveCreep(task) {

    override fun getCreepBody(room: Room): Array<BodyPartConstant> = room.calculateRepairRampartBody()

    override fun prepare(task: TaskMemory, creep: Creep): Boolean {
        if (task.status == "undefined") {
            task.status = "withdraw"
        }
        return true
    }

    override fun executeInternal(creep: Creep) {
        when (task.status) {
            "repair" -> {
                if (task.targetId == "undefined") {
                    val target = getTarget(Game.rooms.get(task.room)!!) ?: return
                    task.targetId = target.id
                    task.targetRequiredHits = target.hits + 90000
                }

                val target = Game.getObjectById<Structure>(task.targetId)

                if (target == null || target.hits > task.targetRequiredHits) {
                    task.targetId = "undefined"
                    task.targetRequiredHits = 0
                    return
                }

                if (creep.repair(target) == ERR_NOT_IN_RANGE) {
                    creep.moveToWithSwap(target)
                }

                if (creep.store.getUsedCapacity() == 0) {
                    task.status = "withdraw"
                }
            }
            "withdraw" -> {
                if (creep.room.storage != null && creep.room.storage!!.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0 > 5000) {
                    if (creep.withdraw(creep.room.storage!!, RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
                        creep.moveToWithSwap(creep.room.storage!!)
                    }
                }
                if (creep.store.getFreeCapacity(RESOURCE_ENERGY) ?: 0 == 0) {
                    task.status = "repair"
                }
            }
        }
    }

    private fun getTarget(room: Room): Structure? {
        val nukes = room.find(FIND_NUKES)
        if (nukes.isNullOrEmpty()) {
            return null
        }

        val damage: MutableList<MutableList<Int>> = MutableList(50) { MutableList(50) { 0 } }

        nukes.forEach {
            damage[it.pos.x][it.pos.y] += 10_000_000
            (it.pos.x - 2..it.pos.x + 2).forEach { x ->
                (it.pos.y - 2..it.pos.y + 2).forEach { y ->
                    if (x in 0..49 && y in 0..49) {
                        damage[x][y] += 5_000_000
                    }
                }
            }
        }

        damage.forEachIndexed { x, list ->
            list.forEachIndexed { y, value ->
                if (value > 0) {
                    val structures = room.lookForAt(LOOK_STRUCTURES, x, y)?: emptyArray()
                    val rampart = structures.firstOrNull { it.structureType == STRUCTURE_RAMPART }
                    if (rampart != null) {
                        val neededHits = value + 1_000_000
                        if (neededHits > rampart.hits) {
                            return rampart
                        }
                    }
                }
            }
        }

        return null
    }
}