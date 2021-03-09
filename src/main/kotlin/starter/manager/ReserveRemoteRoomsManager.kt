package starter.manager

import screeps.api.Game
import screeps.api.ROOM_STATUS_NORMAL
import screeps.api.Room
import screeps.api.get
import screeps.api.values
import screeps.utils.unsafe.jsObject
import starter.extension.isHighway
import starter.extension.isSk
import starter.memory.createLinkedTask
import starter.memory.distance
import starter.memory.reserveRemoteRoomsManager
import starter.memory.room
import starter.memory.sleepUntilTimes
import starter.memory.toRoom
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 13.06.2020
 */
class ReserveRemoteRoomsManager(val room: Room) {
    fun execute() {
        if (room.controller!!.level < 5) {
            return
        }

        if (room.memory.sleepUntilTimes.reserveRemoteRoomsManager > Game.time) {
            return
        }

        val distance = when (room.controller!!.level) {
            5,6 -> 1
            7,8 -> 2
            else -> 0
        }

        val roomStack = mutableSetOf(RoomWithDistance(room.name, 0))
        val alreadyChecked = mutableSetOf<String>()
        val availableRooms = mutableSetOf<RoomWithDistance>()
        while (!roomStack.isEmpty()) {
            val roomWithDistance = roomStack.first()
            roomStack.remove(roomWithDistance)
            alreadyChecked.add(roomWithDistance.roomName)
            if (roomWithDistance.distance > distance) {
                continue
            }
            val roomName = roomWithDistance.roomName
            if (Game.map.getRoomStatus(roomName).status != ROOM_STATUS_NORMAL) {
                continue
            }
            if (isHighway(roomName) || isSk(roomName)) {
                continue
            }

            val toRoom = Game.rooms.get(roomName)
            if (toRoom?.controller?.my == true || toRoom?.controller?.reservation?.username == "temik911") {
                val exits = Game.map.describeExits(roomWithDistance.roomName)
                if (exits == null) {
                    continue
                }
                exits.values.forEach {
                    if (!alreadyChecked.contains(it)) {
                        roomStack.add(RoomWithDistance(it, roomWithDistance.distance + 1))
                    }
                }
            } else {
                availableRooms.add(roomWithDistance)
            }
        }

        availableRooms.forEach {
            room.memory.createLinkedTask(TaskType.REMOTE_ROOM_ANALYZER, jsObject {
                this.room = this@ReserveRemoteRoomsManager.room.name
                this.toRoom = it.roomName
                this.distance = it.distance
            })
        }

        room.memory.sleepUntilTimes.reserveRemoteRoomsManager = Game.time + 5000
    }

    private class RoomWithDistance(val roomName: String, val distance: Int) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class.js != other::class.js) return false

            other as RoomWithDistance

            if (roomName != other.roomName) return false
            if (distance != other.distance) return false

            return true
        }

        override fun hashCode(): Int {
            var result = roomName.hashCode()
            result = 31 * result + distance
            return result
        }
    }
}