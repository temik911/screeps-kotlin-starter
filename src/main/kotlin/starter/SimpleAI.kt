package starter


import screeps.api.Creep
import screeps.api.FIND_NUKES
import screeps.api.Game
import screeps.api.LOOK_STRUCTURES
import screeps.api.Memory
import screeps.api.Record
import screeps.api.ResourceConstant
import screeps.api.Room
import screeps.api.RoomPosition
import screeps.api.STRUCTURE_RAMPART
import screeps.api.STRUCTURE_ROAD
import screeps.api.Store
import screeps.api.component1
import screeps.api.component2
import screeps.api.entries
import screeps.api.get
import screeps.api.iterator
import screeps.api.keys
import screeps.api.options
import screeps.api.set
import screeps.api.values
import screeps.utils.isEmpty
import screeps.utils.memory.memory
import screeps.utils.unsafe.delete
import screeps.utils.unsafe.jsObject
import starter.extension.myRooms
import starter.extension.statistic
import starter.extension.toLink
import starter.manager.CpuUnlockManager
import starter.manager.room.RoomManager
import starter.memory.LockMemory
import starter.memory.TaskMemory
import starter.memory.createLinkedTask
import starter.memory.factoryId
import starter.memory.initiator
import starter.memory.isDone
import starter.memory.isExternalRoom
import starter.memory.isSkRoom
import starter.memory.locks
import starter.memory.reservedRooms
import starter.memory.room
import starter.memory.tasks
import starter.memory.time
import starter.memory.toRoom
import starter.memory.type
import starter.stats.MemoryStatsCollector
import starter.task.TaskType
import starter.task.market.orderBook
import starter.task.power.creep.PowerCreepExecutor
import kotlin.math.round

var Memory.createStuff: Boolean by memory { false }

class CpuUsed(val taskType: TaskType, val room: String, val cpuUsed: Double)

class TaskCpuUsed(val taskTypeToCpuUsed: Map<String, Double>, val roomToCpuUsed: Map<String, Double>)

val metrics = mutableMapOf<String, Any>()
val metricsToStore = mutableMapOf<String, Any>()

fun gameLoop() {
    console.log("**************************** ${Game.time} ****************************")
    metrics.clear()
    withTimer("total") {

        withTimer("storeMetrics") {
            MemoryStatsCollector().storeMetrics(metricsToStore)
            metricsToStore.clear()
        }

        nukeSaver(Game.rooms["E41S55"]!!)


        withTimer("initMemory") { UniqueIdGenerator.reset() }
        withTimer("houseKeeping") { houseKeeping(Game.creeps) }
        withTimer("checkTasks") { checkTasks() }
        withTimer("checkLocks") { checkLocks() }

        withTimer("roomManage") {
            Game.myRooms().forEach {
                RoomManager(it).manage()
            }
        }

        withTimer("cpuUnlockManage") {
            CpuUnlockManager().manage()
        }

        withTimer("createStuff") {
            if (Memory.createStuff) {
                jsObject<TaskMemory> { }.createLinkedTask(TaskType.POWER_OPERATE_FACTORY, jsObject {
                    this.room = "E53S48"
                    this.factoryId = "5e95b5e4f2406562ec20570c"
                })

                Memory.createStuff = false
            }
        }

//        withTimer("generatePixel") {
//            if (Game.cpu.bucket == 10_000) {
//                Game.cpu.generatePixel()
//            }
//        }

        var taskCpuUsed: TaskCpuUsed? = null
        withTimer("executeTask") { taskCpuUsed = executeTasks() }
        withTimer("orderBook") { orderBook() }

        withTimer("roomStatistics") {
            Game.myRooms().sortedByDescending { it.controller!!.level }
                    .forEach { it.statistic() }
        }

        withTimer("processFlags") { processFlags() }

        withTimer("powerCreeps") {
            Game.powerCreeps.keys.forEach {
                try {
                    PowerCreepExecutor(jsObject { }).execute(it)
                } catch (e: Throwable) {
                    console.log(e)
                }
            }
        }

        withTimer("checkConstructionSites") {
            val roomsWithConstructionSites = mutableSetOf<String>()
            Game.constructionSites.values.forEach {
                roomsWithConstructionSites.add(it.pos.roomName)
            }
            if (roomsWithConstructionSites.isNotEmpty()) {
                console.log("Construction sites in rooms: ${roomsWithConstructionSites.joinToString { toLink(it) }}")
            }
        }

        withTimer("statsCollector") {
            metrics.putAll(MemoryStatsCollector().collectStats(taskCpuUsed!!))
        }
    }
    metricsToStore.putAll(metrics)
}

fun nukeSaver(room: Room) {
    val nukes = room.find(FIND_NUKES)
    val damage: MutableList<MutableList<Int>> = MutableList(50) { MutableList(50) { 0 } }

    nukes.forEach {
        damage[it.pos.x][it.pos.y] += 10_000_000
        (it.pos.x - 2..it.pos.x + 2).forEach { x ->
            (it.pos.y - 2..it.pos.y + 2).forEach { y ->
                damage[x][y] += 5_000_000
            }
        }
    }

    damage.forEachIndexed { x, list ->
        list.forEachIndexed { y, value ->

            if (value > 0) {
                val structures = room.lookForAt(LOOK_STRUCTURES, x, y)?: emptyArray()
                val rampart = structures.firstOrNull { it.structureType == STRUCTURE_RAMPART }
                if (rampart != null) {
                    damage[x][y] += 1_000_000 - rampart.hits
                }
            }
        }
    }

    damage.forEachIndexed { x, list ->
        list.forEachIndexed { y, value ->
            if (value > 0) {
                console.log("$x $y $value")
                room.visual.circle(RoomPosition(x, y, room.name), options {
                    radius = 0.5
                    opacity = 1.0
                    fill = when (value) {
                        5_000_000 -> "white"
                        10_000_000 -> "green"
                        15_000_000 -> "blue"
                        20_000_000 -> "red"
                        25_000_000 -> "black"
                        else -> "yellow"
                    }
                })
            }
        }
    }
}

fun checkLocks() {
    Memory.locks.entries.forEach { (key, memory) ->
        if (memory.initiator == TaskType.BOOST_CREEP.code && Game.time > memory.time + 2000) {
            delete(Memory.locks[key])
        }
    }
}

fun withTimer(name: String, function: () -> Unit) {
    val startCpu = Game.cpu.getUsed()
    function.invoke()
    metrics["timer.$name"] = (Game.cpu.getUsed() - startCpu).round(2)
}

fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}

fun withTimer(taskType: TaskType, taskMemory: TaskMemory): CpuUsed {
    val startCpu = Game.cpu.getUsed()
    taskType.execute(taskMemory)
    val totalCpu = Game.cpu.getUsed() - startCpu
    return CpuUsed(taskType, taskMemory.room, totalCpu)
}

fun checkTasks() {
    Memory.tasks.entries.filter { task -> task.component2().isDone }
            .forEach {
                delete(Memory.tasks[it.component1()])
            }
}

private fun houseKeeping(creeps: Record<String, Creep>) {
    if (Game.creeps.isEmpty()) return  // this is needed because Memory.creeps is undefined

    for ((creepName, _) in Memory.creeps) {
        if (creeps[creepName] == null) {
            delete(Memory.creeps[creepName])
        }
    }
}

fun createTask(name: String, taskMemory: TaskMemory): String {
    val uniqueId = UniqueIdGenerator.generateUniqueId()
    val id = "$name|$uniqueId"
    Memory.tasks[id] = taskMemory
    return id
}

fun lock(name: String, lockMemory: LockMemory) {
    lockMemory.time = Game.time
    Memory.locks[name] = lockMemory
}

fun unlock(name: String) {
    delete(Memory.locks[name])
}

fun unlockByInitiator(initiator: String) {
    Memory.locks.entries.filter { it.component2().initiator == initiator }
            .forEach { unlock(it.component1()) }
}

fun isLocked(name: String): Boolean {
    return Memory.locks[name] != null
}

// to tasks
fun stopReserveRoom(room: String, toRoom: String) {
    Memory.tasks.entries.forEach { task ->
        val taskMemory = task.component2()
        if (taskMemory.room == room && taskMemory.toRoom == toRoom && taskMemory.isExternalRoom) {
            taskMemory.isDone = true
        }
    }
}

fun reserveRoom(room: String, toRoom: String) {
    val fromRoom = Game.rooms[room]!!
    val reservedRooms = fromRoom.memory.reservedRooms.toMutableSet()
    reservedRooms.add(toRoom)
    fromRoom.memory.reservedRooms = reservedRooms.toTypedArray()
}

fun skRoom(room: String, toRoom: String) {
//    createTask("${room}|${toRoom}|${TaskType.REPAIR_POPULATION.code}", jsObject {
//        type = TaskType.REPAIR_POPULATION.code
//        this.isSkRoom = true
//        this.room = room
//        this.toRoom = toRoom
//    })
//
//    createTask("${room}|${toRoom}|${TaskType.DEFEND_POPULATION.code}", jsObject {
//        type = TaskType.DEFEND_POPULATION.code
//        this.isSkRoom = true
//        this.room = room
//        this.toRoom = toRoom
//    })
//
//    createTask("${room}|${toRoom}|${TaskType.RESERVE_ROOM_PLAN.code}", jsObject {
//        type = TaskType.RESERVE_ROOM_PLAN.code
//        this.isSkRoom = true
//        this.room = room
//        this.toRoom = toRoom
//    })
//
//    createTask("${room}|${toRoom}|${TaskType.BUILD_POPULATION.code}", jsObject {
//        type = TaskType.BUILD_POPULATION.code
//        this.isSkRoom = true
//        this.room = room
//        this.toRoom = toRoom
//    })
//
//    createTask("${room}|${toRoom}|${TaskType.HARVEST_POPULATION.code}", jsObject {
//        type = TaskType.HARVEST_POPULATION.code
//        this.isSkRoom = true
//        this.room = room
//        this.toRoom = toRoom
//    })
}

fun stopSkRoom(room: String, toRoom: String) {
    Memory.tasks.entries.forEach { task ->
        val taskMemory = task.component2()
        if (taskMemory.room == room && taskMemory.toRoom == toRoom && taskMemory.isSkRoom) {
            taskMemory.isDone = true
        }
    }
}

fun newRoom(room: String, toRoom: String) {
    createTask("${room}|${toRoom}|${TaskType.CLAIM_ROOM_POPULATION.code}", jsObject {
        type = TaskType.CLAIM_ROOM_POPULATION.code
        this.room = room
        this.toRoom = toRoom
    })
}

fun squadAttack(room: String, toRoom: String) {
    createTask("${room}|${toRoom}|${TaskType.SQUAD_ATTACK.code}", jsObject {
        type = TaskType.SQUAD_ATTACK.code
        this.room = room
        this.toRoom = toRoom
    })
}

fun plan(roomName: String) {
    val room = Game.rooms.get(roomName)!!
    room.draw(room.getRoomPlan(room.tryCorePlan()!!, 8))
}

fun Store.getUsed(resource: ResourceConstant): Int {
    return getUsedCapacity(resource) ?: 0
}