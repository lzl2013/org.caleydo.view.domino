/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package org.caleydo.view.domino.internal.ui.prototype;

import java.util.Set;

import org.caleydo.core.data.collection.EDimension;
import org.caleydo.core.id.IDType;
import org.caleydo.core.util.base.ILabeled;
import org.caleydo.core.view.opengl.layout2.GLElement;
import org.caleydo.view.domino.api.model.TypedSet;

/**
 * @author Samuel Gratzl
 *
 */
public interface INode extends ILabeled, Cloneable {
	TypedSet getData(EDimension dim);

	int getSize(EDimension dim);

	IDType getIDType(EDimension dim);

	boolean hasDimension(EDimension dim);

	Set<EDimension> dimensions();

	void transpose();

	/**
	 * @return
	 */
	GLElement createUI();

	INode clone();
}
