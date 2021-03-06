package sonar.flux.client;

import static net.minecraft.client.renderer.GlStateManager.color;
import static net.minecraft.client.renderer.GlStateManager.scale;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.FMLCommonHandler;
import sonar.core.api.energy.EnergyType;
import sonar.core.api.energy.StoredEnergyStack;
import sonar.core.client.gui.GuiSonar;
import sonar.core.helpers.FontHelper;
import sonar.core.utils.CustomColour;
import sonar.flux.FluxNetworks;
import sonar.flux.api.AccessType;
import sonar.flux.api.ClientTransfer;
import sonar.flux.api.network.IFluxCommon;
import sonar.flux.api.tiles.IFlux;
import sonar.flux.api.tiles.IFlux.ConnectionType;
import sonar.flux.common.containers.ContainerFlux;
import sonar.flux.common.tileentity.TileFlux;
import sonar.flux.network.PacketHelper;
import sonar.flux.network.PacketType;

public abstract class AbstractGuiTab<T extends TileFlux> extends GuiSonar {

	public List<GuiTab> tabs;

	public static final ResourceLocation buttons = new ResourceLocation("fluxnetworks:textures/gui/buttons/buttons.png");
	public static final ResourceLocation scroller_flux_gui = new ResourceLocation("fluxnetworks:textures/gui/scroller_flux_gui.png");
	public static final ResourceLocation blank_flux_gui = new ResourceLocation("fluxnetworks:textures/gui/blank_flux_gui.png");

	public static final CustomColour[] colours = new CustomColour[] { new CustomColour(41, 94, 138), new CustomColour(52, 52, 119), new CustomColour(88, 42, 114), new CustomColour(136, 45, 96), new CustomColour(170, 57, 57), new CustomColour(170, 111, 57), new CustomColour(198, 185, 0), new CustomColour(96, 151, 50) };

	public static final int midBlue = FontHelper.getIntFromColor(41, 94, 138);
	public static final int lightBlue = FontHelper.getIntFromColor(90, 180, 255);
	public static final int darkBlue = FontHelper.getIntFromColor(37, 61, 81);
	public static final int grey = FontHelper.getIntFromColor(85, 85, 85);
	public static final int black = FontHelper.getIntFromColor(0, 0, 0);

	public int errorDisplayTicks;
	public int errorDisplayTime = 300;
	public boolean disabled = false;

	public final T flux;
	public IFluxCommon common;

	public AbstractGuiTab(T tile, List<GuiTab> tabs) {
		super(new ContainerFlux(Minecraft.getMinecraft().player, tile));
		this.flux = tile;
		this.tabs = tabs;
		this.common = FluxNetworks.getClientCache().getNetwork(getNetworkID());
	}

	public abstract GuiTab getCurrentTab();

	@Override
	public void initGui() {
		super.initGui();
		int i = 0;
		for (GuiTab state : tabs) {
			buttonList.add(new NavigationButtons(this, state, -i, guiLeft + 2 + 18 * i, guiTop - 15));
			i++;
		}
	}

	public int getNetworkID() {
		return flux.getNetworkID();
	}

	@Override
	public void actionPerformed(GuiButton button) throws IOException {
		super.actionPerformed(button);
		if (button instanceof NavigationButtons) {
			switchTab(((NavigationButtons) button).tab);
		}
	}

	public void switchTab(GuiTab tab) {
		if (tab != getCurrentTab()) {
			Object screen = tab.getGuiScreen(flux, tabs);
			FMLCommonHandler.instance().showGuiScreen(screen);
			PacketHelper.sendPacketToServer(PacketType.GUI_STATE_CHANGE, flux, PacketHelper.createStateChangePacket(tab));
		}
	}

	public Object origin;

	public void setOrigin(Object origin) {
		this.origin = origin;
	}

	@Override
	protected void keyTyped(char c, int i) throws IOException {
		if (isCloseKey(i)) {
			boolean isTyping = this.fieldList.stream().anyMatch(f -> f.isFocused());
			if (!isTyping) {
				if (origin != null) {
					FMLCommonHandler.instance().showGuiScreen(origin);
					return;
				}
				if (getCurrentTab() != GuiTab.INDEX) {
					FMLCommonHandler.instance().showGuiScreen(GuiTab.INDEX.getGuiScreen(flux, tabs));
					return;
				}
			}
		}
		super.keyTyped(c, i);
	}

	public FontRenderer getFontRenderer() {
		return this.fontRenderer;
	}

	//// RENDER METHODs \\\\

	public void renderNetwork(String networkName, AccessType access, int rgb, boolean isSelected, int x, int y) {
		color(1.0F, 1.0F, 1.0F, 1.0F);
		drawRect(x, y, x + 154, y + 12, rgb);
		Minecraft.getMinecraft().getTextureManager().bindTexture(scroller_flux_gui);
		drawTexturedModalRect(x, y, 0, /* isSelected ? 178 : 166 */166, 154, 12);
		FontHelper.text(networkName, x + 3, y + 2, isSelected ? Color.WHITE.getRGB() : Color.DARK_GRAY.getRGB());
	}

	public void renderNetworkInFull(String networkName, AccessType access, int rgb, boolean isSelected, int x, int y) {
		color(1.0F, 1.0F, 1.0F, 1.0F);
		drawRect(x, y, x + 154, y + 24, rgb);
		drawRect(x + 1, y + 1, x + 154 - 1, y + 24 - 1, Color.BLACK.getRGB());
		FontHelper.text(TextFormatting.BOLD + networkName, x + 3, y + 2, isSelected ? Color.WHITE.getRGB() : Color.DARK_GRAY.getRGB());
		FontHelper.text(FontHelper.translate("network.accessSetting") + ": " + TextFormatting.AQUA + FontHelper.translate(access.getName()), x + 3, y + 13, 0);
	}

	public void renderEnergyBar(int x, int y, long stored, long max, int startCol, int endCol) {
		drawRect(x, y, xSize - x, y + 16, Color.DARK_GRAY.getRGB());
		drawRect(x + 1, y + 1, xSize - x - 1, y + 15, Color.BLACK.getRGB());
		if (max != 0 && stored != 0) {
			long k = stored * (xSize - x * 2) / max;
			drawGradientRect(x + 1, y + 1, (int) (x - 1 + k), y + 15, startCol, endCol);
		}
		FontHelper.textCentre(FontHelper.formatStorage(stored) + '/' + FontHelper.formatStorage(max), xSize, y + 4, -1);
	}

	public void renderNavigationPrompt(String error, String prompt) {
		FontHelper.textCentre(FontHelper.translate(error), xSize, 10, Color.GRAY.getRGB());
		scale(0.75, 0.75, 0.75);
		FontHelper.textCentre("Click" + TextFormatting.AQUA + ' ' + prompt + ' ' + TextFormatting.RESET + "Above", (int) (xSize * 1.0 / 0.75), (int) (20 * 1.0 / 0.75), Color.GRAY.getRGB());
		scale(1.0 / 0.75, 1.0 / 0.75, 1.0 / 0.75);
	}

	public void renderFlux(IFlux flux, boolean isSelected, int x, int y) {
		int rgb = this.getCurrentTab() == GuiTab.INDEX ? common.getNetworkColour().getRGB() : flux.getConnectionType().gui_colour;
		color(1.0F, 1.0F, 1.0F, 1.0F);
		drawRect(x, y, x + 154, y + 18, rgb);
		Minecraft.getMinecraft().getTextureManager().bindTexture(scroller_flux_gui);
		if (flux.isChunkLoaded()) {
			drawTexturedModalRect(x, y, 0, 166, 154, 10);
			drawTexturedModalRect(x, y + 10, 0, 166 + 4, 154, 8);
		} else {
			drawTexturedModalRect(x, y, 0, 226, 154, 18);
		}
		drawNormalItemStack(flux.getConnectionType().getDisplayStack(), x + 2, y + 1);
		if (this.getCurrentTab() == GuiTab.INDEX) {
			List<String> textLines = new ArrayList<>();
			addTransferStrings(textLines, flux.getConnectionType(), common.getDefaultEnergyType(), flux.getTransferHandler().getAdded(), flux.getTransferHandler().getRemoved());
			FontHelper.text(textLines.get(0), 24, 5, !flux.isChunkLoaded() ? FontHelper.getIntFromColor(180, 40, 40) : isSelected ? Color.WHITE.getRGB() : Color.DARK_GRAY.getRGB());
		} else {
			FontHelper.text(flux.getCustomName(), 24, 5, !flux.isChunkLoaded() ? FontHelper.getIntFromColor(180, 40, 40) : isSelected ? Color.WHITE.getRGB() : Color.DARK_GRAY.getRGB());
		}
	}

	public List<String> getTextLines(IFlux flux) {
		List<String> textLines = new ArrayList<>();
		textLines.add(TextFormatting.BOLD + flux.getCustomName());
		if (flux.isChunkLoaded()) {
			if (flux.getCoords().getBlockPos().equals(this.flux.getPos())) {
				// textLines.add(TextFormatting.GREEN + "THIS CONNECTION!");
			}
			addTransferStrings(textLines, flux.getConnectionType(), common.getDefaultEnergyType(), flux.getTransferHandler().getAdded(), flux.getTransferHandler().getRemoved());
			textLines.add(GUI.TRANSFER_LIMIT + ": " + TextFormatting.GREEN + (flux.getTransferLimit() == Long.MAX_VALUE ? "NO LIMIT" : flux.getTransferLimit()));
			textLines.add(GUI.PRIORITY + ": " + TextFormatting.GREEN + flux.getCurrentPriority());
		} else {
			textLines.add(TextFormatting.DARK_RED + "CHUNK UNLOADED");
		}
		textLines.add(TextFormatting.ITALIC + flux.getCoords().toString());
		return textLines;

	}

	public void renderFluxTransfer(ClientTransfer transfer, int x, int y, int rgb) {
		color(1.0F, 1.0F, 1.0F, 1.0F);
		drawRect(x, y, x + 154, y + 18, rgb);
		Minecraft.getMinecraft().getTextureManager().bindTexture(scroller_flux_gui);
		drawTexturedModalRect(x, y, 0, 166, 154, 10);
		drawTexturedModalRect(x, y + 10, 0, 166 + 4, 154, 8);
		String direction = (transfer.direction == null ? "PHANTOM" : transfer.direction.toString().toUpperCase());
		String transferS = FontHelper.formatOutput(transfer.energyType, transfer.added);
		drawNormalItemStack(transfer.stack, x + 2, y + 1);
		List<String> textLines = new ArrayList<>();
		addTransferStrings(textLines,transfer.handler.flux.getConnectionType(), transfer.getEnergyType(), transfer.added, transfer.removed);
		GlStateManager.scale(0.75, 0.75, 0.75);
		FontHelper.text(transfer.stack.getDisplayName(), 34, 3, rgb);
		FontHelper.text(textLines.get(0), 34, 14, rgb);
	}

	public List<String> getTextLines(ClientTransfer transfer) {
		List<String> textLines = new ArrayList<>();
		textLines.add(TextFormatting.BOLD + transfer.stack.getDisplayName());
		ConnectionType type = transfer.handler.flux.getConnectionType();
		addTransferStrings(textLines, type, transfer.getEnergyType(), transfer.added, transfer.removed);
		// textLines.add("Limit Usage: " + Math.floor(((double) (transfer.added + transfer.removed) / transfer.handler.add_limit) * 100) + " %");
		// textLines.add("Direction: " + (transfer.direction == null ? "PHANTOM" : transfer.direction.toString().toUpperCase()));
		textLines.add("Type: " + transfer.energyType.getName());

		return textLines;
	}

	public void addTransferStrings(List<String> string, ConnectionType type, EnergyType energyType, long added, long removed) {

		if(type == ConnectionType.STORAGE){
			long change = Math.abs(removed) - added;
			if(change ==0){
				string.add("Change:" + TextFormatting.GOLD + " " + FontHelper.formatOutput(energyType, change));
			}else if(change < 0){
				string.add("Change:" + TextFormatting.RED + " - " + FontHelper.formatOutput(energyType, Math.abs(change)));				
			}else if(change > 0){
				string.add("Change:" + TextFormatting.GREEN + " + " + FontHelper.formatOutput(energyType, change));				
			}
			return;
		}
		
		if (type.canAdd()) {
			String addedString = FontHelper.formatOutput(energyType, added);
			if (added == 0) {
				string.add("Input:" + TextFormatting.GOLD + " " + addedString);
			} else {
				string.add("Input:" + TextFormatting.GREEN + " + " + addedString);
			}
		}
		if (type.canRemove()) {
			String removedString = FontHelper.formatOutput(energyType, removed);
			if (removed == 0) {
				string.add("Output:" + TextFormatting.GOLD + " " + removedString);
			} else {
				string.add("Output:" + TextFormatting.RED + " - " + removedString);
			}
		}
	}

}
