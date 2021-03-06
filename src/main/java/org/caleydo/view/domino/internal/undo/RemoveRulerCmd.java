/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package org.caleydo.view.domino.internal.undo;

import org.caleydo.view.domino.internal.Domino;
import org.caleydo.view.domino.internal.ui.Ruler;

/**
 * @author Samuel Gratzl
 *
 */
public class RemoveRulerCmd implements ICmd {
	private final Ruler ruler;

	public RemoveRulerCmd(Ruler ruler) {
		this.ruler = ruler;
	}

	@Override
	public String getLabel() {
		return "Remove Ruler: " + ruler;
	}

	@Override
	public ICmd run(Domino domino) {
		domino.removeRuler(ruler);
		return new AddRulerCmd(ruler);
	}

}
