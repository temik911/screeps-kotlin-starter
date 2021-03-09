package starter.task.claim

import screeps.api.FIND_CONSTRUCTION_SITES
import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.OK
import screeps.api.STRUCTURE_CONTROLLER
import screeps.api.get
import starter.memory.TaskMemory
import starter.memory.isDone
import starter.memory.room

/**
 *
 *
 * @author zakharchuk
 * @since 22.03.2020
 */
class UnClaimRoomExecutor(val task: TaskMemory) {
    fun execute() {
        val room = Game.rooms.get(task.room)?:return
        val structures = room.find(FIND_STRUCTURES)
        val constructionSites = room.find(FIND_CONSTRUCTION_SITES)

        if ((structures.isNullOrEmpty() || (structures.size == 1 && structures[0].structureType == STRUCTURE_CONTROLLER))
                && (constructionSites.isNullOrEmpty() || constructionSites.all { it.my })) {
            if (room.controller!!.unclaim() == OK) {
                task.isDone = true
            }
        }
    }
}