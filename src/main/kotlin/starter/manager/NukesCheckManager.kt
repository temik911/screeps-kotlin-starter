package starter.manager

import screeps.api.FIND_NUKES
import screeps.api.Game
import screeps.api.LOOK_STRUCTURES
import screeps.api.Room
import screeps.api.STRUCTURE_RAMPART
import screeps.api.structures.Structure
import screeps.utils.unsafe.jsObject
import starter.extension.storageLink
import starter.memory.createLinkedTask
import starter.memory.getTasks
import starter.memory.nukesCheck
import starter.memory.powerProcessor
import starter.memory.resourceBalance
import starter.memory.room
import starter.memory.sleepUntilTimes
import starter.memory.type
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 14.10.2020
 */
class NukesCheckManager(val room: Room) {

    fun execute() {
        if (room.memory.sleepUntilTimes.nukesCheck > Game.time) {
            return
        }

        val target = getTarget(room)

        if (target == null) {
            room.memory.sleepUntilTimes.nukesCheck = Game.time + 5000
            return
        }

        val tasks = room.memory.getTasks { it.type == TaskType.NUKE_SAVER.code }
        if (tasks.isEmpty()) {
            room.memory.createLinkedTask(TaskType.NUKE_SAVER, jsObject {
                this.room = this@NukesCheckManager.room.name
            })
            room.memory.sleepUntilTimes.nukesCheck = Game.time + 1500
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
                    val structures = room.lookForAt(LOOK_STRUCTURES, x, y) ?: emptyArray()
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