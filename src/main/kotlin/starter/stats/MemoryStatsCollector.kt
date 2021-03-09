package starter.stats

import screeps.api.Game
import screeps.api.RESOURCE_CATALYZED_GHODIUM_ACID
import screeps.api.RESOURCE_ENERGY
import screeps.api.RESOURCE_METAL
import screeps.api.RESOURCE_POWER
import screeps.api.RawMemory
import screeps.api.set
import starter.TaskCpuUsed
import starter.extension.myRooms
import starter.getUsed
import starter.memory.rampartHitsMin
import starter.memory.spawnRequests
import starter.round
import kotlin.js.json

/**
 *
 *
 * @author zakharchuk
 * @since 18.05.2020
 */
class MemoryStatsCollector() {

    companion object {
        const val GRAFANA_SEGMENT_ID = 33
    }

    fun storeMetrics(metrics: Map<String, Any>) {
        RawMemory.setActiveSegments(arrayOf(GRAFANA_SEGMENT_ID))
        if (metrics.isNotEmpty()) {
            storeStats(metrics)
        }
    }

    fun collectStats(taskCpuUsed: TaskCpuUsed): Map<String, Any> {
        val statsCollector = mutableMapOf<String, Any>()
        statsCollector.putAll(collectGclStats())
        statsCollector.putAll(collectGplStats())
        statsCollector.putAll(collectRoomsStats())
        statsCollector.putAll(collectMarketStats())
        statsCollector.putAll(collectTasksStats(taskCpuUsed))
        statsCollector.putAll(collectCpuStats())
        return statsCollector
    }

    private fun collectTasksStats(taskCpuUsed: TaskCpuUsed): Map<String, Any> {
        val statsCollector = mutableMapOf<String, Double>()
        taskCpuUsed.taskTypeToCpuUsed.forEach {
            statsCollector["task.${it.component1()}.cpu.used"] = it.component2().round(2)
        }
        taskCpuUsed.roomToCpuUsed.forEach {
            statsCollector["room.${it.component1()}.task.cpu.used"] = it.component2().round(2)
        }
        return statsCollector.toMap()
    }

    private fun collectMarketStats(): Map<String, Any> {
        val statsCollector = mutableMapOf<String, Any>()
        statsCollector["market.credits"] = Game.market.credits
        return statsCollector.toMap()
    }

    private fun storeStats(statsCollector: Map<String, Any>) {
        val json = json()
        statsCollector.entries.forEach { json.set(it.component1(), it.component2()) }
        val stats = JSON.stringify(json)
        RawMemory.segments[GRAFANA_SEGMENT_ID] = stats
    }

    private fun collectGclStats(): Map<String, Any> {
        val statsCollector = mutableMapOf<String, Any>()
        statsCollector["gcl.level"] = Game.gcl.level
        statsCollector["gcl.progress"] = Game.gcl.progress
        statsCollector["gcl.progressTotal"] = Game.gcl.progressTotal
        return statsCollector.toMap()
    }

    private fun collectGplStats(): Map<String, Any> {
        val statsCollector = mutableMapOf<String, Any>()
        statsCollector["gpl.level"] = Game.gpl.level
        statsCollector["gpl.progress"] = Game.gpl.progress
        statsCollector["gpl.progressTotal"] = Game.gpl.progressTotal
        return statsCollector.toMap()
    }

    private fun collectCpuStats(): Map<String, Any> {
        val statsCollector = mutableMapOf<String, Any>()
        statsCollector["cpu.getUsed"] = Game.cpu.getUsed()
        statsCollector["cpu.limit"] = Game.cpu.limit
        statsCollector["cpu.bucket"] = Game.cpu.bucket
        return statsCollector.toMap()
    }

    private fun collectRoomsStats(): Map<String, Any> {
        val statsCollector = mutableMapOf<String, Any>()
        Game.myRooms().forEach { room ->
            val level = room.controller!!.level
            statsCollector["room.${room.name}.controller.level"] = level
            statsCollector["room.${room.name}.controller.progress"] = if (level == 8) 1 else room.controller!!.progress
            statsCollector["room.${room.name}.controller.progressTotal"] = if (level == 8) 1 else room.controller!!.progressTotal
            statsCollector["room.${room.name}.spawnRequests"] = room.memory.spawnRequests.size
            statsCollector["room.${room.name}.rampartHitsMin"] = room.memory.rampartHitsMin

            statsCollector["room.${room.name}.storage.capacity"] = room.storage?.store?.getCapacity() ?: 0
            statsCollector["room.${room.name}.terminal.capacity"] = room.terminal?.store?.getCapacity() ?: 0
            statsCollector["room.${room.name}.storage.usedCapacity"] = room.storage?.store?.getUsedCapacity() ?: 0
            statsCollector["room.${room.name}.terminal.usedCapacity"] = room.terminal?.store?.getUsedCapacity() ?: 0

            statsCollector["room.${room.name}.storage.${RESOURCE_ENERGY}"] = room.storage?.store?.getUsed(RESOURCE_ENERGY) ?: 0
            statsCollector["room.${room.name}.terminal.${RESOURCE_ENERGY}"] = room.terminal?.store?.getUsed(RESOURCE_ENERGY) ?: 0

            statsCollector["room.${room.name}.storage.${RESOURCE_POWER}"] = room.storage?.store?.getUsed(RESOURCE_POWER) ?: 0
            statsCollector["room.${room.name}.terminal.${RESOURCE_POWER}"] = room.terminal?.store?.getUsed(RESOURCE_POWER) ?: 0

            statsCollector["room.${room.name}.storage.${RESOURCE_METAL}"] = room.storage?.store?.getUsed(RESOURCE_METAL) ?: 0
            statsCollector["room.${room.name}.terminal.${RESOURCE_METAL}"] = room.terminal?.store?.getUsed(RESOURCE_METAL) ?: 0

            statsCollector["room.${room.name}.storage.${RESOURCE_CATALYZED_GHODIUM_ACID}"] =
                    room.storage?.store?.getUsed(RESOURCE_CATALYZED_GHODIUM_ACID) ?: 0
            statsCollector["room.${room.name}.terminal.${RESOURCE_CATALYZED_GHODIUM_ACID}"] =
                    room.terminal?.store?.getUsed(RESOURCE_CATALYZED_GHODIUM_ACID) ?: 0
        }
        return statsCollector.toMap()
    }
}