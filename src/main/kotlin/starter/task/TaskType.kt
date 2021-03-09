package starter.task

import starter.memory.TaskMemory
import starter.task.attack.BoostedDismantleAttackExecutor
import starter.task.attack.BoostedDismantleAttackPopulationExecutor
import starter.task.attack.BoostedRangeAttackExecutor
import starter.task.attack.RangeAttackExecutor
import starter.task.attack.SquadAttackExecutor
import starter.task.baseenergysupport.BaseEnergySupportExecutor
import starter.task.baseenergysupport.PrimitiveBaseEnergySupportExecutor
import starter.task.build.BuildExecutor
import starter.task.build.BuildPopulationExecutor
import starter.task.claim.ClaimRoomExecutor
import starter.task.claim.ClaimRoomPopulationExecutor
import starter.task.claim.ClaimToClearRoomExecutor
import starter.task.claim.ClearRoomExecutor
import starter.task.claim.UnClaimRoomExecutor
import starter.task.claim.thief.ClaimToThiefExecutor
import starter.task.claim.thief.ClaimToThiefPopulationExecutor
import starter.task.claim.thief.ThiefExecutor
import starter.task.commodities.CommoditiesProcessor
import starter.task.commodities.ProduceCommoditiesExecutor
import starter.task.controller.reserve.ReserveControllerExecutor
import starter.task.controller.reserve.ReserveControllerPopulationExecutor
import starter.task.controller.upgrade.BoostedLinkUpgradeControllerExecutor
import starter.task.controller.upgrade.ContainerUpgradeControllerExecutor
import starter.task.controller.upgrade.LinkUpgradeControllerExecutor
import starter.task.controller.upgrade.PrimitiveUpgradeControllerExecutor
import starter.task.controller.upgrade.UpgradeControllerExecutor
import starter.task.controller.upgrade.UpgradeControllerPopulationExecutor
import starter.task.defend.DefendExecutor
import starter.task.defend.DefendPopulationExecutor
import starter.task.defend.InvaderCoreDestroyerExecutor
import starter.task.defend.SkDefendExecutor
import starter.task.defend.SkHealExecutor
import starter.task.defend.TowerDefendExecutor
import starter.task.dismantle.DismantlePopulationExecutor
import starter.task.dismantle.DismantleTargetExecutor
import starter.task.harvest.HarvestExecutor
import starter.task.harvest.HarvestPopulationExecutor
import starter.task.harvest.LinkHarvestExecutor
import starter.task.harvest.MineralHarvestExecutor
import starter.task.harvest.PrimitiveHarvestExecutor
import starter.task.harvest.deposit.DepositHarvestExecutor
import starter.task.harvest.deposit.DepositHarvestPopulationExecutor
import starter.task.harvest.power.PowerBankHarvestExecutor
import starter.task.harvest.power.PowerBankHarvestPopulationExecutor
import starter.task.harvest.power.PowerBankTransferExecutor
import starter.task.harvest.sk.SkHarvestExecutor
import starter.task.harvest.sk.SkMineralHarvestExecutor
import starter.task.lab.BoostCreepExecutor
import starter.task.lab.LabReactionExecutor
import starter.task.lab.LabTransferTaskExecutor
import starter.task.lowlevel.LowLevelBuilderExecutor
import starter.task.lowlevel.LowLevelBuilderPopulationExecutor
import starter.task.nuke.NukeSaverExecutor
import starter.task.observe.ObserveAnalyzerExecutor
import starter.task.observe.ObserveExecutor
import starter.task.plan.RoomPlanerV2Executor
import starter.task.power.PowerSpawnFillingExecutor
import starter.task.power.creep.PowerOperateFactoryExecutor
import starter.task.remote.RemoteRoomAnalyzerExecutor
import starter.task.repair.BoostedRepairRampartExecutor
import starter.task.repair.RepairExecutor
import starter.task.repair.RepairPopulationExecutor
import starter.task.repair.RepairRampartExecutor
import starter.task.storagesupport.StorageSupportV2Executor
import starter.task.unclaim.UnclaimMyRoomExecutor
import starter.task.unclaim.UnclaimMyRoomPopulationExecutor

/**
 *
 *
 * @author zakharchuk
 * @since 13.02.2020
 */
enum class TaskType(val code: String, val ttl: Int = 2500, val isUnlimited: Boolean = false, val removeOnUnclaim: Boolean = false) {

    UPGRADE_CONTROLLER_POPULATION("upgradeControllerPopulation", isUnlimited = true, removeOnUnclaim = true) {
        override fun execute(taskMemory: TaskMemory) {
            UpgradeControllerPopulationExecutor(taskMemory).execute()
        }
    },
    UPGRADE_CONTROLLER("upgradeController") {
        override fun execute(taskMemory: TaskMemory) {
            UpgradeControllerExecutor(taskMemory).execute()
        }
    },
    LINK_UPGRADE_CONTROLLER("linkUpgradeController") {
        override fun execute(taskMemory: TaskMemory) {
            LinkUpgradeControllerExecutor(taskMemory).execute()
        }
    },
    BOOSTED_LINK_UPGRADE_CONTROLLER("boostedLinkUpgradeController") {
        override fun execute(taskMemory: TaskMemory) {
            BoostedLinkUpgradeControllerExecutor(taskMemory).execute()
        }
    },
    PRIMITIVE_UPDATE_CONTROLLER("primitiveUpdateController") {
        override fun execute(taskMemory: TaskMemory) {
            PrimitiveUpgradeControllerExecutor(taskMemory).execute()
        }
    },
    CONTAINER_UPGRADE_CONTROLLER("containerUpgradeController") {
        override fun execute(taskMemory: TaskMemory) {
            ContainerUpgradeControllerExecutor(taskMemory).execute()
        }
    },

    BASE_ENERGY_SUPPORT("baseEnergySupport") {
        override fun execute(taskMemory: TaskMemory) {
            BaseEnergySupportExecutor(taskMemory).execute()
        }
    },
    PRIMITIVE_BASE_ENERGY_SUPPORT("primitiveBaseEnergySupport") {
        override fun execute(taskMemory: TaskMemory) {
            PrimitiveBaseEnergySupportExecutor(taskMemory).execute()
        }
    },

    HARVEST_POPULATION("harvestPopulation", isUnlimited = true, removeOnUnclaim = true) {
        override fun execute(taskMemory: TaskMemory) {
            HarvestPopulationExecutor(taskMemory).execute()
        }
    },
    PRIMITIVE_HARVESTER("primitiveHarvester") {
        override fun execute(taskMemory: TaskMemory) {
            PrimitiveHarvestExecutor(taskMemory).execute()
        }
    },
    HARVEST("harvest") {
        override fun execute(taskMemory: TaskMemory) {
            HarvestExecutor(taskMemory).execute()
        }
    },
    LINK_HARVEST("linkHarvest") {
        override fun execute(taskMemory: TaskMemory) {
            LinkHarvestExecutor(taskMemory).execute()
        }
    },
    MINERAL_HARVEST("mineralHarvest") {
        override fun execute(taskMemory: TaskMemory) {
            MineralHarvestExecutor(taskMemory).execute()
        }
    },
    SK_HARVEST("skHarvest") {
        override fun execute(taskMemory: TaskMemory) {
            SkHarvestExecutor(taskMemory).execute()
        }
    },
    SK_MINERAL_HARVEST("skMineralHarvest") {
        override fun execute(taskMemory: TaskMemory) {
            SkMineralHarvestExecutor(taskMemory).execute()
        }
    },
    DEPOSIT_HARVEST_POPULATION("depositHarvestPopulation", isUnlimited = true, removeOnUnclaim = true) {
        override fun execute(taskMemory: TaskMemory) {
            DepositHarvestPopulationExecutor(taskMemory).execute()
        }
    },
    DEPOSIT_HARVEST("depositHarvest") {
        override fun execute(taskMemory: TaskMemory) {
            DepositHarvestExecutor(taskMemory).execute()
        }
    },

    REPAIR_POPULATION("repairPopulation", isUnlimited = true, removeOnUnclaim = true) {
        override fun execute(taskMemory: TaskMemory) {
            RepairPopulationExecutor(taskMemory).execute()
        }
    },
    REPAIR("repair") {
        override fun execute(taskMemory: TaskMemory) {
            RepairExecutor(taskMemory).execute()
        }
    },
    REPAIR_RAMPART("repairRampart") {
        override fun execute(taskMemory: TaskMemory) {
            RepairRampartExecutor(taskMemory).execute()
        }
    },
    BOOSTED_REPAIR_RAMPART("boostedRepairRampart") {
        override fun execute(taskMemory: TaskMemory) {
            BoostedRepairRampartExecutor(taskMemory).execute()
        }
    },
    NUKE_SAVER("nukeSaver") {
        override fun execute(taskMemory: TaskMemory) {
            NukeSaverExecutor(taskMemory).execute()
        }
    },

    BUILD_POPULATION("buildPopulation", isUnlimited = true, removeOnUnclaim = true) {
        override fun execute(taskMemory: TaskMemory) {
            BuildPopulationExecutor(taskMemory).execute()
        }
    },
    BUILD("build") {
        override fun execute(taskMemory: TaskMemory) {
            BuildExecutor(taskMemory).execute()
        }
    },

    STORAGE_SUPPORT_V2("storageSupportV2") {
        override fun execute(taskMemory: TaskMemory) {
            StorageSupportV2Executor(taskMemory).execute()
        }
    },

    SQUAD_ATTACK("squadAttack") {
        override fun execute(taskMemory: TaskMemory) {
            SquadAttackExecutor(taskMemory).execute()
        }
    },
    RANGE_ATTACK("rangeAttack") {
        override fun execute(taskMemory: TaskMemory) {
            RangeAttackExecutor(taskMemory).execute()
        }
    },
    BOOSTED_RANGE_ATTACK("boostedRangeAttack") {
        override fun execute(taskMemory: TaskMemory) {
            BoostedRangeAttackExecutor(taskMemory).execute()
        }
    },
    BOOSTED_DISMANTLE_ATTACK_POPULATION("boostedDismantleAttackPopulation", isUnlimited = true, removeOnUnclaim = true) {
        override fun execute(taskMemory: TaskMemory) {
            BoostedDismantleAttackPopulationExecutor(taskMemory).execute()
        }
    },
    BOOSTED_DISMANTLE_ATTACK("boostedDismantleAttack") {
        override fun execute(taskMemory: TaskMemory) {
            BoostedDismantleAttackExecutor(taskMemory).execute()
        }
    },

    RESERVE_CONTROLLER_POPULATION("reserveControllerPopulation", isUnlimited = true, removeOnUnclaim = true) {
        override fun execute(taskMemory: TaskMemory) {
            ReserveControllerPopulationExecutor(taskMemory).execute()
        }
    },
    RESERVE_CONTROLLER("reserveController") {
        override fun execute(taskMemory: TaskMemory) {
            ReserveControllerExecutor(taskMemory).execute()
        }
    },

    TOWER_DEFEND("towerDefend", isUnlimited = true, removeOnUnclaim = true) {
        override fun execute(taskMemory: TaskMemory) {
            TowerDefendExecutor(taskMemory).execute()
        }
    },
    DEFEND_POPULATION("defendPopulation", isUnlimited = true, removeOnUnclaim = true) {
        override fun execute(taskMemory: TaskMemory) {
            DefendPopulationExecutor(taskMemory).execute()
        }
    },
    DEFEND("defend") {
        override fun execute(taskMemory: TaskMemory) {
            DefendExecutor(taskMemory).execute()
        }
    },
    SK_DEFEND("dkDefend") {
        override fun execute(taskMemory: TaskMemory) {
            SkDefendExecutor(taskMemory).execute()
        }
    },
    SK_HEAL("skHeal") {
        override fun execute(taskMemory: TaskMemory) {
            SkHealExecutor(taskMemory).execute()
        }
    },
    INVADER_CODE_DESTROYER("invaderCoreDestroyer") {
        override fun execute(taskMemory: TaskMemory) {
            InvaderCoreDestroyerExecutor(taskMemory).execute()
        }
    },

    LINK_TRANSFER("linkTransfer") {
        override fun execute(taskMemory: TaskMemory) {
            LinkTransferExecutor(taskMemory).execute()
        }
    },
    TRANSFER("transfer") {
        override fun execute(taskMemory: TaskMemory) {
            TransferExecutor(taskMemory).execute()
        }
    },

    CLAIM_ROOM_POPULATION("claimRoomPopulation", isUnlimited = true, removeOnUnclaim = true) {
        override fun execute(taskMemory: TaskMemory) {
            ClaimRoomPopulationExecutor(taskMemory).execute()
        }
    },
    CLAIM_ROOM("claimRoom", ttl = 1000) {
        override fun execute(taskMemory: TaskMemory) {
            ClaimRoomExecutor(taskMemory).execute()
        }
    },
    CLAIM_TO_CLEAR("claimToClear", ttl = 1000) {
        override fun execute(taskMemory: TaskMemory) {
            ClaimToClearRoomExecutor(taskMemory).execute()
        }
    },
    CLEAR_ROOM("clearRoom") {
        override fun execute(taskMemory: TaskMemory) {
            ClearRoomExecutor(taskMemory).execute()
        }
    },
    UN_CLAIM_ROOM("unClaimRoom") {
        override fun execute(taskMemory: TaskMemory) {
            UnClaimRoomExecutor(taskMemory).execute()
        }
    },

    CLAIM_TO_THIEF_POPULATION("claimToThiefPopulation", isUnlimited = true, removeOnUnclaim = true) {
        override fun execute(taskMemory: TaskMemory) {
            ClaimToThiefPopulationExecutor(taskMemory).execute()
        }
    },
    CLAIM_TO_THIEF("claimToThief") {
        override fun execute(taskMemory: TaskMemory) {
            ClaimToThiefExecutor(taskMemory).execute()
        }
    },
    THIEF("thief") {
        override fun execute(taskMemory: TaskMemory) {
            ThiefExecutor(taskMemory).execute()
        }
    },

    LOW_LEVEL_BUILDER_POPULATION("lowLevelBuilderPopulation", isUnlimited = true, removeOnUnclaim = true) {
        override fun execute(taskMemory: TaskMemory) {
            LowLevelBuilderPopulationExecutor(taskMemory).execute()
        }
    },
    LOW_LEVEL_BUILDER("lowLevelRoomBuilder") {
        override fun execute(taskMemory: TaskMemory) {
            LowLevelBuilderExecutor(taskMemory).execute()
        }
    },

    ROOM_PLANER_V2("roomPlanerV2", isUnlimited = true, removeOnUnclaim = true) {
        override fun execute(taskMemory: TaskMemory) {
            RoomPlanerV2Executor(taskMemory).execute()
        }
    },

    LAB_REACTION("labReaction", isUnlimited = true, removeOnUnclaim = true) {
        override fun execute(taskMemory: TaskMemory) {
            LabReactionExecutor(taskMemory).execute()
        }
    },
    LAB_TRANSFER_TASK("labTransferTask") {
        override fun execute(taskMemory: TaskMemory) {
            LabTransferTaskExecutor(taskMemory).execute()
        }
    },
    BOOST_CREEP("boostCreep") {
        override fun execute(taskMemory: TaskMemory) {
            BoostCreepExecutor(taskMemory).execute()
        }
    },

    DISMANTLE_POPULATION("dismantlePopulation", isUnlimited = true, removeOnUnclaim = true) {
        override fun execute(taskMemory: TaskMemory) {
            DismantlePopulationExecutor(taskMemory).execute()
        }
    },
    DISMANTLE_TARGET("dismantleTarget") {
        override fun execute(taskMemory: TaskMemory) {
            DismantleTargetExecutor(taskMemory).execute()
        }
    },

    OBSERVER("observe", isUnlimited = true, removeOnUnclaim = true) {
        override fun execute(taskMemory: TaskMemory) {
            ObserveExecutor(taskMemory).execute()
        }
    },
    OBSERVE_ANALYZER("observeAnalyzer") {
        override fun execute(taskMemory: TaskMemory) {
            ObserveAnalyzerExecutor(taskMemory).execute()
        }
    },

    POWER_BANK_HARVEST_POPULATION("powerBankHarvestPopulation", isUnlimited = true, removeOnUnclaim = true) {
        override fun execute(taskMemory: TaskMemory) {
            PowerBankHarvestPopulationExecutor(taskMemory).execute()
        }
    },
    POWER_BANK_HARVEST("powerBankHarvest") {
        override fun execute(taskMemory: TaskMemory) {
            PowerBankHarvestExecutor(taskMemory).execute()
        }
    },
    POWER_BANK_TRANSFER("powerBankTransfer") {
        override fun execute(taskMemory: TaskMemory) {
            PowerBankTransferExecutor(taskMemory).execute()
        }
    },

    POWER_SPAWN_FILLING("powerSpawnFilling") {
        override fun execute(taskMemory: TaskMemory) {
            PowerSpawnFillingExecutor(taskMemory).execute()
        }
    },

    COMMODITIES_PROCESSOR("commoditiesProcessor", isUnlimited = true, removeOnUnclaim = true) {
        override fun execute(taskMemory: TaskMemory) {
            CommoditiesProcessor(taskMemory).execute()
        }
    },
    PRODUCE_COMMODITIES("produceCommodities", isUnlimited = true, removeOnUnclaim = true) {
        override fun execute(taskMemory: TaskMemory) {
            ProduceCommoditiesExecutor(taskMemory).execute()
        }
    },

    POWER_OPERATE_FACTORY("powerOperateFactory") {
        override fun execute(taskMemory: TaskMemory) {
            PowerOperateFactoryExecutor(taskMemory).execute()
        }
    },

    UNCLAIM_MY_ROOM_POPULATION("unclaimMyRoomPopulation", isUnlimited = true) {
        override fun execute(taskMemory: TaskMemory) {
            UnclaimMyRoomPopulationExecutor(taskMemory).execute()
        }
    },
    UNCLAIM_MY_ROOM("unclaimMyRoom") {
        override fun execute(taskMemory: TaskMemory) {
            UnclaimMyRoomExecutor(taskMemory).execute()
        }
    },

    REMOTE_ROOM_ANALYZER("remoteRoomAnalyzer") {
        override fun execute(taskMemory: TaskMemory) {
            RemoteRoomAnalyzerExecutor(taskMemory).execute()
        }
    }

    ;

    abstract fun execute(taskMemory: TaskMemory)

    companion object {
        fun of(code: String): TaskType? = values().firstOrNull { it.code == code }
    }

}