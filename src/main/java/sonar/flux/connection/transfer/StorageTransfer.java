package sonar.flux.connection.transfer;

import net.minecraft.item.ItemStack;
import sonar.core.api.energy.EnergyType;
import sonar.core.api.utils.ActionType;
import sonar.core.helpers.ItemStackHelper;
import sonar.flux.api.energy.internal.IEnergyTransfer;
import sonar.flux.common.tileentity.TileStorage;

public class StorageTransfer extends BaseFluxTransfer implements IEnergyTransfer {
	
	public final TileStorage tile;
	
	public StorageTransfer(TileStorage tile) {
		super(tile.getNetwork().getDefaultEnergyType());
		this.tile = tile;
	}

	@Override
	public long addToNetwork(long maxTransferRF, ActionType actionType) {
		long remove = tile.storage.removeEnergy(maxTransferRF, actionType);
		if (!actionType.shouldSimulate()) {
			addedToNetwork(remove, getEnergyType());
		}
		return remove;
	}

	@Override
	public long removeFromNetwork(long maxTransferRF, ActionType actionType) {
		long added = tile.storage.addEnergy(maxTransferRF, actionType);
		if (!actionType.shouldSimulate()) {
			removedFromNetwork(added, getEnergyType()); // even though the storage is part of the network, we still want to know how much it "took" from the network
		}
		return added;
	}

	@Override
	public ItemStack getDisplayStack() {
		return ItemStackHelper.getBlockItem(tile.getWorld(), tile.getPos());
	}

	@Override
	public EnergyType getEnergyType() {
		return tile.getNetwork().getDefaultEnergyType();
	}
	
}
