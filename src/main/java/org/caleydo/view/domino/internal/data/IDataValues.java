/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package org.caleydo.view.domino.internal.data;

import java.util.Collection;

import org.caleydo.core.data.collection.EDimension;
import org.caleydo.core.util.base.ILabeled;
import org.caleydo.core.view.opengl.layout2.manage.GLElementFactoryContext.Builder;
import org.caleydo.core.view.opengl.util.gleem.IColored;
import org.caleydo.view.domino.api.model.typed.ITypedCollection;
import org.caleydo.view.domino.api.model.typed.TypedGroupSet;
import org.caleydo.view.domino.api.model.typed.TypedList;

import com.google.common.base.Predicate;

/**
 * @author Samuel Gratzl
 *
 */
public interface IDataValues extends ILabeled, IColored, Predicate<String> {

	TypedGroupSet getDefaultGroups(EDimension dim);

	int compare(EDimension dim, int a, int b, ITypedCollection otherData);

	/**
	 * @return
	 */
	String getExtensionID();

	/**
	 * @param b
	 * @param dimData
	 * @param recData
	 * @param mediumTranspose TODO
	 */
	void fill(Builder b, TypedList dimData, TypedList recData, boolean[] existNeigbhor, boolean mediumTranspose);

	Collection<String> getDefaultVisualization();

	/**
	 * filter for extension ids
	 */
	@Override
	boolean apply(String input);

	void onSelectionChanged(boolean selected);
}
