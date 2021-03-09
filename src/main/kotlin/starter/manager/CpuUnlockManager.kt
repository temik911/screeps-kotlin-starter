package starter.manager

import screeps.api.CPU_UNLOCK
import screeps.api.Game
import screeps.api.Market
import screeps.api.ORDER_BUY
import screeps.api.ORDER_SELL
import screeps.api.get
import screeps.api.values
import screeps.utils.unsafe.jsObject
import kotlin.js.Date


/**
 *
 *
 * @author zakharchuk
 * @since 30.10.2020
 */
class CpuUnlockManager {

    fun manage() {
        val maxPrice = calculateMaxPrice()
        console.log("Max price for CPU_UNLOCK: $maxPrice")
        tryToBuy(maxPrice)
        checkSelfBuyOrder(maxPrice)
        activateIfNeeded()
    }

    private fun calculateMaxPrice(): Double {
        var maxPrice = 0.0;
        Game.market.getHistory(CPU_UNLOCK).takeLast(7)
                .forEach {
                    maxPrice += 1.05 * it.avgPrice / 7
                }
        return maxPrice
    }

    private fun activateIfNeeded() {
        if ((Game.resources[CPU_UNLOCK.toString()] ?: 0) == 0) {
            return
        }

        val unlockedTime = Game.cpu.unlockedTime ?: return
        val timeUnlockedDate = Date(unlockedTime)
        val diff = (timeUnlockedDate.getTime() - Date().getTime()) / (1000 * 60 * 60 * 24)
        if (diff < 30) {
            Game.cpu.unlock()
        }
    }

    private fun tryToBuy(maxPrice: Double) {
        if (Game.market.credits < 50_000_000) {
            return
        }

        if (maxPrice > 2_500_000) {
            return
        }

        val maxCredits = Game.market.credits - 50_000_000

        val marketOrder = Game.market.getAllOrders(jsObject<Market.Order.Filter> {
            this.resourceType = CPU_UNLOCK.toString()
            this.type = ORDER_SELL
        }).filter {
            it.price < maxPrice && it.amount > 0
        }.minBy { it.price } ?: return

        val maxAmountToBuy = (maxCredits / marketOrder.price).toInt() + 1

        Game.market.deal(marketOrder.id, if (maxAmountToBuy > marketOrder.amount) marketOrder.amount else maxAmountToBuy)
    }

    private fun checkSelfBuyOrder(maxPrice: Double) {
        if ((Game.resources[CPU_UNLOCK.toString()] ?: 0) > 60) {
            return
        }

        if (maxPrice > 2_500_000) {
            return
        }

        val cpuUnlockOrder = Game.market.orders.values.filter {
            it.resourceType == CPU_UNLOCK
        }.firstOrNull()

        if (cpuUnlockOrder == null) {
            if (10 * maxPrice * 1.1 > Game.market.credits) {
                return
            }

            Game.market.createOrder(jsObject {
                this.price = maxPrice
                this.resourceType = CPU_UNLOCK
                this.type = ORDER_BUY
                this.totalAmount = 10
            })
        } else {
            if (cpuUnlockOrder.remainingAmount == 0) {
                Game.market.cancelOrder(cpuUnlockOrder.id)
                return
            }

            if (cpuUnlockOrder.price * 1.05 < maxPrice) {
                Game.market.changeOrderPrice(cpuUnlockOrder.id, maxPrice)
            }
        }
    }

}