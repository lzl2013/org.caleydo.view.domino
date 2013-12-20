/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package org.caleydo.view.domino.internal.ui.prototype;

import java.util.List;

import org.caleydo.core.data.collection.EDimension;
import org.caleydo.view.domino.api.model.typed.TypedGroup;

/**
 * @author Samuel Gratzl
 *
 */
public interface IStratisfyingableNode extends ISortableNode {
	List<TypedGroup> getGroups(EDimension dim);

	boolean isStratisfyable(EDimension dim);
}
