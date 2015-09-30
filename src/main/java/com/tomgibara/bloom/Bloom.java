package com.tomgibara.bloom;

import com.tomgibara.algebra.lattice.Lattice;
import com.tomgibara.bits.BitVector;
import com.tomgibara.hashing.HashSize;
import com.tomgibara.hashing.Hasher;
import com.tomgibara.storage.Storage;
import com.tomgibara.storage.Store;

//TODO should allow immutables to wrapped to make immutable structures?
public final class Bloom<K> {

	private final Hasher<? super K> hasher;
	private final int hashCount;
	private final HashSize size;
	
	static void checkCompatible(BloomFilter<?> a, BloomFilter<?> b) {
		if (b == null) throw new IllegalArgumentException("null filter");
		if (a.getHashCount() != b.getHashCount()) throw new IllegalArgumentException("Incompatible filter, hashCount was " + b.getHashCount() +", expected " + a.getHashCount());
		if (!a.getHasher().equals(b.getHasher())) throw new IllegalArgumentException("Incompatible filter, hashers were not equal");
	}

	public static <K> Bloom<K> withHasher(Hasher<? super K> hasher, int hashCount) {
		if (hasher == null) throw new IllegalArgumentException("null hasher");
		if (hashCount < 1) throw new IllegalArgumentException("hashCount not positive");
		if (hasher.getQuantity() < hashCount) throw new IllegalArgumentException("hashCount exceeds quantity of hashes");
		return new Bloom<>(hasher, hashCount);
	}

	private Bloom(Hasher<? super K> hasher, int hashCount) {
		this.hasher = hasher;
		this.hashCount = hashCount;
		size = hasher.getSize();
	}

	public BloomFilter<K> newFilter(BitVector bits) {
		if (bits == null) throw new IllegalArgumentException("null bits");
		if (!bits.isMutable()) throw new IllegalArgumentException("immutable bits");
		return new BasicBloomFilter<>(bits, sizedHasher(bits.size()), hashCount);
	}

	public BloomFilter<K> newFilter() {
		if (hasher == null) throw new IllegalArgumentException("null hasher");
		if (hashCount < 1) throw new IllegalArgumentException("hashCount not positive");
		if (hasher.getQuantity() < hashCount) throw new IllegalArgumentException("hashCount exceeds quantity of hashes");
		if (!size.isIntSized()) throw new IllegalStateException("hash size too large");
		BitVector bits = new BitVector(size.asInt());
		return new BasicBloomFilter<>(bits, hasher, hashCount);
	}

	public <V> CompactApproximator<K, V> newApproximator(Store<V> values, Lattice<V> lattice) {
		if (values == null) throw new IllegalArgumentException("null values");
		if (!values.isMutable()) throw new IllegalArgumentException("immutable values");
		if (lattice == null) throw new IllegalArgumentException("null lattice");
		if (!lattice.isBoundedBelow()) throw new IllegalArgumentException("lattice not bounded below");
		return new BasicCompactApproximator<K, V>(values, lattice, hasher, hashCount);
	}

	public <V> CompactApproximator<K, V> newApproximator(Storage<V> storage, Lattice<V> lattice) {
		if (storage == null) throw new IllegalArgumentException("null storage");
		if (lattice == null) throw new IllegalArgumentException("null lattice");
		if (!lattice.isBoundedBelow()) throw new IllegalArgumentException("lattice not bounded below");
		if (!size.isIntSized()) throw new IllegalStateException("hash size too large");
		Store<V> values = storage.newStore(size.asInt());
		return new BasicCompactApproximator<K, V>(values, lattice, hasher, hashCount);
	}

	private Hasher<? super K> sizedHasher(int length) {
		HashSize newSize = HashSize.fromInt(length);
		int c = size.compareTo(newSize);
		if (c < 0) throw new IllegalArgumentException("hash size too small");
		return c > 0 ? hasher.sized(newSize) : hasher;
	}
}
