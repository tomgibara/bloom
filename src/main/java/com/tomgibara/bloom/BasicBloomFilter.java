package com.tomgibara.bloom;

import com.tomgibara.bits.BitStore;
import com.tomgibara.hashing.HashCode;

class BasicBloomFilter<E> extends AbstractBloomFilter<E> {

	// fields

	private final BloomConfig<E> config;
	private final BitStore bits;
	private final BitStore publicBits;

	// constructors

	BasicBloomFilter(BitStore bits, BloomConfig<E> config) {
		this.config = config;
		this.bits = bits;
		publicBits = bits.immutableView();
	}

	// bloom filter methods

	@Override
	public void clear() {
		bits.clearWithZeros();
	}

	@Override
	public boolean addAll(BloomFilter<? extends E> filter) {
		Bloom.checkCompatible(this, filter);
		checkMutable();
		boolean contains = bits.contains().store(filter.bits());
		if (contains) return false;
		bits.or().withStore(filter.bits());
		return true;
	}
	
	@Override
	public boolean add(E element) {
		checkMutable();
		HashCode hash = config.hasher().hash(element);
		int hashCount = config.hashCount();
		int i = 0;
		for (; i < hashCount; i++) {
			if (!bits.getThenSetBit(hash.intValue(), true)) break;
		}
		if (i == hashCount) return false;
		for (; i < hashCount; i++) {
			bits.setBit(hash.intValue(), true);
		}
		return true;
	}
	
	@Override
	public BitStore bits() {
		return publicBits;
	}
	
	@Override
	public BloomConfig<E> config() {
		return config;
	}
	
	// mutability methods

	@Override
	public boolean isMutable() {
		return bits.isMutable();
	}

	// private helper methods
	
	private void checkMutable() {
		if (!isMutable()) throw new IllegalStateException("immutable");
	}

}
