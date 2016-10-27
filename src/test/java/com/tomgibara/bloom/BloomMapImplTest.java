package com.tomgibara.bloom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import junit.framework.TestCase;

import com.tomgibara.algebra.lattice.OrderedLattice;
import com.tomgibara.hashing.HashSize;
import com.tomgibara.hashing.Hasher;
import com.tomgibara.hashing.Hashing;
import com.tomgibara.storage.Storage;
import com.tomgibara.storage.StoreNullity;

public class BloomMapImplTest extends TestCase {

	static final HashSize DEFAULT_SIZE = HashSize.fromInt(1000);
	static final StoreNullity<Integer> ZERO_NULL = StoreNullity.settingNullToValue(0);

	public void testBasic() {
		Hasher<Integer> hasher = Hashing.murmur3Int().hasher((i, w) -> w.writeInt(i));
		hasher = hasher.ints().sized(DEFAULT_SIZE);
		OrderedLattice<Integer> lattice = new OrderedLattice<>(10000, 0);
		BloomMap<Integer, Integer> ca = Bloom.withHasher(hasher, 10).newMap(Storage.typed(int.class, ZERO_NULL), lattice);
		Random r = new Random(0L);
		List<Integer> keys = new ArrayList<>();
		List<Integer> values = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			int key = r.nextInt(1000);
			int value = r.nextInt(10000);
			ca.put(key, value);
			keys.add(key);
			values.add(value);
		}
	}
	
	public void testKeys() {
		Hasher<Integer> hasher = Hashing.murmur3Int().hasher((i, w) -> w.writeInt(i));
		hasher = hasher.ints().sized(DEFAULT_SIZE);
		OrderedLattice<Integer> lattice = new OrderedLattice<>(10000, 0);
		BloomMap<Integer, Integer> map = Bloom.withHasher(hasher, 10).newMap(Storage.typed(int.class, ZERO_NULL), lattice);
		BloomSet<Integer> keys = map.keys();
		assertTrue(keys.isEmpty());
		for (int i = 0; i < 30; i++) {
			assertFalse(keys.mightContain(i));
			map.put(i, i + 10);
			assertTrue(keys.mightContain(i));
		}
	}

	public void testMappingTo() {
		Hasher<Integer> hasher = Hashing.murmur3Int().hasher((i, w) -> w.writeInt(i));
		hasher = hasher.ints().sized(DEFAULT_SIZE);
		OrderedLattice<Integer> lattice = new OrderedLattice<>(10000, 0);
		BloomMap<Integer, Integer> map = Bloom.withHasher(hasher, 10).newMap(Storage.typed(int.class, ZERO_NULL), lattice);
		BloomMap<Integer, Integer> submap = map.mappingTo(lattice.bounded(1000, 100));
		
		try {
			submap.put(1, 50);
			fail();
		} catch (IllegalArgumentException e) {
			/* expected */
		}
		try {
			submap.put(1, 5000);
			fail();
		} catch (IllegalArgumentException e) {
			/* expected */
		}
		map.put(1, 50);
		assertEquals(100, submap.getSupremum(1).intValue());
		map.put(2, 150);
		assertEquals(150, submap.getSupremum(2).intValue());
		map.put(3, 1500);
		assertEquals(1000, submap.getSupremum(3).intValue());
		submap.put(2,500);
		assertEquals(500, map.getSupremum(2).intValue());
		
		map.clear();
		assertTrue(submap.isEmpty());
		Random r = new Random(0L);
		Map<Integer, Integer> real = new HashMap<>();
		for (int i = 0; i < 300; i++) {
			int k = r.nextInt(1000);
			int v = r.nextInt(10001);
			map.put(k, v);
			real.put(k, v);
		}
		int count = 0;
		for (Integer k : real.keySet()) {
			int n = map.getSupremum(k);
			int m = submap.getSupremum(k);
			if (n < 100) {
				assertEquals(100, m);
			} else if (n > 1000) {
				assertEquals(1000, m);
			} else {
				assertEquals(n, m);
			}
			Integer t = real.get(k);
			if (t == null) t = 0;
			if (n == t) {
				count ++;
			} else {
				assertTrue(n > t);
			}
		}
		assertTrue(count > real.size() / 2);
	}
	
}
