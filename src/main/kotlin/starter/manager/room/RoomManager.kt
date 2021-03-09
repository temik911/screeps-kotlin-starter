package starter.manager.room

import screeps.api.Room
import starter.manager.BaseEnergyFillManager
import starter.manager.LabProcessor
import starter.manager.NukesCheckManager
import starter.manager.PowerProcessorV2
import starter.manager.ReserveRemoteRoomsManager
import starter.manager.ResourceBalanceManager
import starter.manager.SpawnManager
import starter.manager.TerminalManager
import starter.memory.checkLinkedTasks
import starter.memory.reservedRooms

/**
 *
 *
 * @author zakharchuk
 * @since 12.06.2020
 */
class RoomManager(val room: Room) {

    fun manage() {
        try {
            manageInternal()
        } catch (e: Throwable) {
            console.log("Error on room ${room.name} manage: $e")
        }
    }

    private fun manageInternal() {
        room.memory.checkLinkedTasks()
        BaseEnergyFillManager(room).execute()
        SpawnManager(room).trySpawn()
        ResourceBalanceManager(room).execute()
        ReserveRemoteRoomsManager(room).execute()
        TerminalManager(room).execute()
        LabProcessor(room).execute()
        PowerProcessorV2(room).execute()
        NukesCheckManager(room).execute()
        manageReserveRooms(room)
    }

    private fun manageReserveRooms(room: Room) {
        room.memory.reservedRooms.forEach {
            RemoteRoomManager(room, it).manage()
        }
    }
}