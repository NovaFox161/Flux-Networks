package sonar.flux.client.tabs;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import sonar.core.client.gui.IGridGui;
import sonar.core.client.gui.SelectionGrid;
import sonar.core.client.gui.widgets.SonarScroller;
import sonar.core.utils.CustomColour;
import sonar.flux.client.AbstractGuiTab;
import sonar.flux.client.GuiTab;
import sonar.flux.common.tileentity.TileFlux;

public abstract class GuiTabSelectionGrid<T> extends AbstractGuiTab implements IGridGui<T> {

	public Map<SelectionGrid, SonarScroller> grids = Maps.newHashMap();

	public GuiTabSelectionGrid(TileFlux tile, List tabs) {
		super(tile, tabs);
	}

	public abstract List getGridList(int gridID);

	@Override
	public void initGui() {
		super.initGui();
		Map<SelectionGrid, SonarScroller> newgrids = Maps.newHashMap();
		addGrids(newgrids);
		grids = newgrids;		
		
	}

	public abstract void addGrids(Map<SelectionGrid, SonarScroller> grids);

	@Override
	public void mouseClicked(int x, int y, int button) throws IOException {
		super.mouseClicked(x, y, button);
		if (button == 0 || button == 1) {
			grids.forEach((grid, scroll) -> grid.mouseClicked(this, x, y, button));
		}
	}

	@Override
	public void drawGuiContainerForegroundLayer(int x, int y) {
		super.drawGuiContainerForegroundLayer(x, y);
		for (Entry<SelectionGrid, SonarScroller> entry : grids.entrySet()) {
			renderScroller(entry.getValue());
			entry.getKey().renderGrid(this, x, y);
		}
	}

	public void renderScroller(SonarScroller scroller) {
		drawRect(scroller.left, scroller.top, scroller.left + scroller.width, scroller.top + scroller.length, grey);
		drawRect(scroller.left + 1, scroller.top + 1, scroller.left + scroller.width - 1, scroller.top + scroller.length - 1, black);
		GlStateManager.color(1, 1, 1, 1);
		bindTexture(this.getBackground());
		drawTexturedModalRect(scroller.left, scroller.top + (int) ((float) (scroller.length - 17) * scroller.getCurrentScroll()), 176, 0, 10, 15);
		GlStateManager.color(1, 1, 1, 1);
		
	}

	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		grids.forEach((grid, scroll) -> scroll.handleMouse(grid));
	}

	public void drawScreen(int x, int y, float var) {
		super.drawScreen(x, y, var);		
		for(Entry<SelectionGrid, SonarScroller> entry : grids.entrySet()){
			entry.getKey().setList(Lists.newArrayList(getGridList(entry.getKey().gridID)));
			entry.getValue().drawScreen(x - guiLeft, y - guiTop, entry.getKey().isScrollable());
		}
	}

	@Override
	public float getCurrentScroll(SelectionGrid gridID) {
		return grids.get(gridID).getCurrentScroll();
	}

	@Override
	public ResourceLocation getBackground() {
		return scroller_flux_gui;
	}
}