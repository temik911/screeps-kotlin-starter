package starter.memory

import screeps.api.Game
import screeps.api.Memory
import screeps.api.MemoryMarker
import screeps.api.MutableRecord
import screeps.api.ResourceConstant
import screeps.api.set
import screeps.api.values
import screeps.utils.memory.memory
import screeps.utils.memory.memoryWithSerializer
import screeps.utils.unsafe.jsObject
import starter.UniqueIdGenerator

/**
 *
 *
 * @author zakharchuk
 * @since 24.03.2020
 */

val Memory.marketPlace: MutableRecord<String, Order> by memory { jsObject<MutableRecord<String, Order>> { } }

external interface Order : MemoryMarker

var Order.side: OrderSide by memoryWithSerializer({ OrderSide.BUY }, { value -> value.code }, { code -> OrderSide.byCode(code) })
var Order.createTime: Int? by memory()
var Order.value: Int? by memory()
var Order.resourceType: ResourceConstant? by memory()
var Order.externalOrderId: String? by memory()
var Order.externalOrderCreated: Boolean by memory { false }
var Order.terminalId: String? by memory()
var Order.isDone: Boolean by memory { false }
var Order.isForce: Boolean by memory { false }

enum class OrderSide(val code: String) {
    BUY("buy"), SELL("sell");

    companion object {
        fun byCode(code: String): OrderSide {
            return values().first { it.code == code }
        }
    }
}

fun createOrder(side: OrderSide, value: Int, resourceType: ResourceConstant, terminalId: String, isForce: Boolean = false) {
    val uniqueId = UniqueIdGenerator.generateUniqueId()
    Memory.marketPlace.set(uniqueId, jsObject {
        this.side = side
        this.createTime = Game.time
        this.value = value
        this.resourceType = resourceType
        this.terminalId = terminalId
        this.isForce = isForce
    })
}

fun getOrdersCount(side: OrderSide, resourceType: ResourceConstant, terminalId: String): Int {
    return Memory.marketPlace.values.count { taskMemory ->
        taskMemory.side == side && taskMemory.resourceType == resourceType && taskMemory.terminalId == terminalId
    }
}