/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package org.caleydo.view.domino.internal.ui.prototype.ui;

import gleem.linalg.Vec2f;

import java.util.Set;

import org.caleydo.core.data.selection.SelectionType;
import org.caleydo.core.event.EventListenerManager.ListenTo;
import org.caleydo.core.event.EventPublisher;
import org.caleydo.core.util.color.Color;
import org.caleydo.core.view.opengl.canvas.IGLMouseListener.IMouseEvent;
import org.caleydo.core.view.opengl.layout.Column.VAlign;
import org.caleydo.core.view.opengl.layout2.GLElement;
import org.caleydo.core.view.opengl.layout2.GLGraphics;
import org.caleydo.core.view.opengl.layout2.dnd.IDnDItem;
import org.caleydo.core.view.opengl.layout2.dnd.IDragGLSource;
import org.caleydo.core.view.opengl.layout2.dnd.IDragInfo;
import org.caleydo.core.view.opengl.layout2.renderer.Borders;
import org.caleydo.core.view.opengl.layout2.renderer.Borders.IBorderGLRenderer;
import org.caleydo.core.view.opengl.picking.Pick;
import org.caleydo.view.domino.internal.dnd.NodeDragInfo;
import org.caleydo.view.domino.internal.ui.ReScaleBorder;
import org.caleydo.view.domino.internal.ui.prototype.INode;
import org.caleydo.view.domino.internal.ui.prototype.graph.DominoGraph;
import org.caleydo.view.domino.internal.ui.prototype.graph.DominoGraph.EPlaceHolderFlag;
import org.caleydo.view.domino.internal.ui.prototype.graph.Placeholder;
import org.eclipse.swt.SWT;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * @author Samuel Gratzl
 *
 */
public class NodeElement extends ANodeElement {
	private final IBorderGLRenderer border;

	public NodeElement(INode node) {
		super(node);
		this.border = Borders.createBorder(Color.BLACK);
		this.add(new ReScaleBorder(info).setRenderer(border));
	}

	private final IDragGLSource source = new IDragGLSource() {

		@Override
		public IDragInfo startSWTDrag(IDragEvent event) {
			EventPublisher.trigger(new ShowPlaceHoldersEvent().to(NodeElement.this));
			return new NodeDragInfo(node, event.getOffset());
		}

		@Override
		public void onDropped(IDnDItem info) {
			DominoGraph graph = findGraph();
			graph.removePlaceholders(ImmutableSet.copyOf(Iterables.filter(graph.vertexSet(), PlaceholderNode.class)));
			// if (info.getType() == EDnDType.MOVE) {
			// System.out.println("detach");
			// Collection<IEdge> detached = graph.detach(node, true);
			// }
		}

		@Override
		public GLElement createUI(IDragInfo info) {
			NodeDragInfo d = ((NodeDragInfo) info);
			GLElement e = d.createUI();
			return e;
		}
	};

	@Override
	protected void takeDown() {
		context.getMouseLayer().removeDragSource(source);
		super.takeDown();
	}

	@ListenTo(sendToMe = true)
	private void onShowPlaceHoldersEvent(ShowPlaceHoldersEvent event) {
		DominoGraph graph = findGraph();
		Set<Placeholder> placeholders = graph.findPlaceholders(node, EPlaceHolderFlag.INCLUDE_FREE);
		graph.insertPlaceholders(placeholders, node);

	}

	@Override
	protected void onMainPick(Pick pick) {
		switch (pick.getPickingMode()) {
		case MOUSE_OVER:
			if (info.getConfig().canMove()) {
				context.getMouseLayer().addDragSource(source);
				context.getSWTLayer().setCursor(SWT.CURSOR_HAND);
			}
			break;
		case MOUSE_OUT:
			context.getMouseLayer().removeDragSource(source);
			context.getSWTLayer().setCursor(-1);
			break;
		default:
			break;
		}
	}

	@Override
	public void pick(Pick pick) {
		super.pick(pick);
		IMouseEvent event = ((IMouseEvent) pick);
		boolean isToolBar = pick.getObjectID() > 0;
		switch (pick.getPickingMode()) {
		case MOUSE_OVER:
			if (!isToolBar) {
				border.setColor(SelectionType.MOUSE_OVER.getColor());
				info.setHovered(true);
			}
			break;
		case MOUSE_OUT:
			if (!isToolBar) {
				border.setColor(Color.BLACK);
				info.setHovered(info.isSelected());
			}
			break;
		case MOUSE_RELEASED:
			if (isToolBar)
				info.setSelected(event.isCtrlDown()); // multi selection
			break;
		default:
			break;
		}
	}

	@Override
	protected void renderImpl(GLGraphics g, float w, float h) {
		// g.color(Color.BLACK).drawRect(0, 0, w, h);
		super.renderImpl(g, w, h);
		g.incZ();
		g.incZ();
		float wi = Math.max(100, w);
		float x = (w - wi) * 0.5f;
		g.drawText(node.getLabel(), x, (h - 10) * .5f, wi, 10, VAlign.CENTER);
		g.decZ();
		g.decZ();
	}


	@Override
	public Vec2f getMinSize() {
		Vec2f s = getNodeSize();
		s.add(new Vec2f(BORDER * 2, BORDER * 2));
		return s;
	}
}
