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
 * Operations involving two <code>BloomFilter</code>s are generally only defined
 * for compatible instances. Two <code>BloomFilter</code> instances are
 * compatible if they have the same capacity, hashCount and equal multiHashes.
 * </p>
 * 
 * <p>
 * Two <code>BloomFilter</code>s are equal if they are compatible and their
 * bitVectors are equal.
 * </p>
 * 
 * @author Tom Gibara
 * 
 * @param <E>
 *            the type of element stored in the bloom filter
 */

public interface BloomFilter<E> extends Mutability<BloomFilter<E>> {

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
	 * Whether the Bloom filter might contain the specified element. As per the
	 * characteristics of Bloom filters this method may return true for elements
	 * that were never explicitly added to the filter, though it will never
	 * return false for an element that was.
	 * 
	 * @param element
	 *            an element that could be in the filter
	 * @return false only if the element was never added to the filter
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
	 * Adds an element to the filter. Null elements should be supported if the
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
	 * Adds every element of the supplied iterable to the filter.
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
	 * Whether the filter is empty.
	 * 
	 * @return true if the filter has never had an element added to it, false
	 *         otherwise
	 */
	
	default boolean isEmpty() {
		return bits().zeros().isAll();
	}

	/**
	 * Removes all elements from the filter.
	 */
	
	default void clear() {
		throw new IllegalStateException("immutable");
	}
	
	/**
	 * Whether the Bloom filter might contain all the elements in the supplied
	 * iterable.
	 * 
	 * @param elements
	 *            elements that may be contained in the filter
	 * @return true if the filter might contain every element of the iterable,
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
	 * return true for an element that was not added to the filter. This
	 * estimate will change as the number of elements in the filter increases
	 * and is based on an assumption that hashing is optimal.
	 * 
	 * @return a probability between 0 and 1 inclusive
	 */
	
	default double getFalsePositiveProbability() {
		return Math.pow( (double) bits().ones().count() / bits().size(), config().hashCount());
	}

	/**
	 * Whether the Bloom filter contains all of the elements contained in
	 * another compatible bloom filter
	 * 
	 * @param filter
	 *            a compatible Bloom filter
	 * @return true if every element of the compatible filter is necessarily
	 *         contained in this filter, false otherwise
	 * @throws IllegalArgumentException
	 *             if the supplied filter is null or not compatible with this
	 *             filter
	 */
	
	default boolean containsAll(BloomFilter<?> filter) throws IllegalArgumentException {
		Bloom.checkCompatible(this, filter);
		return bits().contains().store(filter.bits());
	}

	/**
	 * Returns an immutable {@link BloomFilter} that contains an element if and
	 * only if the element cannot be present in this filter without also being
	 * present in the supplied filter. If the returned filter is full, then the
	 * supplied filter contains all the elements of this filter, that is to say
	 * <code>containsAll(filter)<code> is true.
	 * 
	 * @param filter
	 *            a compatible Bloom filter
	 * @return a Bloom filter containing all elements on which the supplied
	 *         filter bounds this filter
	 * @throws IllegalArgumentException
	 *             if the supplied filter is null or not compatible with this
	 *             filter
	 */

	default BloomFilter<E> boundedBy(BloomFilter<E> filter) throws IllegalArgumentException {
		Bloom.checkCompatible(this, filter);
		BitStore thisBits = this.bits();
		BitStore thatBits = filter.bits();
		BitStore bits = new BitStore() {
			final int size = thisBits.size();
			@Override public boolean getBit(int index) { return !thisBits.getBit(index) || thatBits.getBit(index); }
			@Override public int size() { return size; }
		};
		return new BloomFilter<E>() {

			@Override
			public BloomConfig<E> config() {
				return BloomFilter.this.config();
			}

			@Override
			public BitStore bits() {
				return bits;
			}
		};
	}
	
	/**
	 * Adds all of the elements of a compatible bloom filter.
	 * 
	 * @param filter
	 *            a compatible Bloom filter
	 * @return true if the state of the bloom filter was modified by the
	 *         operation, false otherwise
	 * @throws IllegalArgumentException
	 *             if the supplied filter is null or not compatible with this
	 *             filter
	 */
	
	default boolean addAll(BloomFilter<? extends E> filter) throws IllegalArgumentException {
		throw new IllegalStateException("immutable");
	}

	// mutability methods

	@Override
	default boolean isMutable() {
		return false;
	}

	@Override
	default BloomFilter<E> immutableCopy() {
		return new BasicBloomFilter<>(bits().immutableCopy(), config());
	}

	@Override
	default BloomFilter<E> mutableCopy() {
		return new BasicBloomFilter<>(bits().mutableCopy(), config());
	}
	
	@Override
	default BloomFilter<E> immutableView() {
		return new BasicBloomFilter<>(bits().immutable(), config());
	}
}
