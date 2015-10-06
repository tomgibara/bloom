/*
 * Copyright 2011 Tom Gibara
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

import com.tomgibara.algebra.lattice.Lattice;
import com.tomgibara.fundament.Mutability;
import com.tomgibara.storage.Store;

public interface BloomMap<K, V> extends Mutability<BloomMap<K, V>> {

	// accessors

	BloomConfig<K> config();
	
	Lattice<V> lattice();

	Store<V> values();

	// collection-like methods
	
	V put(K key, V value);

	V getSupremum(K key);
	
	boolean mightContain(K key);

	boolean mightContainAll(Iterable<? extends K> keys);
	
	void clear();

	boolean isEmpty();

	boolean isFull();
	
	// bloom methods

	boolean bounds(BloomMap<K, V> ca);
	
	//TODO understand how to implement this
	//BloomSet<K> boundedBy(BloomMap<K, V> ca);
	
	BloomMap<K, V> boundedAbove(V upperBound);
	
	//bit true if corresponding value attains top
	BloomSet<K> asBloomSet();

	// bit true if not mapped to bottom
	BloomSet<K> keys();
}