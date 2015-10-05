/*
 * Copyright 2010 Tom Gibara
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.tomgibara.bloom;

import com.tomgibara.bits.BitStore;
import com.tomgibara.fundament.Mutability;
import com.tomgibara.hashing.HashCode;

/**
 * <p>
 * A Bloom filter. See <a href="http://en.wikipedia.org/wiki/Bloom_filter">the
 * Wikipedia article on Bloom filters</a>.
 * </p>
 * 
 * <p>
 * Operations involving two <code>BloomSet</code>s are generally only defined
 * for compatible instances. Two <code>BloomSet</code> instances are
 * compatible if they have the same {@link BloomConfig}.
 * </p>
 * 
 * <p>
 * Two <code>BloomSet</code>s are equal if they are compatible and their
 * {@link #bits()} are equal.
 * </p>
 * 
 * @author Tom Gibara
 * 
 * @param <E>
 *            the type of element stored in the bloom filter
 */

public interface BloomSet<E> extends Mutability<BloomSet<E>> {

	// accessors

	BloomConfig<E> config();
	
	/**
	 * The bits of the Bloom filter. The returned {@link BitStore} is a live
	 * view of the filter's state and will mutate as items are added to the
	 * filter, but is immutable and may not be mutated externally.
	 * 
	 * @return the state of the filter, never null
	 */
	
	BitStore bits();

	// collection-like methods

	/**
	 * Whether the Bloom set might contain the specified element. As per the
	 * characteristics of Bloom filters this method may return true for elements
	 * that were never explicitly added to the set, though it will never
	 * return false for an element that was.
	 * 
	 * @param element
	 *            an element that could be in the set
	 * @return false only if the element was never added to the set
	 */
	
	default boolean mightContain(E element) {
		int hashCount = config().hashCount();
		BitStore bitStore = bits();
		HashCode hash = config().hasher().hash(element);
		for (int i = 0; i < hashCount; i++) {
			if (!bitStore.getBit(hash.intValue())) return false;
		}
		return true;
	}

	/**
	 * Adds an element to the set. Null elements should be supported if the
	 * underlying {@link MultiHash} supports nulls.
	 * 
	 * @param newElement
	 *            the element to add
	 * @return true if the state of the bloom filter was modified by the
	 *         operation, false otherwise
	 */
	
	default boolean add(E newElement) {
		throw new IllegalStateException("immutable");
	}

	/**
	 * Adds every element of the supplied iterable to the set.
	 * 
	 * @param elements
	 *            the elements to add, not null
	 * @return true if the state of the bloom filter was modified by the
	 *         operation, false otherwise
	 * @throws IllegalArgumentException
	 *             if a null iterable is supplied
	 */
	
	default boolean addAll(Iterable<? extends E> elements) throws IllegalArgumentException {
		if (elements == null) throw new IllegalArgumentException("null elements");
		boolean mutated = false;
		for (E element : elements) if ( add(element) ) mutated = true;
		return mutated;
	}

	/**
	 * Whether the set is empty.
	 * 
	 * @return true if the set has never had an element added to it, false
	 *         otherwise
	 */
	
	default boolean isEmpty() {
		return bits().zeros().isAll();
	}
	
	/**
	 * Whether the set is full. This occurs when every bit of the Bloom filter
	 * is set and {@link #mightContain(Object)} returns <code>true</code> for
	 * all values.
	 * 
	 * @return true if the set contains all elements, false otherwise
	 */
	default boolean isFull() {
		return bits().ones().isAll();
	}

	/**
	 * Removes all elements from the set.
	 */
	
	default void clear() {
		throw new IllegalStateException("immutable");
	}
	
	/**
	 * Whether the set might contain all the elements in the supplied iterable.
	 * 
	 * @param elements
	 *            elements that may be contained in the set
	 * @return true if the set might contain every element of the iterable,
	 *         false otherwise
	 * @throws IllegalArgumentException
	 *             if a null iterable is supplied
	 */
	
	default boolean mightContainAll(Iterable<? extends E> elements) throws IllegalArgumentException {
		if (elements == null) throw new IllegalArgumentException("null elements");
		for (E element : elements) if (!mightContain(element)) return false;
		return true;
	}
	
	// bloom methods

	/**
	 * An estimate of the probability that {@link #mightContain(Object)} will
	 * return true for an element that was not added to the set. This estimate
	 * will change as the number of elements in the filter increases and is
	 * based on an assumption that hashing is optimal.
	 * 
	 * @return a probability between 0 and 1 inclusive
	 */
	
	default double getFalsePositiveProbability() {
		return Math.pow( (double) bits().ones().count() / bits().size(), config().hashCount());
	}

	/**
	 * Whether the set contains all of the elements contained in
	 * another compatible Bloom set.
	 * 
	 * @param set
	 *            a compatible Bloom set
	 * @return true if every element of the compatible set is necessarily
	 *         contained in this set, false otherwise
	 * @throws IllegalArgumentException
	 *             if the supplied set is null or not compatible with this
	 *             set
	 */
	
	default boolean containsAll(BloomSet<?> set) throws IllegalArgumentException {
		Bloom.checkCompatible(this, set);
		return bits().contains().store(set.bits());
	}

	/**
	 * Returns an immutable {@link BloomSet} that contains an element if and
	 * only if the element cannot be present in this set without also being
	 * present in the supplied set. If the returned set is full, then the
	 * supplied set contains all the elements of this set.
	 * 
	 * @param set
	 *            a compatible Bloom set
	 * @return a Bloom set containing all elements on which the supplied
	 *         set bounds this one
	 * @throws IllegalArgumentException
	 *             if the supplied set is null or not compatible with this
	 *             set
	 */

	default BloomSet<E> boundedBy(BloomSet<E> set) throws IllegalArgumentException {
		Bloom.checkCompatible(this, set);
		BitStore thisBits = this.bits();
		BitStore thatBits = set.bits();
		BitStore bits = new BitStore() {
			private final int size = thisBits.size();
			@Override public boolean getBit(int index) { return !thisBits.getBit(index) || thatBits.getBit(index); }
			@Override public int size() { return size; }
		};
		return new BloomSet<E>() {
			private final BloomConfig<E> config = BloomSet.this.config();
			@Override public BloomConfig<E> config() { return config; }
			@Override public BitStore bits() { return bits; }
		};
	}
	
	/**
	 * Adds all of the elements of a compatible bloom set.
	 * 
	 * @param set
	 *            a compatible Bloom set
	 * @return true if the state of the bloom set was modified by the
	 *         operation, false otherwise
	 * @throws IllegalArgumentException
	 *             if the supplied set is null or not compatible with this
	 *             set
	 */
	
	default boolean addAll(BloomSet<? extends E> set) throws IllegalArgumentException {
		throw new IllegalStateException("immutable");
	}

	// mutability methods

	@Override
	default boolean isMutable() {
		return false;
	}

	@Override
	default BloomSet<E> immutableCopy() {
		return new BloomSetImpl<>(bits().immutableCopy(), config());
	}

	@Override
	default BloomSet<E> mutableCopy() {
		return new BloomSetImpl<>(bits().mutableCopy(), config());
	}
	
	@Override
	default BloomSet<E> immutableView() {
		return new BloomSetImpl<>(bits().immutable(), config());
	}
}
