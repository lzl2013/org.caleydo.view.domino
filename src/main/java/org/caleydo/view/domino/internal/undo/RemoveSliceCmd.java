/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package org.caleydo.view.domino.internal.undo;

import java.util.List;

import org.caleydo.core.data.collection.EDimension;
import org.caleydo.view.domino.api.model.typed.TypedGroupSet;
import org.caleydo.view.domino.internal.Block;
import org.caleydo.view.domino.internal.Domino;
import org.caleydo.view.domino.internal.Node;
import org.caleydo.view.domino.internal.NodeGroup;

/**
 * @author Samuel Gratzl
 *
 */
public class RemoveSliceCmd implements ICmd {
	private EDimension dim;
	private NodeGroup toRemove;

	public RemoveSliceCmd(EDimension dim, NodeGroup toRemove) {
		this.dim = dim;
		this.toRemove = toRemove;
	}

	@Override
	public ICmd run(Domino domino) {
		final Node node = toRemove.getNode();
		Block block = node.getBlock();
		List<TypedGroupSet> bak = block.removeSlice(node, dim, toRemove);

		return new UndoRemoveSliceCmd(bak);
	}

	private class UndoRemoveSliceCmd implements ICmd {
		private final List<TypedGroupSet> ori;

		public UndoRemoveSliceCmd(List<TypedGroupSet> bak) {
			this.ori = bak;
		}

		@Override
		public ICmd run(Domino domino) {
			final Node node = toRemove.getNode();
			Block block = node.getBlock();
			block.restoreSlice(node, dim, ori);
			return RemoveSliceCmd.this;
		}

		@Override
		public String getLabel() {
			return "Undo Remove Groups";
		}

	}

	@Override
	public String getLabel() {
		return "Remove Slice";
	}

}
