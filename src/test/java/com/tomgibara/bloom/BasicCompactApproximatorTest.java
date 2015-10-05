package com.tomgibara.bloom;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.tomgibara.algebra.lattice.OrderedLattice;
import com.tomgibara.hashing.HashSize;
import com.tomgibara.hashing.Hasher;
import com.tomgibara.hashing.Hashing;
import com.tomgibara.storage.Storage;

import junit.framework.TestCase;

public class BasicCompactApproximatorTest extends TestCase {

	static final HashSize DEFAULT_SIZE = HashSize.fromInt(1000);

	public void testBasic() {
		Hasher<Integer> hasher = Hashing.murmur3Int().hasher((i, w) -> w.writeInt(i));
		hasher = hasher.ints().sized(DEFAULT_SIZE);
		OrderedLattice<Integer> lattice = new OrderedLattice<>(10000, 0);
		CompactApproximator<Integer, Integer> ca = Bloom.withHasher(hasher, 10).newApproximator(Storage.typed(int.class), lattice);
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
	
//	public void testBoundedBy() {
//		Hasher<Integer> hasher = Hashing.murmur3Int().hasher((i, w) -> w.writeInt(i));
//		hasher = hasher.ints().sized(DEFAULT_SIZE);
//		OrderedLattice<Integer> lattice = new OrderedLattice<>(10000, 0);
//		CompactApproximator<Integer, Integer> ca1 = Bloom.withHasher(hasher, 10).newApproximator(Storage.typed(int.class), lattice);
//		CompactApproximator<Integer, Integer> ca2 = Bloom.withHasher(hasher, 10).newApproximator(Storage.typed(int.class), lattice);
//		for (int i = 0; i < 30; i++) {
//			ca1.put(i, 10);
//			ca2.put(i, 10 + i);
//		}
//		assertTrue(ca1.boundedBy(ca2).isFull());
//		BloomFilter<Integer> b = ca2.boundedBy(ca1);
//		assertFalse(b.isFull());
//		for (int i = 0; i < 30; i++) {
//			System.out.println(i + " " + b.mightContain(i));
//			//assertEquals(i == 0, b.mightContain(i));
//		}
//	}
}
