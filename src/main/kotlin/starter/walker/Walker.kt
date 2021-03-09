package starter.walker

import screeps.api.Creep
import screeps.api.FIND_MINERALS
import screeps.api.FIND_SOURCES
import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.MoveToOptions
import screeps.api.NavigationTarget
import screeps.api.PathFinder
import screeps.api.get
import screeps.api.options

/**
 *
 *
 * @author zakharchuk
 * @since 18.04.2020
 */

fun Creep.walkTo(target: NavigationTarget, noSkRoom: Boolean = true, ignoreRooms: List<String> = emptyList(), maxRooms: Int = 16) {
    moveTo(target, options {
        this.costCallback = { roomName, costMatrix -> calculateCostMatrix(roomName, costMatrix, noSkRoom, ignoreRooms.minus(room.name)) }
        this.visualizePathStyle = options { }
        this.maxOps = 25000
        this.maxRooms = maxRooms
    })
}

fun calculateCostMatrix(roomName: String, costMatrix: PathFinder.CostMatrix, noSkRoom: Boolean, ignoreRooms: List<String>): PathFinder.CostMatrix {
    if (ignoreRooms.contains(roomName)) {
        unWalkableRoom(costMatrix)
        return costMatrix
    }
    if (noSkRoom) {
        val i = roomName.substring(1, 3).toInt() % 10
        val j = roomName.substring(4, 6).toInt() % 10
        if ((i in 4..6) && (j in 4..6)) {
            val room = Game.rooms.get(roomName) ?: return costMatrix
            room.find(FIND_STRUCTURES, options {
                filter = {
                    it.structureType == screeps.api.STRUCTURE_KEEPER_LAIR
                }
            }).forEach { unWalkable(it.pos.x - 5..it.pos.x + 5, it.pos.y - 5..it.pos.y + 5, costMatrix) }
            room.find(FIND_SOURCES).forEach { unWalkable(it.pos.x - 5..it.pos.x + 5, it.pos.y - 5..it.pos.y + 5, costMatrix) }
            room.find(FIND_MINERALS).forEach { unWalkable(it.pos.x - 5..it.pos.x + 5, it.pos.y - 5..it.pos.y + 5, costMatrix) }
        }
    }
    return costMatrix
}

private fun unWalkableRoom(costMatrix: PathFinder.CostMatrix) {
    unWalkable((0..49), (0..49), costMatrix)
}

private fun unWalkable(xRange: IntRange, yRange: IntRange, costMatrix: PathFinder.CostMatrix) {
    xRange.forEach { x ->
        yRange.forEach { y ->
            if (x in 0..49 && y in 0..49) {
                costMatrix.set(x, y, 255)
            }
        }
    }
}