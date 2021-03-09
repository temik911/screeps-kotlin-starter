package starter.task.power.creep

import screeps.api.ERR_NOT_ENOUGH_RESOURCES
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.FIND_MY_POWER_CREEPS
import screeps.api.Game
import screeps.api.OK
import screeps.api.PWR_OPERATE_FACTORY
import screeps.api.PowerCreep
import screeps.api.RESOURCE_OPS
import screeps.api.get
import screeps.api.options
import screeps.api.structures.StructureFactory
import starter.extension.generateOps
import starter.extension.getCooldown
import starter.extension.moveToWithSwap
import starter.getUsed
import starter.memory.TaskMemory
import starter.memory.creepId
import starter.memory.factoryId
import starter.memory.isDone
import starter.memory.isFree
import starter.memory.room

/**
 *
 *
 * @author zakharchuk
 * @since 03.05.2020
 */
class PowerOperateFactoryExecutor(val task: TaskMemory) {
    fun execute() {
        val room = Game.rooms.get(task.room) ?: return
        if (task.creepId == "undefined") {
            val creep = room.find(FIND_MY_POWER_CREEPS, options {
                filter = {
                    it.memory.isFree && (it.ticksToLive ?: 0) > 1000 && it.getCooldown(PWR_OPERATE_FACTORY) == 0
                            && it.store.getUsed(RESOURCE_OPS) >= 100
                }
            }).firstOrNull() ?: return
            task.creepId = creep.id
            creep.memory.isFree = false
        }

        val creep = Game.getObjectById<PowerCreep>(task.creepId)
        if (creep == null) {
            task.isDone = true
            return
        }

        val factory = Game.getObjectById<StructureFactory>(task.factoryId)
        if (factory == null) {
            task.isDone = true
            creep.memory.isFree = true
            return
        }

        val usePower = creep.usePower(PWR_OPERATE_FACTORY, factory)
        when (usePower) {
            OK -> {
                task.isDone = true
                creep.memory.isFree = true
            }
            ERR_NOT_IN_RANGE -> {
                creep.moveToWithSwap(factory)
            }
            ERR_NOT_ENOUGH_RESOURCES -> {
                creep.generateOps()
            }
        }
    }
}