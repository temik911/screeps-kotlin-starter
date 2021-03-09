package starter.task.harvest.sk

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.FIND_STRUCTURES
import screeps.api.FilterOption
import screeps.api.Game
import screeps.api.Room
import screeps.api.STRUCTURE_KEEPER_LAIR
import screeps.api.Source
import screeps.api.get
import screeps.api.options
import screeps.api.structures.Structure
import screeps.api.structures.StructureKeeperLair
import starter.calculateSkHarvestBody
import starter.extension.moveToWithSwap
import starter.memory.TaskMemory
import starter.memory.keeperLairId
import starter.memory.room
import starter.task.harvest.HarvestExecutor

/**
 *
 *
 * @author zakharchuk
 * @since 05.04.2020
 */
class SkHarvestExecutor(task: TaskMemory) : HarvestExecutor(task) {
    override fun harvest(creep: Creep, source: Source) {
        if (task.keeperLairId == null) {
            val keeperLair = source.pos.findClosestByRange(FIND_STRUCTURES, options<FilterOption<Structure>> {
                filter = {
                    it.structureType == STRUCTURE_KEEPER_LAIR
                }
            }) ?: return
            task.keeperLairId = keeperLair.id
        }

        val keeperLair = Game.getObjectById<StructureKeeperLair>(task.keeperLairId) ?: return
        if (keeperLair.ticksToSpawn == null || keeperLair.ticksToSpawn!! < 25) {
            if (creep.pos.getRangeTo(keeperLair.pos) <= 5 || creep.pos.getRangeTo(source.pos) <= 5) {
                val room = Game.rooms.get(task.room)!!
                creep.moveToWithSwap(room.storage!!)
            }
            return
        }

        super.harvest(creep, source)
    }

    override fun getCreepBody(room: Room): Array<BodyPartConstant> = room.calculateSkHarvestBody()
}