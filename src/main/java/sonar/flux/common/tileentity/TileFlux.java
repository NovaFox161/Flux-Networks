package sonar.flux.common.tileentity;

import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;

import cofh.redstoneflux.api.IEnergyHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.common.Optional;
import sonar.core.SonarCore;
import sonar.core.api.IFlexibleGui;
import sonar.core.common.tileentity.TileEntitySonar;
import sonar.core.helpers.NBTHelper.SyncType;
import sonar.core.listener.ListenableList;
import sonar.core.listener.PlayerListener;
import sonar.core.network.sync.SyncTagType;
import sonar.core.network.sync.SyncTagType.INT;
import sonar.core.network.sync.SyncTagType.LONG;
import sonar.core.network.sync.SyncTagType.STRING;
import sonar.core.network.sync.SyncTagTypeList;
import sonar.core.network.sync.SyncUUID;
import sonar.core.network.utils.IByteBufTile;
import sonar.flux.FluxConfig;
import sonar.flux.FluxNetworks;
import sonar.flux.api.AdditionType;
import sonar.flux.api.ClientFlux;
import sonar.flux.api.FluxError;
import sonar.flux.api.FluxListener;
import sonar.flux.api.RemovalType;
import sonar.flux.api.configurator.FluxConfigurationType;
import sonar.flux.api.configurator.IFluxConfigurable;
import sonar.flux.api.network.FluxCache;
import sonar.flux.api.network.IFluxNetwork;
import sonar.flux.api.network.PlayerAccess;
import sonar.flux.api.tiles.IFluxListenable;
import sonar.flux.client.GuiTab;
import sonar.flux.common.block.FluxConnection;
import sonar.flux.common.containers.ContainerFlux;
import sonar.flux.connection.EmptyFluxNetwork;
import sonar.flux.connection.FluxHelper;
import sonar.flux.network.FluxNetworkCache;
import sonar.flux.network.ListenerHelper;
import sonar.flux.network.PacketFluxNetworkList;

@Optional.InterfaceList({ @Optional.Interface(iface = "cofh.redstoneflux.api.IEnergyHandler", modid = "redstoneflux") })
public abstract class TileFlux extends TileEntitySonar implements IFluxListenable, IEnergyHandler, IByteBufTile, IFluxConfigurable, IFlexibleGui {

	private final ConnectionType type;

	public long toReceive; // is reset after each tick, the network calculates the max accept based upon priorities and sorting etc.
	public long toSend;
	public long[] currentTransfer = new long[6];

	//// USER CONFIGURED \\\\
	public SyncTagType.INT priority = new SyncTagType.INT(0);
	public SyncTagType.LONG limit = (LONG) new SyncTagType.LONG(1).setDefault(FluxConfig.defaultLimit);
	public SyncTagType.BOOLEAN disableLimit = new SyncTagType.BOOLEAN(2);
	public SyncTagType.INT networkID = (INT) new SyncTagType.INT(4).setDefault(-1);
	public SyncTagType.STRING customName = (STRING) new SyncTagType.STRING(6).setDefault("Flux Connection");

	//// PLAYER WHO PLACED THIS CONNECTION \\\\
	public SyncUUID playerUUID = new SyncUUID(5);

	//// NETWORK COLOUR \\\\
	public SyncTagType.INT colour = new SyncTagType.INT(7);

	//// PLAYERS VIEWING THE FLUX GUI \\\\
	public ListenableList<PlayerListener> listeners = new ListenableList(this, FluxListener.values().length);

	//// CACHED SIDE CONNECTIONS \\\\
	public SyncTagTypeList<Boolean> connections = new SyncTagTypeList<>(NBT.TAG_END, 8);

	{
		syncList.addParts(priority, limit, disableLimit, networkID, playerUUID, customName, colour, connections); // , colour, connections);
		connections.setObjects(Lists.newArrayList(false, false, false, false, false, false));
	}

	//// SERVER ONLY \\\\
	public IFluxNetwork network = EmptyFluxNetwork.INSTANCE;
	private int checkTicks;

	//// CLIENT ONLY \\\\
	public FluxError error = FluxError.NONE;
	private ClientFlux client_flux;

	public TileFlux(ConnectionType type) {
		super();
		this.type = type;
	}

	public void update() {
		super.update();
		/* if (isServer()) { totalTransferMax = limit.getObject(); for (int i = 0; i < 6; i++) { currentTransfer[i] = limit.getObject(); } if (checkTicks >= 20) { updateTransfers(false); checkTicks = 0; } else { checkTicks++; } listeners.forEach(tally -> FluxHelper.sendPacket(getNetwork(), tally)); } */
	}

	public void updateTransfers(EnumFacing ...faces) {
		getTransferHandler().updateTransfers(faces);
	}

	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase player, ItemStack itemstack) {
		if (player != null && player instanceof EntityPlayer) {
			setPlayerUUID(((EntityPlayer) player).getGameProfile().getId());
			updateTransfers(EnumFacing.VALUES);
		}
	}
	
	//// NETWORK CONNECTION \\\\

	@Override
	public void connect(IFluxNetwork network) {
		IFluxNetwork oldNetwork = this.network;
		this.network = network;
		this.networkID.setObject(network.getNetworkID());
		colour.setObject(network.getNetworkColour().getRGB());
		setConnectionState(true);
		markBlockForUpdate();
		ListenerHelper.onNetworkChanged(this, oldNetwork, network);
	}

	@Override
	public void disconnect(IFluxNetwork network) {
		if (network.getNetworkID() == this.networkID.getObject()) {
			IFluxNetwork oldNetwork = this.network;
			this.network = EmptyFluxNetwork.INSTANCE;
			this.networkID.setObject(-1);
			colour.setObject(EmptyFluxNetwork.colour.getRGB());
			setConnectionState(false);
			markBlockForUpdate();
			ListenerHelper.onNetworkChanged(this, oldNetwork, network);
		}
	}

	/** changes the block state of the Flux Connection to reflect if it is connected or not */
	public void setConnectionState(boolean bool) {
		World world = getWorld();
		IBlockState state = getWorld().getBlockState(getPos());
		if (state.getBlock() instanceof FluxConnection) { // sanity check
			world.setBlockState(getPos(), state.withProperty(FluxConnection.CONNECTED, bool), 2);
		}
	}

	/** makes sure tile entity is kept with connection state is changed */
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
		return oldState.getBlock() != newState.getBlock();
	}

	//// TILE ENTITY CONNECTIONS \\\\

	public EnumFacing[] getValidFaces() {
		return EnumFacing.values();
	}

	//// PLAYER ACCESS \\\\

	/** set the player who placed this Flux Connection */
	public void setPlayerUUID(UUID name) {
		this.playerUUID.setObject(name);
	}

	public PlayerAccess canAccess(EntityPlayer player) {
		if (FluxHelper.isPlayerAdmin(player)) {
			return PlayerAccess.CREATIVE;
		}
		if (playerUUID.getUUID() != null && playerUUID.getUUID().equals(FluxHelper.getOwnerUUID(player))) {
			return PlayerAccess.OWNER;
		}
		return getNetwork().isFakeNetwork() ? PlayerAccess.BLOCKED : getNetwork().getPlayerAccess(player);
	}

	public UUID getConnectionOwner() {
		return playerUUID.getUUID();
	}

	//// TILE ENTITY EVENTS \\\\

	public void onFirstTick() {
		super.onFirstTick();
		if (isServer()) {
			FluxHelper.addConnection(this, AdditionType.ADD);
			updateTransfers();
			SonarCore.sendPacketAround(this, 128, 0);
		}
		if (isClient())
			requestSyncPacket();
	}

	public void invalidate() {
		super.invalidate();
		if (isServer()) {
			FluxHelper.removeConnection(this, RemovalType.REMOVE);
		}
	}

	public void onChunkUnload() {
		super.onChunkUnload();
		if (isServer()) {
			FluxHelper.removeConnection(this, RemovalType.CHUNK_UNLOAD);
		}
	}

	//// IFLUX IMPLEMENTATION \\\\

	@Override
	public int getNetworkID() {
		return networkID.getObject();
	}

	@Override
	public IFluxNetwork getNetwork() {
		return network;
	}

	@Override
	public ConnectionType getConnectionType() {
		return type;
	}

	@Override
	public long getTransferLimit() {
		return disableLimit.getObject() ? Long.MAX_VALUE : limit.getObject();
	}

	@Override
	public int getCurrentPriority() {
		return priority.getObject();
	}

	@Override
	public World getDimension() {
		return world;
	}

	@Override
	public String getCustomName() {
		return customName.getObject();
	}

	//// REDSTONE FLUX \\\\

	@Override
	@Optional.Method(modid = "redstoneflux")
	public boolean canConnectEnergy(EnumFacing from) {
		return true;
	}

	@Override
	@Optional.Method(modid = "redstoneflux")
	public int getEnergyStored(EnumFacing from) {
		return 0;
	}

	@Override
	@Optional.Method(modid = "redstoneflux")
	public int getMaxEnergyStored(EnumFacing from) {
		return Integer.MAX_VALUE;
	}

	public ClientFlux getClientFlux(){
		if(client_flux==null){
			client_flux = new ClientFlux(this);
		}
		return client_flux;
	}
	
	@Override
	public void readData(NBTTagCompound nbt, SyncType type) {
		if (type.isType(SyncType.SPECIAL) || (this.network.isFakeNetwork() && type.isType(SyncType.DEFAULT_SYNC))) {
			if (nbt.hasKey("client_flux")) {
				client_flux = new ClientFlux(nbt.getCompoundTag("client_flux"));
			}
		}
		super.readData(nbt, type);
	}

	@Override
	public NBTTagCompound writeData(NBTTagCompound nbt, SyncType type) {
		if (type.isType(SyncType.SPECIAL) || (this.network.isFakeNetwork() && type.isType(SyncType.DEFAULT_SYNC))) {
			if (listeners.hasListeners(FluxListener.SYNC_INDEX.ordinal())) {
				client_flux = new ClientFlux(this);
				nbt.setTag("client_flux", client_flux.writeData(new NBTTagCompound(), type));
			}
		}
		return super.writeData(nbt, type);
	}

	//// PACKETS \\\\\

	@Override
	public void writePacket(ByteBuf buf, int id) {
		switch (id) {
		case -1:
			disableLimit.writeToBuf(buf);
			break;
		case 1:
			priority.writeToBuf(buf);
			break;
		case 2:
			limit.writeToBuf(buf);
			break;
		case 3:
			customName.writeToBuf(buf);
			break;
		}
	}

	@Override
	public void readPacket(ByteBuf buf, int id) {
		switch (id) {
		case -1:
			disableLimit.readFromBuf(buf);
			break;
		case 1:
			priority.readFromBuf(buf);
			getNetwork().markTypeDirty(FluxCache.flux);
			break;
		case 2:
			limit.readFromBuf(buf);
			break;
		case 3:
			customName.readFromBuf(buf);
			break;
		}
	}

	@Override
	public ListenableList<PlayerListener> getListenerList() {
		return listeners;
	}

	@Override
	public boolean isValid() {
		return !isInvalid();
	}

	@Override
	public void setMaxSend(long send) {
		toSend = send;
	}

	@Override
	public void setMaxReceive(long receive) {
		toReceive = receive;
	}

	@Override
	public boolean isChunkLoaded() {
		return isValid();
	}

	//// Flux Configurator \\\\

	@Override
	public NBTTagCompound addConfigs(NBTTagCompound config, EntityPlayer player) {
		if (!this.getNetwork().isFakeNetwork() && network.getNetworkID() != -1) {
			config.setInteger(FluxConfigurationType.NETWORK.getNBTName(), getNetwork().getNetworkID());
		}
		config.setInteger(FluxConfigurationType.PRIORITY.getNBTName(), priority.getObject());
		config.setLong(FluxConfigurationType.TRANSFER.getNBTName(), limit.getObject());
		config.setBoolean(FluxConfigurationType.DISABLE_LIMIT.getNBTName(), disableLimit.getObject());
		return config;
	}

	@Override
	public void readConfigs(NBTTagCompound config, EntityPlayer player) {
		if (config.hasKey(FluxConfigurationType.NETWORK.getNBTName())) {
			int storedID = config.getInteger(FluxConfigurationType.NETWORK.getNBTName());
			if (storedID != -1) {
				FluxHelper.removeConnection(this, null);
				this.networkID.setObject(storedID);
				FluxHelper.addConnection(this, null);
			}
		}
		if (config.hasKey(FluxConfigurationType.PRIORITY.getNBTName())) {
			this.priority.setObject(config.getInteger(FluxConfigurationType.PRIORITY.getNBTName()));
		}
		if (config.hasKey(FluxConfigurationType.TRANSFER.getNBTName())) {
			this.limit.setObject(config.getLong(FluxConfigurationType.TRANSFER.getNBTName()));
		}
		if (config.hasKey(FluxConfigurationType.DISABLE_LIMIT.getNBTName())) {
			this.disableLimit.setObject(config.getBoolean(FluxConfigurationType.DISABLE_LIMIT.getNBTName()));
		}
	}

	@Override
	public void onGuiOpened(Object obj, int id, World world, EntityPlayer player, NBTTagCompound tag) {
		ListenerHelper.onPlayerOpenTileGui(this, player);
		ListenerHelper.onPlayerOpenTab(this, player, GuiTab.INDEX);

		List<IFluxNetwork> toSend = FluxNetworkCache.instance().getAllowedNetworks(player, false);
		FluxNetworks.network.sendTo(new PacketFluxNetworkList(toSend, false), (EntityPlayerMP) player);
		this.sendSyncPacket(player, SyncType.SAVE);
	}

	@Override
	public Object getServerElement(Object obj, int id, World world, EntityPlayer player, NBTTagCompound tag) {
		return new ContainerFlux(player, this);
	}

	@Override
	public Object getClientElement(Object obj, int id, World world, EntityPlayer player, NBTTagCompound tag) {
		return GuiTab.INDEX.getGuiScreen(this, Lists.newArrayList(GuiTab.INDEX, GuiTab.NETWORK_SELECT, GuiTab.CONNECTIONS, GuiTab.NETWORK_STATS, GuiTab.PLAYERS, GuiTab.NETWORK_EDIT, GuiTab.NETWORK_CREATE));
	}
}
