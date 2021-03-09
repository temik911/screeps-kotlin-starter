package starter.task.harvest.power

import screeps.api.Creep
import screeps.api.Deposit
import screeps.api.Game
import screeps.api.Room
import screeps.api.RoomPosition
import screeps.api.structures.StructurePowerBank
import starter.Role
import starter.calculatePowerBankHarvestBody
import starter.calculatePowerBankHealerBody
import starter.extension.moveToWithSwap
import starter.memory.TaskMemory
import starter.memory.expectedDamage
import starter.memory.harvestTask
import starter.memory.isDone
import starter.memory.role
import starter.memory.sourceId
import starter.memory.targetPosition
import starter.task.ExecutorWithSeveralCreeps

/**
 *
 *
 * @author zakharchuk
 * @since 28.04.2020
 */
class PowerBankHarvestExecutor(task: TaskMemory) : ExecutorWithSeveralCreeps(task) {

    override fun prepare(task: TaskMemory, creeps: List<Creep>): Boolean {
        var isReady = true
        creeps.forEach {
            val harvestTask = task.harvestTask ?: return false
            val source = Game.getObjectById<Deposit>(harvestTask.sourceId!!)

            if (source == null) {
                val targetPosition = harvestTask.targetPosition ?: return false
                it.moveToWithSwap(RoomPosition(targetPosition.x, targetPosition.y, targetPosition.roomName))
                isReady = false
            } else {
                if (it.pos.getRangeTo(source.pos) > 5) {
                    if (it.pos.roomName == source.pos.roomName) {
                        it.moveTo(source)
                    } else {
                        it.moveToWithSwap(source)
                    }
                    isReady = false
                }
            }
        }
        return isReady
    }

    override fun executeInternal(creeps: List<Creep>) {
        val harvester = creeps.firstOrNull { it.memory.role == Role.POWER_BANK_HARVEST }
        val healer = creeps.firstOrNull { it.memory.role == Role.POWER_BANK_HEALER }

        if (healer == null || harvester == null) {
            task.isDone = true
            return
        } else {
            task.expectedDamage = harvester.ticksToLive * 600
        }

        val harvestTask = task.harvestTask ?: return
        val powerBank = Game.getObjectById<StructurePowerBank>(harvestTask.sourceId!!) ?: return

        if (!harvester.pos.isNearTo(powerBank)) {
            harvester.moveTo(powerBank)
        }

        if (!healer.pos.isNearTo(harvester)) {
            healer.moveTo(harvester)
        }

        if (harvester.pos.isNearTo(powerBank) && healer.pos.isNearTo(harvester)) {
            harvester.attack(powerBank)
            healer.heal(harvester)
        }
    }

    override fun getCreepRequests(room: Room): Array<CreepRequest> {
        return arrayOf(
                CreepRequest(room.calculatePowerBankHarvestBody(), Role.POWER_BANK_HARVEST),
                CreepRequest(room.calculatePowerBankHealerBody(), Role.POWER_BANK_HEALER)
        )
    }

    override fun getCreepsCount(): Int = 2
}