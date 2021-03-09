package starter.task.observe

import screeps.api.FIND_MY_STRUCTURES
import screeps.api.Game
import screeps.api.OK
import screeps.api.STRUCTURE_OBSERVER
import screeps.api.get
import screeps.api.options
import screeps.api.structures.StructureObserver
import screeps.api.values
import screeps.utils.unsafe.jsObject
import starter.createTask
import starter.memory.TaskMemory
import starter.memory.isPrepareDone
import starter.memory.observerId
import starter.memory.observerRooms
import starter.memory.room
import starter.memory.sleepUntil
import starter.memory.toRoom
import starter.memory.type
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 28.04.2020
 */
class ObserveExecutor(val task: TaskMemory) {

    fun execute() {
        val room = Game.rooms.get(task.room)!!
        if (!task.isPrepareDone) {
            val roomStack = mutableSetOf(RoomWithDistance(room.name, 0))
            val alreadyChecked = mutableSetOf<String>()
            val availableRooms = mutableSetOf<String>()
            while (!roomStack.isEmpty()) {
                val roomWithDistance = roomStack.first()
                roomStack.remove(roomWithDistance)
                alreadyChecked.add(roomWithDistance.roomName)
                if (roomWithDistance.distance >= 5) {
                    continue
                }
                val roomName = roomWithDistance.roomName
                val i = roomName.substring(1, 3).toInt() % 10
                val j = roomName.substring(4, 6).toInt() % 10
                if ((i == 0) || (j == 0)) {
                    availableRooms.add(roomName)
                }
                val exits = Game.map.describeExits(roomWithDistance.roomName)
                if (exits == null) {
                    continue
                }
                exits.values.forEach {
                    if (!alreadyChecked.contains(it)) {
                        roomStack.add(RoomWithDistance(it, roomWithDistance.distance + 1))
                    }
                }
            }
            task.observerRooms = availableRooms.toTypedArray()
            task.isPrepareDone = true
            return
        }

        if (task.observerId == null) {
            val observer = room.find(FIND_MY_STRUCTURES, options { filter = { it.structureType == STRUCTURE_OBSERVER } }).firstOrNull()?: return
            task.observerId = observer.id
            return
        }

        val observer = Game.getObjectById<StructureObserver>(task.observerId!!)
        if (observer == null) {
            task.observerId = null
            return
        }

        val roomToObserve = task.observerRooms[Game.time % task.observerRooms.size]
        if (observer.observeRoom(roomToObserve) == OK) {
            createTask("${task.room}|$roomToObserve|${TaskType.OBSERVE_ANALYZER.code}", jsObject {
                this.type = TaskType.OBSERVE_ANALYZER.code
                this.room = task.room
                this.toRoom = roomToObserve
                this.sleepUntil = Game.time + 1
            })
        }
    }

    private class RoomWithDistance(val roomName: String, val distance: Int)
}