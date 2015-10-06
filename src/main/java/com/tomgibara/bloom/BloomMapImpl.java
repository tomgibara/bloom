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
import com.tomgibara.storage.Store;

class BloomMapImpl<K,V> implements BloomMap<K, V> {

	private BloomConfig<K> config;
	private final Lattice<V> storeLattice;
	private final Lattice<V> accessLattice;
	private final Store<V> values;
	private final Store<V> accessValues;
	private MapBloomSet bloomSet = null;

	BloomMapImpl(BloomConfig<K> config, Store<V> values, Lattice<V> lattice) {
		this.config = config;
		this.values = values;
		this.storeLattice = lattice;
		this.accessLattice = lattice;
		accessValues = newAccessStore();
		clear();
	}
	
	private BloomMapImpl(BloomMapImpl<K, V> that, Lattice<V> accessLattice) {
		storeLattice = that.storeLattice;
		this.accessLattice = accessLattice;
		config = that.config;
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
		checkMutable();
		HashCode code = config.hasher().hash(key);
		int hashCount = config.hashCount();
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
		HashCode code = config.hasher().hash(key);
		int hashCount = config.hashCount();
		V value = accessLattice.getTop();
		for (int i = 0; i < hashCount; i++) {
			final V v = values.get(code.intValue());
			value = storeLattice.meet(value, v);
		}
		return value;
	}

	public boolean mightContain(K key) {
		HashCode code = config.hasher().hash(key);
		int hashCount = config.hashCount();
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
		checkMutable();
		values.fill( storeLattice.getBottom() );
	}

	public boolean isEmpty() {
		return isAll(storeLattice.getBottom());
	}
	
	public boolean isFull() {
		return isAll(storeLattice.getTop());
	}
	
	@Override
	public boolean bounds(BloomMap<K, V> that) {
		Bloom.checkCompatible(this, that);
		final Store<V> thisValues = accessValues;
		final Store<V> thatValues = that.values();
		final int capacity = thisValues.size();
		for (int i = 0; i < capacity; i++) {
			if (!storeLattice.isOrdered(thatValues.get(i), thisValues.get(i))) return false;
		}
		return true;
	}

//	@Override
//	public BloomSet<K> boundedBy(BloomMap<K, V> that) {
//		Bloom.checkCompatible(this, that);
//		Store<V> thisValues = this.values();
//		Store<V> thatValues = that.values();
//		BitStore bits = new BitStore() {
//			private final int size = thisValues.capacity();
//			@Override public boolean getBit(int index) { return storeLattice.isOrdered(thatValues.get(index), thisValues.get(index)); }
//			@Override public int size() { return size; }
//		};
//		return new BloomSet<K>() {
//			private final BloomConfig<K> config = BloomMapImpl.this.config();
//			@Override public BloomConfig<K> config() { return config; }
//			@Override public BitStore bits() { return bits; }
//		};
//	}

	@Override
	public BloomMap<K,V> boundedAbove(V upperBound) {
		final Lattice<V> subLattice = accessLattice.boundedAbove(upperBound);
		return subLattice.equals(accessLattice) ? this : new BloomMapImpl<K, V>(this, subLattice);
	}
	
	@Override
	public BloomSet<K> asBloomSet() {
		return bloomSet == null ? bloomSet = new MapBloomSet() : bloomSet;
	}

	@Override
	public Lattice<V> lattice() {
		return accessLattice;
	}
	
	@Override
	public BloomConfig<K> config() {
		return config;
	}
	
	@Override
	public Store<V> values() {
		return accessValues;
	}

	// mutability
	
	@Override
	public boolean isMutable() {
		return values.isMutable();
	}
	
	@Override
	public BloomMap<K, V> immutableCopy() {
		return new BloomMapImpl<>(config, values.immutableCopy(), storeLattice);
	}
	
	@Override
	public BloomMap<K, V> mutableCopy() {
		return new BloomMapImpl<>(config, values.mutableCopy(), storeLattice);
	}
	
	@Override
	public BloomMap<K, V> immutableView() {
		return new BloomMapImpl<>(config, values.immutable(), storeLattice);
	}
	
	// object methods
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof BloomMapImpl<?, ?>)) return false;
		BloomMapImpl<?, ?> that = (BloomMapImpl<?, ?>) obj;
		if (!this.config().equals(that.config())) return false;
		if (!this.lattice().equals(that.lattice())) return false;
		if (!this.values().equals(that.values())) return false;
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
		return values().hashCode();
	}
	
	@Override
	public String toString() {
		return values().toString();
	}
	
	// private utility methods
	
	private void checkMutable() {
		if (!isMutable()) throw new IllegalStateException("immutable");
	}

	private boolean isAll(V v) {
		EquRel<V> equality = storeLattice.equality();
		//TODO have stores implement iterable?
		for (V value : values.asList()) {
			if (!equality.isEquivalent(value, v)) return false;
		}
		return true;
	}
	
	// inner classes
	
	private class MapBloomSet extends AbstractBloomSet<K> implements Cloneable {

		final BitStore bits;
		final BitStore publicBits;
		//cached values
		final V top;
		
		MapBloomSet() {
			top = accessLattice.getTop();
			bits = new AbstractBitStore() {

				@Override
				public int size() {
					return values.count();
				}

				@Override
				public boolean getBit(int index) {
					return storeLattice.isOrdered(top, values.get(index));
				}

				@Override
				public void setBit(int index, boolean value) {
					if (!value) throw new IllegalArgumentException("cannot clear bits");
					values.set(index, storeLattice.join(top, values.get(index)));
				}

				@Override
				public void clearWithZeros() {
					clear();
				}

				@Override
				public boolean isMutable() {
					return BloomMapImpl.this.isMutable();
				}

			};
			publicBits = bits.immutable();
		}
		
		@Override
		public boolean add(K key) {
			return !accessLattice.equality().isEquivalent(top, put(key, top));
		}

		@Override
		public boolean addAll(BloomSet<? extends K> set) {
			Bloom.checkCompatible(this, set);
			final BitStore thisBits = this.bits;
			final BitStore thatBits = set.bits();
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
			BloomMapImpl.this.clear();
		}

		@Override
		public BitStore bits() {
			return publicBits;
		}

		@Override
		public BloomConfig<K> config() {
			return config;
		}

		@Override
		public boolean isMutable() {
			return BloomMapImpl.this.isMutable();
		}
	}

}
