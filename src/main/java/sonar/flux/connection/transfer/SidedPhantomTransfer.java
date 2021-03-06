package sonar.flux.connection.transfer;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import sonar.core.api.energy.EnergyType;
import sonar.core.helpers.ItemStackHelper;

public class SidedPhantomTransfer extends BaseFluxTransfer implements ISidedTransfer {

	public final TileEntity tile;
	public final EnumFacing direction;
	
	public SidedPhantomTransfer(EnergyType type, TileEntity tile, EnumFacing direction) {
		super(type);
		this.tile = tile;
		this.direction = direction;
	}

	@Override
	public EnumFacing getDirection() {
		return direction;
	}

	@Override
	public TileEntity getTile() {
		return tile;
	}

	@Override
	public ItemStack getDisplayStack() {
		return ItemStackHelper.getBlockItem(tile.getWorld(), tile.getPos());
	}

}
