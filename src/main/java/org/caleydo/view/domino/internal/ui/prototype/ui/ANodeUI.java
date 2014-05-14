/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package org.caleydo.view.domino.internal.ui.prototype.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.caleydo.core.data.collection.EDimension;
import org.caleydo.core.view.opengl.layout2.GLElement;
import org.caleydo.core.view.opengl.layout2.GLElementDecorator;
import org.caleydo.core.view.opengl.layout2.IGLElementContext;
import org.caleydo.core.view.opengl.layout2.manage.GLElementFactories;
import org.caleydo.core.view.opengl.layout2.manage.GLElementFactories.GLElementSupplier;
import org.caleydo.core.view.opengl.layout2.manage.GLElementFactoryContext;
import org.caleydo.core.view.opengl.layout2.manage.GLElementFactoryContext.Builder;
import org.caleydo.core.view.opengl.layout2.manage.GLElementFactorySwitcher;
import org.caleydo.core.view.opengl.layout2.manage.GLElementFactorySwitcher.ELazyiness;
import org.caleydo.view.domino.api.model.typed.TypedCollections;
import org.caleydo.view.domino.api.model.typed.TypedList;
import org.caleydo.view.domino.internal.ui.prototype.INode;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

/**
 * @author Samuel Gratzl
 *
 */
public abstract class ANodeUI<T extends INode> extends GLElementDecorator implements PropertyChangeListener, INodeUI,
		Predicate<String> {
	protected final T node;

	private TypedList dimData = TypedCollections.INVALID_LIST;
	private TypedList recData = TypedCollections.INVALID_LIST;
	private boolean rebuild = true;

	public ANodeUI(T node) {
		this.node = node;
		setLayoutData(node);
	}

	protected abstract String getExtensionID();

	@Override
	public void layout(int deltaTimeMs) {
		if (rebuild) {
			rebuild = false;
			Builder b = GLElementFactoryContext.builder();
			fill(b, dimData, recData);
			ImmutableList<GLElementSupplier> extensions = GLElementFactories.getExtensions(b.build(), "domino."
					+ getExtensionID(),
					this);
			GLElementFactorySwitcher s = new GLElementFactorySwitcher(extensions, ELazyiness.DESTROY);
			setContent(s);
		}
		super.layout(deltaTimeMs);
	}


	protected abstract void fill(Builder b, TypedList dim, TypedList rec);

	@Override
	public boolean apply(String input) {
		return true;
	}

	@Override
	public final GLElement asGLElement() {
		return this;
	}

	@Override
	public void setData(EDimension dim, TypedList data) {
		if (dim.isHorizontal())
			dimData = data;
		else
			recData = data;
		rebuild();
	}

	@Override
	protected void init(IGLElementContext context) {
		rebuild();
		node.addPropertyChangeListener(INode.PROP_TRANSPOSE, this);
		super.init(context);
	}

	@Override
	protected void takeDown() {
		node.removePropertyChangeListener(INode.PROP_TRANSPOSE, this);
		super.takeDown();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		switch (evt.getPropertyName()) {
		case INode.PROP_TRANSPOSE:
			rebuild();
			break;
		}
	}

	private void rebuild() {
		this.rebuild = true;
		relayout();
	}

}