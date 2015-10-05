package com.tomgibara.bloom;

import com.tomgibara.algebra.lattice.Lattice;
import com.tomgibara.bits.BitStore;
import com.tomgibara.bits.Bits;
import com.tomgibara.hashing.Hasher;
import com.tomgibara.storage.Storage;
import com.tomgibara.storage.Store;

//TODO should allow immutables to wrapped to make immutable structures?
public final class Bloom<K> {

	private final BloomConfig<K> config;

	static void checkCompatible(BloomMap<?, ?> a, BloomMap<?, ?> b) {
		if (b == null) throw new IllegalArgumentException("null approximator");
		checkCompatible(a.config(), b.config());
		if (!a.lattice().equals(b.lattice())) throw new IllegalArgumentException("Incompatible compact approximator, lattices were not equal");
	}

	static void checkCompatible(BloomSet<?> a, BloomSet<?> b) {
		if (b == null) throw new IllegalArgumentException("null set");
		checkCompatible(a.config(), b.config());
	}

	static void checkCompatible(BloomConfig<?> ac, BloomConfig<?> bc) {
		if (ac.hashCount() != bc.hashCount()) throw new IllegalArgumentException("Incompatible set, hashCount was " + bc.hashCount() +", expected " + ac.hashCount());
		if (!ac.hasher().equals(bc.hasher())) throw new IllegalArgumentException("Incompatible set, hashers were not equal");
	}

	public static <K> Bloom<K> withHasher(Hasher<? super K> hasher, int hashCount) {
		return new Bloom<>(new BloomConfig<>(hasher, hashCount));
	}

	public static <K> Bloom<K> withConfig(BloomConfig<K> config) {
		if (config == null) throw new IllegalArgumentException("null config");
		return new Bloom<>(config);
	}

	private Bloom(BloomConfig<K> config) {
		this.config = config;
	}

	public BloomConfig<K> config() {
		return config;
	}
	
	public BloomSet<K> newSet(BitStore bits) {
		if (bits == null) throw new IllegalArgumentException("null bits");
		if (!bits.isMutable()) throw new IllegalArgumentException("immutable bits");
		return new BloomSetImpl<>(bits, config.withCapacity(bits.size()));
	}

	public BloomSet<K> newSet() {
		BitStore bits = Bits.newBitStore(config.capacity());
		return new BloomSetImpl<>(bits, config);
	}

	public <V> BloomMap<K, V> newMap(Store<V> values, Lattice<V> lattice) {
		if (values == null) throw new IllegalArgumentException("null values");
		if (!values.isMutable()) throw new IllegalArgumentException("immutable values");
		if (lattice == null) throw new IllegalArgumentException("null lattice");
		if (!lattice.isBoundedBelow()) throw new IllegalArgumentException("lattice not bounded below");
		return new BloomMapImpl<K, V>(config, values, lattice);
	}

	public <V> BloomMap<K, V> newMap(Storage<V> storage, Lattice<V> lattice) {
		if (storage == null) throw new IllegalArgumentException("null storage");
		if (lattice == null) throw new IllegalArgumentException("null lattice");
		if (!lattice.isBoundedBelow()) throw new IllegalArgumentException("lattice not bounded below");
		Store<V> values = storage.newStore(config.capacity());
		return new BloomMapImpl<K, V>(config, values, lattice);
	}

}
