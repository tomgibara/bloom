/*
 * Copyright 2010 Tom Gibara
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.tomgibara.bloom;

import java.util.Collections;
import java.util.HashSet;

import com.tomgibara.bits.BitVector;
import com.tomgibara.hashing.HashSize;
import com.tomgibara.hashing.Hasher;
import com.tomgibara.hashing.Hashing;

import junit.framework.TestCase;


public class BasicBloomFilterTest extends TestCase {

	static final HashSize DEFAULT_SIZE = HashSize.fromInt(1000);
	
	Hasher<Integer> sha1Hash = Hashing.prng("SHA1PRNG", HashSize.LONG_SIZE).hasher((i,w) -> w.writeInt(i));
	Hasher<Integer> multiHash = sha1Hash.ints().sized(DEFAULT_SIZE);
	Hasher<Object> objHash = Hashing.identityHasher().sized(DEFAULT_SIZE);

	public void testConstructorWithoutBitVector() {
		BloomFilter<Integer> bloom = Bloom.newFilter(multiHash, 10);
		assertEquals(0.0, bloom.getFalsePositiveProbability());
		assertEquals(10, bloom.getHashCount());
		assertEquals(multiHash, bloom.getHasher());
		assertEquals(true, bloom.isEmpty());
		assertEquals(DEFAULT_SIZE.asInt(), bloom.getBitVector().size());
	}
	
	public void testConstructorWithBitVector() {
		BloomFilter<Integer> bloom = Bloom.newFilter(new BitVector(500), multiHash, 10);
		assertEquals(500, bloom.getCapacity());
		assertEquals(500, bloom.getHasher().getSize().asInt());
	}
	
	public void testConstructorWithImmutableBitVector() {
		try {
			Bloom.newFilter(new BitVector(1000).immutableCopy(), multiHash, 10);
			fail();
		} catch (IllegalArgumentException e) {
			/* expected */
		}
	}
	
	public void testIsEmpty() {
		BloomFilter<Integer> bloom = Bloom.newFilter(multiHash, 10);
		assertTrue(bloom.isEmpty());
		bloom.add(1);
		assertFalse(bloom.isEmpty());
		bloom.clear();
		assertTrue(bloom.isEmpty());
	}

	public void testGetFalsePositiveProbability() {
		int size = 10;
		Hasher<Object> hasher = Hashing.objectHasher().sized(HashSize.fromInt(size));
		BloomFilter<Integer> bloom = Bloom.newFilter(hasher, 1);
		double p = bloom.getFalsePositiveProbability();
		assertEquals(0.0, p);
		for (int i = 0; i < size; i++) {
			bloom.add(i);
			final double q = bloom.getFalsePositiveProbability();
			assertTrue(p < q);
			p = q;
		}
		assertEquals(1.0, p);
	}
	
	public void testClear() {
		BloomFilter<Integer> bloom = Bloom.newFilter(multiHash, 10);
		assertTrue(bloom.isEmpty());
		bloom.clear();
		assertTrue(bloom.isEmpty());
		bloom.add(1);
		assertFalse(bloom.isEmpty());
		bloom.clear();
		assertTrue(bloom.isEmpty());
	}
	
	public void testCapacity() {
		BloomFilter<Integer> bloom = Bloom.newFilter(multiHash, 10);
		assertEquals(multiHash.getSize().asInt(), bloom.getCapacity());
	}
	
	public void testEqualsAndHashCode() {
		BloomFilter<Integer> b1 = Bloom.newFilter(multiHash, 1);
		BloomFilter<Integer> b2 = Bloom.newFilter(multiHash, 1);
		BloomFilter<Integer> b3 = Bloom.newFilter(multiHash, 2);
		BloomFilter<Integer> b4 = Bloom.newFilter(objHash, 1);
		assertEquals(b1, b1);
		assertEquals(b1, b2);
		assertEquals(b2, b1);
		assertFalse(b1.equals(b3));
		assertFalse(b3.equals(b1));
		assertFalse(b1.equals(b4));
		assertFalse(b4.equals(b1));
		assertFalse(b3.equals(b4));
		assertFalse(b4.equals(b3));
	
		assertEquals(b1.hashCode(), b2.hashCode());
		
		final Integer e = 1;
		b1.add(e);
		assertFalse(b1.equals(b2));
		b2.add(e);
		assertTrue(b1.equals(b2));

		assertEquals(b1.hashCode(), b2.hashCode());
	}
	
	public void testAdd() {
		BloomFilter<Integer> bloom = Bloom.newFilter(multiHash, 10);
		int bitCount = 0;
		for (int i = 0; i < 10; i++) {
			bloom.add(i);
			BloomFilter<Integer> b = bloom.clone();
			assertFalse(b.add(i));
			assertEquals(bloom, b);
			final int newBitCount = bloom.getBitVector().countOnes();
			assertTrue(newBitCount >= bitCount);
			bitCount = newBitCount;
		}
		for (int i = 0; i < 10; i++) {
			assertTrue(bloom.mightContain(i));
		}
	}
	
	public void testAddAll() {
		BloomFilter<Integer> b1 = Bloom.newFilter(multiHash, 10);
		BloomFilter<Integer> b2 = Bloom.newFilter(multiHash, 10);
		HashSet<Integer> values = new HashSet<Integer>();
		for (int i = 0; i < 10; i++) {
			b1.add(i);
			values.add(i);
		}
		b2.addAll(values);
		assertTrue(b1.equals(b2));
		assertFalse(b2.addAll(values));
		assertTrue(b1.equals(b2));
	}
	
	public void testMightContain() {
		BloomFilter<Integer> b = Bloom.newFilter(multiHash, 10);
		assertFalse(b.mightContain(1));
		for (int i = 0; i < 10; i++) {
			if (b.clone().add(i)) assertFalse(b.mightContain(i));
			b.add(i);
			assertTrue(b.mightContain(i));
		}
	}
	
	public void testMightContainAll() {
		BloomFilter<Integer> b = Bloom.newFilter(multiHash, 10);
		assertFalse(b.mightContainAll(Collections.singleton(1)));
		HashSet<Integer> values = new HashSet<Integer>();
		for (int i = 0; i < 10; i++) {
			b.add(i);
			values.add(i);
		}
		assertTrue(b.mightContainAll(values));
		for (int i = 10; i < 20; i++) {
			HashSet<Integer> vs = new HashSet<Integer>(values);
			if (b.clone().addAll(vs)) assertFalse(b.mightContainAll(vs));
		}
	}
}
