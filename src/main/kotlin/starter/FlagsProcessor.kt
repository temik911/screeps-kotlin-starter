package starter

import screeps.api.COLOR_RED
import screeps.api.Flag
import screeps.api.Game
import screeps.api.component1
import screeps.api.component2
import screeps.api.entries
import screeps.utils.unsafe.jsObject
import starter.memory.DismantleTask
import starter.memory.dismantleTask
import starter.memory.room
import starter.memory.targetPosition
import starter.memory.toRoom
import starter.memory.type
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 01.04.2020
 */

enum class FlagCommand(val code: String) {

    // command=reserveRoom;room=E57S49
    RESERVE_ROOM("reserveRoom") {
        override fun execute(params: Params) {
            reserveRoom(params.get("room")!!, params.flag.pos.roomName)
        }
    },

    STOP_RESERVE_ROOM("stopReserveRoom") {
        override fun execute(params: Params) {
            stopReserveRoom(params.get("room")!!, params.flag.pos.roomName)
        }
    },

    DISMANTLE_TARGET("dismantleTarget") {
        override fun execute(params: Params) {
            val dismantleTask = jsObject<DismantleTask> {
                this.type = "target"
                this.targetPosition = params.flag.pos
            }
            createTask("${params.get("room")}|${TaskType.DISMANTLE_POPULATION.code}", jsObject {
                this.type = TaskType.DISMANTLE_POPULATION.code
                this.room = params.get("room")!!
                this.dismantleTask = dismantleTask
            })
        }
    },

    CLAIM_TO_CLEAR("claimToClear") {
        override fun execute(params: Params) {
            createTask("${params.get("room")}|${TaskType.CLAIM_TO_CLEAR.code}", jsObject {
                this.type = TaskType.CLAIM_TO_CLEAR.code
                this.room = params.get("room")!!
                this.toRoom = params.flag.pos.roomName
            })
        }
    },

    CLAIM_TO_THIEF("claimToThief") {
        override fun execute(params: Params) {
            createTask("${params.get("room")}|${params.flag.pos.roomName}|${TaskType.CLAIM_TO_THIEF_POPULATION.code}", jsObject {
                this.type = TaskType.CLAIM_TO_THIEF_POPULATION.code
                this.room = params.get("room")!!
                this.toRoom = params.flag.pos.roomName
            })
        }
    },

    ATTACK("attack") {
        override fun execute(params: Params) {
            createTask("${params.get("room")}|${params.flag.pos.roomName}|${TaskType.SQUAD_ATTACK.code}", jsObject {
                this.type = TaskType.SQUAD_ATTACK.code
                this.room = params.get("room")!!
                this.toRoom = params.flag.pos.roomName
            })
        }
    },

    RANGE_ATTACK("rangeAttack") {
        override fun execute(params: Params) {
            createTask("${params.get("room")}|${params.flag.pos.roomName}|${TaskType.RANGE_ATTACK.code}", jsObject {
                this.type = TaskType.RANGE_ATTACK.code
                this.room = params.get("room")!!
                this.toRoom = params.flag.pos.roomName
            })
        }
    },

    BOOSTED_RANGE_ATTACK("boostedRangeAttack") {
        override fun execute(params: Params) {
            createTask("${params.get("room")}|${params.flag.pos.roomName}|${TaskType.BOOSTED_RANGE_ATTACK.code}", jsObject {
                this.type = TaskType.BOOSTED_RANGE_ATTACK.code
                this.room = params.get("room")!!
                this.toRoom = params.flag.pos.roomName
            })
        }
    },

    NEW_ROOM("newRoom") {
        override fun execute(params: Params) {
            newRoom(params.get("room")!!, params.flag.pos.roomName)
        }
    },

    SK_ROOM("skRoom") {
        override fun execute(params: Params) {
            skRoom(params.get("room")!!, params.flag.pos.roomName)
        }
    },

    STOP_SK_ROOM("stopSkRoom") {
        override fun execute(params: Params) {
            stopSkRoom(params.get("room")!!, params.flag.pos.roomName)
        }
    },

    BOOSTED_DISMANTLE("boostedDismantle") {
        override fun execute(params: Params) {
            createTask("${params.get("room")}|${params.flag.pos.roomName}|${TaskType.BOOSTED_DISMANTLE_ATTACK_POPULATION.code}", jsObject {
                this.type = TaskType.BOOSTED_DISMANTLE_ATTACK_POPULATION.code
                this.room = params.get("room")!!
                this.toRoom = params.flag.pos.roomName
            })
        }
    },

    ;

    abstract fun execute(params: Params)

    companion object {
        fun byCode(code: String): FlagCommand {
            return values().first { it.code == code }
        }
    }
}

fun processFlags() {
    Game.flags.entries.forEach {
        try {
            processFlag(it.component1(), it.component2())
        } catch (e: Throwable) {
            console.log("${it.component1()}: $e")
        }
    }
}

private fun processFlag(flagName: String, flag: Flag) {
    val parts = flagName.split(";")
    val params = hashMapOf<String, String>()
    parts.forEach {
        val split = it.split("=")
        params[split[0]] = split[1]
    }
    when (flag.color) {
        COLOR_RED -> {
            when (flag.secondaryColor) {
                COLOR_RED -> params.put("command", FlagCommand.BOOSTED_DISMANTLE.code)
            }
        }
    }
    FlagCommand.byCode(params.get("command")!!.unsafeCast<String>()).execute(Params(flagName, flag, params))
    flag.remove()
}

class Params(
        val flagName: String,
        val flag: Flag,
        val params: Map<String, String>
) {
    fun get(key: String): String? = params[key]
}