/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package org.caleydo.view.domino.internal.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.caleydo.core.data.collection.EDimension;
import org.caleydo.core.view.opengl.layout2.GLElement;
import org.caleydo.core.view.opengl.layout2.GLElementDecorator;
import org.caleydo.core.view.opengl.layout2.IGLElementContext;
import org.caleydo.core.view.opengl.layout2.manage.GLElementDimensionDesc;
import org.caleydo.core.view.opengl.layout2.manage.GLElementFactories;
import org.caleydo.core.view.opengl.layout2.manage.GLElementFactories.GLElementSupplier;
import org.caleydo.core.view.opengl.layout2.manage.GLElementFactoryContext;
import org.caleydo.core.view.opengl.layout2.manage.GLElementFactoryContext.Builder;
import org.caleydo.core.view.opengl.layout2.manage.GLElementFactorySwitcher;
import org.caleydo.core.view.opengl.layout2.manage.GLElementFactorySwitcher.ELazyiness;
import org.caleydo.view.domino.api.model.typed.TypedCollections;
import org.caleydo.view.domino.api.model.typed.TypedList;
import org.caleydo.view.domino.internal.ui.model.NodeUIState;
import org.caleydo.view.domino.internal.ui.prototype.INode;

import com.google.common.base.Function;
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
		this.dimData = node.getData(EDimension.DIMENSION).asList();
		this.recData = node.getData(EDimension.RECORD).asList();
		setLayoutData(node);
		build();
	}

	@Override
	public final T asNode() {
		return node;
	}

	protected abstract String getExtensionID();

	@Override
	public void layout(int deltaTimeMs) {
		if (rebuild) {
			rebuild = false;
			build();
		}
		super.layout(deltaTimeMs);
	}

	private void build() {
		Builder b = GLElementFactoryContext.builder();
		fill(b, dimData, recData);
		ImmutableList<GLElementSupplier> extensions = GLElementFactories.getExtensions(b.build(), "domino."
				+ getExtensionID(), node.getUIState().getProximityMode());
		GLElementFactorySwitcher s = new GLElementFactorySwitcher(extensions, ELazyiness.DESTROY);
		setContent(s);
	}

	@Override
	public GLElement getToolBar() {
		GLElementFactorySwitcher s = getSwitcher();
		if (s == null)
			return null;
		return s.createButtonBarBuilder().build();
	}

	private GLElementFactorySwitcher getSwitcher() {
		GLElementFactorySwitcher s = (GLElementFactorySwitcher) getContent();
		return s;
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
	public boolean setData(EDimension dim, TypedList data) {
		int old = dim.select(dimData, recData).size();
		if (dim.isHorizontal())
			dimData = data;
		else
			recData = data;
		rebuild();
		return old != data.size();
	}

	@Override
	public double getSize(EDimension dim) {
		GLElementDimensionDesc desc = getSwitcher().getActiveDesc(dim);
		return desc.getSize(getDataSafe(dim));
	}

	private int getDataSafe(EDimension dim) {
		TypedList l = dim.select(dimData, recData);
		if (!l.isEmpty())
			return l.size();
		return node.getSize(dim);
	}

	@Override
	protected void init(IGLElementContext context) {
		rebuild();
		node.addPropertyChangeListener(INode.PROP_TRANSPOSE, this);
		node.getUIState().addPropertyChangeListener(NodeUIState.PROP_PROXIMITY_MODE, this);
		super.init(context);
	}

	@Override
	protected void takeDown() {
		node.removePropertyChangeListener(INode.PROP_TRANSPOSE, this);
		node.getUIState().removePropertyChangeListener(NodeUIState.PROP_PROXIMITY_MODE, this);
		super.takeDown();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		switch (evt.getPropertyName()) {
		case INode.PROP_TRANSPOSE:
			rebuild();
			break;
		case NodeUIState.PROP_PROXIMITY_MODE:
			rebuild();
			break;
		}
	}

	private void rebuild() {
		this.rebuild = true;
		relayout();
	}

	public static final Function<INodeUI, INode> TO_NODE = new Function<INodeUI, INode>() {
		@Override
		public INode apply(INodeUI input) {
			return input == null ? null : input.asNode();
		}
	};

}