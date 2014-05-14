/*******************************************************************************
 * Caleydo - visualization for molecular biology - http://caleydo.org
 *
 * Copyright(C) 2005, 2012 Graz University of Technology, Marc Streit, Alexander
 * Lex, Christian Partl, Johannes Kepler University Linz </p>
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 *******************************************************************************/
package org.caleydo.view.domino.internal.ui;

import gleem.linalg.Vec2f;

import java.util.List;

import org.caleydo.core.data.collection.EDimension;
import org.caleydo.core.view.opengl.canvas.IGLMouseListener.IMouseEvent;
import org.caleydo.core.view.opengl.layout2.GLElement;
import org.caleydo.core.view.opengl.layout2.basic.ScrollingDecorator.IHasMinSize;
import org.caleydo.core.view.opengl.layout2.layout.IGLLayout;
import org.caleydo.core.view.opengl.layout2.layout.IGLLayoutElement;
import org.caleydo.core.view.opengl.layout2.manage.GLElementFactorySwitcher.IActiveChangedCallback;
import org.caleydo.view.domino.internal.ui.prototype.ui.BlockInfo;
import org.caleydo.view.domino.spi.config.ElementConfig;

/**
 * layout specific information
 *
 * @author Samuel Gratzl
 *
 */
public class DominoLayoutInfo implements IActiveChangedCallback, IGLLayout {
	/**
	 * parent link
	 */
	private final GLElement parent;

	private final ElementConfig config;

	/**
	 * zoom factor in x direction
	 */
	private float zoomFactorX = 1.0f;
	/**
	 * zoom factor in y direction
	 */
	private float zoomFactorY = 1.0f;

	private boolean hovered = false;

	private boolean selected = false;

	private boolean dragged = false;

	private BlockInfo block;

	/**
	 * @param crosswordElement
	 */
	public DominoLayoutInfo(GLElement parent, ElementConfig config) {
		this.parent = parent;
		this.config = config;
	}

	public void transpose() {
		// swap zoom factors
		float t = zoomFactorY;
		zoomFactorY = zoomFactorX;
		zoomFactorX = t;
	}
	/**
	 * @return the config, see {@link #config}
	 */
	public ElementConfig getConfig() {
		return config;
	}

	/**
	 * @return the block, see {@link #block}
	 */
	public BlockInfo getBlock() {
		return block;
	}

	/**
	 * @param block
	 *            setter, see {@link block}
	 */
	public void setBlock(BlockInfo block) {
		this.block = block;
	}

	/**
	 * init from my parent settings, e.g. zoomFactor
	 *
	 * @param baseLayout
	 */
	public void initFromParent(DominoLayoutInfo baseLayout) {
		this.zoomFactorX = baseLayout.zoomFactorX;
		this.zoomFactorY = baseLayout.zoomFactorY;
	}

	/**
	 * @param zoomFactor
	 *            setter, see {@link zoomFactor}
	 */
	public boolean setZoomFactor(float zoomFactorX, float zoomFactorY) {
		if (this.zoomFactorX == zoomFactorX && this.zoomFactorY == zoomFactorY)
			return false;
		this.zoomFactorX = zoomFactorX;
		this.zoomFactorY = zoomFactorY;
		relayoutGrandParent();
		return true;
	}

	private void relayoutGrandParent() {
		if (parent.getParent() != null)
			parent.getParent().relayout();
	}

	@Override
	public void onActiveChanged(int active) {
		// reset to a common zoom factor
		float s = Math.min(zoomFactorX, zoomFactorY);
		if (!setZoomFactor(s, s))
			relayoutGrandParent(); // the min size may have changed
	}

	/**
	 * @return the zoomFactor, see {@link #zoomFactor}
	 */
	public float getZoomFactorX() {
		return zoomFactorX;
	}

	/**
	 * @return the zoomFactorY, see {@link #zoomFactorY}
	 */
	public float getZoomFactorY() {
		return zoomFactorY;
	}

	public boolean zoom(float factor) {
		return zoom(factor, factor);
	}

	/**
	 * @param factor
	 * @param factorY
	 */
	public boolean zoom(float factorX, float factorY) {
		if (!config.canScale())
			return false;
		if (isInvalid(factorX) || isInvalid(factorY))
			return false;
		this.zoomFactorX = zoomFactorX * factorX;
		this.zoomFactorY = zoomFactorY * factorY;
		relayoutGrandParent();
		return true;
	}

	/**
	 * @param factorY
	 * @return
	 */
	private static boolean isInvalid(float factor) {
		return Double.isNaN(factor) || Double.isInfinite(factor) || factor <= 0;
	}

	/**
	 * zoom implementation of the given picking event
	 *
	 * @param event
	 */
	public void zoom(IMouseEvent event) {
		if (!config.canScale())
			return;

		if (event.getWheelRotation() == 0)
			return;
		int dim = toDirection(event, EDimension.DIMENSION);
		int rec = toDirection(event, EDimension.RECORD);

		float factor = (float) Math.pow(1.2, event.getWheelRotation());
		float factorX = dim == 0 ? 1 : factor;
		float factorY = rec == 0 ? 1 : factor;
		boolean isCenteredZoom = false; // !event.isShiftDown();
		zoom(factorX, factorY);
		if (isCenteredZoom) {
			Vec2f pos = parent.toRelative(event.getPoint());
			// compute the new new mouse pos considers zoom
			Vec2f new_ = new Vec2f(pos.x() * factorX, pos.y() * factorY);
			pos.sub(new_);
			// shift the location according to the delta
			shift(pos.x(), pos.y());
		}
	}

	/**
	 * convert a {@link IMouseEvent} to a direction information
	 *
	 * @param event
	 * @param dim
	 * @return -1 smaller, +1 larger, and 0 nothing
	 */
	private static int toDirection(IMouseEvent event, EDimension dim) {
		final int w = event.getWheelRotation();
		if (w == 0)
			return 0;
		int factor = w > 0 ? 1 : -1;
		return event.isCtrlDown() || dim.select(event.isAltDown(), event.isShiftDown()) ? factor : 0;
	}

	/**
	 * shift the location the item
	 *
	 * @param x
	 * @param y
	 */
	public void shift(float x, float y) {
		if (x == 0 && y == 0)
			return;
		Vec2f loc = parent.getLocation();
		parent.setLocation(loc.x() + x, loc.y() + y);
	}

	/**
	 * enlarge the view by moving and rescaling
	 *
	 * @param x
	 *            the dx
	 * @param xDir
	 *            the direction -1 to the left +1 to the right 0 nothing
	 * @param y
	 *            the dy
	 * @param yDir
	 */
	public void enlarge(float x, int xDir, float y, int yDir) {
		Vec2f size = parent.getSize();
		Vec2f loc = parent.getLocation();
		float sx = size.x() + xDir * x;
		float sy = size.y() + yDir * y;
		// convert to scale factor
		sx -= 2; // borders and buttons
		sy -= 2;
		Vec2f minSize = getMinSize();
		setZoomFactor(sx / minSize.x(), sy / minSize.y());
		parent.setLocation(loc.x() + (xDir < 0 ? x : 0), loc.y() + (yDir < 0 ? y : 0));
	}

	/**
	 * @param hovered
	 *            setter, see {@link hovered}
	 */
	public void setHovered(boolean hovered) {
		if (this.hovered == hovered)
			return;
		this.hovered = hovered;
		parent.relayout();
	}

	/**
	 * @param b
	 */
	public void setSelected(boolean selected) {
		if (this.selected == selected)
			return;
		this.selected = selected;
	}

	/**
	 * @return the dragged, see {@link #dragged}
	 */
	public boolean isDragged() {
		return dragged;
	}

	/**
	 * @param dragged
	 *            setter, see {@link dragged}
	 */
	public void setDragged(boolean dragged) {
		if (this.dragged == dragged)
			return;
		this.dragged = dragged;
		parent.relayout();
	}

	/**
	 * @return the selected, see {@link #selected}
	 */
	public boolean isSelected() {
		return selected;
	}

	/**
	 * @return the hovered, see {@link #hovered}
	 */
	public boolean isHovered() {
		return hovered;
	}

	@Override
	public void doLayout(List<? extends IGLLayoutElement> children, float w, float h) {
		IGLLayoutElement content = children.get(0);
		IGLLayoutElement border = children.get(1);
		content.setBounds(0, 0, w, h);
		border.setBounds(0, 0, w, h);
	}

	private void scale(Vec2f size) {
		size.setX(size.x() * zoomFactorX);
		size.setY(size.y() * zoomFactorY);
		size.setX(size.x() + 2);
		size.setY(size.y() + 2); // for buttons and border
	}

	private Vec2f getMinSize() {
		IHasMinSize minSize = parent.getLayoutDataAs(IHasMinSize.class, null);
		if (minSize != null)
			return minSize.getMinSize();
		return new Vec2f(1, 1);
	}

	/**
	 * @return
	 */
	public Vec2f getSize() {
		Vec2f minSize = getMinSize();
		scale(minSize);
		if (minSize.x() < 4)
			minSize.setX(4);
		if (minSize.y() < 4)
			minSize.setY(4);
		return minSize;
	}

	/**
	 * @param old
	 */
	public void fromOld(DominoLayoutInfo old) {
		this.zoomFactorX = old.zoomFactorX;
		this.zoomFactorY = old.zoomFactorY;
	}
}