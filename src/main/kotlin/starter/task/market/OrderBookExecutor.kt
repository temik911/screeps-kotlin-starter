package starter.task.market

import screeps.api.CPU_UNLOCK
import screeps.api.Game
import screeps.api.Market
import screeps.api.Memory
import screeps.api.OK
import screeps.api.ORDER_BUY
import screeps.api.ORDER_SELL
import screeps.api.OrderConstant
import screeps.api.RESOURCE_ENERGY
import screeps.api.RESOURCE_OPS
import screeps.api.RESOURCE_POWER
import screeps.api.component1
import screeps.api.component2
import screeps.api.entries
import screeps.api.get
import screeps.api.structures.StructureTerminal
import screeps.utils.unsafe.delete
import screeps.utils.unsafe.jsObject
import starter.MECHANICAL_CHAIN
import starter.SIMPLE_COMMODITIES
import starter.SIMPLE_MINERALS
import starter.getUsed
import starter.memory.Order
import starter.memory.OrderSide
import starter.memory.createTime
import starter.memory.externalOrderCreated
import starter.memory.externalOrderId
import starter.memory.isDone
import starter.memory.marketPlace
import starter.memory.resourceType
import starter.memory.side
import starter.memory.terminalId
import starter.memory.value

/**
 *
 *
 * @author zakharchuk
 * @since 25.03.2020
 */

fun orderBook() {
    Game.market.orders.entries.forEach {
        if (it.component2().resourceType == CPU_UNLOCK) {
            return@forEach
        }

        if (!it.component2().active && it.component2().created + 10 < Game.time) {
            Game.market.cancelOrder(it.component1())
        } else if ((Game.time - it.component2().created) % 2000 == 0) {
            recalculatePrice(it.component2())
        }
    }

    Memory.marketPlace.entries.filter { order -> order.component2().externalOrderCreated }
            .forEach { entry ->
                val order = entry.component2()
                if (order.externalOrderId == null) {
                    val orderId = Game.market.orders.entries.firstOrNull { order.isMarketOrder(it.component2()) }?.component2()?.id
                            ?: return@forEach
                    order.externalOrderId = orderId
                } else {
                    val marketOrder = Game.market.getOrderById(order.externalOrderId!!)
                    if (marketOrder == null || !marketOrder.active) {
                        order.isDone = true
                    }
                }
            }

    Memory.marketPlace.entries.filter { order -> order.component2().isDone }
            .forEach { delete(Memory.marketPlace[it.component1()]) }

    Memory.marketPlace.entries.filter { order -> order.component2().externalOrderCreated && order.component2().externalOrderId == null && ((order.component2().createTime!! + 250) < Game.time) }
            .forEach { delete(Memory.marketPlace[it.component1()]) }

    val entries = Memory.marketPlace.entries
    for (entity in entries) {
        if (entity.component2().isDone) {
            continue
        }
        val order = entity.component2()

        if (order.externalOrderCreated) {
            continue
        }

        val receiverTerminal = Game.getObjectById<StructureTerminal>(order.terminalId) ?: continue
        val closeEntity = entries.firstOrNull { order.isCloseOrder(it.component2()) } ?: continue

        val closeOrder = closeEntity.component2()
        val terminal = Game.getObjectById<StructureTerminal>(closeOrder.terminalId) ?: continue

        if (closeOrder.side == OrderSide.SELL) {
            if (receiverTerminal.id == terminal.id || terminal.cooldown != 0) {
                continue
            }

            val cost = Game.market.calcTransactionCost(closeOrder.value!!, terminal.room.name, receiverTerminal.room.name)
            if (terminal.store.getUsed(RESOURCE_ENERGY) < (cost + (if (order.resourceType == RESOURCE_ENERGY) order.value!! else 0))) {
                continue
            }
            terminal.send(order.resourceType!!, order.value!!, receiverTerminal.room.name)

            order.isDone = true
            closeOrder.isDone = true
            return
        }
    }

    for (entity in entries) {
        val order = entity.component2()

        if (order.isDone) {
            continue
        }

        var isProceeded = false
        if (order.side == OrderSide.BUY && order.resourceType == RESOURCE_ENERGY) {
            isProceeded = processBuyEnergyOrder(order)
        } else if (order.side == OrderSide.SELL && order.resourceType == RESOURCE_ENERGY) {
            isProceeded = processSellEnergyOrder(order)
        } else if (order.side == OrderSide.BUY && SIMPLE_MINERALS.contains(order.resourceType)) {
            isProceeded = processBuySimpleMineralOrder(order)
        } else if (order.side == OrderSide.SELL && SIMPLE_MINERALS.contains(order.resourceType)) {
            isProceeded = processSellSimpleMineralOrder(order)
        } else if (order.side == OrderSide.SELL && MECHANICAL_CHAIN.contains(order.resourceType)) {
            isProceeded = processSellCommoditiesOrder(order)
        } else if (order.side == OrderSide.BUY && order.resourceType == RESOURCE_POWER) {
            isProceeded = processBuyPowerOrder(order)
        } else if (order.side == OrderSide.SELL && order.resourceType == RESOURCE_OPS) {
            isProceeded = processSellOpsOrder(order)
        } else if (order.side == OrderSide.SELL && SIMPLE_COMMODITIES.contains(order.resourceType)) {
            isProceeded = processSellSimpleCommoditiesOrder(order)
        }

        if (isProceeded) {
            return
        }
    }
}

fun processBuyPowerOrder(order: Order): Boolean {
    if (Game.market.credits < 1_000_000) {
        return false
    }

    val terminal = Game.getObjectById<StructureTerminal>(order.terminalId) ?: return false
    if (terminal.cooldown != 0) {
        return false
    }

    var maxPrice = 0.0
    Game.market.getHistory(RESOURCE_POWER).takeLast(4)
            .forEach {
                maxPrice += 1.1 * it.avgPrice / 4
            }

    val marketOrder = Game.market.getAllOrders(jsObject<Market.Order.Filter> {
        this.resourceType = order.resourceType.toString()
        this.type = ORDER_SELL
    }).filter {
        it.amount > 0 && it.price < maxPrice
    }.minBy { it.price } ?: return false

    val amount = if (marketOrder.amount > order.value!!) order.value!! else marketOrder.amount
    if (Game.market.deal(marketOrder.id, amount, terminal.room.name) == OK) {
        order.isDone = true
        return true
    }
    return false
}

fun processSellCommoditiesOrder(order: Order): Boolean {
    return processDefaultSellOrder(order)
}

fun processSellOpsOrder(order: Order): Boolean {
    return processDefaultSellOrder(order)
}

fun processSellSimpleCommoditiesOrder(order: Order): Boolean {
    return processDefaultSellOrder(order)
}

fun processDefaultSellOrder(order: Order): Boolean {
    val terminal = Game.getObjectById<StructureTerminal>(order.terminalId) ?: return false

    var minPrice = 0.0
    Game.market.getHistory(order.resourceType).takeLast(4)
            .forEach {
                minPrice += 0.9 * it.avgPrice / 4
            }

    val marketOrder = Game.market.getAllOrders(jsObject<Market.Order.Filter> {
        this.resourceType = order.resourceType.toString()
        this.type = ORDER_BUY
    }).filter {
        it.price > minPrice
    }.maxBy { it.price } ?: return false

    val value = if (marketOrder.amount >= order.value!!) order.value!! else marketOrder.amount

    if (Game.market.deal(marketOrder.id, value, terminal.room.name) == OK) {
        order.isDone = true
        return true
    }
    return false
}

fun processSellEnergyOrder(order: Order): Boolean {
    val terminal = Game.getObjectById<StructureTerminal>(order.terminalId) ?: return false

    if (terminal.cooldown != 0) {
        return false
    }

    if (order.createTime!! + 50 > Game.time) {
        return false
    }

    if (terminal.store.getUsed(order.resourceType!!) < 50000) {
        return false
    }

    val storage = terminal.room.storage ?: return false

    if (storage.store.getUsed(order.resourceType!!) < 200000) {
        return false
    }

    val marketOrder = Game.market.getAllOrders(jsObject<Market.Order.Filter> {
        this.resourceType = order.resourceType.toString()
        this.type = order.side.getOppositeSide().toOrderConstant()
    }).filter { marketOrder ->
        marketOrder.amount > 0 && energyCostIsOk(order, marketOrder, terminal)
    }.maxBy { marketOrder ->
        marketOrder.price
    } ?: return false

    val amountToSell = if (marketOrder.amount >= order.value!!) order.value!! else marketOrder.amount
    if (Game.market.deal(marketOrder.id, amountToSell, terminal.room.name) == OK) {
        order.isDone = true
        return true
    }
    return false
}

fun processSellSimpleMineralOrder(order: Order): Boolean {
    val terminal = Game.getObjectById<StructureTerminal>(order.terminalId) ?: return false

    if (terminal.cooldown != 0) {
        return false
    }

    if (order.createTime!! + 50 > Game.time) {
        return false
    }

    val storageStore = terminal.room.storage?.store?.getUsed(order.resourceType!!) ?: 0
    if (storageStore < 100000) {
        return false
    }

    var minPrice = 0.0
    Game.market.getHistory(order.resourceType).takeLast(4)
            .forEach {
                minPrice += 0.9 * it.avgPrice / 4
            }

    val marketOrder = Game.market.getAllOrders(jsObject<Market.Order.Filter> {
        this.resourceType = order.resourceType.toString()
        this.type = order.side.getOppositeSide().toOrderConstant()
    }).filter { marketOrder ->
        marketOrder.amount > 0 && marketOrder.price > minPrice && energyCostIsOk(order, marketOrder, terminal)
    }.maxBy { marketOrder ->
        marketOrder.price
    } ?: return false

    val amountToSell = if (marketOrder.amount >= order.value!!) order.value!! else marketOrder.amount
    if (Game.market.deal(marketOrder.id, amountToSell, terminal.room.name) == OK) {
        order.isDone = true
        return true
    }
    return false
}

fun energyCostIsOk(order: Order, marketOrder: Market.Order, terminal: StructureTerminal): Boolean {
    val amountToSell = if (marketOrder.amount >= order.value!!) order.value!! else marketOrder.amount
    val cost = Game.market.calcTransactionCost(amountToSell, terminal.room.name, marketOrder.roomName)
    return terminal.store.getUsed(RESOURCE_ENERGY) > (cost + if (order.resourceType == RESOURCE_ENERGY) amountToSell else 0)
}

fun recalculatePrice(order: Market.Order) {
    if (order.resourceType == CPU_UNLOCK) {
        return
    }

    var price = 0.0
    Game.market.getHistory(order.resourceType).takeLast(4)
            .forEach {
                price += 1.1 * it.avgPrice / 4
            }

    Game.market.changeOrderPrice(order.id, price)
}

fun processBuySimpleMineralOrder(order: Order): Boolean {
    val terminal = Game.getObjectById<StructureTerminal>(order.terminalId) ?: return false
    if (terminal.room.controller!!.level < 7) {
        return false
    }

    if (terminal.cooldown != 0) {
        return false
    }

    if (order.createTime!! + 250 > Game.time) {
        return false
    }

    return processBuyEnergyOrder(order)
}

private fun processBuyEnergyOrder(order: Order): Boolean {
    if (order.createTime!! + 25 > Game.time) {
        return false
    }

    if (order.externalOrderCreated) {
        return false
    }

    if (Game.market.credits < 150000) {
        return false
    }

    val terminal = Game.getObjectById<StructureTerminal>(order.terminalId) ?: return false

    var price = 0.0
    Game.market.getHistory(order.resourceType).takeLast(4)
            .forEach {
                price += 1.03 * it.avgPrice / 4
            }

//    val marketOrder = Game.market.getAllOrders(jsObject<Market.Order.Filter> {
//        this.resourceType = order.resourceType.toString()
//        this.type = order.side.getOppositeSide().toOrderConstant()
//    }).filter { marketOrder ->
//        marketOrder.price < maxPrice
//    }.sortedBy { marketOrder ->
//        marketOrder.price
//    }.firstOrNull()?: return false

//    console.log("${marketOrder.type} ${marketOrder.remainingAmount} ${marketOrder.price} ${marketOrder.resourceType}")
//
//    val receiverTerminal = Game.getObjectById<StructureTerminal>(order.terminalId) ?: return false
//    val cost = Game.market.calcTransactionCost(25000, marketOrder.roomName, receiverTerminal.room.name)
//    console.log(cost)

    val createOrderResult = Game.market.createOrder(jsObject {
        this.price = price
        this.resourceType = order.resourceType!!
        this.type = order.side.toOrderConstant()
        this.totalAmount = order.value!!
        this.roomName = terminal.room.name
    })

    if (createOrderResult == OK) {
        order.externalOrderCreated = true
        return true
    }

    return false
}

private fun OrderSide.getOppositeSide(): OrderSide {
    return when (this) {
        OrderSide.BUY -> OrderSide.SELL
        OrderSide.SELL -> OrderSide.BUY
    }
}

private fun OrderSide.toOrderConstant(): OrderConstant {
    return when (this) {
        OrderSide.BUY -> ORDER_BUY
        OrderSide.SELL -> ORDER_SELL
    }
}

private fun Order.isCloseOrder(closeOrder: Order): Boolean {
    return !isDone && !closeOrder.isDone
            && resourceType == closeOrder.resourceType
            && value == closeOrder.value
            && side == closeOrder.side.getOppositeSide()
            && terminalId != closeOrder.terminalId
}

private fun Order.isMarketOrder(marketOrder: Market.Order): Boolean {
    val terminal = Game.getObjectById<StructureTerminal>(terminalId) ?: return false
    return resourceType == marketOrder.resourceType
            && value == marketOrder.totalAmount
            && side.toOrderConstant() == marketOrder.type
            && terminal.room.name == marketOrder.roomName
            && marketOrder.active
}