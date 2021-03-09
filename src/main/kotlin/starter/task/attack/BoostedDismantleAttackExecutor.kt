package starter.task.attack

import screeps.api.COSTMATRIX_FALSE
import screeps.api.CostMatrix
import screeps.api.Creep
import screeps.api.FIND_CREEPS
import screeps.api.FIND_HOSTILE_CREEPS
import screeps.api.FIND_HOSTILE_STRUCTURES
import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.HEAL
import screeps.api.IStructure
import screeps.api.LOOK_STRUCTURES
import screeps.api.MOVE
import screeps.api.NavigationTarget
import screeps.api.PathFinder
import screeps.api.RANGED_ATTACK
import screeps.api.RESOURCE_CATALYZED_GHODIUM_ALKALIDE
import screeps.api.RESOURCE_CATALYZED_KEANIUM_ALKALIDE
import screeps.api.RESOURCE_CATALYZED_LEMERGIUM_ALKALIDE
import screeps.api.RESOURCE_CATALYZED_ZYNTHIUM_ACID
import screeps.api.RESOURCE_CATALYZED_ZYNTHIUM_ALKALIDE
import screeps.api.Room
import screeps.api.RoomPosition
import screeps.api.STRUCTURE_CONTROLLER
import screeps.api.STRUCTURE_EXTRACTOR
import screeps.api.STRUCTURE_RAMPART
import screeps.api.STRUCTURE_ROAD
import screeps.api.STRUCTURE_TERMINAL
import screeps.api.STRUCTURE_TOWER
import screeps.api.STRUCTURE_WALL
import screeps.api.TERRAIN_MASK_WALL
import screeps.api.TOUGH
import screeps.api.WORK
import screeps.api.get
import screeps.api.options
import screeps.api.structures.StructureTower
import screeps.utils.unsafe.jsObject
import starter.Role
import starter.calculateBoostedDismantleBody
import starter.calculateBoostedHealerBody
import starter.createTask
import starter.memory.BoostCreepTask
import starter.memory.TaskMemory
import starter.memory.amount
import starter.memory.boost
import starter.memory.boostCreepTask
import starter.memory.checkLinkedTasks
import starter.memory.creepId
import starter.memory.lastHealId
import starter.memory.lastTimeDamage
import starter.memory.linkedTaskIds
import starter.memory.role
import starter.memory.room
import starter.memory.status
import starter.memory.toRoom
import starter.memory.type
import starter.task.ExecutorWithSeveralCreeps
import starter.task.TaskType
import starter.walker.walkTo

/**
 *
 *
 * @author zakharchuk
 * @since 11.05.2020
 */
class BoostedDismantleAttackExecutor(task: TaskMemory) : ExecutorWithSeveralCreeps(task) {

    override fun getCreepRequests(room: Room) = arrayOf(
            CreepRequest(room.calculateBoostedDismantleBody(), Role.BOOSTED_DISMANTLE),
            CreepRequest(room.calculateBoostedHealerBody(), Role.BOOSTED_HEALER)
    )

    override fun getCreepsCount() = 2

    override fun prepare(task: TaskMemory, creeps: List<Creep>): Boolean {
        if (task.status == "undefined") {
            task.status = "goToRoom"
        }

        creeps.forEach {
            if (!boostCreep(it)) {
                return false
            }
        }
        return true
    }

    private fun boostCreep(creep: Creep): Boolean {
        task.checkLinkedTasks()
        if (creep.body.none { it.boost == null }) {
            return true
        }
        if (task.linkedTaskIds.isNullOrEmpty()) {
            val unBoostedPart = creep.body.firstOrNull { it.boost == null } ?: return false
            val amount = creep.body.count { it.type == unBoostedPart.type }
            val boost = when (unBoostedPart.type) {
                HEAL -> RESOURCE_CATALYZED_LEMERGIUM_ALKALIDE
                RANGED_ATTACK -> RESOURCE_CATALYZED_KEANIUM_ALKALIDE
                MOVE -> RESOURCE_CATALYZED_ZYNTHIUM_ALKALIDE
                TOUGH -> RESOURCE_CATALYZED_GHODIUM_ALKALIDE
                WORK -> RESOURCE_CATALYZED_ZYNTHIUM_ACID
                else -> throw IllegalArgumentException("Unexpected body part: ${unBoostedPart.type}")
            }
            val boostCreepTask = jsObject<BoostCreepTask> {
                this.creepId = creep.id
                this.boost = boost
                this.amount = amount
            }

            val taskId = createTask("${task.room}|${TaskType.BOOST_CREEP.code}", jsObject {
                this.type = TaskType.BOOST_CREEP.code
                this.room = task.room
                this.boostCreepTask = boostCreepTask
            })
            val linkedTaskIds = task.linkedTaskIds.toMutableList()
            linkedTaskIds.add(taskId)
            task.linkedTaskIds = linkedTaskIds.toTypedArray()
        }

        return false
    }

    override fun executeInternal(creeps: List<Creep>) {
        val dismantler = creeps.firstOrNull { it.memory.role == Role.BOOSTED_DISMANTLE } ?: return
        val healer = creeps.firstOrNull { it.memory.role == Role.BOOSTED_HEALER } ?: return

        heal(dismantler, healer)

        console.log("${task.status}")

        when (task.status) {
            "escapeRoom" -> {
                if (!notInTargetRoom(dismantler) || !notInTargetRoom(healer)) {
                    moveTogether(dismantler, healer, Game.rooms.get(task.room)!!.storage!!)
                }

                if (dismantler.hits == dismantler.hitsMax && healer.hits == healer.hitsMax) {
                    task.status = "goToRoom"
                    return
                }
            }
            "goToRoom" -> {
                if (inTargetRoom(dismantler) && inTargetRoom(healer)) {
                    task.lastTimeDamage = Game.time
                    task.status = "withdrawEnergy"
                    return
                }

                if (healer.pos.roomName == task.toRoom) {
                    moveTogether(healer, dismantler, RoomPosition(25, 25, task.toRoom), maxRooms = 1)
                } else {
                    moveTogether(healer, dismantler, RoomPosition(25, 25, task.toRoom))
                }
            }
            "withdrawEnergy" -> {
                rangedMassAttack(healer)

                if (dismantler.hits < dismantler.hitsMax || healer.hits < healer.hitsMax) {
                    task.lastTimeDamage = Game.time
                }

                if (dismantler.hits < dismantler.hitsMax * 0.9 || healer.hits < healer.hitsMax * 0.9) {
                    task.status = "escapeRoom"
                    return
                }

                val towers = dismantler.room.find(FIND_HOSTILE_STRUCTURES, options {
                    filter = {
                        it.structureType == STRUCTURE_TOWER && it.unsafeCast<StructureTower>().store.getUsedCapacity(screeps.api.RESOURCE_ENERGY) ?: 0 > 0
                    }
                })

                if (towers.isNullOrEmpty() || task.lastTimeDamage + 10 < Game.time) {
                    task.status = "attack"
                }
            }
            "attack" -> {
                rangedMassAttack(healer)

                val calculateTowerArea = calculateTowerArea(room = dismantler.room)

                if (calculateTowerArea[dismantler.pos.x][dismantler.pos.y] > 4 ||
                        calculateTowerArea[healer.pos.x][healer.pos.y] > 4) {
                    task.status = "escapeRoom"
                    return
                }

                if (dismantler.hits < dismantler.hitsMax * 0.9 || healer.hits < healer.hitsMax * 0.9) {
                    task.status = "escapeRoom"
                    return
                }

                val withoutRampart = dismantler.pos.findClosestByPath(FIND_HOSTILE_STRUCTURES, options {
                    filter = {
                        it.structureType != STRUCTURE_CONTROLLER
                                && it.structureType != STRUCTURE_EXTRACTOR
                                && it.pos.lookFor(LOOK_STRUCTURES)?.firstOrNull { it.structureType == STRUCTURE_RAMPART } == null
                                && calculateTowerArea[it.pos.x][it.pos.y] <= 4
                    }
                    costCallback = { roomName, costMatrix ->
                        costMatrix(calculateTowerArea, dismantler.room, costMatrix)
                    }
                    maxRooms = 1
                })
                if (withoutRampart != null) {
                    dismantler.room.visual.circle(withoutRampart.pos, options {
                        radius = 0.4
                        fill = "black"
                    })
                    console.log("without rampart: ${withoutRampart.pos.x} ${withoutRampart.pos.y}")
                    attack(dismantler, healer, withoutRampart)
                } else {
                    val findSpawns = dismantler.pos.findClosestByPath(FIND_HOSTILE_STRUCTURES, options {
                        filter = {
                            it.structureType == screeps.api.STRUCTURE_SPAWN
                                    && calculateTowerArea[it.pos.x][it.pos.y] <= 4
                        }
                        costCallback = { roomName, costMatrix ->
                            costMatrix(calculateTowerArea, dismantler.room, costMatrix)
                        }
                        maxRooms = 1
                    })
                    if (findSpawns != null) {
                        dismantler.room.visual.circle(findSpawns.pos, options {
                            radius = 0.4
                            fill = "black"
                        })
                        console.log("spawn: ${findSpawns.pos.x} ${findSpawns.pos.y}")
                        attack(dismantler, healer, findSpawns)
                    } else {
                        val findTerminal = dismantler.pos.findClosestByPath(FIND_HOSTILE_STRUCTURES, options {
                            filter = {
                                it.structureType == STRUCTURE_TERMINAL
                                        && calculateTowerArea[it.pos.x][it.pos.y] <= 4
                            }
                            costCallback = { roomName, costMatrix ->
                                costMatrix(calculateTowerArea, dismantler.room, costMatrix)
                            }
                            maxRooms = 1
                        })
                        if (findTerminal != null) {
                            dismantler.room.visual.circle(findTerminal.pos, options {
                                radius = 0.4
                                fill = "black"
                            })
                            console.log("terminal: ${findTerminal.pos.x} ${findTerminal.pos.y}")
                            attack(dismantler, healer, findTerminal)
                        }
                    }
                }
            }
        }
    }

    private fun rangedMassAttack(creep: Creep) {
        val creeps = creep.pos.findInRange(FIND_HOSTILE_CREEPS, 3, options {
            filter = {
                it.owner.username == "FeTiD"
            }
        })

        if (creeps.isNullOrEmpty()) {
            creep.rangedMassAttack()
        }
    }

    private fun costMatrix(calculateTowerArea: MutableList<MutableList<Int>>,
                           room: Room,
                           costMatrix: PathFinder.CostMatrix): PathFinder.CostMatrix {
        val maxHits = room.find(FIND_STRUCTURES)
                .maxBy { if (it.structureType == STRUCTURE_WALL || it.structureType == STRUCTURE_RAMPART) it.hits else 0 }
                ?.hits ?: 1_000_000

        room.find(FIND_STRUCTURES).forEach { structure ->
            costMatrix.set(structure.pos.x, structure.pos.y, 1)
        }

        room.find(FIND_STRUCTURES).forEach { structure ->
            if (structure.structureType == STRUCTURE_RAMPART || structure.structureType == STRUCTURE_WALL) {
                val percent = structure.hits * 1.0 / maxHits
                val value = (50 * percent).toInt()
                costMatrix.set(structure.pos.x, structure.pos.y, value)
            }
        }

        calculateTowerArea.forEachIndexed { x, list ->
            list.forEachIndexed { y, value ->
                if (value > 4) {
                    costMatrix.set(x, y, 255)
                }
            }
        }

        return costMatrix
    }

    private fun attack(dismantler: Creep, healer: Creep, structure: IStructure, isRampartWalkable: Boolean = true) {
        if (dismantler.pos.isNearTo(structure)) {
            dismantler.dismantle(structure)
            return
        }

        val path = PathFinder.search(dismantler.pos, structure.pos, options {
            maxRooms = 1
            plainCost = 1
            swampCost = 5
            roomCallback = { roomName -> callback(roomName, isRampartWalkable) }
        })

        path.path.forEach {
            dismantler.room.visual.circle(it)
        }

        val target = path.path.get(0)
        val lookFor = target.lookFor(LOOK_STRUCTURES)?.firstOrNull { it.structureType != STRUCTURE_ROAD }
        if (lookFor != null) {
            dismantler.dismantle(lookFor)
        } else {
            moveTogether(dismantler, healer, target, maxRooms = 1)
        }
    }

    private fun heal(dismantler: Creep, healer: Creep) {
        if (dismantler.pos.roomName != healer.pos.roomName) {
            healer.heal(healer)
            task.lastHealId = healer.id
            return
        }

        if (dismantler.hits < healer.hits) {
            if (dismantler.pos.isNearTo(healer.pos)) {
                healer.heal(dismantler)
            } else {
                healer.rangedHeal(dismantler)
            }
            task.lastHealId = dismantler.id
            return
        }

        if (healer.hits != healer.hitsMax) {
            healer.heal(healer)
            task.lastHealId = healer.id
            return
        }

        val lastHealId = task.lastHealId
        if (lastHealId == null) {
            healer.heal(dismantler)
            task.lastHealId = dismantler.id
            return
        }
        healer.heal(if (dismantler.id == lastHealId) dismantler else healer)
    }

    private fun inTargetRoom(creep: Creep): Boolean {
        return creep.pos.roomName == task.toRoom && creep.pos.x in 1..48 && creep.pos.y in 1..48
    }

    private fun notInTargetRoom(creep: Creep): Boolean {
        return creep.pos.roomName != task.toRoom && creep.pos.x in 1..48 && creep.pos.y in 1..48
    }

    private fun moveTogether(mainCreep: Creep, secondCreep: Creep, target: NavigationTarget, maxRooms: Int = 16) {
        if (!(mainCreep.pos.x in 1..48 && mainCreep.pos.y in 1..48)) {
            mainCreep.walkTo(target, maxRooms = maxRooms, ignoreRooms = listOf("E39S57", "E39S58", "E40S57"))
            return
        }

        if (!secondCreep.pos.isNearTo(mainCreep.pos)) {
            secondCreep.moveTo(mainCreep.pos, options { reusePath = 0 })
            return
        }

        if (mainCreep.fatigue == 0 && secondCreep.fatigue == 0) {
            mainCreep.walkTo(target, maxRooms = maxRooms, ignoreRooms = listOf("E39S57", "E39S58", "E40S57"))
            secondCreep.moveTo(mainCreep.pos, options { reusePath = 0 })
        }
    }

    private fun callback(roomName: String, isRampartWalkable: Boolean): CostMatrix {
        val room = Game.rooms[roomName] ?: return COSTMATRIX_FALSE
        val costMatrix = PathFinder.CostMatrix()

        val maxHits = room.find(FIND_STRUCTURES).maxBy { if (it.structureType == STRUCTURE_WALL || it.structureType == STRUCTURE_RAMPART) it.hits else 0 }?.hits
                ?: 1_000_000

        room.find(FIND_STRUCTURES).forEach { structure ->
            costMatrix.set(structure.pos.x, structure.pos.y, 1)
        }

        room.find(FIND_STRUCTURES).forEach { structure ->
            if (structure.structureType == STRUCTURE_RAMPART || structure.structureType == STRUCTURE_WALL) {
                if (!isRampartWalkable) {
                    costMatrix.set(structure.pos.x, structure.pos.y, 255)
                } else {
                    val percent = structure.hits * 1.0 / maxHits
                    val value = (50 * percent).toInt()
                    costMatrix.set(structure.pos.x, structure.pos.y, if (value < 5) 5 else value)
                }
            }
        }
        val terrain = room.getTerrain()
        room.find(FIND_HOSTILE_STRUCTURES).forEach { structure ->
            if (structure.structureType == STRUCTURE_TOWER) {
                val x = structure.pos.x
                val y = structure.pos.y
//                isDanger(x - 8..x + 8, y - 8..y + 8, costMatrix, terrain)
            }
        }

        room.find(FIND_CREEPS).forEach { myCreep ->
            costMatrix.set(myCreep.pos.x, myCreep.pos.y, 255)
        }

        return costMatrix
    }

    private fun isDanger(xRange: IntRange, yRange: IntRange, costMatrix: PathFinder.CostMatrix, terrain: Room.Terrain) {
        xRange.forEach { x ->
            yRange.forEach { y ->
                if (x in 0..49 && y in 0..49) {
                    if (terrain.get(x, y) != TERRAIN_MASK_WALL) {
                        if (costMatrix.get(x, y) < 150) {
                            costMatrix.set(x, y, 150)
                        }
                    }
                }
            }
        }
    }

    private fun calculateTowerArea(room: Room): MutableList<MutableList<Int>> {
        val matrix: MutableList<MutableList<Int>> = MutableList(50) { MutableList(50) { 0 } }

        room.find(FIND_HOSTILE_STRUCTURES).forEach { structure ->
            if (structure.structureType == STRUCTURE_TOWER
                    && (structure.unsafeCast<StructureTower>().store.getUsedCapacity(screeps.api.RESOURCE_ENERGY) ?: 0) >= 10) {
                val towerX = structure.pos.x
                val towerY = structure.pos.y
                (towerX - 7..towerX + 7).forEach { x ->
                    (towerY - 7..towerY + 7).forEach { y ->
                        if (x in 0..49 && y in 0..49) {
                            matrix[x][y] += 1
                        }
                    }
                }
            }
        }
        matrix.forEachIndexed { x, list ->
            list.forEachIndexed { y, value ->
                if (value > 4) {
                    room.visual.circle(RoomPosition(x, y, room.name), options {
                        radius = 0.4
                        fill = "red"
                    })
                }
            }
        }

        return matrix
    }
}