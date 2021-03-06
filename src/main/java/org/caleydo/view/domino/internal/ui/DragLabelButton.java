/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package org.caleydo.view.domino.internal.ui;

import org.caleydo.core.id.IDCategory;
import org.caleydo.core.view.opengl.layout.Column.VAlign;
import org.caleydo.core.view.opengl.layout2.dnd.IDragInfo;
import org.caleydo.core.view.opengl.layout2.renderer.GLRenderers;
import org.caleydo.view.domino.internal.Node;
import org.caleydo.view.domino.internal.data.LabelDataValues;
import org.caleydo.view.domino.internal.dnd.NodeDragInfo;

/**
 * @author Samuel Gratzl
 *
 */
public class DragLabelButton extends ADragButton {
	private final LabelDataValues data;

	public DragLabelButton(IDCategory category) {
		this.data = new LabelDataValues(category);
		setRenderer(GLRenderers.drawText("L", VAlign.CENTER));
		setTooltip(data.getLabel());
	}

	@Override
	public IDragInfo startSWTDrag(IDragEvent event) {
		return new NodeDragInfo(event.getMousePos(), new Node(data));
	}
}
