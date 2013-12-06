/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package org.caleydo.view.domino.api.model;

import java.util.Collection;

/**
 * @author Samuel Gratzl
 *
 */
public interface ITypedCollection extends Collection<Integer>, IHasIDType {

	TypedList asList();
}
