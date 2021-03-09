package starter.task.plan

import screeps.api.BuildableStructureConstant
import screeps.api.Game
import screeps.api.LOOK_CONSTRUCTION_SITES
import screeps.api.LOOK_STRUCTURES
import screeps.api.Memory
import screeps.api.Room
import screeps.api.STRUCTURE_CONTAINER
import screeps.api.STRUCTURE_CONTROLLER
import screeps.api.STRUCTURE_EXTRACTOR
import screeps.api.STRUCTURE_LAB
import screeps.api.STRUCTURE_LINK
import screeps.api.STRUCTURE_RAMPART
import screeps.api.STRUCTURE_ROAD
import screeps.api.STRUCTURE_SPAWN
import screeps.api.STRUCTURE_STORAGE
import screeps.api.STRUCTURE_TERMINAL
import screeps.api.STRUCTURE_TOWER
import screeps.api.get
import screeps.api.set
import screeps.api.structures.Structure
import screeps.utils.unsafe.jsObject
import starter.createTask
import starter.getRoomPlan
import starter.memory.TaskMemory
import starter.memory.currentLevel
import starter.memory.isPrepare
import starter.memory.plainRoomPlan
import starter.memory.room
import starter.memory.sleepUntil
import starter.memory.tasks
import starter.memory.toRoom
import starter.memory.type
import starter.task.TaskType
import starter.timer
import starter.tryCorePlan

/**
 *
 *
 * @author zakharchuk
 * @since 18.03.2020
 */
class RoomPlanerV2Executor(val task: TaskMemory) {
    fun execute() {
        if (task.sleepUntil > Game.time) {
            return
        }

        if (Game.cpu.bucket < 1000) {
            return
        }

        val room = Game.rooms.get(task.room)!!

        if (task.isPrepare) {
            room.memory.plainRoomPlan = room.tryCorePlan()!!
            task.currentLevel = 1
            task.isPrepare = false
        }

        val roomPlan = room.getRoomPlan(room.memory.plainRoomPlan, task.currentLevel)
        if (!checkRoomPlan(roomPlan, room)) {
            return
        } else {
            task.sleepUntil = Game.time + 50
        }

        if (task.currentLevel < room.controller!!.level) {
            if (task.currentLevel == 1) {
                Memory.tasks.set("${room.name}|${TaskType.UPGRADE_CONTROLLER_POPULATION.code}|${Memory.timer}", jsObject {
                    type = TaskType.UPGRADE_CONTROLLER_POPULATION.code
                    this.room = room.name
                })
                Memory.timer++
            }

            if (task.currentLevel == 2) {
                Memory.tasks.set("${room.name}|${TaskType.HARVEST_POPULATION.code}|${Memory.timer}", jsObject {
                    type = TaskType.HARVEST_POPULATION.code
                    this.room = room.name
                })
                Memory.timer++

                Memory.tasks.set("${room.name}|${TaskType.REPAIR_POPULATION.code}|${Memory.timer}", jsObject {
                    type = TaskType.REPAIR_POPULATION.code
                    this.room = room.name
                    this.toRoom = room.name
                })
                Memory.timer++

                Memory.tasks.set("${room.name}|${TaskType.BUILD_POPULATION.code}|${Memory.timer}", jsObject {
                    type = TaskType.BUILD_POPULATION.code
                    this.room = room.name
                })
                Memory.timer++

                Memory.tasks.set("${room.name}|${TaskType.TOWER_DEFEND.code}|${Memory.timer}", jsObject {
                    type = TaskType.TOWER_DEFEND.code
                    this.room = room.name
                })
                Memory.timer++
            }

            if (task.currentLevel == 7) {
                createTask("${room.name}|${TaskType.OBSERVER.code}", jsObject {
                    this.room = room.name
                    this.type = TaskType.OBSERVER.code
                })
            }

            task.currentLevel += 1
        }
    }

    fun checkRoomPlan(roomPlan: Array<Triple<Int, Int, BuildableStructureConstant>>, room: Room): Boolean {
        var isOk = true
        roomPlan.forEach {
            val structures = room.lookForAt(LOOK_STRUCTURES, it.first, it.second)?.filter { it.structureType != STRUCTURE_RAMPART }
            if (structures.isNullOrEmpty()) {
                isOk = false
                val constructionSites = room.lookForAt(LOOK_CONSTRUCTION_SITES, it.first, it.second)?.filter { it.structureType != STRUCTURE_RAMPART }
                if (constructionSites.isNullOrEmpty()) {
                    room.createConstructionSite(it.first, it.second, it.third)
                } else {
                    if (constructionSites[0].structureType != it.third) {
                        constructionSites[0].remove()
                        room.createConstructionSite(it.first, it.second, it.third)
                    }
                }
            } else {
                val structure = structures.firstOrNull { str -> str.structureType == it.third }
                if (structure == null) {
                    structures[0].destroy()
                    room.createConstructionSite(it.first, it.second, it.third)
                    isOk = false
                } else if (task.currentLevel >= 6) {
                    if (!rampartIsOk(structure, task.currentLevel)) {
                        isOk = false
                    }
                }
            }
        }
        return isOk
    }

    fun rampartIsOk(structure: Structure, currentLevel: Int): Boolean {
        val pos = structure.pos
        val room = structure.room
        val roadRamparts = currentLevel == 8
        return when (structure.structureType) {
            STRUCTURE_ROAD, STRUCTURE_EXTRACTOR -> true
            STRUCTURE_LINK, STRUCTURE_CONTAINER ->
                if (currentLevel >= 7) {
                    buildRampartsInRange((pos.x..pos.x), (pos.y..pos.y), room, roadRamparts)
                } else {
                    true
                }
            STRUCTURE_SPAWN, STRUCTURE_STORAGE, STRUCTURE_TERMINAL, STRUCTURE_LAB, STRUCTURE_TOWER -> {
                val diff = if (currentLevel >= 7) 2 else 0
                buildRampartsInRange((pos.x - diff..pos.x + diff), (pos.y - diff..pos.y + diff), room, roadRamparts)
            }
            else ->
                if (currentLevel >= 7) {
                    buildRampartsInRange((pos.x - 1..pos.x + 1), (pos.y - 1..pos.y + 1), room, roadRamparts)
                } else {
                    true
                }
        }
    }

    fun buildRampartsInRange(xRange: IntRange, yRange: IntRange, room: Room, roadRamparts: Boolean): Boolean {
        var isOk = true
        xRange.forEach { x ->
            for (y in yRange) {
                val forRampart = room.lookForAt(LOOK_STRUCTURES, x, y)
                        ?.filter { roadRamparts || it.structureType != STRUCTURE_ROAD }
                        ?.filter { it.structureType != STRUCTURE_CONTROLLER && it.structureType != STRUCTURE_EXTRACTOR}
                if (forRampart.isNullOrEmpty()) {
                    continue
                }

                val rampart = forRampart.firstOrNull { str -> str.structureType == STRUCTURE_RAMPART }
                if (rampart != null) {
                    continue
                }

                val rampartConstructionSites = room.lookForAt(LOOK_CONSTRUCTION_SITES, x, y)
                if (rampartConstructionSites.isNullOrEmpty()) {
                    room.createConstructionSite(x, y, STRUCTURE_RAMPART)
                    isOk = false
                    continue
                }

                isOk = false
            }
        }
        return isOk
    }
}