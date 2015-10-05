package com.tomgibara.bloom;

import com.tomgibara.hashing.HashSize;
import com.tomgibara.hashing.Hasher;

public final class BloomConfig<E> {

	private static void check(Hasher<?> hasher, int hashCount) {
		if (hasher == null) throw new IllegalArgumentException("null hasher");
		if (hashCount < 1) throw new IllegalArgumentException("hashCount not positive");
		if (hashCount > hasher.getQuantity()) throw new IllegalArgumentException("hashCount exceeds hasher quantity");
	}
	
	private final Hasher<? super E> hasher;
	private final int hashCount;

	private final HashSize size;
	private final int capacity;

	public BloomConfig(int capacity, Hasher<? super E> hasher, int hashCount) {
		check(hasher, hashCount);
		if (capacity < 0) throw new IllegalArgumentException("negative capacity");
		this.capacity = capacity;
		size = HashSize.fromInt(capacity);
		int c = hasher.getSize().compareTo(size);
		if (c < 0) throw new IllegalArgumentException("hash size too small");
		this.hasher = c > 0 ? hasher.sized(size) : hasher;
		this.hashCount = hashCount;
	}

	public BloomConfig(Hasher<? super E> hasher, int hashCount) {
		check(hasher, hashCount);

		this.hasher = hasher;
		this.hashCount = hashCount;
		this.size = hasher.getSize();
		this.capacity = size.asInt();
	}

	/**
	 * The number of bits that support the collection. This value will match the
	 * size of the stores returned by {@link BloomSet#bits()} and
	 * {@link BloomMap#values()}.
	 * 
	 * @return the number of bits backing the collection, always positive
	 */
	public int capacity() { return capacity; }

	/**
	 * The hasher that generates hash values for the Bloom collection.
	 * 
	 * @return a Hasher instance, never null
	 */
	
	public Hasher<? super E> hasher() { return hasher; }

	/**
	 * The number of hashes used to mark bits in the Bloom collection.
	 * 
	 * @return the hash count, always positive
	 */
	public int hashCount() { return hashCount; }

	public BloomConfig<E> withCapacity(int capacity) {
		return capacity == this.capacity ? this : new BloomConfig<>(capacity, hasher, hashCount);
	}
	
	@Override
	public int hashCode() {
		return hashCount + hasher.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof BloomConfig<?>)) return false;
		BloomConfig<?> that = (BloomConfig<?>) obj;
		if (this.hashCount != that.hashCount) return false;
		if (!this.hasher.equals(that.hasher)) return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "hashCount: " + hashCount + ", hasher: " + hasher;
	}
}
