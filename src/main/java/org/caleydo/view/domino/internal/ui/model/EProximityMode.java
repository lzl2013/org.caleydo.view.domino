/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package org.caleydo.view.domino.internal.ui.model;

import java.util.EnumSet;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;

/**
 * @author Samuel Gratzl
 *
 */
public enum EProximityMode implements Predicate<String> {
	ATTACHED("heatmap"), DETACHED("distribution.hist", "boxandwhiskers", "kaplanmaier"), FREE("distribution.pie");

	private final Set<String> include;

	private EProximityMode(String... include) {
		this.include = ImmutableSet.copyOf(include);
	}

	@Override
	public boolean apply(String input) {
		for (EProximityMode mode : EnumSet.range(EProximityMode.ATTACHED, this))
			if (mode.include.contains(input))
				return true;
		return false;
	}

	public static EProximityMode min(EProximityMode a, EProximityMode b) {
		return a.ordinal() < b.ordinal() ? a : b;
	}

	public static EProximityMode min(Iterable<IEdge> edges) {
		EProximityMode m = EProximityMode.FREE;
		for (IEdge edge : edges) {
			m = min(m, edge.asMode());
		}
		return m;
	}
}