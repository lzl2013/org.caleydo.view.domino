/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package org.caleydo.view.domino.internal.toolbar;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.caleydo.core.event.ADirectedEvent;
import org.caleydo.core.event.EventListenerManager.ListenTo;
import org.caleydo.core.view.contextmenu.AContextMenuItem;
import org.caleydo.core.view.contextmenu.GenericContextMenuItem;
import org.caleydo.core.view.contextmenu.item.SeparatorMenuItem;
import org.caleydo.core.view.opengl.layout2.GLElement;
import org.caleydo.core.view.opengl.layout2.GLElementContainer;
import org.caleydo.core.view.opengl.layout2.GLGraphics;
import org.caleydo.core.view.opengl.layout2.basic.GLButton;
import org.caleydo.core.view.opengl.layout2.layout.GLLayouts;
import org.caleydo.core.view.opengl.layout2.layout.IGLLayout2;
import org.caleydo.core.view.opengl.layout2.layout.IGLLayoutElement;
import org.caleydo.core.view.opengl.layout2.renderer.IGLRenderer;
import org.caleydo.view.domino.internal.UndoStack;

/**
 * @author Samuel Gratzl
 *
 */
public abstract class AItemTools extends GLElementContainer implements GLButton.ISelectionCallback, IGLLayout2 {
	protected final UndoStack undo;


	/**
	 * @param selection
	 */
	public AItemTools(UndoStack undo) {
		this.undo = undo;
		setLayout(this);
	}
	@Override
	public boolean doLayout(List<? extends IGLLayoutElement> children, float w, float h, IGLLayoutElement parent,
			int deltaTimeMs) {
		float x = 0;
		for (IGLLayoutElement child : children) {
			float wi = GLLayouts.defaultValue(child.getSetWidth(), h);
			child.setBounds(x, 0, wi, h);
			x += wi + 3;
		}
		return false;
	}

	public float getWidth(float h) {
		float r = 0;
		for (GLElement elem : this) {
			float w = elem.getSize().x();
			if (Float.isNaN(w))
				w = h;
			r += w;
		}
		return r + 3 * (size() - 1);
	}

	/**
	 * @param string
	 * @param iconSortDim
	 */
	protected void addButton(String string, URL iconSortDim) {
		GLButton b = new GLButton();
		b.setCallback(this);
		b.setRenderer(new ImageRenderer(iconSortDim));
		b.setTooltip(string);
		this.add(b);
	}

	/**
	 * tries to convert the contained buttons to context menu items, that trigger {@link TriggerButtonEvent} events
	 *
	 * @param receiver
	 *            event receiver
	 * @param locator
	 *            loader to load the image for a button
	 * @return
	 */
	public List<AContextMenuItem> asContextMenu() {
		List<AContextMenuItem> items = new ArrayList<>(size());
		for (GLElement elem : this) {
			if (elem instanceof GLButton) {
				items.add(asItem((GLButton) elem));
			} else {
				items.add(SeparatorMenuItem.INSTANCE);
			}
		}
		return items;
	}

	private AContextMenuItem asItem(GLButton elem) {
		String label = Objects.toString(elem.getTooltip(), elem.toString());
		ADirectedEvent event = new TriggerButtonEvent(elem).to(this);
		AContextMenuItem item = new GenericContextMenuItem(label, event);
		// if (elem.getMode() == EButtonMode.CHECKBOX) {
		// item.setType(EContextMenuType.CHECK);
		// item.setState(elem.isSelected());
		// }
		URL imagePath = toImagePath(elem.isSelected() ? elem.getSelectedRenderer() : elem.getRenderer());
		item.setImageURL(imagePath);
		return item;
	}

	@ListenTo(sendToMe = true)
	private void onTriggerButton(TriggerButtonEvent event) {
		GLButton b = event.getButton();
		b.setSelected(!b.isSelected());
	}

	private URL toImagePath(IGLRenderer renderer) {
		if (renderer instanceof ImageRenderer) {
			return ((ImageRenderer) renderer).image;
		}
		return null;
	}

	private static class ImageRenderer implements IGLRenderer {
		private final URL image;

		public ImageRenderer(URL image) {
			this.image = image;
		}

		@Override
		public void render(GLGraphics g, float w, float h, GLElement parent) {
			g.fillImage(image, 0, 0, w, h);
		}
	}
}
