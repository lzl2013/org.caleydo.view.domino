/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package org.caleydo.view.domino.internal.band;

import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.caleydo.core.data.collection.EDimension;
import org.caleydo.core.data.selection.SelectionType;
import org.caleydo.core.id.IDType;
import org.caleydo.core.util.base.ILabeled;
import org.caleydo.core.util.collection.Pair;
import org.caleydo.core.util.color.Color;
import org.caleydo.core.view.opengl.layout2.GLGraphics;
import org.caleydo.core.view.opengl.layout2.util.PickingPool;
import org.caleydo.view.domino.api.model.typed.MultiTypedSet;
import org.caleydo.view.domino.api.model.typed.TypedCollections;
import org.caleydo.view.domino.api.model.typed.TypedGroupList;
import org.caleydo.view.domino.api.model.typed.TypedSet;
import org.caleydo.view.domino.internal.INodeLocator;
import org.caleydo.view.domino.internal.band.IBandHost.SourceTarget;

/**
 * @author Samuel Gratzl
 *
 */
public abstract class ABand implements ILabeled {
	protected final static Color color = new Color(0, 0, 0, 0.5f);

	protected static final List<SelectionType> SELECTION_TYPES = Arrays.asList(SelectionType.SELECTION,
			SelectionType.MOUSE_OVER);

	protected final MultiTypedSet shared;

	protected final TypedGroupList sData;
	protected final TypedGroupList tData;

	protected EBandMode mode = EBandMode.GROUPS;

	protected INodeLocator sLocator, tLocator;

	protected final EDimension sDim;
	protected final EDimension tDim;

	private final String identifier;

	public ABand(MultiTypedSet shared, TypedGroupList sData, TypedGroupList tData,
			INodeLocator sLocator, INodeLocator tLocator, EDimension sDim,
 EDimension tDim, String identifier) {
		this.shared = shared;
		this.sData = sData;
		this.tData = tData;
		this.sLocator = sLocator;
		this.tLocator = tLocator;
		this.sDim = sDim;
		this.tDim = tDim;
		this.identifier = identifier;
	}

	/**
	 * @return the identifier, see {@link #identifier}
	 */
	public String getIdentifier() {
		return identifier;
	}

	public void initFrom(ABand band) {
		this.mode = band.mode;
	}

	/**
	 *
	 */
	public abstract boolean stubify();

	protected void updateBand(INodeLocator sLocator, INodeLocator tLocator) {
		this.sLocator = sLocator;
		this.tLocator = tLocator;
	}

	@Override
	public String getLabel() {
		return overviewRoute().getLabel();
	}

	public EDimension getDimension(SourceTarget type) {
		return type.select(sDim, tDim);
	}

	public Pair<TypedSet, TypedSet> intersectingIds(Rectangle2D bounds) {
		IBandRenderAble r = overviewRoute();
		if (!r.intersects(bounds))
			return Pair.make(TypedCollections.empty(getIdType(SourceTarget.SOURCE)),
					TypedCollections.empty(getIdType(SourceTarget.TARGET)));

		Iterable<? extends IBandRenderAble> l;
		switch(mode) {
		case OVERVIEW:
			return Pair.make(r.asSet(SourceTarget.SOURCE), r.asSet(SourceTarget.TARGET));
		case GROUPS:
			l = groupRoutes();
			break;
		case DETAIL:
			l = detailRoutes();
			break;
		default:
			throw new IllegalStateException();
		}

		Set<Integer> rs = new HashSet<>();
		Set<Integer> rt = new HashSet<>();
		for(IBandRenderAble ri : l) {
			if (ri.intersects(bounds)) {
				rs.addAll(ri.asSet(SourceTarget.SOURCE));
				rt.addAll(ri.asSet(SourceTarget.TARGET));
			}
		}
		return Pair.make(new TypedSet(rs, getIdType(SourceTarget.SOURCE)), new TypedSet(rs,
				getIdType(SourceTarget.TARGET)));
	}

	public final boolean intersects(Rectangle2D bounds) {
		return overviewRoute().intersects(bounds);
	}


	public abstract void renderMiniMap(GLGraphics g);

	public final void render(GLGraphics g, float w, float h, IBandHost host) {
		switch (mode) {
		case OVERVIEW:
			overviewRoute().renderRoute(g, host, 1);
			break;
		case GROUPS:
			float z = g.z();
			final Collection<? extends IBandRenderAble> gR = groupRoutes();
			if (gR.isEmpty()) {
				mode = EBandMode.OVERVIEW;
				render(g, w, h, host);
				return;
			}
			for (IBandRenderAble r : gR) {
				g.incZ(0.0001f);
				r.renderRoute(g, host, gR.size());
			}
			g.incZ(z - g.z());
			break;
		case DETAIL:
			z = g.z();
			final List<? extends IBandRenderAble> lR = detailRoutes();
			if (lR.isEmpty()) {
				mode = EBandMode.GROUPS;
				render(g, w, h, host);
				return;
			}
			for (IBandRenderAble r : lR) {
				g.incZ(0.0001f);
				r.renderRoute(g, host, lR.size());
			}
			g.incZ(z - g.z());
			break;
		}
	}

	/**
	 * @return
	 */
	protected abstract IBandRenderAble overviewRoute();

	protected abstract List<? extends IBandRenderAble> groupRoutes();

	protected abstract List<? extends IBandRenderAble> detailRoutes();

	public final int renderPick(GLGraphics g, float w, float h, IBandHost host, PickingPool pickingPool, int start) {
		switch (mode) {
		case OVERVIEW:
			g.pushName(pickingPool.get(start++));
			overviewRoute().renderRoute(g, host, 1);
			g.popName();
			break;
		case GROUPS:
			final List<? extends IBandRenderAble> gR = groupRoutes();
			for (IBandRenderAble r : gR) {
				g.pushName(pickingPool.get(start++));
				r.renderRoute(g, host, gR.size());
				g.popName();
			}
			break;
		case DETAIL:
			final List<? extends IBandRenderAble> dR = detailRoutes();
			for (IBandRenderAble r : dR) {
				g.pushName(pickingPool.get(start++));
				r.renderRoute(g, host, dR.size());
				g.popName();
			}
			break;
		}
		return start;
	}
	private boolean canHaveDetailMode() {
		return sLocator.hasLocator(EBandMode.DETAIL) && tLocator.hasLocator(EBandMode.DETAIL);
	}
	/**
	 * @param b
	 */
	public void changeLevel(boolean increase) {
		if ((mode == EBandMode.OVERVIEW && !increase))
			return;
		boolean detailsThere = canHaveDetailMode();
		if ((mode == EBandMode.GROUPS && !detailsThere && increase) || (mode == EBandMode.DETAIL && increase))
			return;
		mode = EBandMode.values()[this.mode.ordinal() + (increase ? 1 : -1)];
	}

	public void setLevel(EBandMode mode) {
		this.mode = mode;
	}


	public TypedSet getIds(SourceTarget type, int subIndex) {
		IBandRenderAble r = getRoute(subIndex);
		return r == null ? overviewRoute().asSet(type) : r.asSet(type);
	}

	public String getLabel(int subIndex) {
		IBandRenderAble r = getRoute(subIndex);
		return r == null ? "" : r.getLabel();
	}

	private IBandRenderAble getRoute(int subIndex) {
		switch (mode) {
		case OVERVIEW:
			return overviewRoute();
		case GROUPS:
			final List<? extends IBandRenderAble> g = groupRoutes();
			if (subIndex < 0 || g.size() <= subIndex)
				return null;
			return g.get(subIndex);
		case DETAIL:
			final List<? extends IBandRenderAble> l = detailRoutes();
			if (subIndex < 0 || l.size() <= subIndex)
				return null;
			return l.get(subIndex);
		}
		throw new IllegalStateException();
	}

	public IDType getIdType(SourceTarget type) {
		return overviewRoute().asSet(type).getIdType();
	}

	protected interface IBandRenderAble extends ILabeled {
		void renderRoute(GLGraphics g, IBandHost host, int nrBands);

		/**
		 * @param bounds
		 * @return
		 */
		boolean intersects(Rectangle2D bounds);

		/**
		 * @param type
		 * @return
		 */
		TypedSet asSet(SourceTarget type);
	}
}
