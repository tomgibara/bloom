package com.tomgibara.bloom;

import com.tomgibara.bits.BitVector;
import com.tomgibara.hashing.HashCode;

class BasicBloomFilter<E> extends AbstractBloomFilter<E> {

	// fields
	
	private final BloomConfig<E> config;
	private final BitVector bits;
	private final BitVector publicBits;
	
	// constructors
	
	BasicBloomFilter(BitVector bits, BloomConfig<E> config) {
		this.config = config;
		this.bits = bits;
		publicBits = bits.immutableView();
	}
	
	private BasicBloomFilter(BasicBloomFilter<E> that) {
		this(that.bits.mutableCopy(), that.config);
	}
	
	// bloom filter methods
	
	@Override
	public void clear() {
		bits.clearWithZeros();
	}

	@Override
	public boolean addAll(BloomFilter<? extends E> filter) {
		Bloom.checkCompatible(this, filter);
		boolean contains = bits.contains().store(filter.bits());
		if (contains) return false;
		bits.or().withStore(filter.bits());
		return true;
	}
	
	@Override
	public boolean add(E element) {
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
	public BitVector bits() {
		return publicBits;
	}
	
	@Override
	public BloomConfig<E> config() {
		return config;
	}
	
	// object methods
	
	@Override
	public BasicBloomFilter<E> clone() {
		return new BasicBloomFilter<E>(this);
	}

}
