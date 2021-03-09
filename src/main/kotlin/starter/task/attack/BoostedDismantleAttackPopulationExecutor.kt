package starter.task.attack

import screeps.api.FIND_HOSTILE_STRUCTURES
import screeps.api.Game
import screeps.api.Market
import screeps.api.OK
import screeps.api.ORDER_SELL
import screeps.api.RESOURCE_CATALYZED_GHODIUM_ALKALIDE
import screeps.api.RESOURCE_CATALYZED_KEANIUM_ALKALIDE
import screeps.api.RESOURCE_CATALYZED_LEMERGIUM_ALKALIDE
import screeps.api.RESOURCE_CATALYZED_ZYNTHIUM_ACID
import screeps.api.RESOURCE_CATALYZED_ZYNTHIUM_ALKALIDE
import screeps.api.ResourceConstant
import screeps.api.Room
import screeps.api.STRUCTURE_SPAWN
import screeps.api.get
import screeps.utils.unsafe.jsObject
import starter.extension.myRooms
import starter.getUsed
import starter.memory.TaskMemory
import starter.memory.checkLinkedTasks
import starter.memory.createLinkedTask
import starter.memory.isDone
import starter.memory.linkedTaskIds
import starter.memory.resourceType
import starter.memory.room
import starter.memory.sleepUntil
import starter.memory.toRoom
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 11.05.2020
 */
class BoostedDismantleAttackPopulationExecutor(val task: TaskMemory) {
    fun execute() {
        if (task.sleepUntil > Game.time) {
            return
        }

        val toRoom = Game.rooms.get(task.toRoom)
        if (toRoom != null) {
            if ((toRoom.controller!!.safeMode ?: 0) > 0) {
                task.sleepUntil = Game.time + toRoom.controller!!.safeMode!!
                return
            }

            val spawn = toRoom.find(FIND_HOSTILE_STRUCTURES).firstOrNull { it.structureType == STRUCTURE_SPAWN }
            if (spawn == null) {
                task.isDone = true
                return
            }
        }

        task.checkLinkedTasks()
        if (task.linkedTaskIds.size < 1) {
            val room = Game.rooms.get(task.room) ?: return
            val notEnoughResource = getNotEnoughResource(room)
            if (notEnoughResource != null) {
                getResource(room, notEnoughResource)
                return
            }
            task.createLinkedTask(TaskType.BOOSTED_DISMANTLE_ATTACK, jsObject {
                this.room = task.room
                this.toRoom = task.toRoom
            })
        }
    }

    private fun getResource(room: Room, resource: ResourceConstant) {
        val roomWithResource = Game.myRooms().firstOrNull {
            isEnoughToSend(it, resource)
        }

        if (roomWithResource != null) {
            val terminal = roomWithResource.terminal ?: return
            if (terminal.cooldown != 0) {
                return
            }

            terminal.send(resource, 1000, room.name)
        } else {
            val marketOrder = Game.market.getAllOrders(jsObject<Market.Order.Filter> {
                this.resourceType = resource.toString()
                this.type = ORDER_SELL
            }).filter { it.amount > 0 }
                    .minBy { it.price } ?: return

            Game.market.deal(marketOrder.id, marketOrder.amount, room.name)
        }
    }

    private fun isEnoughToSend(room: Room, resource: ResourceConstant): Boolean {
        val terminal = room.terminal ?: return false
        return terminal.store.getUsed(resource) > 1000
    }

    private fun getNotEnoughResource(room: Room): ResourceConstant? {
        val resources = listOf(RESOURCE_CATALYZED_LEMERGIUM_ALKALIDE, RESOURCE_CATALYZED_KEANIUM_ALKALIDE,
                RESOURCE_CATALYZED_ZYNTHIUM_ALKALIDE, RESOURCE_CATALYZED_GHODIUM_ALKALIDE, RESOURCE_CATALYZED_ZYNTHIUM_ACID)
        val storage = room.storage!!
        val terminal = room.terminal!!
        return resources.firstOrNull {
            storage.store.getUsed(it) + terminal.store.getUsed(it) < 2000
        }
    }
}