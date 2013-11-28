/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package org.caleydo.view.domino.internal.ui.prototype.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.caleydo.core.view.opengl.layout2.GLElementContainer;
import org.caleydo.core.view.opengl.layout2.layout.IGLLayout2;
import org.caleydo.view.domino.internal.ui.prototype.INode;
import org.caleydo.view.domino.internal.ui.prototype.graph.DominoGraph;
import org.caleydo.vis.lineup.ui.GLPropertyChangeListeners;

import com.google.common.collect.Iterables;

/**
 * @author Samuel Gratzl
 *
 */
public class DominoNodeLayer extends GLElementContainer {
	private final PropertyChangeListener repaint = GLPropertyChangeListeners.repaintOnEvent(this);
	private final PropertyChangeListener relayout = GLPropertyChangeListeners.relayoutOnEvent(this);
	/**
	 * @param graph
	 * @param graphElement
	 */
	public DominoNodeLayer(IGLLayout2 layout, DominoGraph graph) {
		super(layout);
		graph.addPropertyChangeListener(DominoGraph.PROP_EDGES, repaint);
		graph.addPropertyChangeListener(DominoGraph.PROP_TRANSPOSED, relayout);
		graph.addPropertyChangeListener(DominoGraph.PROP_VERTICES, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getNewValue() != null)
					add((INode) evt.getNewValue());
				if (evt.getOldValue() != null)
					remove((INode) evt.getOldValue());
			}
		});
		for (INode node : graph.vertexSet()) {
			add(node);
		}
	}

	/**
	 * @param vertex
	 */
	private void remove(INode vertex) {
		for (NodeElement elem : Iterables.filter(this, NodeElement.class)) {
			if (elem.getNode() == vertex) {
				remove(elem);
				break;
			}
		}
	}

	/**
	 * @param node
	 */
	private void add(INode node) {
		this.add(new NodeElement(node));
	}

	/**
	 * @return
	 */
	public Iterable<NodeElement> getNodes() {
		return Iterables.filter(this, NodeElement.class);
	}

}