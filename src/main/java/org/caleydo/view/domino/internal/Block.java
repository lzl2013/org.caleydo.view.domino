/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package org.caleydo.view.domino.internal;

import gleem.linalg.Vec2f;

import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.caleydo.core.data.collection.EDimension;
import org.caleydo.core.data.selection.SelectionType;
import org.caleydo.core.event.EventListenerManager.ListenTo;
import org.caleydo.core.id.IDType;
import org.caleydo.core.util.base.Labels;
import org.caleydo.core.util.collection.Pair;
import org.caleydo.core.util.color.Color;
import org.caleydo.core.view.opengl.canvas.IGLMouseListener.IMouseEvent;
import org.caleydo.core.view.opengl.layout2.GLElement;
import org.caleydo.core.view.opengl.layout2.GLElementContainer;
import org.caleydo.core.view.opengl.layout2.GLGraphics;
import org.caleydo.core.view.opengl.layout2.IGLElementContext;
import org.caleydo.core.view.opengl.layout2.dnd.IDnDItem;
import org.caleydo.core.view.opengl.layout2.dnd.IDragGLSource;
import org.caleydo.core.view.opengl.layout2.dnd.IDragInfo;
import org.caleydo.core.view.opengl.layout2.geom.Rect;
import org.caleydo.core.view.opengl.layout2.layout.AGLLayoutElement;
import org.caleydo.core.view.opengl.layout2.layout.IGLLayout2;
import org.caleydo.core.view.opengl.layout2.layout.IGLLayoutElement;
import org.caleydo.core.view.opengl.picking.IPickingListener;
import org.caleydo.core.view.opengl.picking.Pick;
import org.caleydo.core.view.opengl.util.spline.TesselatedPolygons;
import org.caleydo.view.domino.api.model.EDirection;
import org.caleydo.view.domino.api.model.typed.TypedGroupList;
import org.caleydo.view.domino.internal.band.ABand;
import org.caleydo.view.domino.internal.band.BandFactory;
import org.caleydo.view.domino.internal.band.EBandMode;
import org.caleydo.view.domino.internal.band.IBandHost.SourceTarget;
import org.caleydo.view.domino.internal.data.VisualizationTypeOracle;
import org.caleydo.view.domino.internal.dnd.ADragInfo;
import org.caleydo.view.domino.internal.dnd.BlockDragInfo;
import org.caleydo.view.domino.internal.event.HideNodeEvent;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;

/**
 *
 * @author Samuel Gratzl
 *
 */
public class Block extends GLElementContainer implements IGLLayout2, IPickingListener {
	/**
	 *
	 */
	static final float DETACHED_OFFSET = 50.f;

	private final List<LinearBlock> linearBlocks = new ArrayList<>();

	private boolean fadeOut = false;
	private int fadeOutTime = 0;

	private boolean armed;

	private final OffsetShifts offsets = new OffsetShifts();

	private final BlockBands bands = new BlockBands();

	private final IDragGLSource source = new IDragGLSource() {
		@Override
		public IDragInfo startSWTDrag(IDragEvent event) {
			return findDomino().startSWTDrag(event,Block.this);
		}

		@Override
		public void onDropped(IDnDItem info) {
			if (info.getInfo() instanceof BlockDragInfo) {
				for (Block block : ((BlockDragInfo) info.getInfo()).getBlocks())
					block.showAgain();
			}
		}

		@Override
		public GLElement createUI(IDragInfo info) {
			if (info instanceof ADragInfo)
				return ((ADragInfo) info).createUI(findDomino());
			return null;
		}
	};

	public Block(Node node) {
		setLayout(this);
		this.add(bands);
		node.setLocation(0, 0);
		addFirstNode(node);

		onPick(this);
	}


	@Override
	public void pick(Pick pick) {
		final NodeSelections domino = findDomino().getSelections();
		IMouseEvent event = (IMouseEvent) pick;
		boolean ctrl = event.isCtrlDown();
		switch (pick.getPickingMode()) {
		case MOUSE_OVER:
			domino.select(SelectionType.MOUSE_OVER, this, false);
			context.getMouseLayer().addDragSource(source);
			repaint();
			break;
		case MOUSE_OUT:
			domino.clear(SelectionType.MOUSE_OVER, (Block) null);
			context.getMouseLayer().removeDragSource(source);
			repaint();
			break;
		case CLICKED:
			armed = true;
			break;
		case MOUSE_RELEASED:
			if (armed) {
				if (domino.isSelected(SelectionType.SELECTION, this))
					domino.clear(SelectionType.SELECTION, ctrl ? this : null);
				else
					domino.select(SelectionType.SELECTION, this, ctrl);
				repaint();
				armed = false;
			}
			break;
		default:
			break;
		}
	}

	Domino findDomino() {
		return findParent(Domino.class);
	}

	@Override
	protected void takeDown() {
		context.getMouseLayer().removeDragSource(source);
		super.takeDown();
	}

	/**
	 * @param fadeOut
	 *            setter, see {@link fadeOut}
	 */
	public void setFadeOut(boolean fadeOut) {
		if (this.fadeOut == fadeOut)
			return;
		this.fadeOut = fadeOut;
		if (fadeOut)
			this.fadeOutTime = 300;
		repaint();
	}

	@Override
	public void layout(int deltaTimeMs) {
		if (fadeOutTime > 0) {
			fadeOutTime -= deltaTimeMs;
			repaint();
		}
		super.layout(deltaTimeMs);
	}

	@Override
	protected void renderImpl(GLGraphics g, float w, float h) {
		Collection<Vec2f> outline = getOutline();
		g.color(Color.WHITE).fillPolygon(TesselatedPolygons.polygon2(outline));

		super.renderImpl(g, w, h);

		renderDetachedBands(g);

		Domino domino = findDomino();

		if (domino.isShowDebugInfos())
			g.color(Color.BLUE).drawRect(0, 0, w, h);

		if (domino.isShowBlockLabels()) {
			for (LinearBlock b : linearBlocks)
				b.renderLabels(g);
		}

		NodeSelections selections = domino.getSelections();
		final boolean selected = selections.isSelected(SelectionType.SELECTION, this);
		final boolean mouseOver = selections.isSelected(SelectionType.MOUSE_OVER, this);
		if (selected || mouseOver) {
			g.lineWidth(3).color((selected ? SelectionType.SELECTION : SelectionType.MOUSE_OVER).getColor());
			g.incZ().drawPath(getOutline(), true).decZ();
			g.lineWidth(1);
		}


		if (fadeOut) {
			fadeOutElements(g, w, h);
		}
	}

	/**
	 * @param g
	 */
	private void renderDetachedBands(GLGraphics g) {
		// TODO Auto-generated method stub

	}

	/**
	 * @return
	 */
	private Collection<Vec2f> getOutline() {
		Collection<Vec2f> r = new ArrayList<>(3);
		// if (this.size() == 1) {
		// Rect b = get(0).getRectBounds();
		// r.add(b.xy());
		// r.add(b.x2y());
		// r.add(b.x2y2());
		// r.add(b.xy2());
		// return r;
		// }
		// if (linearBlocks.size() == 1) {
		// LinearBlock b = linearBlocks.get(0);
		// Rect r1 = b.getNode(true).getRectBounds();
		// Rect r2 = b.getNode(false).getRectBounds();
		// r.add(r1.xy());
		// if (b.getDim().isHorizontal())
		// r.add(r1.x2y());
		// else
		// r.add(r2.x2y());
		// r.add(r2.x2y2());
		// r.add(r2.xy2());
		// return r;
		// }

		Node start = nodes().iterator().next();
		// search for a start point
		while (start.getNeighbor(EDirection.NORTH) != null)
			start = start.getNeighbor(EDirection.NORTH);
		while (start.getNeighbor(EDirection.WEST) != null)
			start = start.getNeighbor(EDirection.WEST);

		EDirection act = EDirection.EAST;
		Node actN = start;
		follow(actN, act, start, r);
		return r;
	}

	/**
	 * follow along the nodes neighbors to get the outline
	 *
	 * @param node
	 * @param dir
	 * @param start
	 * @param r
	 */
	private static void follow(Node node, EDirection dir, Node start, Collection<Vec2f> r) {
		Node next = node.getNeighbor(dir);
		if (node == start && dir == EDirection.EAST && !r.isEmpty()) // starting point
			return;
		if (next == null) { // no neighbor change direction
			r.add(corner(node.getRectBounds(), dir));
			follow(node, dir.rot90(), start, r);
		} else if (next.getNeighbor(dir.opposite().rot90()) != null) {
			r.add(corner(node.getRectBounds(), dir));
			follow(next.getNeighbor(dir.opposite().rot90()), dir.opposite().rot90(), start, r);
		} else {
			Vec2f my = node.getLocation();
			Vec2f ne = next.getLocation();
			EDimension shiftDir = dir.asDim().opposite();
			float shift = shiftDir.select(my) - shiftDir.select(ne);
			if (shift != 0) {
				r.add(corner(node.getRectBounds(), dir));
				r.add(corner(next.getRectBounds(), dir.opposite().rot90()));
			}
			follow(next, dir, start, r);
		}
	}

	/**
	 * @param rectBounds
	 * @param dir
	 * @return
	 */
	private static Vec2f corner(Rect r, EDirection dir) {
		switch (dir) {
		case EAST:
			return r.x2y();
		case NORTH:
			return r.xy();
		case SOUTH:
			return r.x2y2();
		case WEST:
			return r.xy2();
		}
		throw new IllegalStateException();
	}

	private Shape getOutlineShape() {
		if (this.nodeCount() == 1)
			return get(0).getRectangleBounds();
		if (linearBlocks.size() == 1) {
			LinearBlock b = linearBlocks.get(0);
			Rect r1 = b.getNode(true).getRectBounds();
			Rect r2 = b.getNode(false).getRectBounds();
			Rectangle2D.Float r = new Rectangle2D.Float();
			Rectangle2D.union(r1.asRectangle2D(), r2.asRectangle2D(), r);
			return r;
		}
		Collection<Vec2f> outline = getOutline();
		Polygon r = new Polygon();
		for (Vec2f p : outline)
			r.addPoint((int) p.x(), (int) p.y());
		return r;
	}

	private void fadeOutElements(GLGraphics g, float w, float h) {
		final float alpha = 1 - (Math.max(0, fadeOutTime) / 300.f);
		g.color(1, 1, 1, Math.min(alpha, 0.8f));

		g.incZ(3).fillPolygon(TesselatedPolygons.polygon2(getOutline())).incZ(-3);
	}



	/**
	 * @param node
	 * @param n
	 */
	public void replace(Node node, Node with) {
		assert nodeCount() == 1;

		this.remove(node);
		if (node != with)
			findDomino().cleanup(node);
		linearBlocks.clear();
		addFirstNode(with);
		updateBands();
	}

	public Collection<APlaceholder> addPlaceholdersFor(Node node) {
		List<APlaceholder> r = new ArrayList<>();
		for (LinearBlock block : linearBlocks) {
			block.addPlaceholdersFor(node, r);
		}
		return r;
	}

	public void addNode(Node neighbor, EDirection dir, Node node) {
		this.add(node);
		LinearBlock block = getBlock(neighbor, dir.asDim());
		if (block == null)
			return;
		block.add(neighbor, dir, node);
		EDimension other = dir.asDim().opposite();
		node.copyScaleFactors(neighbor, other);
		if (node.has(other.opposite()))
			linearBlocks.add(new LinearBlock(other, node));

		if (neighbor.getDetachedOffset() > 0 || node.getDetachedOffset() > 0) {
			setOffset(node, neighbor, Math.max(neighbor.getDetachedOffset(), node.getDetachedOffset()));
		}

		realign();
		updateBlock();
		// shiftBlock(dir, node);
	}

	private void shiftBlock(EDirection dir, Node node) {
		Vec2f loc = getLocation();
		if (dir == EDirection.WEST)
			setLocation(loc.x() - node.getDetachedRectBounds().width(), loc.y());
		else if (dir == EDirection.NORTH)
			setLocation(loc.x(), loc.y() - node.getDetachedRectBounds().height());
	}

	private void shiftRemoveBlock(Node node, EDimension dim) {
		Vec2f loc = getLocation();
		if (dim.isHorizontal())
			setLocation(loc.x() + node.getDetachedRectBounds().width(), loc.y());
		else
			setLocation(loc.x(), loc.y() + node.getDetachedRectBounds().height());
	}

	public void realign() {
		realign(nodes().iterator().next());
	}
	public void realign(Node startPoint) {
		realign(startPoint, null);
	}
	/**
	 * @param neighbor
	 */
	private void realign(Node startPoint, EDimension commingFrom) {
		LinearBlock dim = commingFrom == EDimension.DIMENSION ? null : getBlock(startPoint, EDimension.DIMENSION);
		LinearBlock rec = commingFrom == EDimension.RECORD ? null : getBlock(startPoint, EDimension.RECORD);
		if (rec != null) {
			rec.alignAlong(startPoint, offsets);
			for (Node node : rec) {
				if (node != startPoint)
					realign(node, EDimension.RECORD);
			}
		}
		if (dim != null) {
			dim.alignAlong(startPoint, offsets);
			for (Node node : dim) {
				if (node != startPoint)
					realign(node, EDimension.DIMENSION);
			}
		}
	}

	private void addFirstNode(Node node) {
		this.add(node);
		for (EDimension dim : EDimension.values()) {
			if (!node.has(dim.opposite()))
				continue;
			linearBlocks.add(new LinearBlock(dim, node));
		}
		if (context != null)
			updateSize();
	}

	@Override
	protected void init(IGLElementContext context) {
		super.init(context);
		updateSize();
	}

	public void updatedNode(Node node, float wasDetached, float isDetached) {
		if (wasDetached != isDetached)
			updateOffsets(node);
		realign();
		updateBlock();
	}

	/**
	 * @param node
	 */
	private void updateOffsets(Node node) {
		float d = node.getDetachedOffset();
		for (EDirection dir : EDirection.values()) {
			Node n = node.getNeighbor(dir);
			if (n == null)
				continue;
			float dn = n.getDetachedOffset();

			if (d > 0 || dn > 0) {
				setOffset(n, node, Math.max(d, dn));
			} else {
				offsets.remove(node,n);
			}
		}
	}

	/**
	 * @param node
	 */
	private void autoStratify(Node node) {
		if (this.nodeCount() != 1) // just in single case
			return;
		if (!VisualizationTypeOracle.stratifyByDefault(node.getVisualizationType()))
			return;
		for (EDimension dim : EDimension.values()) {
			if (!node.has(dim))
				continue;
			LinearBlock b = getBlock(node, dim.opposite());
			if (b == null)
				continue;
			if (b.isStratisfied())
				continue;
			if (node.getUnderlyingData(dim).getGroups().size() <= 1)
				continue;
			sortBy(node, dim);
		}
	}

	/**
	 *
	 */
	private void updateSize() {
		Rectangle2D r = null;
		for (LinearBlock elem : linearBlocks) {
			if (r == null) {
				r = elem.getBounds();
			} else
				Rectangle2D.union(r, elem.getBounds(), r);
		}
		if (r == null)
			return;
		setSize((float) r.getWidth(), (float) r.getHeight());
		relayout();
	}

	/**
	 * @param event
	 */
	public void zoom(Vec2f shift, Node just) {

		float shiftX = shift.x();
		float shiftY = shift.y();

		if (just != null) {
			just.shiftBy(shiftX, shiftY);
			for (EDimension d : just.dimensions()) {
				d = d.opposite();
				LinearBlock b = getBlock(just, d);
				if (b == null) // error
					continue;
				for (Node node : b)
					if (node != just)
						node.copyScaleFactors(just, d.opposite());
				for (Node node : b)
					updateShifts(node);
			}
			realign();
		} else {
			for (Node node : nodes())
				node.shiftBy(shiftX, shiftY);
			for (Node node : nodes())
				updateShifts(node);
			realign();
		}
		updateBlock();
	}

	/**
	 * @param node
	 */
	private void updateShifts(Node node) {
		boolean d = node.getDetachedOffset() > 0;
		for (EDirection dir : EDirection.primaries()) {
			Node n = node.getNeighbor(dir);
			if (n == null)
				continue;
			boolean nd = n.getDetachedOffset() > 0;
			if (!d && !nd)
				continue;
			Vec2f delta = n.getSize().minus(node.getSize());
			offsets.setShift(node, n, dir.asDim().opposite().select(delta) * 0.5f);
		}
	}

	/**
	 *
	 */
	private void shiftToZero() {
		Vec2f offset = new Vec2f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
		for (Node n : nodes()) {
			Vec2f l = n.getLocation();
			if (l.x() < offset.x()) // && !n.isDetached(EDimension.RECORD));
				offset.setX(l.x());
			if (l.y() < offset.y()) // FIXME && !n.isDetached(EDimension.DIMENSION))
				offset.setY(l.y());
		}
		Node first = nodes().iterator().next();
		Vec2f loc = first.getLocation();

		if (offset.x() == 0 && offset.y() == 0)
			return;

		for (Node n : nodes()) {
			n.shiftLocation(-offset.x(), -offset.y());
		}

		Vec2f loc_new = first.getLocation();
		Vec2f shift = loc_new.minus(loc);
		shiftLocation(-shift.x(), -shift.y());
	}

	public void shiftLocation(float x, float y) {
		Vec2f l = getLocation();
		setLocation(l.x() + x, l.y() + y);
	}

	/**
	 * @return
	 */
	Iterable<Node> nodes() {
		return Iterables.filter(this, Node.class);
	}

	public int nodeCount() {
		return Iterables.size(nodes());
	}

	public boolean removeNode(Node node) {
		if (this.nodeCount() == 1) {
			this.remove(node);
			return true;
		}

		for (EDimension dim : EDimension.values()) {
			if (!node.has(dim.opposite()))
				continue;
			LinearBlock block = getBlock(node, dim);
			if (block != null) {
				if (block.size() == 1)
					linearBlocks.remove(block);
				else {
					// merge offsets
					Node left = node.getNeighbor(EDirection.getPrimary(dim));
					Node right = node.getNeighbor(EDirection.getPrimary(dim).opposite());
					offsets.remove(left, node);
					offsets.remove(right, node);

					int index = block.remove(node);

					if (left != null && right != null
							&& (left.getDetachedOffset() > 0 || right.getDetachedOffset() > 0)) // update
																											// offsets
						setOffset(left, right, Math.max(left.getDetachedOffset(), right.getDetachedOffset()));

					if (index == 0) {
						realign();
						// shiftRemoveBlock(node, dim);
					} else {
						realign();
					}
				}
			}
		}
		this.remove(node);

		updateBlock();
		return this.isEmpty();
	}

	private LinearBlock getBlock(Node node, EDimension dim) {
		for (LinearBlock block : linearBlocks) {
			if (block.getDim() == dim && block.contains(node))
				return block;
		}
		// throw new IllegalStateException();
		return null;
	}

	@Override
	public boolean doLayout(List<? extends IGLLayoutElement> children, float w, float h, IGLLayoutElement parent,
			int deltaTimeMs) {
		final Map<GLElement, ? extends IGLLayoutElement> lookup = Maps.uniqueIndex(children,
				AGLLayoutElement.TO_GL_ELEMENT);
		for (LinearBlock block : linearBlocks)
			block.doLayout(lookup);
		children.get(0).setBounds(0, 0, w, h);
		return false;
	}

	/**
	 * @param node
	 * @return
	 */
	public boolean containsNode(Node node) {
		return this.asList().contains(node);
	}

	/**
	 * @param node
	 * @param dim
	 */
	public void resort(Node node, EDimension dim) {
		LinearBlock block = getBlock(node, dim.opposite());
		block.update();
		block.apply();
		updateBlock();
	}

	private void updateBlock() {
		shiftToZero();
		updateSize();
		updateBands();
	}



	private void updateBands() {
		findDomino().updateBands();
		bands.relayout();
	}

	/**
	 * @param node
	 * @param dim
	 */
	public Pair<List<Node>, Boolean> sortByImpl(Node node, EDimension dim, boolean forceStratify) {
		LinearBlock block = getBlock(node, dim.opposite());
		if (block == null)
			return null;
		Pair<List<Node>, Boolean> old = block.sortBy(node, forceStratify);
		realign();
		updateBlock();
		return old;
	}

	public Pair<List<Node>, Boolean> sortBy(Node node, EDimension dim) {
		return sortByImpl(node, dim, false);
	}

	public Pair<List<Node>, Boolean> restoreSorting(Node node, EDimension dim, List<Node> sortCriteria,
			boolean stratified) {
		LinearBlock block = getBlock(node, dim.opposite());
		if (block == null)
			return null;
		Pair<List<Node>, Boolean> old = block.sortBy(sortCriteria, stratified);
		realign();
		updateBlock();
		return old;
	}

	public Pair<List<Node>, Boolean> stratifyBy(Node node, EDimension dim) {
		return sortByImpl(node, dim, true);
	}

	public Node limitTo(Node node, EDimension dim) {
		LinearBlock block = getBlock(node, dim.opposite());
		Node bak = block.limitDataTo(node);
		realign();
		updateBlock();
		return bak;
	}

	/**
	 * @param subList
	 * @param routes
	 */
	public void createBandsTo(List<Block> blocks, List<ABand> routes) {
		for (LinearBlock lblock : linearBlocks) {
			for (Block block : blocks) {
				for (LinearBlock rblock : block.linearBlocks) {
					if (isCompatible(lblock.getIdType(), rblock.getIdType()))
						createRoute(this, lblock, block, rblock, routes);
				}
			}
		}

	}


	private void createRoute(Block a, LinearBlock la, Block b, LinearBlock lb, List<ABand> routes) {
		TypedGroupList sData = la.getData();
		TypedGroupList tData = lb.getData();

		Rect ra = a.getAbsoluteBounds(la);
		Rect rb = b.getAbsoluteBounds(lb);

		String label = la.getNode(true).getLabel() + " x " + lb.getNode(false).getLabel();

		final INodeLocator sNodeLocator = la.getNodeLocator(true);
		final INodeLocator tNodeLocator = lb.getNodeLocator(false);
		final EDimension sDir = la.getDim().opposite();
		final EDimension tDir = lb.getDim().opposite();

		String id = toId(la.getNode(true), lb.getNode(false));
		ABand band = BandFactory.create(label, sData, tData, ra, rb, sNodeLocator, tNodeLocator, sDir, tDir, id);
		if (band == null)
			return;

		boolean swapped = band.getLocator(SourceTarget.SOURCE) != sNodeLocator;
		EDirection sdir = band.getAttachingDirection(SourceTarget.SOURCE);
		EDirection tdir = band.getAttachingDirection(SourceTarget.TARGET);
		if (swapped) {
			band.setLocators(lb.getNodeLocator(sdir.isPrimaryDirection()), la.getNodeLocator(tdir.isPrimaryDirection()));
		} else {
			band.setLocators(la.getNodeLocator(sdir.isPrimaryDirection()), lb.getNodeLocator(tdir.isPrimaryDirection()));
		}

		EBandMode defaultMode = EBandMode.OVERVIEW;
		if (sData.getGroups().size() > 1 || tData.getGroups().size() > 1)
			defaultMode = EBandMode.GROUPS;
		else if (sNodeLocator.hasLocator(EBandMode.DETAIL) && tNodeLocator.hasLocator(EBandMode.DETAIL))
			defaultMode = EBandMode.DETAIL;
		band.setLevel(defaultMode);

		routes.add(band);
	}


	/**
	 * @param node
	 * @param node2
	 * @return
	 */
	private static String toId(Node a, Node b) {
		int ai = a.getID();
		int bi = b.getID();
		if (ai < bi)
			return ai + "X" + bi;
		return bi + "X" + ai;
	}

	/**
	 * @param la
	 * @return
	 */
	public Rect getAbsoluteBounds(LinearBlock b) {
		Rect r = new Rect(b.getBounds());
		Vec2f xy = toAbsolute(r.xy());
		xy.sub(findParent(DominoCanvas.class).getAbsoluteLocation());
		r.xy(xy);
		return r;
	}

	private static boolean isCompatible(IDType a, IDType b) {
		return a.getIDCategory().isOfCategory(b);
	}



	/**
	 * @param pickable
	 */
	public void setContentPickable(boolean pickable) {
		for (Node n : nodes())
			n.setContentPickable(pickable);
	}

	/**
	 * @param node
	 * @param dimension
	 * @return
	 */
	public Color getStateColor(Node node, EDimension dim) {
		LinearBlock block = getBlock(node, dim.opposite());
		if (block == null)
			return Color.WHITE;
		return block.getStateColor(node);
	}

	public String getStateString(Node node, EDimension dim) {
		LinearBlock block = getBlock(node, dim.opposite());
		if (block == null)
			return "";
		return block.getStateString(node);
	}

	/**
	 * @return the linearBlocks, see {@link #linearBlocks}
	 */
	public List<LinearBlock> getLinearBlocks() {
		return Collections.unmodifiableList(linearBlocks);
	}

	/**
	 * @param r
	 */
	public void selectByBounds(Rectangle2D r, EToolState tool) {
		r = (Rectangle2D) r.clone(); // local copy

		Vec2f l = getLocation(); // to relative coordinates;
		r = new Rectangle2D.Double(r.getX() - l.x(), r.getY() - l.y(), r.getWidth(), r.getHeight());
		if (tool == EToolState.BANDS) {
			if (getOutlineShape().intersects(r)) {
				final NodeSelections domino = findDomino().getSelections();
				if (!domino.isSelected(SelectionType.SELECTION, this))
					domino.select(SelectionType.SELECTION, this, true);
				repaint();
			}
		} else {
			for (Node node : nodes()) {
				if (node.getRectangleBounds().intersects(r)) {
					node.selectByBounds(r);
				}
			}
		}
	}

	/**
	 * @param node
	 */
	public void tranposedNode(Node node) {
		replace(node, node);
	}

	/**
	 * @return
	 */
	public String getLabel() {
		return StringUtils.join(Iterators.transform(nodes().iterator(), Labels.TO_LABEL), ", ");
	}

	/**
	 *
	 */
	public void showAgain() {
		setVisibility(findDomino().getTool() == EToolState.BANDS ? EVisibility.PICKABLE : EVisibility.VISIBLE);
	}

	@ListenTo(sendToMe = true)
	private void onHideNodeEvent(HideNodeEvent event) {
		context.getMouseLayer().removeDragSource(source);
		setVisibility(EVisibility.HIDDEN);
	}

	/**
	 *
	 */
	public void selectAll() {
		for (Node n : nodes())
			n.selectAll();
	}

	/**
	 * @param tool
	 */
	public void setTool(EToolState tool) {
		switch (tool) {
		case BANDS:
			for (Node n : nodes()) {
				n.setVisibility(EVisibility.VISIBLE);
			}
			break;
		default:
			setFadeOut(false);
			repaint();
			for (Node n : nodes()) {
				n.setVisibility(EVisibility.PICKABLE);
				n.setContentPickable(tool == EToolState.SELECT);
			}
		}
		setVisibility(tool == EToolState.BANDS ? EVisibility.PICKABLE : EVisibility.VISIBLE);
	}

	public void transpose() {
		for (Node n : nodes())
			n.transposeMe();
		for (LinearBlock b : linearBlocks)
			b.transposedMe();
		realign();
		updateBlock();

	}

	/**
	 * @param left
	 * @param right
	 * @param offset
	 */
	private void setOffset(Node left, Node right, float offset) {
		// offset = Math.max(offset, Math.max(left.getDetachedOffset(), right.getDetachedOffset()));
		offsets.setOffset(left, right, offset);
	}

	/**
	 * @param bands
	 */
	public void createOffsetBands(List<ABand> bands) {
		for (LinearBlock l : getLinearBlocks()) {
			Node prev = null;
			for (Node node : l) {
				float offset = offsets.getOffset(prev, node);
				if (offset > 0) {
					final ABand b = create(prev, node, l, l.getDim());
					if (b != null)
						bands.add(b);
				}
				prev = node;
			}
		}
	}

	private ABand create(Node s, Node t, LinearBlock b, EDimension dim) {
		EDimension d = dim.opposite();
		TypedGroupList sData = s.getDetachedOffset() > 0 ? s.getData(d) : b.getData();
		TypedGroupList tData = t.getDetachedOffset() > 0 ? t.getData(d) : b.getData();

		Rect ra = s.getRectBounds();
		Rect rb = t.getRectBounds();
		String label = s.getLabel() + " x " + s.getLabel();
		final INodeLocator sNodeLocator = s.getNodeLocator(d);
		final INodeLocator tNodeLocator = t.getNodeLocator(d);
		String id = s.getID() + "D" + t.getID();
		ABand band = BandFactory.create(label, sData, tData, ra, rb, sNodeLocator, tNodeLocator, d, d, id);
		return band;
	}

	public boolean isSingle() {
		return nodeCount() == 1;
	}

	private boolean isStratified(Node node, EDimension dim) {
		LinearBlock b = getBlock(node, dim.opposite());
		return b == null ? false : b.isStratisfied(node);
	}

	/**
	 * @param node
	 * @param id
	 */
	public void setVisualizationType(Node node, String id) {
		boolean isSingle = nodeCount() == 1;
		boolean localChange = true;
		if (isSingle) {
			boolean stratify = VisualizationTypeOracle.stratifyByDefault(id);
			for (EDimension dim : node.dimensions()) {
				if (isStratified(node, dim) != stratify) {
					stratifyBy(node, dim);
					localChange = false;
				}
			}
		}
		if (localChange)
			node.setVisualizationTypeImpl(id);
	}

}
