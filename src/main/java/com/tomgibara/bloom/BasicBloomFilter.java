package com.tomgibara.bloom;

import com.tomgibara.bits.BitVector;
import com.tomgibara.hashing.HashCode;
import com.tomgibara.hashing.Hasher;

class BasicBloomFilter<E> extends AbstractBloomFilter<E> {

	// fields
	
	private final Hasher<? super E> hasher;
	private final int hashCount;
	private final BitVector bits;
	private final BitVector publicBits;
	
	// constructors
	
	BasicBloomFilter(BitVector bits, Hasher<? super E> hasher, int hashCount) {
		this.bits = bits;
		this.hasher = hasher;
		this.hashCount = hashCount;
		publicBits = bits.immutableView();
	}
	
	private BasicBloomFilter(BasicBloomFilter<E> that) {
		this(that.bits.mutableCopy(), that.hasher, that.hashCount);
	}
	
	// bloom filter methods
	
	@Override
	public void clear() {
		bits.set(false);
	}

	@Override
	public boolean addAll(BloomFilter<? extends E> filter) {
		Bloom.checkCompatible(this, filter);
		boolean contains = bits.testContains(filter.getBitVector());
		if (contains) return false;
		bits.orVector(filter.getBitVector());
		return true;
	}
	
	@Override
	public boolean add(E element) {
		HashCode hash = hasher.hash(element);
		boolean mutated = false;
		for (int i = 0; i < hashCount; i++) {
			final int code = hash.intValue();
			if (mutated) {
				bits.setBit(code, true);
			} else if (!bits.getBit(code)) {
				bits.setBit(code, true);
				mutated = true;
			}
		}
		return mutated;
	}
	
	@Override
	public BitVector getBitVector() {
		return publicBits;
	}
	
	@Override
	public int getHashCount() {
		return hashCount;
	}
	
	@Override
	public Hasher<? super E> getHasher() {
		return hasher;
	}
	
	// object methods
	
	@Override
	public BasicBloomFilter<E> clone() {
		return new BasicBloomFilter<E>(this);
	}

}
