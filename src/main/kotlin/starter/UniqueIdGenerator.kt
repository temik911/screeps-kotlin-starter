package starter

import screeps.api.Game
import screeps.api.Memory
import screeps.utils.memory.memory

/**
 *
 *
 * @author zakharchuk
 * @since 14.06.2020
 */
var Memory.uniqueId: Int by memory { 1 }

class UniqueIdGenerator {
    companion object {
        fun reset() {
            Memory.uniqueId = 1
        }

        fun generateUniqueId(): String {
            val uniqueId = Memory.uniqueId.toString()
            Memory.uniqueId++
            return "${Game.time}_$uniqueId"
        }
    }
}