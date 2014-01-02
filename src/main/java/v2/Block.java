/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package v2;

import gleem.linalg.Vec2f;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.caleydo.core.data.collection.EDimension;
import org.caleydo.core.id.IDType;
import org.caleydo.core.util.color.Color;
import org.caleydo.core.view.opengl.layout2.GLElement;
import org.caleydo.core.view.opengl.layout2.GLElementContainer;
import org.caleydo.core.view.opengl.layout2.geom.Rect;
import org.caleydo.core.view.opengl.layout2.layout.AGLLayoutElement;
import org.caleydo.core.view.opengl.layout2.layout.IGLLayout2;
import org.caleydo.core.view.opengl.layout2.layout.IGLLayoutElement;
import org.caleydo.core.view.opengl.layout2.renderer.GLRenderers;
import org.caleydo.view.domino.api.model.graph.EDirection;
import org.caleydo.view.domino.api.model.typed.MultiTypedSet;
import org.caleydo.view.domino.api.model.typed.TypedGroupList;
import org.caleydo.view.domino.api.model.typed.TypedSets;

import v2.band.Band;
import v2.band.BandLine;
import v2.band.BandLines;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

/**
 *
 * @author Samuel Gratzl
 *
 */
public class Block extends GLElementContainer implements IGLLayout2 {

	private final List<LinearBlock> linearBlocks = new ArrayList<>();
	private final Vec2f shift = new Vec2f(0, 0);

	public Block(Node node) {
		setLayout(this);
		this.add(node);
		for (EDimension dim : EDimension.values()) {
			if (!node.has(dim.opposite()))
				continue;
			linearBlocks.add(new LinearBlock(dim, node));
		}
		updateSize();
		setRenderer(GLRenderers.drawRect(Color.BLUE));
	}

	public Collection<Placeholder> addPlaceholdersFor(Node node) {
		List<Placeholder> r = new ArrayList<>();
		for (LinearBlock block : linearBlocks) {
			if (!node.has(block.getDim().opposite()))
				continue;
			block.addPlaceholdersFor(node, r);
		}
		return r;
	}

	public void addNode(Node neighbor, EDirection dir, Node node) {
		this.add(node);
		LinearBlock block = getBlock(neighbor, dir.asDim());
		block.add(neighbor, dir, node);
		EDimension other = dir.asDim().opposite();
		if (node.has(other.opposite()))
			linearBlocks.add(new LinearBlock(other, node));
		shiftNode(node, shift.x(), shift.y());
		shiftToZero();
		updateSize();
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
	}

	public void incSizes(float x, float y) {
		shift.setX(shift.x() + x);
		shift.setY(shift.y() + y);

		for (Node n : nodes()) {
			shiftNode(n, x, y);
		}
		Set<LinearBlock> b = filterSingleBlocks();
		if (!b.isEmpty()) {
			LinearBlock f = b.iterator().next();
			Node n = f.get(0);
			shiftBlocks(f, n, x, y, b);

			shiftToZero();
		}
		updateSize();
	}

	/**
	 *
	 */
	private void shiftToZero() {
		Vec2f offset = new Vec2f(0, 0);
		for (Node n : nodes()) {
			Vec2f l = n.getLocation();
			if (l.x() < offset.x())
				offset.setX(l.x());
			if (l.y() < offset.y())
				offset.setY(l.y());
		}
		if (offset.x() == 0 && offset.y() == 0)
			return;

		for (Node n : nodes()) {
			Vec2f l = n.getLocation();
			n.setLocation(l.x() - offset.x(), l.y() - offset.y());
		}
	}

	private void shiftNode(Node n, float x, float y) {
		Vec2f s = n.getSize().copy();
		s.setX(Math.max(s.x() + x, 10));
		s.setY(Math.max(s.y() + y, 10));
		n.setSize(s.x(), s.y());
		n.relayout();
	}

	private void shiftBlocks(LinearBlock block, Node start, float x, float y, Set<LinearBlock> b) {
		b.remove(block);
		block.shift(start, x, y);
		for (Node node : block) {
			for (LinearBlock bblock : new HashSet<>(b)) {
				if (bblock.contains(node)) {
					shiftBlocks(bblock, node, x, y, b);
				}
			}
		}
	}

	/**
	 * @return
	 */
	private Iterable<Node> nodes() {
		return Iterables.filter(this, Node.class);
	}

	/**
	 * @return
	 */
	private Set<LinearBlock> filterSingleBlocks() {
		Set<LinearBlock> r = new HashSet<>();
		for (LinearBlock b : linearBlocks) {
			if (b.size() > 1)
				r.add(b);
		}
		return r;
	}

	public boolean removeNode(Node node) {
		for (EDimension dim : EDimension.values()) {
			if (!node.has(dim.opposite()))
				continue;
			LinearBlock block = getBlock(node, dim);
			if (block.size() == 1)
				linearBlocks.remove(block);
			block.remove(node);
		}
		this.remove(node);
		shiftToZero();
		updateSize();
		return this.isEmpty();
	}

	private LinearBlock getBlock(Node node, EDimension dim) {
		for (LinearBlock block : linearBlocks) {
			if (block.getDim() == dim && block.contains(node))
				return block;
		}
		throw new IllegalStateException();
	}

	@Override
	public boolean doLayout(List<? extends IGLLayoutElement> children, float w, float h, IGLLayoutElement parent,
			int deltaTimeMs) {
		final Map<GLElement, ? extends IGLLayoutElement> lookup = Maps.uniqueIndex(children,
				AGLLayoutElement.TO_GL_ELEMENT);
		for (LinearBlock block : linearBlocks)
			block.doLayout(lookup);
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
		findParent(Domino.class).updateBands();
		updateSize();
	}

	/**
	 * @param node
	 * @param dim
	 */
	public void sortByMe(Node node, EDimension dim) {
		LinearBlock block = getBlock(node, dim.opposite());
		block.sortBy(node);
	}

	/**
	 * @param subList
	 * @param routes
	 */
	public void createBandsTo(List<Block> blocks, List<Band> routes) {
		for (LinearBlock lblock : linearBlocks) {
			for (Block block : blocks) {
				for (LinearBlock rblock : block.linearBlocks) {
					if (isCompatible(lblock.getIdType(), rblock.getIdType()))
						createRoute(this, lblock, block, rblock, routes);
				}
			}
		}

	}


	private void createRoute(Block a, LinearBlock la, Block b, LinearBlock lb, List<Band> routes) {
		TypedGroupList sData = la.getData(true);
		TypedGroupList tData = lb.getData(false);
		MultiTypedSet shared = TypedSets.intersect(sData.asSet(), tData.asSet());
		if (shared.isEmpty())
			return;

		Rect ra = a.getAbsoluteBounds(la);
		Rect rb = b.getAbsoluteBounds(lb);

		BandLine line = BandLines.create(ra, la.getDim(), rb, lb.getDim());
		if (line == null)
			return;

		Band band = new Band(line, shared, sData, tData);
		routes.add(band);
	}

	/**
	 * @param la
	 * @return
	 */
	private Rect getAbsoluteBounds(LinearBlock b) {
		Rect r = new Rect(b.getBounds());
		r.xy(toAbsolute(r.xy()));
		return r;
	}

	private static boolean isCompatible(IDType a, IDType b) {
		return a.getIDCategory().isOfCategory(b);
	}

}