/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package v2;

import gleem.linalg.Vec2f;

import java.awt.geom.Rectangle2D;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.caleydo.core.data.collection.EDimension;
import org.caleydo.core.id.IDType;
import org.caleydo.core.util.color.Color;
import org.caleydo.core.view.opengl.layout2.GLElement;
import org.caleydo.core.view.opengl.layout2.geom.Rect;
import org.caleydo.core.view.opengl.layout2.layout.IGLLayoutElement;
import org.caleydo.core.view.opengl.layout2.manage.GLLocation.ILocator;
import org.caleydo.view.domino.api.model.graph.EDirection;
import org.caleydo.view.domino.api.model.typed.IMultiTypedCollection;
import org.caleydo.view.domino.api.model.typed.ITypedComparator;
import org.caleydo.view.domino.api.model.typed.ITypedGroup;
import org.caleydo.view.domino.api.model.typed.MultiTypedList;
import org.caleydo.view.domino.api.model.typed.MultiTypedSet;
import org.caleydo.view.domino.api.model.typed.RepeatingList;
import org.caleydo.view.domino.api.model.typed.TypedCollections;
import org.caleydo.view.domino.api.model.typed.TypedGroupList;
import org.caleydo.view.domino.api.model.typed.TypedList;
import org.caleydo.view.domino.api.model.typed.TypedListGroup;
import org.caleydo.view.domino.api.model.typed.TypedSet;
import org.caleydo.view.domino.api.model.typed.TypedSetGroup;
import org.caleydo.view.domino.api.model.typed.TypedSets;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * @author Samuel Gratzl
 *
 */
public class LinearBlock extends AbstractCollection<Node> {
	private final EDimension dim;
	private final List<Node> nodes = new ArrayList<>();

	private List<Node> sortCriteria = new ArrayList<>(2);
	private Node dataSelection = null;

	private boolean stratified = true;
	private MultiTypedList data;

	public LinearBlock(EDimension dim, Node node) {
		this.dim = dim;
		this.nodes.add(node);
		this.sortCriteria.add(node);
		this.dataSelection = node;
	}

	public boolean isStratisfied() {
		return stratified;
	}

	public Rectangle2D getBounds() {
		Rectangle2D r = null;
		for (Node elem : nodes) {
			if (r == null) {
				r = elem.getRectangleBounds();
			} else
				Rectangle2D.union(r, elem.getRectangleBounds(), r);
		}
		return r;
	}

	public IDType getIdType() {
		return nodes.get(0).getIdType(dim.opposite());
	}

	public Node get(int index) {
		return nodes.get(index);
	}

	/**
	 * @param node
	 * @param r
	 */
	public void addPlaceholdersFor(Node node, List<Placeholder> r) {
		IDType idtype = node.getIdType(dim.opposite());
		if (!isCompatible(idtype, getIdType()))
			return;
		Node n = nodes.get(0);
		if (n != node)
			r.add(new Placeholder(n, EDirection.getPrimary(dim)));
		n = nodes.get(nodes.size()-1);
		if (n != node)
			r.add(new Placeholder(n, EDirection.getPrimary(dim).opposite()));
	}

	private static boolean isCompatible(IDType a, IDType b) {
		return a.getIDCategory().isOfCategory(b);
	}
	/**
	 * @return the dim, see {@link #dim}
	 */
	public EDimension getDim() {
		return dim;
	}

	@Override
	public int size() {
		return nodes.size();
	}

	@Override
	public Iterator<Node> iterator() {
		return nodes.iterator();
	}

	@Override
	public boolean add(Node node) {
		this.add(this.nodes.get(this.nodes.size() - 1), EDirection.getPrimary(dim).opposite(), node);
		return true;
	}

	/**
	 * @param node
	 */
	public void remove(Node node) {
		int index = nodes.indexOf(node);
		this.nodes.remove(index);
		if (nodes.isEmpty())
			return;
		// update neighbors
		if (index == 0) {
			nodes.get(0).setNeighbor(EDirection.getPrimary(dim), null);
		} else if (index >= nodes.size()) {
			nodes.get(index - 1).setNeighbor(EDirection.getPrimary(dim).opposite(), null);
		} else {
			nodes.get(index - 1).setNeighbor(EDirection.getPrimary(dim).opposite(), nodes.get(index));
		}

		Vec2f shift;
		if (dim.isHorizontal()) {
			shift = new Vec2f(-node.getSize().x(), 0);
		} else {
			shift = new Vec2f(0, -node.getSize().y());
		}
		shift(index, nodes.size(), shift);
		sortCriteria.remove(node);
		if (dataSelection == node) {
			dataSelection = nodes.size() == 1 ? nodes.get(0) : null;
		}
		if (sortCriteria.isEmpty() && !nodes.isEmpty())
			sortCriteria.add(nodes.get(0));
		resort();
		apply();
	}


	/**
	 * @param neighbor
	 * @param dir
	 * @param node
	 */
	public void add(Node neighbor, EDirection dir, Node node) {
		int index = nodes.indexOf(neighbor);
		assert index >= 0;

		Node old = neighbor.getNeighbor(dir);
		neighbor.setNeighbor(dir, node);
		node.setNeighbor(dir, old);
		{
			Vec2f s = neighbor.getShift();
			node.shiftSize(dim.select(0, s.x()), dim.select(s.y(), 0)); // synchronize the other dimension
		}

		Rect bounds = neighbor.getRectBounds();
		Vec2f shift;
		if (dir.isPrimaryDirection()) {
			if (dim.isHorizontal()) {
				node.setLocation(bounds.x() - node.getSize().x(), bounds.y());
				shift = new Vec2f(-node.getSize().x(), 0);
			} else {
				node.setLocation(bounds.x(), bounds.y() - node.getSize().y());
				shift = new Vec2f(0, -node.getSize().y());
			}
			shift(0, index, shift);
		} else {
			if (dim.isHorizontal()) {
				node.setLocation(bounds.x2(), bounds.y());
				shift = new Vec2f(node.getSize().x(), 0);
			} else {
				node.setLocation(bounds.x(), bounds.y2());
				shift = new Vec2f(0, node.getSize().y());
			}
			shift(index + 1, nodes.size(), shift);
		}
		if (dir.isPrimaryDirection())
			this.nodes.add(index, node);
		else
			this.nodes.add(index + 1, node);

		sortCriteria.add(node);
		update();
		apply();
	}

	public void shift(Node node) {
		int i = nodes.indexOf(node);
		Vec2f change = node.getLayoutDataAs(Vec2f.class, null);
		float x = change.x();
		float y = change.y();
		y = dim.select(0, y);
		x = dim.select(x, 0);
		// shift(0, i, new Vec2f(-x, -y));
		shift(i + 1, nodes.size(), new Vec2f(x, y));
	}

	private void shift(int from, int to, Vec2f shift) {
		for (int i = from; i < to; ++i) {
			final Node nnode = nodes.get(i);
			Vec2f loc = nnode.getLocation();
			nnode.setLocation(loc.x() + shift.x(), loc.y() + shift.y());
		}
	}


	public void updateNeighbors() {
		Node prev = null;
		EDirection dir = EDirection.getPrimary(dim).opposite();
		for (Node node : nodes) {
			node.setNeighbor(dir, prev);
			prev = node;
		}
	}

	public void doLayout(Map<GLElement, ? extends IGLLayoutElement> lookup) {

	}

	/**
	 * @param neighbor
	 * @return
	 */
	public boolean contains(Node node) {
		return nodes.contains(node);
	}

	public void resort() {
		if (this.data == null)
			update();
		else
			resortImpl(this.data);
	}

	/**
	 * @param data2
	 */
	private void resortImpl(IMultiTypedCollection data) {
		List<ITypedComparator> c = asComparators(dim.opposite());
		this.data = TypedSets.sort(data, c.toArray(new ITypedComparator[0]));
	}

	public void update() {
		if (nodes.isEmpty())
			return;
		MultiTypedSet union;

		if (dataSelection == null) {
			union = unionAll();
		} else {
			union = intersectSome();
		}
		resortImpl(union);
	}

	/**
	 * @return
	 */
	private MultiTypedSet intersectSome() {
		TypedSet dataSelection = this.dataSelection.getGroups(dim.opposite());
		Collection<TypedSet> all = Collections2.transform(nodes, new Function<Node, TypedSet>() {
			@Override
			public TypedSet apply(Node input) {
				return input.getGroups(dim.opposite());
			}
		});
		MultiTypedSet intersect = TypedSets.intersect(dataSelection);
		return intersect.expand(all);
	}

	private MultiTypedSet unionAll() {
		Collection<TypedSet> sets = Collections2.transform(nodes, new Function<Node, TypedSet>() {
			@Override
			public TypedSet apply(Node input) {
				return input.getGroups(dim.opposite());
			}
		});
		return TypedSets.unionDeep(sets.toArray(new TypedSet[0]));
	}

	private List<ITypedComparator> asComparators(final EDimension dim) {
		return ImmutableList.copyOf(Iterables.transform(sortCriteria, new Function<Node, ITypedComparator>() {
			@Override
			public ITypedComparator apply(Node input) {
				return input.getComparator(dim);
			}
		}));
	}

	public void apply() {
		List<? extends ITypedGroup> g = asGroupList();

		for (Node node : nodes) {
			final TypedList slice = data.slice(node.getIdType(dim.opposite()));
			node.setData(dim.opposite(), TypedGroupList.create(slice, g));
		}
		{
			Node bak = nodes.get(0);
			for (Node node : nodes.subList(1, nodes.size())) {
				node.updateNeighbor(EDirection.getPrimary(dim), bak);
			}
		}
	}

	private List<? extends ITypedGroup> asGroupList() {
		if (!isStratisfied())
			return Collections.singletonList(ungrouped(data.size()));
		final Node sortedBy = sortCriteria.get(0);
		final Node selected = dataSelection;

		List<TypedSetGroup> groups = sortedBy.getGroups(dim.opposite()).getGroups();
		if (selected == sortedBy) // 1:1 mapping
			return groups;
		List<ITypedGroup> g = new ArrayList<>(groups.size() + 1);
		if (selected == null) {
			int sum = 0;
			TypedList gdata = data.slice(groups.get(0).getIdType());
			for (ITypedGroup group : groups) {
				int bak = sum;
				sum += group.size();
				while (sum < gdata.size() && group.contains(gdata.get(sum)))
					sum++;
				if ((bak + groups.size()) == sum) { // no extra elems
					g.add(group);
				} else { // have repeating elements
					g.add(new TypedListGroup(new RepeatingList<>(TypedCollections.INVALID_ID, sum - bak), group
							.getIdType(), group.getLabel(), group.getColor()));
				}
			}
			if (sum < data.size())
				g.add(unmapped(data.size() - sum));
		} else {
			// we have data selection but a different grouping
			int sum = 0;
			TypedList gdata = data.slice(groups.get(0).getIdType());
			for (ITypedGroup group : groups) {
				int bak = sum;
				while (sum < gdata.size() && group.contains(gdata.get(sum)))
					sum++;
				if ((bak + groups.size()) == sum) { // no extra elems
					g.add(group);
				} else { // have repeating or less elements
					g.add(new TypedListGroup(new RepeatingList<>(TypedCollections.INVALID_ID, sum - bak), group
							.getIdType(), group.getLabel(), group.getColor()));
				}
			}
			if (sum < data.size())
				g.add(unmapped(data.size() - sum));
		}
		return g;
	}

	private static ITypedGroup ungrouped(int size) {
		return TypedGroupList.createUngroupedGroup(TypedCollections.INVALID_IDTYPE, size);
	}

	private static ITypedGroup unmapped(int size) {
		return TypedGroupList.createUnmappedGroup(TypedCollections.INVALID_IDTYPE, size);
	}


	/**
	 * @param b
	 * @return
	 */
	public TypedGroupList getData(boolean first) {
		return getNode(first).getData(dim.opposite());
	}

	Node getNode(boolean first) {
		return first ? nodes.get(0) : nodes.get(nodes.size() - 1);
	}

	/**
	 * @param b
	 * @return
	 */
	public ILocator getLocator(boolean first) {
		return getNode(first).getLocator(dim.opposite());
	}

	/**
	 * @param node
	 */
	public boolean sortBy(Node node) {
		if (nodes.size() == 1) {
			this.stratified = !this.stratified;
			update();
			apply();
			return true;
		}
		int index = sortCriteria.indexOf(node);
		if (index == 0 && stratified) {
			stratified = false;
		} else if (index == 0)
			sortCriteria.remove(index);
		if (index != 0) {
			sortCriteria.add(0, node);
			this.stratified = true;
		} else if (sortCriteria.isEmpty()) {
			for (Node n : nodes)
				if (n != node) {
					sortCriteria.add(n);
					stratified = true;
					break;
				}
		}

		update();
		apply();
		return true;
	}

	public void limitDataTo(Node node) {
		if (nodes.size() == 1) {
			return;
		}
		// add if it was not part of
		if (dataSelection == node) {
			dataSelection = null;
		} else
			dataSelection = node;
		update();
		apply();
	}

	/**
	 * @param node
	 * @return
	 */
	public Color getStateColor(Node node) {
		int index = sortCriteria.indexOf(node);
		boolean limited = dataSelection == node;
		boolean stratified = index == 0 && isStratisfied();
		boolean sorted = index != -1;
		Color c = Color.GRAY;
		if (stratified) {
			c = Color.RED;
		} else if (sorted)
			c = Color.MAGENTA;
		if (limited)
			c = c.darker();
		return c;
	}

}
