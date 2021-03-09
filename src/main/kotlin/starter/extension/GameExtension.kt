package starter.extension

import screeps.api.Game
import screeps.api.Room
import screeps.api.values

/**
 *
 *
 * @author zakharchuk
 * @since 11.05.2020
 */

fun Game.myRooms(): List<Room> {
    return rooms.values.filter { it.controller != null && it.controller!!.my }
}