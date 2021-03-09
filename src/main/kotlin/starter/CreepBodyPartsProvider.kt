package starter

import screeps.api.ATTACK
import screeps.api.ActiveBodyPartConstant
import screeps.api.BODYPART_COST
import screeps.api.BodyPartConstant
import screeps.api.CARRY
import screeps.api.CLAIM
import screeps.api.FIND_HOSTILE_CREEPS
import screeps.api.Game
import screeps.api.HEAL
import screeps.api.MOVE
import screeps.api.RANGED_ATTACK
import screeps.api.Room
import screeps.api.TOUGH
import screeps.api.WORK
import screeps.api.get
import screeps.api.options

fun Room.calculateRemoteRoomAnalyzer(): Array<BodyPartConstant> {
    val body = mutableListOf(MOVE)
    return body.toTypedArray()
}

fun Room.calculateBaseEnergySupportBody(): Array<BodyPartConstant> {
    val body = mutableListOf(MOVE, CARRY, MOVE, CARRY, MOVE, CARRY)
    val part = arrayOf(MOVE, CARRY)
    return calculateBody(body, part, 20)
}

fun Room.calculateStorageLinkEnergySupportBody(): Array<BodyPartConstant> {
    val body = mutableListOf(MOVE, CARRY)
    val part = arrayOf(CARRY)
    return calculateBody(body, part, 9)
}

fun Room.calculateStorageSupportBody(): Array<BodyPartConstant> {
    val body = mutableListOf(MOVE, CARRY, CARRY)
    val part = arrayOf(MOVE, CARRY, CARRY)
    return calculateBody(body, part, 15)
}

fun Room.calculateDismantleBody(): Array<BodyPartConstant> {
    val body = mutableListOf(MOVE, WORK)
    val part = arrayOf(MOVE, WORK)
    return calculateBody(body, part, 50)
}

fun Room.calculateRepairBody(): Array<BodyPartConstant> {
    val body = mutableListOf(WORK, CARRY, MOVE)
    val part = arrayOf(WORK, CARRY, MOVE)
    return calculateBody(body, part, 15)
}

fun Room.calculateRepairRampartBody(): Array<BodyPartConstant> {
    val body = mutableListOf(CARRY, WORK, WORK, WORK, MOVE, MOVE)
    val part = arrayOf(CARRY, WORK, WORK, WORK, MOVE, MOVE)
    return calculateBody(body, part, 50)
}

fun Room.calculateBoostedRepairRampartBody(): Array<BodyPartConstant> {
    val body: MutableList<ActiveBodyPartConstant> = mutableListOf()
    repeat(10) { body.add(CARRY) }
    repeat(30) { body.add(WORK) }
    repeat(10) { body.add(MOVE) }
    body.sortBy { toInt(it) }
    return body.toTypedArray()
}

fun Room.calculateDepositHarvestBody(): Array<BodyPartConstant> {
    val body = mutableListOf(WORK, CARRY, MOVE)
    val part = arrayOf(WORK, CARRY, MOVE)
    return calculateBody(body, part, 50)
}

fun Room.calculateUpgradeBody(): Array<BodyPartConstant> {
    val body = mutableListOf(WORK, CARRY, MOVE)
    val part = arrayOf(WORK, CARRY, MOVE)
    return calculateBody(body, part, 50)
}

fun Room.calculateLinkUpgradeBody(): Array<BodyPartConstant> {
    val body = mutableListOf(CARRY, CARRY, CARRY, CARRY)
    val part = arrayOf(WORK, MOVE)
    while (energyCapacityAvailable / 2 > body.sumBy { BODYPART_COST[it]!! } && body.size <= 34 - part.size) {
        body.addAll(part)
    }
    body.sortBy { toInt(it) }
    return body.toTypedArray()
}

fun Room.calculateBuilderBody(): Array<BodyPartConstant> {
    val body = mutableListOf(WORK, CARRY, MOVE)
    val part = arrayOf(WORK, CARRY, MOVE)
    return calculateBody(body, part, 30)
}

fun Room.calculateHarvestBody(): Array<BodyPartConstant> {
    val body = mutableListOf(WORK, WORK, WORK, WORK, CARRY, MOVE, MOVE)
    if (energyCapacityAvailable - 100 >= body.sumBy { BODYPART_COST[it]!! }) {
        body.add(WORK)
    }
    if (energyCapacityAvailable - 50 >= body.sumBy { BODYPART_COST[it]!! }) {
        body.add(MOVE)
    }
    if (energyCapacityAvailable - 150 >= body.sumBy { BODYPART_COST[it]!! }) {
        body.addAll(listOf(WORK, CARRY))
    }
    body.sortBy { toInt(it) }
    return body.toTypedArray()
}

fun Room.calculateSkHarvestBody(): Array<BodyPartConstant> {
    val body: MutableList<ActiveBodyPartConstant> = mutableListOf()
    repeat(10) { body.add(WORK) }
    repeat(2) { body.add(CARRY) }
    repeat(6) { body.add(MOVE) }
    body.sortBy { toInt(it) }
    return body.toTypedArray()
}

fun Room.calculateMineralHarvestBody(): Array<BodyPartConstant> {
    val body = mutableListOf(WORK, WORK, MOVE)
    val part = arrayOf(WORK, WORK, MOVE)
    return calculateBody(body, part, 50)
}

fun Room.calculatePrimitiveBody(): Array<BodyPartConstant> {
    val body = mutableListOf(WORK, CARRY, MOVE)
    val part = listOf(WORK, CARRY, MOVE)
    val partCost = part.sumBy { BODYPART_COST[it]!! }
    while (energyAvailable >= body.sumBy { BODYPART_COST[it]!! } + partCost
            && body.size <= 30 - part.size) {
        body.addAll(part)
    }
    body.sortBy { toInt(it) }
    return body.toTypedArray()
}

fun Room.calculatePrepareExternalRoom(): Array<BodyPartConstant> {
    val body = mutableListOf(MOVE)
    return body.toTypedArray()
}

fun Room.calculateReserveController(): Array<BodyPartConstant> {
    val body = mutableListOf(MOVE, MOVE, CLAIM, CLAIM)
    val part = arrayOf(MOVE, CLAIM)
    return calculateBody(body, part, 12)
}

fun Room.calculateTransfer(): Array<BodyPartConstant> {
    val body = mutableListOf(WORK, MOVE, CARRY, CARRY, MOVE)
    val part = arrayOf(CARRY, CARRY, MOVE)
    return calculateBody(body, part, 50)
}

fun Room.calculateLab(): Array<BodyPartConstant> {
    val body = mutableListOf(MOVE, CARRY, CARRY)
    val part = arrayOf(CARRY, CARRY, MOVE)
    return calculateBody(body, part, 15)
}

fun Room.calculateDefendExternalRoom(attackedRoomName: String): Array<BodyPartConstant> {
    if (controller!!.level == 8 && energyCapacityAvailable >= 5500) {
        val attackedRoom = Game.rooms.get(attackedRoomName)
        if (attackedRoom != null && attackedRoom.controller == null) {
            val invaders = attackedRoom.find(FIND_HOSTILE_CREEPS, options {
                filter = {
                    it.owner.username == "Invader"
                }
            })
            val body: MutableList<ActiveBodyPartConstant> = mutableListOf()
            return when (invaders.count()) {
                1 -> {
                    val part = arrayOf(RANGED_ATTACK, HEAL, MOVE, MOVE)
                    calculateBody(body, part, 36)
                }
                else -> {
                    repeat(5) { body.add(MOVE) }
                    repeat(20) { body.add(RANGED_ATTACK) }
                    repeat(20) { body.add(MOVE) }
                    repeat(5) { body.add(HEAL) }
                    body.toTypedArray()
                }
            }
        }
    }

    val body = mutableListOf(RANGED_ATTACK, RANGED_ATTACK, HEAL, MOVE, MOVE, MOVE)
    val part = arrayOf(RANGED_ATTACK, RANGED_ATTACK, HEAL, MOVE, MOVE, MOVE)
    return calculateBody(body, part, 36)
}

fun Room.calculateDefendSkRoom(): Array<BodyPartConstant> {
    val body: MutableList<ActiveBodyPartConstant> = mutableListOf()
    repeat(20) { body.add(MOVE) }
    repeat(20) { body.add(ATTACK) }
    repeat(5) { body.add(MOVE) }
    repeat(5) { body.add(HEAL) }
    return body.toTypedArray()
}

fun Room.calculateHealSkRoom(): Array<BodyPartConstant> {
    val body: MutableList<ActiveBodyPartConstant> = mutableListOf()
    repeat(5) { body.add(TOUGH) }
    repeat(10) { body.add(MOVE) }
    repeat(5) { body.add(HEAL) }
    return body.toTypedArray()
}

fun Room.calculatePowerBankHarvestBody(): Array<BodyPartConstant> {
    val body: MutableList<ActiveBodyPartConstant> = mutableListOf()
    repeat(10) { body.add(MOVE) }
    repeat(20) { body.add(ATTACK) }
    repeat(10) { body.add(MOVE) }
    return body.toTypedArray()
}

fun Room.calculatePowerBankHealerBody(): Array<BodyPartConstant> {
    val body: MutableList<ActiveBodyPartConstant> = mutableListOf()
    repeat(25) { body.add(MOVE) }
    repeat(25) { body.add(HEAL) }
    return body.toTypedArray()
}

fun Room.calculateClaimRoom(): Array<BodyPartConstant> {
    val body = mutableListOf(MOVE, CLAIM)
    val part = arrayOf(MOVE, CLAIM)
    return calculateBody(body, part, 50)
}

fun Room.calculateClaimToThiefRoom(): Array<BodyPartConstant> {
    return mutableListOf(MOVE, CLAIM).toTypedArray()
}

fun Room.calculateAttackBody(): Array<BodyPartConstant> {
    val body = mutableListOf(HEAL, HEAL, ATTACK, ATTACK, ATTACK, MOVE, MOVE, MOVE, MOVE, MOVE)
    val part = arrayOf(HEAL, HEAL, ATTACK, ATTACK, ATTACK, MOVE, MOVE, MOVE, MOVE, MOVE)
    val partCost = part.sumBy { BODYPART_COST[it]!! }
    while (energyCapacityAvailable >= body.sumBy { BODYPART_COST[it]!! } + partCost && body.size <= 50 - part.size) {
        body.addAll(part)
    }
    body.sortBy { toInt(it) }
    return body.toTypedArray()
}

fun Room.calculateRangeAttackBody(): Array<BodyPartConstant> {
    val body: MutableList<ActiveBodyPartConstant> = mutableListOf()
    repeat(15) { body.add(RANGED_ATTACK) }
    repeat(10) { body.add(HEAL) }
    repeat(25) { body.add(MOVE) }
    return body.toTypedArray()
}

fun Room.calculateBoostedRangeAttackBody(): Array<BodyPartConstant> {
    val body: MutableList<ActiveBodyPartConstant> = mutableListOf()
    repeat(10) { body.add(TOUGH) }
    repeat(20) { body.add(RANGED_ATTACK) }
    repeat(10) { body.add(MOVE) }
    repeat(10) { body.add(HEAL) }
    return body.toTypedArray()
}

fun Room.calculateBoostedDismantleBody(): Array<BodyPartConstant> {
    val body: MutableList<ActiveBodyPartConstant> = mutableListOf()
    repeat(10) { body.add(TOUGH) }
    repeat(30) { body.add(WORK) }
    repeat(10) { body.add(MOVE) }
    return body.toTypedArray()
}

fun Room.calculateBoostedHealerBody(): Array<BodyPartConstant> {
    val body: MutableList<ActiveBodyPartConstant> = mutableListOf()
    repeat(10) { body.add(TOUGH) }
    repeat(10) { body.add(RANGED_ATTACK) }
    repeat(10) { body.add(MOVE) }
    repeat(20) { body.add(HEAL) }
    return body.toTypedArray()
}

fun Room.calculateInvaderCoreDestroyerBody(): Array<BodyPartConstant> {
    val body = mutableListOf(ATTACK, MOVE)
    val part = arrayOf(ATTACK, MOVE)
    return calculateBody(body, part, 50)
}

fun Room.calculateHealBody(): Array<BodyPartConstant> {
    val body = mutableListOf(HEAL, MOVE)
    val part = arrayOf(HEAL, MOVE)
    val partCost = part.sumBy { BODYPART_COST[it]!! }
    while (energyCapacityAvailable > (body.sumBy { BODYPART_COST[it]!! } + partCost) && body.size <= 50 - part.size) {
        body.addAll(part)
    }
    body.sortBy { toInt(it) }
    return body.toTypedArray()
}

private fun <T> Room.calculateBody(body: MutableList<T>, part: Array<T>, maxSize: Int): Array<BodyPartConstant> where T : BodyPartConstant {
    val partCost = part.sumBy { BODYPART_COST[it]!! }
    while (energyCapacityAvailable / 2 >= body.sumBy { BODYPART_COST[it]!! }
            && energyCapacityAvailable >= body.sumBy { BODYPART_COST[it]!! } + partCost
            && body.size <= maxSize - part.size) {
        body.addAll(part)
    }
    body.sortBy { toInt(it) }
    return body.toTypedArray()
}

private fun toInt(part: BodyPartConstant): Int {
    return when (part) {
        CARRY -> 10
        MOVE -> 20
        WORK -> 30
        TOUGH -> -100
        ATTACK -> 0
        HEAL -> 21
        else -> 0
    }
}


