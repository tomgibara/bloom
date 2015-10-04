package com.tomgibara.bloom;

abstract class AbstractBloomFilter<E> implements BloomFilter<E> {

	// object methods
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof BloomFilter<?>)) return false;
		final BloomFilter<?> that = (BloomFilter<?>) obj;
		if (!this.config().equals(that.config())) return false;
		if (!this.bits().equals(that.bits())) return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		return bits().hashCode();
	}
	
	@Override
	public String toString() {
		return bits().toString();
	}

}
