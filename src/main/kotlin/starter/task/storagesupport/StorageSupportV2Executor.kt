package starter.task.storagesupport

import screeps.api.BodyPartConstant
import screeps.api.Creep
import screeps.api.ERR_NOT_IN_RANGE
import screeps.api.FIND_MY_STRUCTURES
import screeps.api.FIND_STRUCTURES
import screeps.api.Game
import screeps.api.OK
import screeps.api.RESOURCE_CATALYST
import screeps.api.RESOURCE_ENERGY
import screeps.api.RESOURCE_GHODIUM
import screeps.api.RESOURCE_OPS
import screeps.api.RESOURCE_POWER
import screeps.api.ResourceConstant
import screeps.api.Room
import screeps.api.STRUCTURE_LINK
import screeps.api.STRUCTURE_TERMINAL
import screeps.api.StoreOwner
import screeps.api.options
import screeps.api.structures.Structure
import screeps.api.structures.StructureLink
import screeps.api.structures.StructureStorage
import screeps.utils.unsafe.jsObject
import starter.ALL_COMPOUNDS_WITH_MINERALS
import starter.MECHANICAL_CHAIN
import starter.SIMPLE_COMMODITIES
import starter.TIER_3_COMPOUNDS
import starter.calculateStorageSupportBody
import starter.extension.moveToWithSwap
import starter.getUsed
import starter.memory.TaskMemory
import starter.memory.TransferTask
import starter.memory.checkLinkedTasks
import starter.memory.controllerLinkId
import starter.memory.createLinkedTask
import starter.memory.fromId
import starter.memory.linkId
import starter.memory.linkedTaskIds
import starter.memory.resource
import starter.memory.room
import starter.memory.storageLinkId
import starter.memory.terminalId
import starter.memory.toId
import starter.memory.transferTask
import starter.memory.value
import starter.task.ExecutorWithExclusiveCreep
import starter.task.TaskType

/**
 *
 *
 * @author zakharchuk
 * @since 27.03.2020
 */
class StorageSupportV2Executor(task: TaskMemory) : ExecutorWithExclusiveCreep(task) {

    override fun getCreepBody(room: Room): Array<BodyPartConstant> = room.calculateStorageSupportBody()

    override fun priority(): Boolean = true

    override fun executeInternal(creep: Creep) {
        task.checkLinkedTasks()

        if (task.transferTask == null) {
            task.transferTask = pickTask(task, creep)
        }

        if (task.transferTask != null) {
            val transferTask = task.transferTask!!
            val resource = transferTask.resource!!
            val value = transferTask.value!!
            if ((creep.store.getUsedCapacity(resource) ?: 0) < value) {
                val from = Game.getObjectById<Structure>(transferTask.fromId!!)
                if (from == null) {
                    task.transferTask = null
                    return
                }
                if (creep.withdraw(from, resource, value - (creep.store.getUsedCapacity(resource) ?: 0)) == ERR_NOT_IN_RANGE) {
                    creep.moveToWithSwap(from)
                }
            } else {
                val to = Game.getObjectById<Structure>(transferTask.toId!!)
                if (to == null) {
                    task.transferTask = null
                    return
                }

                when (creep.transfer(to, resource, value)) {
                    OK -> {
                        task.transferTask = null
                        if (to.id == task.storageLinkId) {
                            val storageLink = if (task.storageLinkId != "undefined") Game.getObjectById<StructureLink>(task.storageLinkId) else null
                            val controllerLink = if (task.controllerLinkId != "undefined") Game.getObjectById<StructureLink>(task.controllerLinkId) else null

                            if (storageLink == null || controllerLink == null) {
                                return
                            }

                            if (storageLink.cooldown == 0 && ((storageLink.store.getUsedCapacity(RESOURCE_ENERGY)
                                            ?: 0) + value) >= controllerLink.store.getFreeCapacity(RESOURCE_ENERGY) ?: 0) {
                                if (task.linkedTaskIds.isNullOrEmpty()) {
                                    task.createLinkedTask(TaskType.LINK_TRANSFER, jsObject {
                                        linkId = storageLink.id
                                    })
                                }
                            }
                        }
                    }
                    ERR_NOT_IN_RANGE -> creep.moveToWithSwap(to)
                    else -> task.transferTask = null
                }
            }
        }
    }

    override fun prepare(task: TaskMemory, creep: Creep): Boolean {
        val storage = creep.room.storage ?: return false
        if (task.storageLinkId == "undefined") {
            val storageLink = storage.pos.findInRange(FIND_MY_STRUCTURES, 2, options {
                filter = { it.structureType == STRUCTURE_LINK }
            }).firstOrNull()
            if (storageLink != null) {
                task.storageLinkId = storageLink.id
            }
        }

        if (task.controllerLinkId == "undefined") {
            val controller = creep.room.controller ?: return false
            val controllerLink = controller.pos.findInRange(FIND_MY_STRUCTURES, 2, options {
                filter = { it.structureType == STRUCTURE_LINK }
            }).firstOrNull()
            if (controllerLink != null) {
                task.controllerLinkId = controllerLink.id
            }
        }

        if (task.terminalId == null) {
            val terminal = creep.room.find(FIND_STRUCTURES, options {
                filter = { it.structureType == STRUCTURE_TERMINAL }
            }).firstOrNull()
            if (terminal != null) {
                task.terminalId = terminal.id
            }
        }

        return true
    }

    private fun pickTask(task: TaskMemory, creep: Creep): TransferTask? {
        val room = creep.room
        val storage = room.storage!!


        val storageLink =
                if (task.storageLinkId != "undefined" && task.linkedTaskIds.isNullOrEmpty()) {
                    Game.getObjectById<StructureLink>(task.storageLinkId)
                } else {
                    null
                }
        val controllerLink = if (task.controllerLinkId != "undefined") Game.getObjectById<StructureLink>(task.controllerLinkId) else null
        val terminal = if (task.terminalId != "undefined") Game.getObjectById<StructureLink>(task.terminalId) else null

        val energyTask = pickEnergyTask(creep, storage, storageLink, controllerLink, terminal)
        if (energyTask != null) {
            return energyTask
        }

        if (terminal != null) {
            ALL_COMPOUNDS_WITH_MINERALS.forEach {
                val mineralTask = pickMineralTask(creep, storage, terminal, it)
                if (mineralTask != null) {
                    return mineralTask
                }
            }

            SIMPLE_COMMODITIES.forEach {
                val commoditiesTask = pickCommoditiesTask(creep, storage, terminal, it)
                if (commoditiesTask != null) {
                    return commoditiesTask
                }
            }

            MECHANICAL_CHAIN.forEach {
                val commoditiesTask = pickCommoditiesTask(creep, storage, terminal, it)
                if (commoditiesTask != null) {
                    return commoditiesTask
                }
            }

            val opsTask = pickOpsTask(creep, storage, terminal)
            if (opsTask != null) {
                return opsTask
            }

            val powerTask = pickPowerTask(creep, storage, terminal)
            if (powerTask != null) {
                return powerTask
            }
        }

        return null
    }

    private fun pickPowerTask(creep: Creep, storage: StructureStorage, terminal: StructureLink): TransferTask? {
        val terminalWithMin = StructureWithMinValue.create(terminal, 0)
        if (terminal.store.getUsed(RESOURCE_POWER) > 0) {
            return transferResource(creep, arrayOf(terminalWithMin), storage, RESOURCE_POWER)
        } else {
            return null
        }
    }

    private fun pickMineralTask(creep: Creep, storage: StructureStorage, terminal: StructureLink, resource: ResourceConstant): TransferTask? {
        if (storage.room.controller!!.level == 8) {
            val storageValue = if (TIER_3_COMPOUNDS.contains(resource) || resource == RESOURCE_GHODIUM) 5000 else 10000
            val terminalValue = 5000
            if (storage.store.getUsed(resource) < storageValue) {
                val terminalWithMin = StructureWithMinValue.create(terminal, 0)
                return transferResource(creep, arrayOf(terminalWithMin), storage, resource, storageValue)
            } else if (terminal.store.getUsed(resource) > terminalValue) {
                val terminalWithMin = StructureWithMinValue.create(terminal, terminalValue)
                return transferResource(creep, arrayOf(terminalWithMin), storage, resource)
            } else {
                val storageWithMin = StructureWithMinValue.create(storage, storageValue)
                if (terminal.store.getFreeCapacity() > 15000) {
                    return transferResource(creep, arrayOf(storageWithMin), terminal, resource, terminalValue)
                } else {
                    return null
                }
            }
        }

        val terminalWithMin = StructureWithMinValue.create(terminal, 0)
        val storageWithMin = StructureWithMinValue.create(storage, 10000)
        if (storage.store.getUsedCapacity(resource) ?: 0 < 10000) {
            return transferResource(creep, arrayOf(terminalWithMin), storage, resource)
        } else if (terminal.store.getFreeCapacity() > 15000) {
            return transferResource(creep, arrayOf(storageWithMin), terminal, resource)
        } else {
            return null
        }
    }

    private fun pickCommoditiesTask(creep: Creep, storage: StructureStorage, terminal: StructureLink, resource: ResourceConstant): TransferTask? {
        val storageWithMin = StructureWithMinValue.create(storage, 0)
        if (terminal.store.getFreeCapacity() > 15000) {
            return transferResource(creep, arrayOf(storageWithMin), terminal, resource, 5000)
        } else {
            return null
        }
    }

    private fun pickOpsTask(creep: Creep, storage: StructureStorage, terminal: StructureLink): TransferTask? {
        val storageWithMin = StructureWithMinValue.create(storage, 0)
        if (terminal.store.getFreeCapacity() > 15000) {
            return transferResource(creep, arrayOf(storageWithMin), terminal, RESOURCE_OPS, 5000)
        } else {
            return null
        }
    }

    private fun pickDefaultTask(creep: Creep, storage: StructureStorage, terminal: StructureLink, resource: ResourceConstant,
                                storageValue: Int = 25000, terminalValue: Int = 5000): TransferTask? {
        if (storage.store.getUsed(resource) < storageValue) {
            val terminalWithMin = StructureWithMinValue.create(terminal, 0)
            return transferResource(creep, arrayOf(terminalWithMin), storage, resource, storageValue)
        } else if (terminal.store.getUsed(resource) > terminalValue) {
            val terminalWithMin = StructureWithMinValue.create(terminal, terminalValue)
            return transferResource(creep, arrayOf(terminalWithMin), storage, resource)
        } else {
            val storageWithMin = StructureWithMinValue.create(storage, storageValue)
            if (terminal.store.getFreeCapacity() > 15000) {
                return transferResource(creep, arrayOf(storageWithMin), terminal, resource, terminalValue)
            } else {
                return null
            }
        }
    }

    private fun pickEnergyTask(creep: Creep, storage: StructureStorage, storageLink: StructureLink?, controllerLink: StructureLink?, terminal: StructureLink?): TransferTask? {
        val linkWithMin = StructureWithMinValue.create(storageLink, 0)
        val terminalWithMin = StructureWithMinValue.create(terminal, 25000)
        val storageWithMin = StructureWithMinValue.create(storage, 5000)
        if (storage.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0 < 5000) {
            return transferResource(creep, arrayOf(linkWithMin, terminalWithMin), storage, RESOURCE_ENERGY)
        } else {
            if (storage.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0 < 75000) {
                if (controllerLink == null) {
                    return transferResource(creep, arrayOf(linkWithMin, terminalWithMin), storage, RESOURCE_ENERGY)
                }

                return if (storageLink != null && storageLink.cooldown == 0
                        && storageLink.store.getFreeCapacity(RESOURCE_ENERGY) ?: 0 > 0
                        && controllerLink.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0 <= 400) {
                    transferResource(creep, arrayOf(storageWithMin), storageLink, RESOURCE_ENERGY)
                } else {
                    transferResource(creep, arrayOf(linkWithMin, terminalWithMin), storage, RESOURCE_ENERGY)
                }
            } else if (storage.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0 in 75000..124999) {
                if (controllerLink == null) {
                    return transferResource(creep, arrayOf(linkWithMin), storage, RESOURCE_ENERGY)
                }

                return if (storageLink != null && storageLink.cooldown == 0
                        && storageLink.store.getFreeCapacity(RESOURCE_ENERGY) ?: 0 > 0
                        && controllerLink.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0 <= 400) {
                    transferResource(creep, arrayOf(storageWithMin), storageLink, RESOURCE_ENERGY)
                } else {
                    transferResource(creep, arrayOf(linkWithMin), storage, RESOURCE_ENERGY)
                }
            } else {
                if (controllerLink == null) {
                    return if (terminal != null && terminal.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0 < 50000 && terminal.store.getFreeCapacity() > 15000) {
                        transferResource(creep, arrayOf(linkWithMin, StructureWithMinValue.create(storage, 125000)), terminal, RESOURCE_ENERGY, 50000)
                    } else {
                        transferResource(creep, arrayOf(linkWithMin), storage, RESOURCE_ENERGY)
                    }
                }

                if (storageLink != null && storageLink.cooldown == 0
                        && storageLink.store.getFreeCapacity(RESOURCE_ENERGY) ?: 0 > 0
                        && controllerLink.store.getFreeCapacity(RESOURCE_ENERGY) ?: 0 >= 400) {
                    return transferResource(creep, arrayOf(storageWithMin), storageLink, RESOURCE_ENERGY)
                } else {
                    return if (terminal != null && terminal.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0 < 50000 && terminal.store.getFreeCapacity() > 15000) {
                        transferResource(creep, arrayOf(linkWithMin, StructureWithMinValue.create(storage, 125000)), terminal, RESOURCE_ENERGY, 50000)
                    } else {
                        transferResource(creep, arrayOf(linkWithMin), storage, RESOURCE_ENERGY)
                    }
                }
            }
        }
    }

    private fun <Z> transferResource(creep: Creep, from: Array<StructureWithMinValue?>, to: Z, resource: ResourceConstant,
                                     maxValue: Int? = null): TransferTask? where Z : StoreOwner {
        val first = from.firstOrNull {
            it != null && (it.storeOwner.store.getUsed(resource) > it.minValue)
        } ?: return null
        return createTransferTask(creep, first, to, resource, maxValue ?: Int.MAX_VALUE)
    }

    private fun <Z> createTransferTask(creep: Creep, from: StructureWithMinValue, to: Z, resource: ResourceConstant,
                                       maxValue: Int): TransferTask? where Z : StoreOwner {
        val requiredCapacity = maxValue - to.store.getUsed(resource)
        val freeCapacity = to.store.getFreeCapacity(resource) ?: 0
        val capacity = from.storeOwner.store.getUsed(resource) - from.minValue
        val creepCapacity = creep.store.getCapacity(resource) ?: 0
        if (requiredCapacity <= 0 || freeCapacity == 0 || capacity <= 0 || creepCapacity == 0) {
            return null
        }

        return jsObject<TransferTask> {
            this.fromId = from.storeOwner.id
            this.toId = to.id
            this.resource = resource
            this.value = arrayOf(requiredCapacity, freeCapacity, capacity, creepCapacity).min()
        }
    }

    private class StructureWithMinValue(val storeOwner: StoreOwner, val minValue: Int) {
        companion object {
            fun create(storeOwner: StoreOwner?, minValue: Int): StructureWithMinValue? {
                return if (storeOwner == null) null else StructureWithMinValue(storeOwner, minValue)
            }
        }
    }
}