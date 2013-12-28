/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package org.caleydo.view.domino.internal.dnd;

import gleem.linalg.Vec2f;

import org.caleydo.core.view.opengl.layout2.dnd.EDnDType;
import org.caleydo.view.domino.spi.model.graph.INode;

/**
 * @author Samuel Gratzl
 *
 */
public class NodeDragInfo extends ANodeDragInfo implements INodeCreator {
	protected final INode node;

	public NodeDragInfo(INode node, Vec2f mousePos) {
		super(mousePos);
		this.node = node;
	}

	@Override
	public INode apply(EDnDType type) {
		return (type == EDnDType.COPY) ? node.clone() : node;
	}

	@Override
	public String getLabel() {
		return node.getLabel();
	}

	/**
	 * @return the node, see {@link #node}
	 */
	public INode getNode() {
		return node;
	}

}
