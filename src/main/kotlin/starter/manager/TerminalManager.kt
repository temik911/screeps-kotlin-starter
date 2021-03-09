package starter.manager

import screeps.api.Game
import screeps.api.RESOURCE_CATALYZED_GHODIUM_ACID
import screeps.api.RESOURCE_ENERGY
import screeps.api.RESOURCE_MACHINE
import screeps.api.RESOURCE_OPS
import screeps.api.RESOURCE_POWER
import screeps.api.Room
import starter.MECHANICAL_CHAIN
import starter.SIMPLE_COMMODITIES
import starter.SIMPLE_MINERALS
import starter.extension.store
import starter.getUsed
import starter.memory.OrderSide
import starter.memory.createOrder
import starter.memory.getOrdersCount
import starter.memory.sleepUntilTimes
import starter.memory.terminalManager

/**
 *
 *
 * @author zakharchuk
 * @since 13.06.2020
 */
class TerminalManager(val room: Room) {
    fun execute() {
        if (room.controller!!.level < 6) {
            return
        }

        if (room.memory.sleepUntilTimes.terminalManager > Game.time) {
            return
        }

        val storage = room.storage
        val terminal = room.terminal
        if (storage == null || terminal == null) {
            room.memory.sleepUntilTimes.terminalManager = Game.time + 1000
            return
        }

        if (storage.store.getUsed(RESOURCE_ENERGY) < 50000 && terminal.store.getUsed(RESOURCE_ENERGY) < 50000) {
            if (getOrdersCount(OrderSide.BUY, RESOURCE_ENERGY, terminal.id) == 0) {
                createOrder(OrderSide.BUY, 25000, RESOURCE_ENERGY, terminal.id)
                return
            }
        }

        if (storage.store.getUsed(RESOURCE_ENERGY) >= 125000 && terminal.store.getUsed(RESOURCE_ENERGY) >= 50000) {
            if (getOrdersCount(OrderSide.SELL, RESOURCE_ENERGY, terminal.id) == 0) {
                createOrder(OrderSide.SELL, 25000, RESOURCE_ENERGY, terminal.id)
                return
            }
        }

        SIMPLE_MINERALS.forEach {
            val storageStore = storage.store.getUsed(it)
            val terminalStore = terminal.store.getUsed(it)
            if (storageStore < 10000 && terminalStore < 3000) {
                if (getOrdersCount(OrderSide.BUY, it, terminal.id) == 0) {
                    createOrder(OrderSide.BUY, 3000, it, terminal.id)
                    return
                }
            } else if (storageStore >= 10000 && terminalStore >= 5000) {
                if (getOrdersCount(OrderSide.SELL, it, terminal.id) == 0) {
                    val isForce = storage.store.getUsedCapacity() > 750000
                    createOrder(OrderSide.SELL, 3000, it, terminal.id, isForce)
                    return
                }
            }
        }

        if (storage.store.getUsed(RESOURCE_OPS) >= 25000 && terminal.store.getUsed(RESOURCE_OPS) >= 5000) {
            if (getOrdersCount(OrderSide.SELL, RESOURCE_OPS, terminal.id) == 0) {
                createOrder(OrderSide.SELL, 3000, RESOURCE_OPS, terminal.id)
                return
            }
        }

        SIMPLE_COMMODITIES.forEach {
            if (storage.store.getUsed(it) >= 100000 && terminal.store.getUsed(it) >= 5000) {
                if (getOrdersCount(OrderSide.SELL, it, terminal.id) == 0) {
                    createOrder(OrderSide.SELL, 3000, it, terminal.id)
                    return
                }
            }
        }

        if (room.controller!!.level < 8 && storage.store.getUsed(RESOURCE_CATALYZED_GHODIUM_ACID) <= 3000 && terminal.store.getUsed(RESOURCE_CATALYZED_GHODIUM_ACID) == 0) {
            if (getOrdersCount(OrderSide.BUY, RESOURCE_CATALYZED_GHODIUM_ACID, terminal.id) == 0) {
                createOrder(OrderSide.BUY, 3000, RESOURCE_CATALYZED_GHODIUM_ACID, terminal.id)
                return
            }
        }

        if (room.controller!!.level == 8) {
            if (terminal.store.getUsed(RESOURCE_CATALYZED_GHODIUM_ACID) >= 3000) {
                if (getOrdersCount(OrderSide.SELL, RESOURCE_CATALYZED_GHODIUM_ACID, terminal.id) == 0) {
                    createOrder(OrderSide.SELL, 3000, RESOURCE_CATALYZED_GHODIUM_ACID, terminal.id)
                    return
                }
            }

            if (terminal.store.getUsed(RESOURCE_MACHINE) >= 1) {
                if (getOrdersCount(OrderSide.SELL, RESOURCE_MACHINE, terminal.id) == 0) {
                    createOrder(OrderSide.SELL, 1, RESOURCE_MACHINE, terminal.id)
                    return
                }
            }

//            if (room.store(RESOURCE_POWER) < 3000) {
//                if (getOrdersCount(OrderSide.BUY, RESOURCE_POWER, terminal.id) == 0) {
//                    createOrder(OrderSide.BUY, 3000, RESOURCE_POWER, terminal.id)
//                    return
//                }
//            }
        }
    }
}