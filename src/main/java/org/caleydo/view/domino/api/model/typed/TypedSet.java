/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package org.caleydo.view.domino.api.model.typed;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import org.caleydo.core.data.virtualarray.VirtualArray;
import org.caleydo.core.id.IDType;
import org.caleydo.view.domino.api.model.typed.util.BitSetSet;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;

/**
 * a read only {@link Set} of integers with its {@link IDType}
 *
 * @author Samuel Gratzl
 *
 */
public class TypedSet extends AbstractSet<Integer> implements ITypedCollection {
	private final Set<Integer> wrappee;
	private final IDType idType;

	public TypedSet(Set<Integer> wrappee, IDType idType) {
		this.wrappee = wrappee instanceof TypedSet ? ((TypedSet) wrappee).wrappee : Preconditions.checkNotNull(wrappee);
		this.idType = Preconditions.checkNotNull(idType);
	}

	public TypedSet(TypedSet data) {
		this.wrappee = data.wrappee;
		this.idType = data.idType;
	}

	public static TypedSet of(VirtualArray per) {
		return new TypedSet(BitSetSet.of(per), per.getIdType());
	}

	@Override
	public TypedList asList() {
		return new TypedList(ImmutableList.copyOf(wrappee), idType);
	}

	@Override
	public TypedSet asSet() {
		return this;
	}

	/**
	 * @return the idType, see {@link #idType}
	 */
	@Override
	public IDType getIdType() {
		return idType;
	}

	@Override
	public int size() {
		return wrappee.size();
	}

	@Override
	public boolean isEmpty() {
		return wrappee.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return wrappee.contains(o);
	}

	@Override
	public Iterator<Integer> iterator() {
		return Iterators.unmodifiableIterator(wrappee.iterator());
	}

	@Override
	public Object[] toArray() {
		return wrappee.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return wrappee.toArray(a);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return wrappee.containsAll(c);
	}

	/**
	 * set intersection
	 *
	 * @param that
	 * @return
	 */
	public final TypedSet intersect(TypedSet that) {
		if (this.isEmpty()) // return empty
			return this;
		if (that.isEmpty())
			return that;
		if (!Objects.equals(this.idType, that.idType)) // not matching id types returning empty set
			return new TypedSet(Collections.<Integer> emptySet(), idType);

		if (this.wrappee instanceof BitSetSet)
			return intersection((BitSetSet) wrappee, that);
		if (that.wrappee instanceof BitSetSet)
			return intersection((BitSetSet) that.wrappee, this);

		Set<Integer> r = ImmutableSet.copyOf(intersectImpl(that));
		if (r.size() == this.size()) // all shared
			return this;
		return new TypedSet(r, idType);
	}

	private Set<Integer> intersectImpl(TypedSet that) {
		Set<Integer> r;
		if (this.size() < that.size()) { // smaller at the beginning
			r = Sets.intersection(that.wrappee, this.wrappee);
		} else
			r = Sets.intersection(this.wrappee, that.wrappee);
		return r;
	}

	/**
	 * return the number of shared items
	 *
	 * @param a
	 * @param b
	 * @return
	 */
	public int and(TypedSet that) {
		if (this.isEmpty()) // return empty
			return 0;
		if (that.isEmpty())
			return 0;
		if (!Objects.equals(this.idType, that.idType)) // not matching id types returning empty set
			return 0;
		if (this.wrappee instanceof BitSetSet) {
			return and((BitSetSet) this.wrappee, that);
		}
		if (that.wrappee instanceof BitSetSet)
			return and((BitSetSet) that.wrappee, this);
		return intersectImpl(that).size();
	}

	public TypedSet union(TypedSet that) {
		if (this.isEmpty()) // return empty
			return that;
		if (that.isEmpty())
			return this;
		if (!Objects.equals(this.idType, that.idType)) // not matching id types returning empty set
			return TypedCollections.empty(idType);
		if (this.wrappee instanceof BitSetSet)
			return union((BitSetSet) wrappee, that);
		if (that.wrappee instanceof BitSetSet)
			return union((BitSetSet) that.wrappee, this);

		Set<Integer> r = ImmutableSet.copyOf(unionImpl(that));
		if (r.size() == this.size()) // all shared
			return this;
		else if (r.size() == that.size()) // all shared in b
			return that;
		return new TypedSet(r, idType);
	}

	public int or(TypedSet that) {
		if (this.isEmpty()) // return empty
			return that.size();
		if (this.isEmpty())
			return that.size();
		if (!Objects.equals(this.idType, that.idType)) // not matching id types returning empty set
			return 0;
		if (this.wrappee instanceof BitSetSet) {
			return or((BitSetSet) this.wrappee, that);
		}
		if (that.wrappee instanceof BitSetSet)
			return or((BitSetSet) that.wrappee, this);
		return unionImpl(that).size();
	}

	private Set<Integer> unionImpl(TypedSet that) {
		Set<Integer> r;
		if (this.size() < that.size()) { // smaller at the beginning
			r = Sets.union(that.wrappee, this.wrappee);
		} else
			r = Sets.union(this.wrappee, that.wrappee);
		return r;
	}

	public TypedSet difference(TypedSet that) {
		if (this.isEmpty() || that.isEmpty()) // return empty
			return this;
		if (!Objects.equals(this.idType, that.idType)) // not matching id types
			return this;
		Set<Integer> r = ImmutableSet.copyOf(Sets.difference(this, that));
		if (r.size() == this.size()) // all shared
			return this;
		return new TypedSet(r, idType);
	}

	public int without(TypedSet that) {
		if (this.isEmpty()) // return empty
			return 0;
		if (that.isEmpty())
			return that.size();
		if (!Objects.equals(this.idType, that.idType)) // not matching id types
			return this.size();
		if (this.wrappee instanceof BitSetSet) {
			return without((BitSetSet) this.wrappee, that.wrappee);
		}
		return Sets.difference(this.wrappee, that.wrappee).size();
	}

	public static TypedSet intersection(BitSetSet a, TypedSet b) {
		if (b.wrappee instanceof BitSetSet) {
			return new TypedSet(BitSetSet.and(a, ((BitSetSet) b.wrappee)), b.idType);
		}
		return new TypedSet(ImmutableSet.copyOf(Sets.intersection(b, a)), b.idType); // as the predicate is: in the
																						// second argument
	}

	private static TypedSet union(BitSetSet a, TypedSet b) {
		if (b.wrappee instanceof BitSetSet) {
			return new TypedSet(BitSetSet.or(a, (BitSetSet) b.wrappee), b.idType);
		}
		return new TypedSet(ImmutableSet.copyOf(Sets.union(b, a)), b.idType); // as the predicate is: in the second
																				// argument
	}

	private static int and(BitSetSet a, Set<Integer> b) {
		if (b instanceof BitSetSet) {
			BitSetSet s = BitSetSet.and(a, (BitSetSet) b);
			return s.size();
		}
		return Sets.intersection(b, a).size(); // as the predicate is: in the second argument
	}

	private static int or(BitSetSet a, Set<Integer> b) {
		if (b instanceof BitSetSet) {
			BitSetSet s = BitSetSet.or(a, (BitSetSet) b);
			return s.size();
		}
		return Sets.union(b, a).size(); // as the predicate is: in the second argument
	}

	private static int without(BitSetSet a, Set<Integer> b) {
		if (b instanceof BitSetSet) {
			BitSetSet s = BitSetSet.andNot(a, (BitSetSet) b);
			return s.size();
		}
		return Sets.difference(a, b).size(); // as the predicate is: in the second argument
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((idType == null) ? 0 : idType.hashCode());
		result = prime * result + ((wrappee == null) ? 0 : wrappee.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		TypedSet other = (TypedSet) obj;
		return Objects.equals(idType, other.idType) && Objects.equals(wrappee, other.wrappee);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TypedSet [idType=");
		builder.append(idType);
		builder.append(", wrappee=");
		builder.append(wrappee);
		builder.append("]");
		return builder.toString();
	}

	/**
	 * @param same
	 * @return
	 */
	public static TypedSet union(Iterable<? extends TypedSet> same) {
		Builder<Integer> b = ImmutableSet.builder();
		for (TypedSet s : same)
			b.addAll(s.wrappee);
		return new TypedSet(b.build(), same.iterator().next().getIdType());

	}

	/**
	 * @param same
	 * @return
	 */
	public static TypedSet intersection(Iterable<? extends TypedSet> same) {
		Set<Integer> r = new HashSet<>(same.iterator().next());
		for (TypedSet s : same)
			r.retainAll(s);
		return new TypedSet(r, same.iterator().next().getIdType());

	}

}
