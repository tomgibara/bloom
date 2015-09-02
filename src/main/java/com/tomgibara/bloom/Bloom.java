package com.tomgibara.bloom;

import com.tomgibara.bits.BitVector;
import com.tomgibara.hashing.HashSize;
import com.tomgibara.hashing.Hasher;

public final class Bloom {

	public static <E> BloomFilter<E> newFilter(BitVector bits, Hasher<? super E> hasher, int hashCount) {
		if (bits == null) throw new IllegalArgumentException("null bits");
		if (!bits.isMutable()) throw new IllegalArgumentException("immutable bits");
		if (hasher == null) throw new IllegalArgumentException("null hasher");
		if (hashCount < 1) throw new IllegalArgumentException("hashCount not positive");
		if (hasher.getQuantity() < hashCount) throw new IllegalArgumentException("hashCount exceeds quantity of hashes");
		HashSize size = hasher.getSize();
		HashSize newSize = HashSize.fromInt(bits.size());
		int c = size.compareTo(newSize);
		if (c < 0) throw new IllegalArgumentException("hash size too small");
		if (c > 0) hasher = hasher.sized(newSize);
		return new BasicBloomFilter<>(bits, hasher, hashCount);
	}

	public static <E> BloomFilter<E> newFilter(Hasher<? super E> hasher, int hashCount) {
		if (hasher == null) throw new IllegalArgumentException("null hasher");
		if (hashCount < 1) throw new IllegalArgumentException("hashCount not positive");
		if (hasher.getQuantity() < hashCount) throw new IllegalArgumentException("hashCount exceeds quantity of hashes");
		HashSize size = hasher.getSize();
		if (!size.isIntSized()) throw new IllegalArgumentException("hash size too large");
		BitVector bits = new BitVector(size.asInt());
		return new BasicBloomFilter<>(bits, hasher, hashCount);
	}

	static void checkCompatible(BloomFilter<?> a, BloomFilter<?> b) {
		if (b == null) throw new IllegalArgumentException("null filter");
		if (a.getHashCount() != b.getHashCount()) throw new IllegalArgumentException("Incompatible filter, hashCount was " + b.getHashCount() +", expected " + a.getHashCount());
		if (!a.getHasher().equals(b.getHasher())) throw new IllegalArgumentException("Incompatible filter, hashers were not equal");
	}

}
