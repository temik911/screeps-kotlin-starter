package starter

import screeps.api.BuildableStructureConstant
import screeps.api.CostMatrix
import screeps.api.ExitConstant
import screeps.api.FIND_EXIT_BOTTOM
import screeps.api.FIND_EXIT_LEFT
import screeps.api.FIND_EXIT_RIGHT
import screeps.api.FIND_EXIT_TOP
import screeps.api.FIND_MINERALS
import screeps.api.FIND_SOURCES
import screeps.api.PathFinder
import screeps.api.Room
import screeps.api.RoomPosition
import screeps.api.STRUCTURE_CONTAINER
import screeps.api.STRUCTURE_EXTENSION
import screeps.api.STRUCTURE_EXTRACTOR
import screeps.api.STRUCTURE_FACTORY
import screeps.api.STRUCTURE_LAB
import screeps.api.STRUCTURE_LINK
import screeps.api.STRUCTURE_NUKER
import screeps.api.STRUCTURE_OBSERVER
import screeps.api.STRUCTURE_POWER_SPAWN
import screeps.api.STRUCTURE_ROAD
import screeps.api.STRUCTURE_SPAWN
import screeps.api.STRUCTURE_STORAGE
import screeps.api.STRUCTURE_TERMINAL
import screeps.api.STRUCTURE_TOWER
import screeps.api.TERRAIN_MASK_WALL
import screeps.api.options
import screeps.utils.unsafe.jsObject
import starter.StructurePlan.CONTR_LINK
import starter.StructurePlan.PROHIBITED
import starter.StructurePlan._CONTAINER
import starter.StructurePlan._EXTENSION
import starter.StructurePlan._EXTRACTOR
import starter.StructurePlan._MNRAL_CNT
import starter.StructurePlan._PWR_SPAWN
import starter.StructurePlan._STOR_LINK
import starter.StructurePlan.__OBSERVER
import starter.StructurePlan.__RESERVED
import starter.StructurePlan.__RSV_CNTR
import starter.StructurePlan.__RSV_CONT
import starter.StructurePlan.__TERMINAL
import starter.StructurePlan.___BLOCKED
import starter.StructurePlan.___FACTORY
import starter.StructurePlan.___STORAGE
import starter.StructurePlan._____EMPTY
import starter.StructurePlan._____NUKER
import starter.StructurePlan._____SPAWN
import starter.StructurePlan._____TOWER
import starter.StructurePlan.______LINK
import starter.StructurePlan.______ROAD
import starter.StructurePlan._______LAB
import kotlin.math.pow
import kotlin.math.sqrt

enum class StructurePlan(val code: String) {
    PROHIBITED("prohibited"),
    __RESERVED("reserved"),
    __RSV_CONT("reservedCont"),
    __RSV_CNTR("reservedController"),
    _____EMPTY("empty"),
    ______ROAD("road"),
    _____SPAWN("spawn"),
    _EXTENSION("extension"),
    _____TOWER("tower"),
    CONTR_LINK("controllerLink"),
    _STOR_LINK("storageLink"),
    ______LINK("link"), // harvesterLink
    __TERMINAL("terminal"),
    ___STORAGE("storage"),
    _______LAB("lab"),
    _EXTRACTOR("extractor"),
    _CONTAINER("container"),
    _MNRAL_CNT("mineralContainer"),
    __OBSERVER("observer"),
    _PWR_SPAWN("powerSpawn"),
    _____NUKER("nuker"),
    ___FACTORY("factory"),
    ___BLOCKED("blocked")
}

val spawnPlan: List<List<StructurePlan>> = listOf(
        listOf(_____EMPTY, _____EMPTY, ______ROAD, _____EMPTY, _____EMPTY),
        listOf(_____EMPTY, ______ROAD, ______ROAD, ______ROAD, _____EMPTY),
        listOf(______ROAD, ______ROAD, _____SPAWN, ______ROAD, ______ROAD),
        listOf(_____EMPTY, ______ROAD, ______ROAD, ______ROAD, _____EMPTY),
        listOf(_____EMPTY, _____EMPTY, ______ROAD, _____EMPTY, _____EMPTY)
)

val extensionPlan: List<List<StructurePlan>> = listOf(
        listOf(_____EMPTY, _____EMPTY, ______ROAD, _____EMPTY, _____EMPTY),
        listOf(_____EMPTY, ______ROAD, _EXTENSION, ______ROAD, _____EMPTY),
        listOf(______ROAD, _EXTENSION, _EXTENSION, _EXTENSION, ______ROAD),
        listOf(_____EMPTY, ______ROAD, _EXTENSION, ______ROAD, _____EMPTY),
        listOf(_____EMPTY, _____EMPTY, ______ROAD, _____EMPTY, _____EMPTY)
)

val towerPlan: List<List<StructurePlan>> = listOf(
        listOf(_____EMPTY, _____EMPTY, _____EMPTY, _____EMPTY, _____EMPTY),
        listOf(_____EMPTY, _____EMPTY, ______ROAD, _____EMPTY, _____EMPTY),
        listOf(_____EMPTY, ______ROAD, _____TOWER, ______ROAD, _____EMPTY),
        listOf(_____EMPTY, _____EMPTY, ______ROAD, _____EMPTY, _____EMPTY),
        listOf(_____EMPTY, _____EMPTY, _____EMPTY, _____EMPTY, _____EMPTY)
)

val storageTerminalPlan: List<List<StructurePlan>> = listOf(
        listOf(_____EMPTY, _____EMPTY, _____EMPTY, _____EMPTY, _____EMPTY),
        listOf(_____EMPTY, _____EMPTY, ______ROAD, _____EMPTY, _____EMPTY),
        listOf(_____EMPTY, ______ROAD, ______LINK, ______ROAD, _____EMPTY),
        listOf(______ROAD, __TERMINAL, ______ROAD, ___STORAGE, ______ROAD),
        listOf(_____EMPTY, ______ROAD, _____EMPTY, ______ROAD, _____EMPTY)
)

val labsPlan: List<List<StructurePlan>> = listOf(
        listOf(_____EMPTY, ______ROAD, ______ROAD, _____EMPTY, _____EMPTY),
        listOf(______ROAD, _______LAB, _______LAB, ______ROAD, _____EMPTY),
        listOf(_______LAB, ______ROAD, _______LAB, _______LAB, ______ROAD),
        listOf(_______LAB, _______LAB, ______ROAD, _______LAB, ______ROAD),
        listOf(_____EMPTY, _______LAB, _______LAB, ______ROAD, _____EMPTY)
)

val observerPlan: List<List<StructurePlan>> = listOf(
        listOf(_____EMPTY, _____EMPTY, ______ROAD, _____EMPTY, _____EMPTY),
        listOf(_____EMPTY, ______ROAD, _____NUKER, ______ROAD, _____EMPTY),
        listOf(______ROAD, _PWR_SPAWN, ______ROAD, ___FACTORY, ______ROAD),
        listOf(_____EMPTY, ______ROAD, __OBSERVER, ______ROAD, _____EMPTY),
        listOf(_____EMPTY, _____EMPTY, ______ROAD, _____EMPTY, _____EMPTY)
)

val corePlan: List<List<StructurePlan>> = listOf(
        listOf(___BLOCKED, ___BLOCKED, ___BLOCKED, ___BLOCKED, ___BLOCKED, ___BLOCKED, ___BLOCKED, ___BLOCKED, ___BLOCKED, ___BLOCKED, ___BLOCKED, ___BLOCKED, ___BLOCKED, ___BLOCKED, ___BLOCKED),
        listOf(___BLOCKED, _____EMPTY, __RESERVED, __RESERVED, _____EMPTY, _____EMPTY, __RESERVED, __RESERVED, __RESERVED, _____EMPTY, _____EMPTY, __RESERVED, __RESERVED, _____EMPTY, ___BLOCKED),
        listOf(___BLOCKED, __RESERVED, _____EMPTY, _____EMPTY, __RESERVED, __RESERVED, _____EMPTY, _____EMPTY, _____EMPTY, __RESERVED, __RESERVED, _____EMPTY, _____EMPTY, __RESERVED, ___BLOCKED),
        listOf(___BLOCKED, __RESERVED, _____EMPTY, __RESERVED, _____EMPTY, _____EMPTY, __RESERVED, _____EMPTY, __RESERVED, _____EMPTY, _____EMPTY, __RESERVED, _____EMPTY, __RESERVED, ___BLOCKED),
        listOf(___BLOCKED, _____EMPTY, __RESERVED, _____EMPTY, _____EMPTY, _____EMPTY, _____EMPTY, ______ROAD, _____EMPTY, _____EMPTY, _____EMPTY, _____EMPTY, __RESERVED, _____EMPTY, ___BLOCKED),
        listOf(___BLOCKED, _____EMPTY, __RESERVED, _____EMPTY, _____EMPTY, _____TOWER, ______ROAD, _____SPAWN, ______ROAD, _____TOWER, _____EMPTY, _____EMPTY, __RESERVED, _____EMPTY, ___BLOCKED),
        listOf(___BLOCKED, __RESERVED, _____EMPTY, __RESERVED, _____EMPTY, ______ROAD, ______ROAD, _____TOWER, ______ROAD, ______ROAD, _____EMPTY, __RESERVED, _____EMPTY, __RESERVED, ___BLOCKED),
        listOf(___BLOCKED, __RESERVED, _____EMPTY, _____EMPTY, ______ROAD, _____SPAWN, _____TOWER, ___STORAGE, _STOR_LINK, _PWR_SPAWN, ______ROAD, _____EMPTY, _____EMPTY, __RESERVED, ___BLOCKED),
        listOf(___BLOCKED, __RESERVED, _____EMPTY, __RESERVED, _____EMPTY, ______ROAD, ______ROAD, _____TOWER, ______ROAD, ______ROAD, __RESERVED, ______ROAD, ______ROAD, PROHIBITED, PROHIBITED),
        listOf(___BLOCKED, _____EMPTY, __RESERVED, _____EMPTY, _____EMPTY, _____TOWER, ______ROAD, _____SPAWN, ______ROAD, __TERMINAL, ______ROAD, _______LAB, _______LAB, ______ROAD, PROHIBITED),
        listOf(___BLOCKED, _____EMPTY, __RESERVED, _____EMPTY, _____EMPTY, _____EMPTY, _____EMPTY, ______ROAD, ___FACTORY, ______ROAD, ______ROAD, _______LAB, _______LAB, ______ROAD, PROHIBITED),
        listOf(___BLOCKED, __RESERVED, _____EMPTY, __RESERVED, _____EMPTY, _____EMPTY, __RESERVED, _____EMPTY, ______ROAD, _______LAB, _______LAB, ______ROAD, _______LAB, ______ROAD, PROHIBITED),
        listOf(___BLOCKED, __RESERVED, _____EMPTY, _____EMPTY, __RESERVED, __RESERVED, _____EMPTY, _____EMPTY, ______ROAD, _______LAB, _______LAB, _______LAB, ______ROAD, PROHIBITED, PROHIBITED),
        listOf(___BLOCKED, _____EMPTY, __RESERVED, __RESERVED, _____EMPTY, _____EMPTY, __RESERVED, __RESERVED, PROHIBITED, ______ROAD, ______ROAD, ______ROAD, PROHIBITED, PROHIBITED, PROHIBITED),
        listOf(___BLOCKED, ___BLOCKED, ___BLOCKED, ___BLOCKED, ___BLOCKED, ___BLOCKED, ___BLOCKED, ___BLOCKED, PROHIBITED, PROHIBITED, PROHIBITED, PROHIBITED, PROHIBITED, PROHIBITED, PROHIBITED)
)

val levelStructureFilter : Map<Int, Map<String, Int>> = mapOf(
        1 to mapOf(
                _____SPAWN.code to 1
        ),
        2 to mapOf(
                _____SPAWN.code to 1,
                _EXTENSION.code to 5
        ),
        3 to mapOf(
                ______ROAD.code to 2500,
                _CONTAINER.code to 3,
                _____SPAWN.code to 1,
                _EXTENSION.code to 10,
                _____TOWER.code to 1
        ),
        4 to mapOf(
                ______ROAD.code to 2500,
                _CONTAINER.code to 3,
                _____SPAWN.code to 1,
                _EXTENSION.code to 20,
                _____TOWER.code to 1,
                ___STORAGE.code to 1
        ),
        5 to mapOf(
                ______ROAD.code to 2500,
                _CONTAINER.code to 3,
                _____SPAWN.code to 1,
                _EXTENSION.code to 30,
                _____TOWER.code to 2,
                ___STORAGE.code to 1,
                _STOR_LINK.code to 1,
                CONTR_LINK.code to 1
        ),
        6 to mapOf(
                ______ROAD.code to 2500,
                _CONTAINER.code to 3,
                _____SPAWN.code to 1,
                _EXTENSION.code to 40,
                _____TOWER.code to 2,
                ___STORAGE.code to 1,
                _STOR_LINK.code to 1,
                CONTR_LINK.code to 1,
                ______LINK.code to 1,
                _EXTRACTOR.code to 1,
                _MNRAL_CNT.code to 1,
                _______LAB.code to 3,
                __TERMINAL.code to 1
        ),
        7 to mapOf(
                ______ROAD.code to 2500,
                _CONTAINER.code to 3,
                _____SPAWN.code to 2,
                _EXTENSION.code to 50,
                _____TOWER.code to 3,
                ___STORAGE.code to 1,
                _STOR_LINK.code to 1,
                CONTR_LINK.code to 1,
                ______LINK.code to 2,
                _EXTRACTOR.code to 1,
                _MNRAL_CNT.code to 1,
                _______LAB.code to 6,
                __TERMINAL.code to 1
        ),
        8 to mapOf(
                ______ROAD.code to 2500,
                _CONTAINER.code to 3,
                _____SPAWN.code to 3,
                _EXTENSION.code to 60,
                _____TOWER.code to 6,
                ___STORAGE.code to 1,
                _STOR_LINK.code to 1,
                CONTR_LINK.code to 1,
                ______LINK.code to 2,
                _EXTRACTOR.code to 1,
                _MNRAL_CNT.code to 1,
                _______LAB.code to 10,
                __TERMINAL.code to 1,
                __OBSERVER.code to 1,
                _____NUKER.code to 1,
                _PWR_SPAWN.code to 1,
                ___FACTORY.code to 1
        )
)

fun Room.getRoomPlan(roomPlan: Array<Triple<Int, Int, String>>, level: Int): Array<Triple<Int, Int, BuildableStructureConstant>> {
    val filter = HashMap(levelStructureFilter.get(level)!!)
    val structures: MutableList<Triple<Int, Int, String>> = mutableListOf()
    roomPlan.forEach { plan ->
        val currentValue = filter[plan.third]?: 0
        if (currentValue != 0) {
            structures.add(plan)
            filter[plan.third] = currentValue - 1
        }
    }

    return toPlan(structures)
}

fun toPlan(structures: MutableList<Triple<Int, Int, String>>): Array<Triple<Int, Int, BuildableStructureConstant>> {
    val plan = mutableListOf<Triple<Int, Int, BuildableStructureConstant>>()
    structures.forEach { structure ->
        when (structure.third) {
            ______ROAD.code -> plan.add(Triple(structure.first, structure.second, STRUCTURE_ROAD))
            _____SPAWN.code -> plan.add(Triple(structure.first, structure.second, STRUCTURE_SPAWN))
            _EXTENSION.code -> plan.add(Triple(structure.first, structure.second, STRUCTURE_EXTENSION))
            _____TOWER.code -> plan.add(Triple(structure.first, structure.second, STRUCTURE_TOWER))
            ______LINK.code -> plan.add(Triple(structure.first, structure.second, STRUCTURE_LINK))
            CONTR_LINK.code -> plan.add(Triple(structure.first, structure.second, STRUCTURE_LINK))
            _STOR_LINK.code -> plan.add(Triple(structure.first, structure.second, STRUCTURE_LINK))
            __TERMINAL.code -> plan.add(Triple(structure.first, structure.second, STRUCTURE_TERMINAL))
            ___STORAGE.code -> plan.add(Triple(structure.first, structure.second, STRUCTURE_STORAGE))
            _______LAB.code -> plan.add(Triple(structure.first, structure.second, STRUCTURE_LAB))
            _EXTRACTOR.code -> plan.add(Triple(structure.first, structure.second, STRUCTURE_EXTRACTOR))
            _CONTAINER.code -> plan.add(Triple(structure.first, structure.second, STRUCTURE_CONTAINER))
            _MNRAL_CNT.code -> plan.add(Triple(structure.first, structure.second, STRUCTURE_CONTAINER))
            __OBSERVER.code -> plan.add(Triple(structure.first, structure.second, STRUCTURE_OBSERVER))
            _PWR_SPAWN.code -> plan.add(Triple(structure.first, structure.second, STRUCTURE_POWER_SPAWN))
            _____NUKER.code -> plan.add(Triple(structure.first, structure.second, STRUCTURE_NUKER))
            ___FACTORY.code -> plan.add(Triple(structure.first, structure.second, STRUCTURE_FACTORY))
        }
    }
    return plan.toTypedArray()
}

fun Room.tryCorePlan(numberOfIteration: Int = 50): Array<Triple<Int, Int, String>>? {
    val matrix: MutableList<MutableList<String>> = createMatrix2()

    val startPosition = findStartPosition(matrix)
    console.log(startPosition)

    corePlan.forEachIndexed { y, list ->
        list.forEachIndexed { x, structurePlan ->
            if (matrix[startPosition.first!!.x + x][startPosition.first!!.y + y] == _____EMPTY.code) {
                matrix[startPosition.first!!.x + x][startPosition.first!!.y + y] = structurePlan.code
            }
        }
    }

    var totalExtensions = 0
    var isChanged = true
    var iteration = 0
    var position = startPosition.first!!
    var size = Pair(corePlan[0].size, corePlan.size)
    while (iteration < numberOfIteration && isChanged) {
        val pair = addExtensions(totalExtensions, matrix)
        totalExtensions = pair.first
        isChanged = pair.second
        if (totalExtensions != 60 && !isChanged) {
            val moveBlocked = moveBlocked(position, size, matrix)
            position = moveBlocked.first
            size = moveBlocked.second
            isChanged = moveBlocked.third
        }
        iteration++
    }

    matrix.forEachIndexed { x, list ->
        list.forEachIndexed { y, structurePlan ->
            if (structurePlan == ___BLOCKED.code) {
                matrix[x][y] = _____EMPTY.code
            }
        }
    }

    var storagePosition: RoomPosition? = null
    matrix.forEachIndexed { x, list ->
        list.forEachIndexed { y, structurePlan ->
            if (structurePlan == ___STORAGE.code) {
                storagePosition = getPositionAt(x, y)!!
            }
        }
    }

    val nukerPosition = findPositionFor(storagePosition!!, matrix)
    matrix[nukerPosition.x][nukerPosition.y] = _____NUKER.code
    val observerPosition = findPositionFor(storagePosition!!, matrix)
    matrix[observerPosition.x][observerPosition.y] = __OBSERVER.code

    placeContainer(controller!!.pos, storagePosition!!, _CONTAINER, matrix, __RSV_CNTR)
    placeLink(controller!!.pos, storagePosition!!, CONTR_LINK, matrix, __RSV_CNTR)

    find(FIND_SOURCES).forEach { source ->
        placeContainer(source.pos, storagePosition!!, _CONTAINER, matrix)
        console.log(source.pos)
        placeLink(source.pos, storagePosition!!, ______LINK, matrix, __RSV_CONT)
    }

    find(FIND_MINERALS).forEach { mineral ->
        placeContainer(mineral.pos, storagePosition!!, _MNRAL_CNT, matrix)
        matrix[mineral.pos.x][mineral.pos.y] = _EXTRACTOR.code
    }

    matrix.forEachIndexed { x, list ->
        list.forEachIndexed { y, structurePlan ->
            if (structurePlan == __RSV_CONT.code || structurePlan == __RSV_CNTR.code || structurePlan == PROHIBITED.code) {
                matrix[x][y] = _____EMPTY.code
            }
        }
    }

    findRoads2(matrix, storagePosition!!)
    return toPlainPlan(matrix)
}

fun toPlainPlan(matrix: MutableList<MutableList<String>>): Array<Triple<Int, Int, String>> {
    val plan = mutableListOf<Triple<Int, Int, String>>()
    matrix.forEachIndexed { x, list ->
        list.forEachIndexed { y, structurePlan ->
            when (structurePlan) {
                _____EMPTY.code, PROHIBITED.code, __RESERVED.code, __RSV_CONT.code, __RSV_CNTR.code, ___BLOCKED.code -> {
                    // do nothing
                }
                else -> {
                    plan.add(Triple(x, y, structurePlan))
                }
            }
        }
    }
    return plan.toTypedArray()
}

private fun Room.placeContainer(pos: RoomPosition, storagePosition: RoomPosition, containerType: StructurePlan, matrix: MutableList<MutableList<String>>, reserveStructure: StructurePlan = __RSV_CONT) {
    val possiblePositions: MutableList<RoomPosition> = mutableListOf()
    (pos.x - 1..pos.x + 1).forEach { posX ->
        (pos.y - 1..pos.y + 1).forEach { posY ->
            if (posX in 0..49 && posY in 0..49 && matrix[posX][posY] == reserveStructure.code) {
                possiblePositions.add(getPositionAt(posX, posY)!!)
            }
        }
    }

    val containerPosition = possiblePositions.minBy { it.getRangeTo(storagePosition) }!!
    matrix[containerPosition.x][containerPosition.y] = containerType.code
}

private fun Room.placeLink(pos: RoomPosition, storagePosition: RoomPosition, linkType: StructurePlan, matrix: MutableList<MutableList<String>>, reserveStructure: StructurePlan) {
    val linkPossiblePositions: MutableList<RoomPosition> = mutableListOf()
    (pos.x - 2..pos.x + 2).forEach { posX ->
        (pos.y - 2..pos.y + 2).forEach { posY ->
            if (posX in 0..49 && posY in 0..49 && matrix[posX][posY] == reserveStructure.code) {
                linkPossiblePositions.add(getPositionAt(posX, posY)!!)
            }
        }
    }
    val linkPosition = linkPossiblePositions.minBy { it.getRangeTo(storagePosition) }!!
    matrix[linkPosition.x][linkPosition.y] = linkType.code
}

fun Room.moveBlocked(startPosition: RoomPosition, size: Pair<Int, Int>, matrix: MutableList<MutableList<String>>): Triple<RoomPosition, Pair<Int, Int>, Boolean> {
    var isChanged = false
    val leftXDif = if (startPosition.x - 1 > 0) 1 else 0
    val leftYDif = if (startPosition.y - 1 > 0) 1 else 0
    val newStartPosition = RoomPosition(
            startPosition.x - leftXDif,
            startPosition.y - leftYDif,
            startPosition.roomName
    )
    val rightXDif = if (startPosition.x + size.first + 1 < 49) 1 else 0
    val rightYDif = if (startPosition.y + size.second + 1 < 49) 1 else 0
    val newXSize = size.first + leftXDif + rightXDif
    val newYSize = size.second + leftYDif + rightYDif
    val roadPosition: MutableList<RoomPosition> = mutableListOf()
    (0 until newXSize).forEach { x ->
        (0 until newYSize).forEach { y ->
            if ((x == 0 || x == newXSize - 1 || y == 0 || y == newYSize - 1)
                    && (matrix[newStartPosition.x + x][newStartPosition.y + y] == _____EMPTY.code
                            || matrix[newStartPosition.x + x][newStartPosition.y + y] == ___BLOCKED.code)) {
                matrix[newStartPosition.x + x][newStartPosition.y + y] = ___BLOCKED.code
                isChanged = true
            } else if (matrix[newStartPosition.x + x][newStartPosition.y + y] == ___BLOCKED.code) {
                var roadCount = 0
                (newStartPosition.x + x - 1..newStartPosition.x + x + 1).forEach { blockedPosX ->
                    (newStartPosition.y + y - 1..newStartPosition.y + y + 1).forEach { blockedPosY ->
                        if (blockedPosX in 0..49 && blockedPosY in 0..49) {
                            when (matrix[blockedPosX][blockedPosY]) {
                                ______ROAD.code -> roadCount++
                            }
                        }
                    }
                }
                if (roadCount == 1) {
                    roadPosition.add(getPositionAt(newStartPosition.x + x, newStartPosition.y + y)!!)
                } else {
                    matrix[newStartPosition.x + x][newStartPosition.y + y] = _____EMPTY.code
                    isChanged = true
                }
            }
        }
    }

    roadPosition.forEach { pos ->
        matrix[pos.x][pos.y] = ______ROAD.code
        isChanged = true
    }

    return Triple(newStartPosition, Pair(newXSize, newYSize), isChanged)
}

fun Room.addExtensions(currentExtension: Int, matrix: MutableList<MutableList<String>>): Pair<Int, Boolean> {
    var isChanged = false
    var totalExtensions = currentExtension
    val roadPosition: MutableList<RoomPosition> = mutableListOf()
    matrix.forEachIndexed { x, list ->
        list.forEachIndexed { y, structurePlan ->
            if (structurePlan == ______ROAD.code) {
                roadPosition.add(getPositionAt(x, y)!!)
            }
        }
    }

    roadPosition.forEach { pos ->
        (pos.x - 1..pos.x + 1).forEach { x ->
            (pos.y - 1..pos.y + 1).forEach { y ->
                if (x >= 0 && x <= 49 && y >= 0 && y <= 49) {
                    when (matrix[x][y]) {
                        _____EMPTY.code -> {
                            if (totalExtensions < 60) {
                                matrix[x][y] = _EXTENSION.code
                                totalExtensions++
                                isChanged = true
                            }
                        }
                        __RESERVED.code -> {
                            isChanged = true
                            matrix[x][y] = ______ROAD.code
                        }
                    }
                }
            }
        }
    }

    return Pair(totalExtensions, isChanged)
}

fun Room.findStartPosition(matrix: MutableList<MutableList<String>>): Pair<RoomPosition?, Int> {
    var startPosition: RoomPosition? = null
    var minValue = Int.MAX_VALUE
    (0..49).forEach { x ->
        (0..49).forEach { y ->
            val okCore = isOkCore(x, y, matrix)
            if (okCore > 0 && minValue > okCore) {
                startPosition = getPositionAt(x, y)!!
                minValue = okCore
            } else if (okCore == 0) {
                return Pair(getPositionAt(x, y)!!, okCore)
            }
        }
    }
    return Pair(startPosition, minValue)
}

fun Room.findPositionFor(storagePosition: RoomPosition, matrix: MutableList<MutableList<String>>): RoomPosition {
    val possiblePositions: MutableList<RoomPosition> = mutableListOf()
    matrix.forEachIndexed { x, list ->
        list.forEachIndexed { y, structurePlan ->
            if (structurePlan == ______ROAD.code) {
                (x - 1..x + 1).forEach { posX ->
                    (y - 1..y + 1).forEach { posY ->
                        if (posX in 0..49 && posY in 0..49 && matrix[posX][posY] == _____EMPTY.code) {
                            possiblePositions.add(getPositionAt(posX, posY)!!)
                        }
                    }
                }
            }
        }
    }

    return possiblePositions.minBy { pos ->
        pos.getRangeTo(storagePosition)
    }!!
}

fun Room.isOkCore(startX: Int, startY: Int, matrix: MutableList<MutableList<String>>): Int {
    var value = 0
    corePlan.forEachIndexed { y, list ->
        list.forEachIndexed { x, structurePlan ->
            if (startX + x < 0 || startX + x > 49 || startY + y < 0 || startY + y > 49) {
                return -1
            }
            when (structurePlan) {
                _____EMPTY, __RESERVED -> {
                    if (matrix[startX + x][startY + y] != _____EMPTY.code) {
                        value++
                    }
                }
                PROHIBITED, ___BLOCKED -> {

                }
                else -> {
                    if (matrix[startX + x][startY + y] != _____EMPTY.code) {
                        return -1
                    }
                }
            }
        }
    }
    return value
}

fun Room.createMatrix2(): MutableList<MutableList<String>> {
    val matrix: MutableList<MutableList<String>> = MutableList(50) { MutableList(50) { _____EMPTY.code } }
    val terrain = getTerrain()

    reserveInRange(controller!!.pos, 1, 1, terrain, matrix, __RSV_CNTR)
    find(FIND_SOURCES).forEach { source ->
        reserveInRange(source.pos, 1, 1, terrain, matrix, __RSV_CONT)
    }
    find(FIND_MINERALS).forEach { mineral ->
        reserveInRange(mineral.pos, 1, 0, terrain, matrix, __RSV_CONT)
    }

    prohibitExits(FIND_EXIT_TOP, matrix)
    prohibitExits(FIND_EXIT_RIGHT, matrix)
    prohibitExits(FIND_EXIT_BOTTOM, matrix)
    prohibitExits(FIND_EXIT_LEFT, matrix)

//    prohibitExits(FIND_EXIT_TOP, 0, 1, matrix)
//    prohibitExits(FIND_EXIT_RIGHT, -1, 0, matrix)
//    prohibitExits(FIND_EXIT_BOTTOM, 0, -1, matrix)
//    prohibitExits(FIND_EXIT_LEFT, 1, 0, matrix)

    (0..49).forEach { x ->
        (0..49).forEach { y ->
            if (terrain.get(x, y) == TERRAIN_MASK_WALL) {
                matrix[x][y] = PROHIBITED.code
            }
        }
    }

    return matrix
}

fun Room.reserveInRange(pos: RoomPosition, firstRange: Int, secondRange: Int, terrain: Room.Terrain, matrix: MutableList<MutableList<String>>, reserveStructure: StructurePlan) {
    (pos.x - firstRange..pos.x + firstRange).forEach { x ->
        (pos.y - firstRange..pos.y + firstRange).forEach { y ->
            if ((x >= 0 && x <= 49) && (y >= 0 && y <= 49) && terrain.get(x, y) != TERRAIN_MASK_WALL) {
                reserved((x - secondRange..x + secondRange), (y - secondRange..y + secondRange), matrix, reserveStructure)
            }
        }
    }
}

private fun reserved(xRange: IntRange, yRange: IntRange, matrix: List<MutableList<String>>, reserveStructure: StructurePlan) {
    xRange.forEach { x ->
        yRange.forEach { y ->
            if (x >= 0 || x <= 49) {
                if (y >= 0 || y <= 49) {
                    matrix[x][y] = reserveStructure.code
                }
            }
        }
    }
}

fun Room.prohibitExits(exitConstant: ExitConstant, matrix: MutableList<MutableList<String>>) {
    find(exitConstant).forEach { exit ->
        prohibit((exit.x - 1 .. exit.x + 1), (exit.y - 1..exit.y + 1), matrix)
    }
}

private fun Room.findRoads2(matrix: MutableList<MutableList<String>>, storagePosition: RoomPosition) {
    val to: MutableList<RoomPosition> = mutableListOf()
    (0..49).forEach { x ->
        (0..49).forEach { y ->
            if (matrix[x][y] == _CONTAINER.code || matrix[x][y] == _MNRAL_CNT.code
                    || matrix[x][y] == CONTR_LINK.code || matrix[x][y] == ______LINK.code) {
                to.add(getPositionAt(x, y)!!)
            }
        }
    }

    to.forEach { toPos ->
        val path = PathFinder.search(storagePosition, jsObject<PathFinder.GoalWithRange> {
            this.pos = toPos
            range = 1
        }, options {
            maxRooms = 1
            plainCost = 5
            swampCost = 5

            roomCallback = { _ -> callBack(matrix) }
        })

        if (!path.incomplete) {
            path.path.forEach {
                if (matrix[it.x][it.y] == _____EMPTY.code) {
                    matrix[it.x][it.y] = ______ROAD.code
                }
            }
        }
    }
}

fun callBack(matrix: MutableList<MutableList<String>>): CostMatrix {
    val costMatrix = PathFinder.CostMatrix()
    (0..49).forEach { x ->
        (0..49).forEach { y ->
            when (matrix[x][y]) {
                ______ROAD.code -> costMatrix.set(x, y, 1)
                _____EMPTY.code -> {
                }
                else -> costMatrix.set(x, y, 255)
            }
        }
    }
    return costMatrix
}


/* -------------------------------------------------------------------- */

fun Room.roadPlan(): Array<Triple<Int, Int, BuildableStructureConstant>>? {
    val roadPlan = mutableListOf<Triple<Int, Int, BuildableStructureConstant>>()
    val plan = plan(8) ?: return null
    plan.forEach {
        roadPlan.add(it)
    }
    return roadPlan.toTypedArray()
}

fun Room.externalPlan(): Array<Triple<Int, Int, BuildableStructureConstant>>? {
    val matrix: MutableList<MutableList<String>> = createMatrix()
    val terrain = getTerrain()

    containerEnergyPlan(matrix, terrain)
    return toPlan(matrix)

}

fun Room.skRoomPlan(): Array<Triple<Int, Int, BuildableStructureConstant>>? {
    val matrix: MutableList<MutableList<String>> = createMatrix()
    val terrain = getTerrain()

    containerEnergyPlan(matrix, terrain)
    find(FIND_MINERALS).forEach { findContainerPosition(matrix, terrain, it.pos) }
    return toPlan(matrix)

}

private fun Room.createMatrix(): MutableList<MutableList<String>> {
    val matrix: MutableList<MutableList<String>> = MutableList(50) { MutableList(50) { _____EMPTY.code } }
    val terrain = getTerrain()
    (0..49).forEach { x ->
        (0..49).forEach { y ->
            matrix[x][y] = if (terrain.get(x, y) == TERRAIN_MASK_WALL) PROHIBITED.code else _____EMPTY.code
        }
    }

    find(FIND_SOURCES).forEach {
        prohibit((it.pos.x - 2..it.pos.x + 2), (it.pos.y - 2..it.pos.y + 2), matrix)
    }
    if (controller != null) {
        prohibit((controller!!.pos.x - 2..controller!!.pos.x + 2), (controller!!.pos.y - 2..controller!!.pos.y + 2), matrix)
    }
    prohibit((0..4), (0..49), matrix)
    prohibit((45..49), (0..49), matrix)
    prohibit((0..49), (0..4), matrix)
    prohibit((0..49), (45..49), matrix)
    return matrix
}

fun Room.plan(level: Int, roadPlan: Array<Triple<Int, Int, BuildableStructureConstant>>? = null): Array<Triple<Int, Int, BuildableStructureConstant>>? {
    val matrix: MutableList<MutableList<String>> = createMatrix()
    val terrain = getTerrain()

    // 1 level
    val centralSpawn = find(matrix, spawnPlan, RoomPosition(25, 25, name), false) ?: return null

    if (level == 1) {
        matrix.forEachIndexed { x, line ->
            line.forEachIndexed { y, value ->
                if (value == ______ROAD.code) {
                    matrix[x][y] = _____EMPTY.code
                }
            }
        }
        return toPlan(matrix)
    }

    // 2 level
    find(matrix, extensionPlan, centralSpawn) ?: return null

    if (level == 2) {
        matrix.forEachIndexed { x, line ->
            line.forEachIndexed { y, value ->
                if (value == ______ROAD.code) {
                    matrix[x][y] = _____EMPTY.code
                }
            }
        }
        return toPlan(matrix)
    }

    // 3 level
    find(matrix, extensionPlan, centralSpawn) ?: return null
    find(matrix, towerPlan, centralSpawn) ?: return null
    containerEnergyPlan(matrix, terrain)
    containerControllerPlan(matrix, terrain)

    if (level == 3) {
        if (roadPlan != null) {
            findRoads(matrix, roadPlan)
        }
        return toPlan(matrix)
    }

    // 4 level
    find(matrix, extensionPlan, centralSpawn) ?: return null
    find(matrix, extensionPlan, centralSpawn) ?: return null
    val storageTerminal = find(matrix, storageTerminalPlan, centralSpawn) ?: return null

    if (level == 4) {
        if (roadPlan != null) {
            findRoads(matrix, roadPlan)
        }
        matrix.forEachIndexed { x, line ->
            line.forEachIndexed { y, value ->
                if (value == __TERMINAL.code || value == ______LINK.code) {
                    matrix[x][y] = _____EMPTY.code
                }
            }
        }
        return toPlan(matrix)
    }

    // 5 level
    find(matrix, extensionPlan, centralSpawn) ?: return null
    find(matrix, extensionPlan, centralSpawn) ?: return null
    find(matrix, towerPlan, centralSpawn) ?: return null

    if (level == 5) {
        if (roadPlan != null) {
            findRoads(matrix, roadPlan)
        }
        matrix.forEachIndexed { x, line ->
            line.forEachIndexed { y, value ->
                if (value == __TERMINAL.code || value == ______LINK.code) {
                    matrix[x][y] = _____EMPTY.code
                }
            }
        }
        return toPlan(matrix)
    }

    // 6 level
    find(matrix, extensionPlan, centralSpawn) ?: return null
    find(matrix, extensionPlan, centralSpawn) ?: return null
    extractorPlan(matrix, terrain)
    linksEnergyPlan(matrix, terrain)

    if (level == 6) {
        if (roadPlan != null) {
            findRoads(matrix, roadPlan)
        }
        return toPlan(matrix)
    }

    // 7 level
    find(matrix, spawnPlan, centralSpawn) ?: return null
    find(matrix, extensionPlan, centralSpawn) ?: return null
    find(matrix, extensionPlan, centralSpawn) ?: return null
    find(matrix, towerPlan, centralSpawn) ?: return null
    linkControllerPlan(matrix, terrain)

    if (level == 7) {
        if (roadPlan != null) {
            findRoads(matrix, roadPlan)
        }
        return toPlan(matrix)
    }

    // 8 level
    find(matrix, spawnPlan, centralSpawn) ?: return null
    find(matrix, extensionPlan, centralSpawn) ?: return null
    find(matrix, extensionPlan, centralSpawn) ?: return null
    find(matrix, labsPlan, storageTerminal) ?: return null
    find(matrix, observerPlan, storageTerminal) ?: return null
    find(matrix, towerPlan, centralSpawn) ?: return null
    find(matrix, towerPlan, centralSpawn) ?: return null
    find(matrix, towerPlan, centralSpawn) ?: return null
    findRoads(matrix, roadPlan)

    return toPlan(matrix)
}

fun toPlan(matrix: MutableList<MutableList<String>>): Array<Triple<Int, Int, BuildableStructureConstant>> {
    val plan = mutableListOf<Triple<Int, Int, BuildableStructureConstant>>()
    (0..49).forEach { x ->
        (0..49).forEach { y ->
            when (matrix[x][y]) {
                ______ROAD.code -> plan.add(Triple(x, y, STRUCTURE_ROAD))
                _____SPAWN.code -> plan.add(Triple(x, y, STRUCTURE_SPAWN))
                _EXTENSION.code -> plan.add(Triple(x, y, STRUCTURE_EXTENSION))
                _____TOWER.code -> plan.add(Triple(x, y, STRUCTURE_TOWER))
                ______LINK.code -> plan.add(Triple(x, y, STRUCTURE_LINK))
                __TERMINAL.code -> plan.add(Triple(x, y, STRUCTURE_TERMINAL))
                ___STORAGE.code -> plan.add(Triple(x, y, STRUCTURE_STORAGE))
                _______LAB.code -> plan.add(Triple(x, y, STRUCTURE_LAB))
                _EXTRACTOR.code -> plan.add(Triple(x, y, STRUCTURE_EXTRACTOR))
                _CONTAINER.code -> plan.add(Triple(x, y, STRUCTURE_CONTAINER))
                __OBSERVER.code -> plan.add(Triple(x, y, STRUCTURE_OBSERVER))
                _PWR_SPAWN.code -> plan.add(Triple(x, y, STRUCTURE_POWER_SPAWN))
                _____NUKER.code -> plan.add(Triple(x, y, STRUCTURE_NUKER))
                ___FACTORY.code -> plan.add(Triple(x, y, STRUCTURE_FACTORY))
            }
        }
    }
    return plan.toTypedArray()
}

private fun Room.findRoads(matrix: MutableList<MutableList<String>>, roadPlan: Array<Triple<Int, Int, BuildableStructureConstant>>? = null) {
    val from = mutableListOf<Pair<Int, Int>>()
    val to = mutableListOf<Pair<Int, Int>>()
    (0..49).forEach { x ->
        (0..49).forEach { y ->
            if (matrix[x][y] == ___STORAGE.code || matrix[x][y] == _____SPAWN.code) {
                from.add(Pair(x, y))
            }
            if (matrix[x][y] == _CONTAINER.code) {
                to.add(Pair(x, y))
            }
        }
    }

    from.forEach { fromPos ->
        to.forEach { toPos ->
            val path = PathFinder.search(getPositionAt(fromPos.first, fromPos.second)!!, getPositionAt(toPos.first, toPos.second)!!, options {
                maxRooms = 1
                plainCost = 5
                swampCost = 5

                roomCallback = { roomName -> callBackWithMatrix(matrix, roadPlan) }
            })

            if (!path.incomplete) {
                path.path.forEach {
                    if (matrix[it.x][it.y] == _____EMPTY.code || matrix[it.x][it.y] == PROHIBITED.code) {
                        matrix[it.x][it.y] = ______ROAD.code
                    }
                }
            }
        }
    }
}

fun callBackWithMatrix(matrix: MutableList<MutableList<String>>, roadPlan: Array<Triple<Int, Int, BuildableStructureConstant>>?): CostMatrix {
    val costMatrix = PathFinder.CostMatrix()
    (0..49).forEach { x ->
        (0..49).forEach { y ->
            when (matrix[x][y]) {
                ______ROAD.code -> costMatrix.set(x, y, 1)
                _CONTAINER.code -> costMatrix.set(x, y, 10)
                _____EMPTY.code, PROHIBITED.code -> {
                }
                else -> costMatrix.set(x, y, 255)
            }
        }
    }
    roadPlan?.forEach { costMatrix.set(it.first, it.second, 1) }
    return costMatrix
}

private fun Room.extractorPlan(matrix: MutableList<MutableList<String>>, terrain: Room.Terrain) {
    val mineral = find(FIND_MINERALS)[0]
    val pos = mineral.pos
    matrix[pos.x][pos.y] = _EXTRACTOR.code
    (pos.x - 1..pos.x + 1).forEach { x ->
        (pos.y - 1..pos.y + 1).forEach { y ->
            if (x != pos.x || y != pos.y) {
                if (terrain.get(x, y) != TERRAIN_MASK_WALL && (matrix[x][y] == _____EMPTY.code || matrix[x][y] == PROHIBITED.code)) {
                    matrix[x][y] = _CONTAINER.code
                    return
                }
            }
        }
    }
}

private fun Room.containerControllerPlan(matrix: MutableList<MutableList<String>>, terrain: Room.Terrain) {
    findContainerPosition(matrix, terrain, controller!!.pos)
}

private fun Room.linkControllerPlan(matrix: MutableList<MutableList<String>>, terrain: Room.Terrain) {
    findLinkPosition(matrix, terrain, controller!!.pos)
}

private fun Room.containerEnergyPlan(matrix: MutableList<MutableList<String>>, terrain: Room.Terrain) {
    find(FIND_SOURCES).forEach { findContainerPosition(matrix, terrain, it.pos) }
}

private fun Room.linksEnergyPlan(matrix: MutableList<MutableList<String>>, terrain: Room.Terrain) {
    find(FIND_SOURCES).forEach { findLinkPosition(matrix, terrain, it.pos) }
}

private fun findContainerPosition(matrix: MutableList<MutableList<String>>, terrain: Room.Terrain, pos: RoomPosition) {
    (pos.x - 1..pos.x + 1).forEach { x ->
        (pos.y - 1..pos.y + 1).forEach { y ->
            if (x != pos.x || y != pos.y) {
                if (terrain.get(x, y) != TERRAIN_MASK_WALL && (matrix[x][y] == _____EMPTY.code || matrix[x][y] == PROHIBITED.code)) {
                    matrix[x][y] = _CONTAINER.code
                    return
                }
            }
        }
    }
}

private fun findLinkPosition(matrix: MutableList<MutableList<String>>, terrain: Room.Terrain, pos: RoomPosition) {
    (pos.x - 1..pos.x + 1).forEach { spotX ->
        (pos.y - 1..pos.y + 1).forEach { spotY ->
            if (spotX != pos.x || spotY != pos.y) {
                if (terrain.get(spotX, spotY) != TERRAIN_MASK_WALL) {
                    (spotX - 1..spotX + 1).forEach { x ->
                        (spotY - 1..spotY + 1).forEach { y ->
                            if (terrain.get(x, y) != TERRAIN_MASK_WALL && (matrix[x][y] == _____EMPTY.code || matrix[x][y] == PROHIBITED.code)) {
                                matrix[x][y] = ______LINK.code
                                return
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Room.find(matrix: List<MutableList<String>>, plan: List<List<StructurePlan>>, startPosition: RoomPosition,
                      shouldBeConnected: Boolean = true): RoomPosition? {
    val startPositions = mutableListOf<RoomPosition>()
    (5..44).forEach { x ->
        (5..44).forEach { y ->
            if (isOk(x, y, matrix, plan, shouldBeConnected)) {
                startPositions.add(getPositionAt(x, y)!!)
            }
        }
    }

    val position = startPositions.minBy { roomPosition -> rangeBetween(getPositionAt(roomPosition.x, roomPosition.y)!!, startPosition) }
            ?: return null

    (position.x - 2..position.x + 2).forEach { x ->
        (position.y - 2..position.y + 2).forEach { y ->
            val structurePlan = plan[x - position.x + 2][y - position.y + 2].code
            if (structurePlan != _____EMPTY.code) {
                matrix[x][y] = structurePlan
            }
        }
    }

    return position
}

fun isOk(startX: Int, startY: Int, matrix: List<MutableList<String>>, plan: List<List<StructurePlan>>, shouldBeConnected: Boolean): Boolean {
    (0..4).forEach { x ->
        (0..4).forEach { y ->
            val matrixValue = matrix[startX + x - 2][startY + y - 2]
            when (plan[x][y].code) {
                ______ROAD.code -> {
                    if (matrixValue != _____EMPTY.code && matrixValue != ______ROAD.code) {
                        return false
                    }
                }
                _EXTENSION.code, _____SPAWN.code, _____TOWER.code, ______LINK.code, ___STORAGE.code, __TERMINAL.code, _______LAB.code, __OBSERVER.code, _____NUKER.code, _PWR_SPAWN.code, ___FACTORY.code -> {
                    if (matrixValue != _____EMPTY.code) {
                        return false
                    }
                }
            }
        }
    }
    if (!shouldBeConnected) {
        return true
    }

    var isConnected = false
    (0..4).forEach { x ->
        (0..4).forEach { y ->
            val matrixValue = matrix[startX + x - 2][startY + y - 2]
            when (plan[x][y].code) {
                ______ROAD.code -> {
                    if (matrixValue == ______ROAD.code) {
                        isConnected = true
                    }
                }
            }
        }
    }
    return isConnected
}

private fun prohibit(xRange: IntRange, yRange: IntRange, matrix: List<MutableList<String>>) {
    xRange.forEach { x ->
        yRange.forEach { y ->
            if (x in (0 .. 49)) {
                if (y in (0 .. 49)) {
                    matrix[x][y] = PROHIBITED.code
                }
            }
        }
    }
}

private fun rangeBetween(p1: RoomPosition, p2: RoomPosition): Double {
    return sqrt((p1.x - p2.x).toDouble().pow(2) + (p1.y - p2.y).toDouble().pow(2))
}

public fun Room.draw(plan: Array<Triple<Int, Int, BuildableStructureConstant>>) {
    plan.forEach {
        val x = it.first
        val y = it.second
        when (it.third) {
            STRUCTURE_ROAD -> visual.circle(RoomPosition(x, y, name), options {
                radius = 0.4
                fill = "grey"
            })
            STRUCTURE_EXTENSION -> visual.circle(RoomPosition(x, y, name), options {
                radius = 0.4
                fill = "yellow"
            })
            STRUCTURE_SPAWN -> visual.circle(RoomPosition(x, y, name), options {
                radius = 0.4
                fill = "blue"
            })
            STRUCTURE_TOWER -> visual.circle(RoomPosition(x, y, name), options {
                radius = 0.4
                fill = "red"
            })
            STRUCTURE_LINK -> visual.circle(RoomPosition(x, y, name), options {
                radius = 0.4
                fill = "white"
            })
            STRUCTURE_TERMINAL -> visual.circle(RoomPosition(x, y, name), options {
                radius = 0.4
                fill = "orange"
            })
            STRUCTURE_STORAGE -> visual.circle(RoomPosition(x, y, name), options {
                radius = 0.4
                fill = "pink"
            })
            STRUCTURE_LAB -> visual.circle(RoomPosition(x, y, name), options {
                radius = 0.4
                fill = "purple"
            })
            STRUCTURE_EXTRACTOR -> visual.circle(RoomPosition(x, y, name), options {
                radius = 0.4
                fill = "brown"
            })
            STRUCTURE_CONTAINER -> visual.circle(RoomPosition(x, y, name), options {
                radius = 0.4
                fill = "green"
            })
            STRUCTURE_OBSERVER -> visual.circle(RoomPosition(x, y, name), options {
                radius = 0.4
                fill = "#6F8E88"
            })
            STRUCTURE_POWER_SPAWN -> visual.circle(RoomPosition(x, y, name), options {
                radius = 0.4
                fill = "#4A0004"
            })
            STRUCTURE_NUKER -> visual.circle(RoomPosition(x, y, name), options {
                radius = 0.4
                fill = "#CFCC7C"
            })
            STRUCTURE_FACTORY -> visual.circle(RoomPosition(x, y, name), options {
                radius = 0.4
                fill = "#DBDAC0"
            })
        }
    }
}

fun Room.drawMatrix(matrix: MutableList<MutableList<String>>) {
    (0..49).forEach { x ->
        (0..49).forEach { y ->
            when (matrix[x][y]) {
                ______ROAD.code -> visual.circle(RoomPosition(x, y, name), options {
                    radius = 0.4
                    fill = "grey"
                })
                _EXTENSION.code -> visual.circle(RoomPosition(x, y, name), options {
                    radius = 0.4
                    fill = "yellow"
                })
                _____SPAWN.code -> visual.circle(RoomPosition(x, y, name), options {
                    radius = 0.4
                    fill = "blue"
                })
                _____TOWER.code -> visual.circle(RoomPosition(x, y, name), options {
                    radius = 0.4
                    fill = "red"
                })
                ______LINK.code -> visual.circle(RoomPosition(x, y, name), options {
                    radius = 0.4
                    fill = "#00FFFF"
                })
                _STOR_LINK.code -> visual.circle(RoomPosition(x, y, name), options {
                    radius = 0.4
                    fill = "#FFFFFF"
                })
                CONTR_LINK.code -> visual.circle(RoomPosition(x, y, name), options {
                    radius = 0.4
                    fill = "#FFFF00"
                })
                __TERMINAL.code -> visual.circle(RoomPosition(x, y, name), options {
                    radius = 0.4
                    fill = "orange"
                })
                ___STORAGE.code -> visual.circle(RoomPosition(x, y, name), options {
                    radius = 0.4
                    fill = "pink"
                })
                _______LAB.code -> visual.circle(RoomPosition(x, y, name), options {
                    radius = 0.4
                    fill = "purple"
                })
                _EXTRACTOR.code -> visual.circle(RoomPosition(x, y, name), options {
                    radius = 0.4
                    fill = "brown"
                })
                _CONTAINER.code -> visual.circle(RoomPosition(x, y, name), options {
                    radius = 0.4
                    fill = "#00FF00"
                })
                _MNRAL_CNT.code -> visual.circle(RoomPosition(x, y, name), options {
                    radius = 0.4
                    fill = "#00AA00"
                })
                __OBSERVER.code -> visual.circle(RoomPosition(x, y, name), options {
                    radius = 0.4
                    fill = "#6F8E88"
                })
                _PWR_SPAWN.code -> visual.circle(RoomPosition(x, y, name), options {
                    radius = 0.4
                    fill = "#4A0004"
                })
                _____NUKER.code -> visual.circle(RoomPosition(x, y, name), options {
                    radius = 0.4
                    fill = "#CFCC7C"
                })
                ___FACTORY.code -> visual.circle(RoomPosition(x, y, name), options {
                    radius = 0.4
                    fill = "#DBDAC0"
                })
                __RESERVED.code -> visual.circle(RoomPosition(x, y, name), options {
                    radius = 0.4
                    fill = "pink"
                })
                PROHIBITED.code -> visual.circle(RoomPosition(x, y, name), options {
                    radius = 0.4
                    fill = "black"
                })
                ___BLOCKED.code -> visual.circle(RoomPosition(x, y, name), options {
                    radius = 0.4
                    fill = "blue"
                })
                __RSV_CONT.code -> visual.circle(RoomPosition(x, y, name), options {
                    radius = 0.4
                    fill = "red"
                })
                __RSV_CNTR.code -> visual.circle(RoomPosition(x, y, name), options {
                    radius = 0.4
                    fill = "red"
                })
            }
        }
    }
}