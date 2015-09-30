package com.tomgibara.bloom;

abstract class AbstractBloomFilter<E> implements BloomFilter<E> {

	// object methods
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof BloomFilter<?>)) return false;
		final BloomFilter<?> that = (BloomFilter<?>) obj;
		if (this.getHashCount() != that.getHashCount()) return false;
		if (!this.getHasher().equals(that.getHasher())) return false;
		if (!this.getBits().equals(that.getBits())) return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		return getBits().hashCode();
	}
	
	@Override
	public String toString() {
		return getBits().toString();
	}

	public abstract BloomFilter<E> clone();

	
}
