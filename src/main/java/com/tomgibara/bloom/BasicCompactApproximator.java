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
import com.tomgibara.bits.AbstractBitStore;
import com.tomgibara.bits.BitStore;
import com.tomgibara.bits.BitStore.Positions;
import com.tomgibara.bits.Operation;
import com.tomgibara.collect.EquRel;
import com.tomgibara.hashing.HashCode;
import com.tomgibara.hashing.Hasher;
import com.tomgibara.storage.Store;

class BasicCompactApproximator<K,V> implements CompactApproximator<K, V> {

	private final Hasher<? super K> hasher;
	private final int hashCount;
	private final Lattice<V> storeLattice;
	private final Lattice<V> accessLattice;
	private final Store<V> values;
	private final Store<V> accessValues;
	private CompactBloomFilter bloomFilter = null;

	BasicCompactApproximator(Store<V> values, Lattice<V> lattice, Hasher<? super K> hasher, int hashCount) {
		this.values = values;
		this.storeLattice = lattice;
		this.accessLattice = lattice;
		this.hasher = hasher;
		this.hashCount = hashCount;
		accessValues = newAccessStore();
		clear();
	}
	
	private BasicCompactApproximator(BasicCompactApproximator<K, V> that) {
		storeLattice = that.storeLattice;
		accessLattice = that.accessLattice;
		hasher = that.hasher;
		hashCount = that.hashCount;
		values = that.values.mutableCopy();
		accessValues = newAccessStore();
	}

	private BasicCompactApproximator(BasicCompactApproximator<K, V> that, Lattice<V> accessLattice) {
		storeLattice = that.storeLattice;
		this.accessLattice = accessLattice;
		hasher = that.hasher;
		hashCount = that.hashCount;
		values = that.values.mutableCopy();
		accessValues = newAccessStore();
	}
	
	private Store<V> newAccessStore() {
		if (storeLattice.equals(accessLattice)) return values.immutableView();
		V top = accessLattice.getTop();
		return values.transformedBy(v -> storeLattice.meet(top, v));
	}

	public V put(K key, V value) {
		if (!accessLattice.contains(value)) throw new IllegalArgumentException();
		HashCode code = hasher.hash(key);
		V previous = accessLattice.getTop();
		for (int i = 0; i < hashCount; i++) {
			final int hash = code.intValue();
			final V v = values.get(hash);
			previous = storeLattice.meet(previous, v);
			values.set(hash, storeLattice.join(value, v));
		}
		//assumes putting has resulted in a change
		return previous;
	}
	
	public V getSupremum(K key) {
		HashCode code = hasher.hash(key);
		V value = accessLattice.getTop();
		for (int i = 0; i < hashCount; i++) {
			final V v = values.get(code.intValue());
			value = storeLattice.meet(value, v);
		}
		return value;
	}

	public boolean mightContain(K key) {
		HashCode code = hasher.hash(key);
		V bottom = storeLattice.getBottom();
		EquRel<V> equality = storeLattice.equality();
		for (int i = 0; i < hashCount; i++) {
			if (equality.isEquivalent(values.get(code.intValue()), bottom)) return false;
		}
		return true;
	}
	
	@Override
	public boolean mightContainAll(Iterable<? extends K> keys) {
		for (K key : keys) if (!mightContain(key)) return false;
		return true;
	}
	
	public void clear() {
		values.fill( storeLattice.getBottom() );
	}

	public boolean isEmpty() {
		final V bottom = storeLattice.getBottom();
		EquRel<V> equality = storeLattice.equality();
		//TODO have stores implement iterable?
		for (V value : values.asList()) {
			if (!equality.isEquivalent(value, bottom)) return false;
		}
		return true;
	}
	
	@Override
	public boolean bounds(CompactApproximator<K, V> that) {
		checkCompatibility(that);
		final Store<V> thisValues = accessValues;
		final Store<V> thatValues = that.getValues();
		final int capacity = thisValues.capacity();
		for (int i = 0; i < capacity; i++) {
			if (!storeLattice.isOrdered(thatValues.get(i), thisValues.get(i))) return false;
		}
		return true;
	}
	
	@Override
	public CompactApproximator<K,V> boundedAbove(V upperBound) {
		final Lattice<V> subLattice = accessLattice.boundedAbove(upperBound);
		return subLattice.equals(accessLattice) ? this : new BasicCompactApproximator<K, V>(this, subLattice);
	}
	
	@Override
	public BloomFilter<K> asBloomFilter() {
		return bloomFilter == null ? bloomFilter = new CompactBloomFilter() : bloomFilter;
	}

	@Override
	public Lattice<V> getLattice() {
		return accessLattice;
	}
	
	@Override
	public int getCapacity() {
		return values.size();
	}
	
	@Override
	public int getHashCount() {
		return hashCount;
	}
	
	@Override
	public Hasher<? super K> getHasher() {
		return hasher;
	}

	@Override
	public Store<V> getValues() {
		return accessValues;
	}
	
	// object methods
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof BasicCompactApproximator<?, ?>)) return false;
		BasicCompactApproximator<?, ?> that = (BasicCompactApproximator<?, ?>) obj;
		if (this.getHashCount() != that.getHashCount()) return false;
		if (!this.getHasher().equals(that.getHasher())) return false;
		if (!this.getLattice().equals(that.getLattice())) return false;
		if (!this.getValues().equals(that.getValues())) return false;
		/*
		 * This idea doesn't work, because how do you produce a consistent hashcode - create a bounded lattice with same top & bottom?
		 * must simply be a rule that for equality to be defined, equality in lattice must be consistent with object equality
		 */
		/*
		//compare values as per lattice, not equality
		final V[] thisValues = this.values;
		final List<V> thatValues = (List<V>) that.getValueList();
		//TODO should Lattice.contains be much more forgiving?
		if (thatValues instanceof RandomAccess) {
			for (int i = 0; i < thisValues.length; i++) {
				if (!storeLattice.equalInLattice(thisValues[i], thatValues.get(i))) return false;
			}
		} else {
			final Iterator<V> it = thatValues.iterator();
			for (int i = 0; i < thisValues.length; i++) {
				if (!storeLattice.equalInLattice(thisValues[i], it.next())) return false;
			}
		}
		*/
		return true;
	}

	@Override
	public int hashCode() {
		return getValues().hashCode();
	}
	
	@Override
	public String toString() {
		return getValues().toString();
	}
	
	@Override
	//TODO replace clone with mutability?
	public BasicCompactApproximator<K, V> clone() {
		return new BasicCompactApproximator<K, V>(this);
	}
	
	private void checkCompatibility(CompactApproximator<K, V> that) {
		if (this.hashCount != that.getHashCount()) throw new IllegalArgumentException("Incompatible compact approximator, hashCount was " + that.getHashCount() +", expected " + hashCount);
		if (!this.hasher.equals(that.getHasher())) throw new IllegalArgumentException("Incompatible compact approximator, multiHashes were not equal.");
		if (!this.accessLattice.equals(that.getLattice())) throw new IllegalArgumentException("Incompatible compact approximator, lattices were not equal.");
	}

	private class CompactBloomFilter extends AbstractBloomFilter<K> implements Cloneable {

		final BitStore bits;
		final BitStore publicBits;
		//cached values
		final V top;
		
		CompactBloomFilter() {
			bits = new CompactBits();
			publicBits = bits.immutableView();
			top = accessLattice.getTop();
		}
		
		CompactBloomFilter(BitStore bits, BitStore publicBits, V top) {
			this.bits = bits;
			this.publicBits = publicBits;
			this.top = top;
		}
		
		@Override
		public boolean add(K key) {
			return !accessLattice.equality().isEquivalent(top, put(key, top));
		}

		@Override
		public boolean addAll(BloomFilter<? extends K> filter) {
			Bloom.checkCompatible(this, filter);
			final BitStore thisBits = this.bits;
			final BitStore thatBits = filter.getBits();
			final BitStore combined = Operation.AND.stores(thisBits.flipped(), thatBits);
			Positions positions = combined.ones().positions();
			if (!positions.hasNext()) return false;
			do {
				int i = positions.nextPosition();
				V value = storeLattice.join(top, values.get(i));
				values.set(i, value);
			} while (positions.hasNext());
			return true;
		}

		@Override
		public void clear() {
			BasicCompactApproximator.this.clear();
		}

		@Override
		public BitStore getBits() {
			return publicBits;
		}

		@Override
		public int getHashCount() {
			return hashCount;
		}

		@Override
		public Hasher<? super K> getHasher() {
			return hasher;
		}
		
		@Override
		public BloomFilter<K> clone() {
			// cannot access object clone here??
			return new CompactBloomFilter(bits, publicBits, top);
		}
		
	}

	private class CompactBits extends AbstractBitStore implements BitStore {

		final V top = accessLattice.getTop();

		@Override
		public int size() {
			return values.size();
		}

		@Override
		public boolean getBit(int index) {
			return storeLattice.isOrdered(top, values.get(index));
		}

	}

}
