package starter.extension

import screeps.api.BOTTOM
import screeps.api.BOTTOM_LEFT
import screeps.api.BOTTOM_RIGHT
import screeps.api.Creep
import screeps.api.DirectionConstant
import screeps.api.FIND_HOSTILE_CREEPS
import screeps.api.Game
import screeps.api.GenericCreep
import screeps.api.LEFT
import screeps.api.LOOK_CREEPS
import screeps.api.LOOK_POWER_CREEPS
import screeps.api.PWR_GENERATE_OPS
import screeps.api.PathFinder
import screeps.api.PowerCreep
import screeps.api.PowerEffectConstant
import screeps.api.RIGHT
import screeps.api.Room
import screeps.api.RoomObject
import screeps.api.RoomPosition
import screeps.api.TOP
import screeps.api.TOP_LEFT
import screeps.api.TOP_RIGHT
import screeps.api.get
import screeps.api.options
import starter.memory._move
import starter.memory.path

fun GenericCreep.moveToWithSwap(target: screeps.api.NavigationTarget, maxRooms: Int = 16): screeps.api.ScreepsReturnCode {
    val moveToResult = moveTo(target, options {
        this.ignoreCreeps = true
        this.maxRooms = maxRooms
        this.costCallback = { roomName, costMatrix -> calculateCostMatrix(roomName, costMatrix) }
    })

    val _move = memory._move
    if (_move != null && _move.path != null && _move.path!!.isNotEmpty()) {
        val path = _move.path!!
        room.visual.circle(pos, options {
            radius = 0.6
            fill = "white"
        })
        val deserializePath = Room.deserializePath(path)
        if (deserializePath.isNotEmpty()) {
            val nextPathStep = deserializePath[0]
            if (nextPathStep.x != pos.x || nextPathStep.y != pos.y) {
                val nextPosition = RoomPosition(nextPathStep.x, nextPathStep.y, room.name)
                room.visual.circle(nextPosition, options {
                    radius = 0.4
                    fill = "red"
                })
                val anotherCreep = nextPosition.lookFor(LOOK_CREEPS)?.filter { it.my }?.getOrNull(0)
                        ?:nextPosition.lookFor(LOOK_POWER_CREEPS)?.filter { it.my }?.getOrNull(0)
                if (anotherCreep != null && ((anotherCreep is Creep && anotherCreep.fatigue == 0) || anotherCreep  is PowerCreep)) {
                    val direction = nextPathStep.direction
                    anotherCreep.move(oppositeDirection(direction))
                    say("Thank's", toPublic = true)
                    anotherCreep.say("No problem", toPublic = true)
                }
            }
        }
    }
    return moveToResult
}

fun oppositeDirection(direction: DirectionConstant): DirectionConstant {
    return when (direction) {
        TOP -> BOTTOM
        TOP_RIGHT -> BOTTOM_LEFT
        RIGHT -> LEFT
        BOTTOM_RIGHT -> TOP_LEFT
        BOTTOM -> TOP
        BOTTOM_LEFT -> TOP_RIGHT
        LEFT -> RIGHT
        TOP_LEFT -> BOTTOM_RIGHT
        else -> throw IllegalStateException("Unknown direction: $direction")
    }
}

fun Creep.moveToOppositeDirection(roomObject: RoomObject) {
    move(oppositeDirection(pos.getDirectionTo(roomObject)))
}

fun PowerCreep.generateOps(): Boolean {
    if (getCooldown(PWR_GENERATE_OPS) != 0) {
        return false
    }
    usePower(PWR_GENERATE_OPS)
    return true
}

fun PowerCreep.getCooldown(powerEffectConstant: PowerEffectConstant): Int {
    return this.powers[powerEffectConstant]?.cooldown?: 0
}

fun calculateCostMatrix(roomName: String, costMatrix: PathFinder.CostMatrix): PathFinder.CostMatrix {
    val room = Game.rooms.get(roomName) ?: return costMatrix
    room.find(FIND_HOSTILE_CREEPS).forEach { it -> costMatrix.set(it.pos.x, it.pos.y, 255) }
    return costMatrix
}