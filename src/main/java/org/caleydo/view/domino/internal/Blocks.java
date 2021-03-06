/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package org.caleydo.view.domino.internal;

import gleem.linalg.Vec2f;

import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Set;

import org.caleydo.core.data.collection.EDimension;
import org.caleydo.core.data.selection.SelectionType;
import org.caleydo.core.id.IDCategory;
import org.caleydo.core.id.IDType;
import org.caleydo.core.util.base.ICallback;
import org.caleydo.core.util.collection.Pair;
import org.caleydo.core.util.color.Color;
import org.caleydo.core.util.function.DoubleStatistics;
import org.caleydo.core.view.opengl.layout.Column.VAlign;
import org.caleydo.core.view.opengl.layout2.GLElement;
import org.caleydo.core.view.opengl.layout2.GLElementContainer;
import org.caleydo.core.view.opengl.layout2.GLGraphics;
import org.caleydo.core.view.opengl.layout2.IGLElementContext;
import org.caleydo.core.view.opengl.layout2.geom.Rect;
import org.caleydo.core.view.opengl.layout2.layout.IGLLayout2;
import org.caleydo.core.view.opengl.layout2.layout.IGLLayoutElement;
import org.caleydo.view.domino.api.model.typed.util.BitSetSet;
import org.caleydo.view.domino.internal.MiniMapCanvas.IHasMiniMap;
import org.caleydo.view.domino.internal.dnd.DragElement;
import org.caleydo.view.domino.internal.ui.AItem;
import org.caleydo.view.domino.internal.ui.Ruler;
import org.caleydo.view.domino.internal.ui.SelectionInfo;

import com.google.common.collect.EnumMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * @author Samuel Gratzl
 *
 */
public class Blocks extends GLElementContainer implements ICallback<SelectionType>, IHasMiniMap, IGLLayout2 {

	private final ICallback<MiniMapCanvas> viewportchange = new ICallback<MiniMapCanvas>() {
		@Override
		public void on(MiniMapCanvas data) {
			updateAccordingToMiniMap();
		}
	};
	public Blocks(NodeSelections selections) {
		selections.onBlockSelectionChanges(this);
		setLayout(this);
	}

	@Override
	public void on(SelectionType data) {
		final Domino domino = findParent(Domino.class);
		if (data == SelectionType.SELECTION && domino.getTool() == EToolState.BANDS) {
			Set<Block> s = domino.getSelections().getBlockSelection(SelectionType.SELECTION);
			if (s.isEmpty()) {
				for (Block b : getBlocks())
					b.setFadeOut(false);
			} else if (s.size() == 1) {
				Block sel = s.iterator().next();
				for (Block b : getBlocks())
					b.setFadeOut(b != sel && !canHaveBands(sel, b));
			} else {
				for (Block b : getBlocks())
					b.setFadeOut(!s.contains(b));
			}
		}
	}

	@Override
	protected void init(IGLElementContext context) {
		super.init(context);
		if (getParent() instanceof MiniMapCanvas) {
			MiniMapCanvas c = (MiniMapCanvas) getParent();
			c.addOnViewPortChange(viewportchange);
		}
	}

	@Override
	protected void takeDown() {
		if (getParent() instanceof MiniMapCanvas) {
			MiniMapCanvas c = (MiniMapCanvas) getParent();
			c.removeOnViewPortChange(viewportchange);
		}
		super.takeDown();
	}

	void updateAccordingToMiniMap() {
		// MiniMapCanvas c = (MiniMapCanvas) getParent();
		// Rectangle2D r = c.getClippingRect().asRectangle2D();
		// EVisibility ifVisible = findParent(Domino.class).getTool() == EToolState.BANDS ? EVisibility.PICKABLE
		// : EVisibility.VISIBLE;
		// Vec2f loc = getLocation();
		// for (Block elem : getBlocks()) {
		// Rect b = elem.getRectBounds().clone();
		// b.xy(b.xy().plus(loc));
		// boolean v = r.intersects(b.asRectangle2D());
		// elem.setVisibility(v ? ifVisible : EVisibility.HIDDEN);
		// }
	}

	@Override
	public boolean doLayout(List<? extends IGLLayoutElement> children, float w, float h, IGLLayoutElement parent,
			int deltaTimeMs) {
		if (getParent() instanceof MiniMapCanvas)
			updateAccordingToMiniMap();
		return false;
	}


	/**
	 * @param sel
	 * @param b
	 * @return
	 */
	private boolean canHaveBands(Block a, Block b) {
		return !Sets.intersection(a.getIDTypes(), b.getIDTypes()).isEmpty();
	}

	public void addBlock(Block b) {

		this.add(b);
		final Domino domino = findParent(Domino.class);
		if (domino.getTool() == EToolState.BANDS) {
			Set<Block> s = domino.getSelections().getBlockSelection(SelectionType.SELECTION);
			b.setFadeOut((s.size() >= 2 && !s.contains(b)) || (s.size() == 1 && !canHaveBands(s.iterator().next(), b)));
		}
	}

	public Iterable<Block> getBlocks() {
		return Iterables.filter(this, Block.class);
	}

	public Iterable<Ruler> rulers() {
		return Iterables.filter(this, Ruler.class);
	}

	public Iterable<SelectionInfo> selectionInfos() {
		return Iterables.filter(this, SelectionInfo.class);
	}

	public Iterable<AItem> separators() {
		return Iterables.filter(this, AItem.class);
	}

	/**
	 * @param tool
	 */
	public void setTool(EToolState tool) {
		for (Block b : getBlocks()) {
			b.setTool(tool);
		}
	}

	public void zoom(Vec2f shift, Vec2f mousePos) {
		for (Block block : getBlocks()) {
			block.zoom(shift, null);
			shiftZoomLocation(block, mousePos, shift);
		}
		for (Ruler ruler : rulers()) {
			ruler.zoom(shift);
			shiftZoomLocation(ruler, mousePos, shift);
		}
		getParent().getParent().relayout();
	}

	private void shiftZoomLocation(GLElement elem, Vec2f mousePos, Vec2f shift) {
		Rect b = elem.getRectBounds();
		if (b.contains(mousePos)) // inner
			return;

		float x = b.x();
		float y = b.y();

		if (mousePos.x() < b.x())
			x += shift.x();
		else if (mousePos.x() > b.x2())
			x -= shift.x();

		if (mousePos.y() < b.y())
			y += shift.y();
		else if (mousePos.y() > b.y2())
			y -= shift.y();

		elem.setLocation(x, y);
	}

	@Override
	public Vec2f getMinSize() {
		Rectangle2D r = null;
		for (GLElement b : this) {
			if (r == null) {
				r = b.getRectangleBounds();
			} else
				Rectangle2D.union(r, b.getRectangleBounds(), r);
		}
		if (r == null)
			return new Vec2f(100, 100);
		return new Vec2f((float) r.getMaxX(), (float) r.getMaxY());
	}

	@Override
	public Rect getBoundingBox() {
		Rect r = null;
		for(Block b : getBlocks()) {
			if (r == null)
				r = shiftedBoundingBox(b);
			else {
				r = Rect.union(r, shiftedBoundingBox(b));
			}
		}
		for (Ruler b : rulers()) {
			if (r == null)
				r = b.getRectBounds();
			else
				r = Rect.union(r, b.getRectBounds());
		}
		return r;
	}

	/**
	 * @param b
	 * @return
	 */
	private Rect shiftedBoundingBox(Block b) {
		Rect bb = b.getBoundingBox();
		Vec2f loc = b.getLocation();
		if (bb != null) {
			bb.xy(bb.xy().plus(loc));
		}
		return bb;
	}

	/**
	 * @param relativePosition
	 * @return
	 */
	public Pair<Rect, Vec2f> findSnapTo(Vec2f pos) {
		// grid lines ??
		// linear to a block?
		float x = Float.NaN;
		float w = Float.NaN;
		float x_hint = Float.NaN;
		float y = Float.NaN;
		float h = Float.NaN;
		float y_hint = Float.NaN;

		for (GLElement elem : this) {
			Rect bounds = elem.getRectBounds();
			if (Float.isNaN(x) && inRange(pos.x(), bounds.x())) { // near enough
				x = bounds.x(); // set it as target pos
				w = bounds.width();
				x_hint = bounds.y() - pos.y();
			}
			if (Float.isNaN(x) && inRange(pos.x(), bounds.x2())) { // near enough
				x = bounds.x2(); // set it as target pos
				w = bounds.width();
				x_hint = bounds.y() - pos.y();
			}
			if (inRange(pos.y(), bounds.y())) { // near enough
				y = bounds.y();
				h = bounds.height();
				y_hint = bounds.x() - pos.x();
			}
			if (inRange(pos.y(), bounds.y2())) { // near enough
				y = bounds.y2();
				h = bounds.height();
				y_hint = bounds.x() - pos.x();
			}
		}
		if (Float.isNaN(x) && Float.isNaN(y))
			return null;
		return Pair.make(new Rect(x, y, w, h), new Vec2f(x_hint, y_hint));
	}

	/**
	 * @param x
	 * @param x2
	 * @return
	 */
	private static boolean inRange(float a, float b) {
		return Math.abs(a - b) < 20;
	}

	/**
	 * @param currentlyDraggedVis
	 */
	public void snapDraggedVis(DragElement current) {

		Pair<Rect, Vec2f> stickTo = findSnapTo(current.getRelativePosition(getAbsoluteLocation()));
		if (stickTo == null)
			current.stickTo(null, null, null);
		else {
			Vec2f pos = toAbsolute(stickTo.getFirst().xy());
			current.stickTo(pos, stickTo.getFirst().size(), stickTo.getSecond());
		}
	}

	@Override
	public void renderMiniMap(GLGraphics g) {
		for(Block block : getBlocks()) {
			block.renderMiniMap(g);
		}
		for (Ruler ruler : rulers()) {
			final Rect bounds = ruler.getRectBounds();
			g.color(Color.LIGHT_GRAY).fillRect(bounds);
			float hi = Math.min(10, bounds.height());
			g.drawText(ruler.getIDCategory().getCategoryName(), bounds.x(), bounds.y() + (bounds.height() - hi) * 0.5f,
					bounds.width(), hi, VAlign.CENTER);
		}
		for (AItem item : separators()) {
			g.color(Color.LIGHT_GRAY).fillRect(item.getRectBounds());
		}
	}

	/**
	 * @param category
	 * @param shift
	 */
	public void moveRuler(IDCategory category, Vec2f shift) {
		Ruler r = getRuler(category);
		if (r != null)
			r.shiftLocation(shift);
		getParent().getParent().relayout();
	}

	/**
	 * @param category
	 * @param scale
	 */
	public void zoomRuler(IDCategory category, float scale) {
		for (Ruler ruler : rulers()) {
			if (ruler.getIDCategory().equals(category)) {
				ruler.zoom(scale);
				break;
			}
		}
		for (Block block : getBlocks()) {
			block.zoom(category, scale);
		}
	}

	/**
	 * @param idCategory
	 * @return
	 */
	public boolean hasRuler(IDCategory category) {
		for (Ruler ruler : rulers()) {
			if (ruler.getIDCategory().equals(category)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param separator
	 * @return
	 */
	public boolean hasItem(AItem separator) {
		return this.asList().contains(separator);
	}

	public void addRuler(Ruler ruler) {
		add(ruler);
		final IDCategory idCategory = ruler.getIDCategory();
		DoubleStatistics stats = getScaleFactorsStats(idCategory);
		if (stats.getN() > 0) {
			ruler.zoom((float) stats.getMean());
		} else {
			Vec2f size = findParent(MiniMapCanvas.class).getSize();
			if (ruler.getDim().isHorizontal()) {
				final Vec2f v = Node.initialScaleFactors(size, ruler.getMaxElements(), 1);
				ruler.zoom(v.x());
			} else {
				final Vec2f v = Node.initialScaleFactors(size, 1, ruler.getMaxElements());
				ruler.zoom(v.y());
			}
		}

		EnumMultiset<EDimension> count = EnumMultiset.create(EDimension.class);
		for (Block block : getBlocks()) {
			block.directions(idCategory, count);
		}
		if (count.count(EDimension.DIMENSION) > count.count(EDimension.RECORD))
			ruler.transpose();

	}

	/**
	 * @param ruler
	 */
	public void addItem(AItem item) {
		add(item);
	}

	public void removeItem(AItem item) {
		remove(item);
	}

	private DoubleStatistics getScaleFactorsStats(final IDCategory idCategory) {
		// guess the initial ruler scaling as the mean of all the available ones
		DoubleStatistics.Builder b = DoubleStatistics.builder();
		for (Block block : getBlocks()) {
			block.scaleFactors(idCategory, b);
		}
		DoubleStatistics stats = b.build();
		return stats;
	}

	/**
	 * @param ruler
	 */
	public void removeRuler(Ruler ruler) {
		remove(ruler);

	}

	/**
	 * @param category
	 * @return
	 */
	public int getVisibleItemCount(IDCategory category) {
		IDType primary = category.getPrimaryMappingType();
		Set<Integer> ids = new BitSetSet();
		for (Block block : getBlocks())
			block.addVisibleItems(category, ids, primary);
		return ids.size();
	}

	/**
	 * @param block
	 * @param dim
	 * @return
	 */
	public List<Block> explode(Block block, EDimension dim) {
		List<Block> blocks = block.explode(dim);
		remove(block);
		findParent(Domino.class).getSelections().cleanup(block);
		for (Block b : blocks) {
			addBlock(b);
		}
		return blocks;
	}

	public Block combine(List<Block> blocks, EDimension dim) {
		final NodeSelections selections = findParent(Domino.class).getSelections();

		Block first = blocks.get(0);
		Block new_ = first.combine(blocks.subList(1, blocks.size()), dim);
		for (Block b : blocks) {
			remove(b);
			selections.cleanup(b);
		}
		addBlock(new_);
		return new_;
	}

	/**
	 * @param idType
	 * @param guess
	 * @param x
	 * @return
	 */
	public float getRulerScale(IDType idType) {
		Ruler r = getRuler(idType.getIDCategory());
		if (r != null)
			return r.getScaleFactor();
		DoubleStatistics stats = getScaleFactorsStats(idType.getIDCategory());
		if (stats.getN() > 0)
			return (float) stats.getMean();
		return Float.NaN;
	}

	/**
	 * @param category
	 * @return
	 */
	public Ruler getRuler(IDCategory category) {
		for (Ruler ruler : rulers()) {
			if (ruler.getIDCategory().equals(category))
				return ruler;
		}
		return null;
	}

}
