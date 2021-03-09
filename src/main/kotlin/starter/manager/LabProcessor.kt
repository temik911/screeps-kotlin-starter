package starter.manager

import screeps.api.FIND_MY_STRUCTURES
import screeps.api.Game
import screeps.api.REACTIONS
import screeps.api.RESOURCE_CATALYZED_GHODIUM_ACID
import screeps.api.RESOURCE_GHODIUM
import screeps.api.ResourceConstant
import screeps.api.Room
import screeps.api.STRUCTURE_LAB
import screeps.api.component1
import screeps.api.component2
import screeps.api.entries
import screeps.api.options
import screeps.api.structures.StructureStorage
import screeps.utils.unsafe.jsObject
import starter.SIMPLE_MINERALS
import starter.STORAGE_COMPOUNDS_AMOUNT
import starter.TIER_3_COMPOUNDS
import starter.getUsed
import starter.memory.LabReactionTask
import starter.memory.amount
import starter.memory.createLinkedTask
import starter.memory.getTasks
import starter.memory.labProcessor
import starter.memory.labReactionTask
import starter.memory.resource1
import starter.memory.resource2
import starter.memory.room
import starter.memory.sleepUntilTimes
import starter.memory.type
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 30.03.2020
 */
class LabProcessor(val room: Room) {
    fun execute() {
        if (room.controller!!.level < 7) {
            return
        }

        if (room.memory.sleepUntilTimes.labProcessor > Game.time) {
            return
        }

        if (room.find(FIND_MY_STRUCTURES, options { filter = { it.structureType == STRUCTURE_LAB } }).count() < 6) {
            room.memory.sleepUntilTimes.labProcessor = Game.time + 5000
            return
        }

        val currentTaskReaction = room.memory.getTasks { it.type == TaskType.LAB_REACTION.code }

        if (currentTaskReaction.isNotEmpty()) {
            room.memory.sleepUntilTimes.labProcessor = Game.time + 100
            return
        }

        val storage = room.storage ?: return

        val neededCompounds = if (room.controller!!.level != 8) {
            mutableListOf()
        } else {
            TIER_3_COMPOUNDS.plus(RESOURCE_GHODIUM).filter { storage.store.getUsed(it) < STORAGE_COMPOUNDS_AMOUNT }.toMutableList()
        }
        if (neededCompounds.isEmpty()) {
            neededCompounds.add(RESOURCE_CATALYZED_GHODIUM_ACID)
        }

        neededCompounds.sortBy { resource -> storage.store.getUsed(resource) }

        val labReactionTask = findNextReaction(storage, neededCompounds)

        if (labReactionTask == null) {
            room.memory.sleepUntilTimes.labProcessor = Game.time + 50
            return
        }

        room.memory.createLinkedTask(TaskType.LAB_REACTION, jsObject {
            this.room = this@LabProcessor.room.name
            this.labReactionTask = labReactionTask
        })
    }

    private fun findNextReaction(storage: StructureStorage, neededCompounds: List<ResourceConstant>): LabReactionTask? {
        for (it in neededCompounds) {
            val amount = (storage.store.getUsedCapacity(it) ?: 0) + 3000
            val nextReaction = findNextReaction(storage, it, amount) ?: continue

            return jsObject<LabReactionTask> {
                this.resource1 = nextReaction.first
                this.resource2 = nextReaction.second
                this.amount = nextReaction.amount
            }
        }

        return null
    }

    private fun findNextReaction(storage: StructureStorage, resource: ResourceConstant, amount: Int): NextReaction? {
        val unModedAmount = amount - (storage.store.getUsedCapacity(resource) ?: 0)
        val moded = unModedAmount / 5 * 5
        val requiredAmount = if (moded < unModedAmount) moded + 5 else moded
        if (requiredAmount <= 0 || SIMPLE_MINERALS.contains(resource)) {
            return null
        }
        REACTIONS.entries.forEach { firstComponent ->
            firstComponent.component2().entries.forEach { secondComponent ->
                if (secondComponent.component2() == resource) {
                    val first = firstComponent.component1()
                    val second = secondComponent.component1()
                    if (storage.store.getUsedCapacity(first) ?: 0 >= requiredAmount && storage.store.getUsedCapacity(second) ?: 0 >= requiredAmount) {
                        return NextReaction(requiredAmount, first, second)
                    }
                    return findNextReaction(storage, first, requiredAmount) ?: findNextReaction(storage, second, requiredAmount)
                }
            }
        }
        return null
    }

    private class NextReaction(val amount: Int, val first: ResourceConstant, val second: ResourceConstant) {
        override fun toString(): String {
            return "NextReaction(amount=$amount, first=$first, second=$second)"
        }
    }
}