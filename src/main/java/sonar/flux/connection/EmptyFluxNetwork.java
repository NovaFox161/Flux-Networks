package sonar.flux.connection;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import sonar.core.api.energy.EnergyType;
import sonar.core.api.utils.ActionType;
import sonar.core.helpers.NBTHelper.SyncType;
import sonar.core.utils.CustomColour;
import sonar.flux.api.AccessType;
import sonar.flux.api.AdditionType;
import sonar.flux.api.ClientFlux;
import sonar.flux.api.RemovalType;
import sonar.flux.api.network.FluxCache;
import sonar.flux.api.network.FluxPlayersList;
import sonar.flux.api.network.IFluxNetwork;
import sonar.flux.api.network.PlayerAccess;
import sonar.flux.api.tiles.IFluxController;
import sonar.flux.api.tiles.IFluxListenable;
import sonar.flux.connection.transfer.stats.NetworkStatistics;

public class EmptyFluxNetwork implements IFluxNetwork {

	public final static IFluxNetwork INSTANCE = new EmptyFluxNetwork();
	public final NetworkStatistics stats = new NetworkStatistics(this);
    public final static CustomColour colour = new CustomColour(41, 94, 138);

	@Override
	public void onStartServerTick() {
	}
	
	@Override
	public void onEndServerTick() {
	}

	@Override
	public AccessType getAccessType() {
		return AccessType.PRIVATE;
	}

	@Override
	public int getNetworkID() {
		return -1;
	}

	@Override
	public CustomColour getNetworkColour() {
		return colour;
	}

	@Override
	public String getNetworkName() {
		return "Please select a network";
	}

	@Override
	public UUID getOwnerUUID() {
		return null;
	}

	@Override
	public String getCachedPlayerName() {
		return "";
	}

	@Override
	public boolean hasController() {
		return false;
	}

	@Override
	public IFluxController getController() {
		return null;
	}

	@Override
	public void setNetworkName(String name) {
	}

	@Override
	public void setAccessType(AccessType type) {
	}

	@Override
	public void setCustomColour(CustomColour colour) {
	}

	@Override
    public void markDirty() {
	}

	@Override
	public void removePlayerAccess(UUID playerUUID, PlayerAccess access) {
	}

	@Override
	public void addPlayerAccess(UUID playerUUID, PlayerAccess access) {
	}

	@Override
	public long receiveEnergy(long maxReceive, EnergyType energyType, ActionType simulate) {
		return 0;
	}

	@Override
	public long extractEnergy(long maxExtract, EnergyType energyType, ActionType simulate) {
		return 0;
	}

	@Override
	public long getEnergyAvailable() {
		return 0;
	}

	@Override
	public long getMaxEnergyStored() {
		return 0;
	}

	@Override
    public void addConnection(IFluxListenable flux, AdditionType type) {
	}

	@Override
    public void removeConnection(IFluxListenable flux, RemovalType type) {
	}

	@Override
	public NetworkStatistics getStatistics() {
		return stats;
	}

	@Override
	public void setClientConnections(List<ClientFlux> flux) {

	}

	@Override
	public void readData(NBTTagCompound nbt, SyncType type) {
	}

	@Override
	public NBTTagCompound writeData(NBTTagCompound nbt, SyncType type) {
		return nbt;
	}

	@Override
	public List<ClientFlux> getClientFluxConnection() {
        return new ArrayList<>();
	}

	@Override
	public void buildFluxConnections() {
	}

	@Override
	public boolean isFakeNetwork() {
		return true;
	}

	@Override
	public FluxPlayersList getPlayers() {
		return new FluxPlayersList();
	}

	@Override
	public PlayerAccess getPlayerAccess(EntityPlayer player) {
		return PlayerAccess.BLOCKED;
	}

	@Override
	public IFluxNetwork updateNetworkFrom(IFluxNetwork network) {
		return this;
	}

	@Override
    public void onRemoved() {
    }

	@Override
    public void markTypeDirty(FluxCache... caches) {
    }

    @Override
    public <T extends IFluxListenable> List<T> getConnections(FluxCache<T> type) {
        return new ArrayList<>();
    }

    @Override
    public void setHasConnections(boolean bool) {
    }

	@Override
	public void addFluxListener(IFluxListenable listener) {}

	@Override
	public void removeFluxListener(IFluxListenable listener) {}

	@Override
	public boolean disabledConversion() {
		return false;	}

	@Override
	public EnergyType getDefaultEnergyType() {
		return EnergyType.FE;
	}

	@Override
	public void setDisableConversion(boolean disable) {}

	@Override
	public void setDefaultEnergyType(EnergyType type) {}

	@Override
	public boolean canConvert(EnergyType from, EnergyType to) {
		return false;		
	}

	@Override
	public boolean canTransfer(EnergyType type) {
		return false;
	}
}
