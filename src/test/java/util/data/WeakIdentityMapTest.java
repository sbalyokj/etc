package util.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Test;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class WeakIdentityMapTest {
	@Test
	public void testIsEmpty() {
		List<Integer> list = new ArrayList<>();
		list.add(new Integer(1));
				
		WeakIdentityMap<Integer,Integer> weakIdentityMap = new WeakIdentityMap<>();
		
		assertEquals(true, weakIdentityMap.isEmpty());
		assertSame(0, weakIdentityMap.size());
		
		weakIdentityMap.put(list.get(0), list.get(0));
		assertEquals(false, weakIdentityMap.isEmpty());
		assertSame(1, weakIdentityMap.size());
	}
	
	private <K, V> WeakIdentityMap<K, V> createForSameHashCodeTest() throws Exception {
		WeakIdentityMap<K, V> weakIdentityMap = new WeakIdentityMap<>();
		Field field = WeakIdentityMap.class.getDeclaredField("hashCodeMapper");
		boolean accessible = field.isAccessible();
		try {
			field.setAccessible(true);
			field.set(weakIdentityMap, (Function<Integer, Integer>)(i -> i));
		} finally {
			field.setAccessible(accessible);
		}
		return weakIdentityMap;
	}
	
	private <K, V> WeakIdentityMap<K, V> createForAllSameHashCodeTest() throws Exception {
		WeakIdentityMap<K, V> weakIdentityMap = new WeakIdentityMap<>();
		Field field = WeakIdentityMap.class.getDeclaredField("hashCodeMapper");
		boolean accessible = field.isAccessible();
		try {
			field.setAccessible(true);
			field.set(weakIdentityMap, (Function<Integer, Integer>)(i -> 1));
		} finally {
			field.setAccessible(accessible);
		}
		return weakIdentityMap;
	}
	
	@Test
	public void testSameHashCode() throws Exception {
		WeakIdentityMap<Integer,Integer> weakIdentityMap = createForSameHashCodeTest();
		
		List<Integer> list = new ArrayList<>();
		list.add(new Integer(1));
		list.add(new Integer(1));
		list.add(new Integer(1));
		list.add(new Integer(1));
		
		assertSame(null, weakIdentityMap.put(list.get(0), list.get(0)));
		assertSame(null, weakIdentityMap.put(list.get(1), list.get(1)));
		assertSame(null, weakIdentityMap.put(list.get(2), list.get(2)));
		
		// replace value for existing key
		assertSame(list.get(2), weakIdentityMap.put(list.get(2), list.get(3)));

		// same hash, not contains
		assertSame(null, weakIdentityMap.get(new Integer(1)));
		assertSame(false, weakIdentityMap.containsKey(new Integer(1)));
		assertSame(null, weakIdentityMap.remove(new Integer(1)));
		
		assertSame(list.get(1), weakIdentityMap.remove(list.get(1)));
		assertSame(list.get(0), weakIdentityMap.remove(list.get(0)));
		assertSame(list.get(3), weakIdentityMap.remove(list.get(2)));
	}
	
	@Test
	public void testPutAllAndEquals() {
		List<Integer> list = new ArrayList<>();
		list.add(new Integer(1));
		list.add(new Integer(1));
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = new WeakIdentityMap<>();
		list.forEach(i -> weakIdentityMap.put(i, i));
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap2 = new WeakIdentityMap<>();
		weakIdentityMap2.putAll(weakIdentityMap);
		
		assertEquals(weakIdentityMap, weakIdentityMap2);
		assertNotEquals(weakIdentityMap, null);
	}
	
	@Test
	public void testClear() {
		List<Integer> list = new ArrayList<>();
		list.add(new Integer(1));
		list.add(new Integer(1));
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = new WeakIdentityMap<>();
		list.forEach(i -> weakIdentityMap.put(i, i));
		
		assertSame(false, weakIdentityMap.isEmpty());
		assertSame(true, weakIdentityMap.containsKey(list.get(0)));
		assertSame(true, weakIdentityMap.containsKey(list.get(1)));
		weakIdentityMap.clear();
		assertSame(true, weakIdentityMap.isEmpty());
		assertSame(false, weakIdentityMap.containsKey(list.get(0)));
		assertSame(false, weakIdentityMap.containsKey(list.get(1)));
	}
	
	// keyset is identical set
	@Test
	public void testKeySet() {
		List<Integer> list = new ArrayList<>();
		list.add(new Integer(1));
		list.add(new Integer(1));
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = new WeakIdentityMap<>();
		list.forEach(i -> weakIdentityMap.put(i, i));
		
		assertEquals(2, weakIdentityMap.keySet().size());
		assertEquals(false, weakIdentityMap.keySet().isEmpty());
		assertEquals(true, weakIdentityMap.keySet().contains(list.get(0)));
		assertEquals(true, weakIdentityMap.keySet().contains(list.get(1)));
		assertEquals(false, weakIdentityMap.keySet().contains(new Integer(1)));
		assertEquals(false, weakIdentityMap.keySet().remove(new Integer(1)));
		assertEquals(true, weakIdentityMap.keySet().remove(list.get(0)));
		assertEquals(false, weakIdentityMap.keySet().remove(list.get(0)));
		assertEquals(true, weakIdentityMap.keySet().remove(list.get(1)));
		assertEquals(true, weakIdentityMap.keySet().isEmpty());
		
		list.forEach(i -> weakIdentityMap.put(i, i));
		assertEquals(false, weakIdentityMap.keySet().isEmpty());
		weakIdentityMap.keySet().clear();
		assertEquals(true, weakIdentityMap.keySet().isEmpty());
		assertEquals(true, weakIdentityMap.values().isEmpty());
		assertEquals(true, weakIdentityMap.entrySet().isEmpty());
		
		list.forEach(i -> weakIdentityMap.put(i, i));
		weakIdentityMap.keySet().forEach(k -> {
			assertEquals(true, list.contains(k));
		});
	}
	
	// values is not identical collection
	@Test
	public void testValues() {
		List<Integer> list = new ArrayList<>();
		list.add(new Integer(1));
		list.add(new Integer(1));
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = new WeakIdentityMap<>();
		list.forEach(i -> weakIdentityMap.put(i, i));
		
		assertEquals(2, weakIdentityMap.values().size());
		assertEquals(false, weakIdentityMap.values().isEmpty());
		assertEquals(true, weakIdentityMap.values().contains(list.get(0)));
		assertEquals(true, weakIdentityMap.values().contains(list.get(1)));
		assertEquals(true, weakIdentityMap.values().contains(new Integer(1)));
		assertEquals(true, weakIdentityMap.values().remove(new Integer(1)));
		assertEquals(true, weakIdentityMap.values().remove(list.get(0)));
		assertEquals(false, weakIdentityMap.values().remove(list.get(0)));
		assertEquals(false, weakIdentityMap.values().remove(list.get(1)));
		assertEquals(true, weakIdentityMap.values().isEmpty());
		
		list.forEach(i -> weakIdentityMap.put(i, i));
		assertEquals(false, weakIdentityMap.values().isEmpty());
		weakIdentityMap.values().clear();
		assertEquals(true, weakIdentityMap.keySet().isEmpty());
		assertEquals(true, weakIdentityMap.values().isEmpty());
		assertEquals(true, weakIdentityMap.entrySet().isEmpty());
		
		list.forEach(i -> weakIdentityMap.put(i, i));
		weakIdentityMap.values().forEach(k -> {
			assertEquals(true, list.contains(k));
		});
	}
	
	// entrySet is collection, identical for key but not for value
	@Test
	public void testEntrySet() {
		List<Integer> list = new ArrayList<>();
		list.add(new Integer(1));
		list.add(new Integer(1));
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = new WeakIdentityMap<>();
		list.forEach(i -> weakIdentityMap.put(i, i));
		
		assertEquals(2, weakIdentityMap.entrySet().size());
		assertEquals(false, weakIdentityMap.entrySet().isEmpty());
		assertEquals(true, weakIdentityMap.entrySet().contains(new EntryImpl<Integer, Integer>(list.get(0), list.get(0))));
		assertEquals(true, weakIdentityMap.entrySet().contains(new EntryImpl<Integer, Integer>(list.get(0), list.get(1))));
		assertEquals(true, weakIdentityMap.entrySet().contains(new EntryImpl<Integer, Integer>(list.get(0), new Integer(1))));
		assertEquals(true, weakIdentityMap.entrySet().contains(new EntryImpl<Integer, Integer>(list.get(1), list.get(0))));
		assertEquals(true, weakIdentityMap.entrySet().contains(new EntryImpl<Integer, Integer>(list.get(1), list.get(1))));
		assertEquals(true, weakIdentityMap.entrySet().contains(new EntryImpl<Integer, Integer>(list.get(1), new Integer(1))));
		assertEquals(false, weakIdentityMap.entrySet().contains(new EntryImpl<Integer, Integer>(new Integer(1), list.get(0))));
		assertEquals(false, weakIdentityMap.entrySet().contains(new EntryImpl<Integer, Integer>(new Integer(1), list.get(1))));
		assertEquals(false, weakIdentityMap.entrySet().contains(new EntryImpl<Integer, Integer>(new Integer(1), new Integer(1))));
		assertEquals(false, weakIdentityMap.entrySet().contains(null));
		assertEquals(false, weakIdentityMap.entrySet().remove(null));
		
		for (Entry<Integer, Integer> entry : weakIdentityMap.entrySet()) {
			assertEquals(false, entry.equals(null));
			assertEquals(false, entry.equals(new Object()));
			assertEquals(true, entry.equals(entry));
			assertEquals(false, entry.equals(new EntryImpl<Integer, Integer>(entry.getKey(), new Integer(2))));
			assertEquals(false, entry.equals(new EntryImpl<Integer, Integer>(new Integer(entry.getKey()), entry.getValue())));
			break;
		}
		
		assertEquals(false, weakIdentityMap.entrySet().remove(new EntryImpl<Integer, Integer>(new Integer(1), list.get(0))));
		assertEquals(false, weakIdentityMap.entrySet().remove(new EntryImpl<Integer, Integer>(new Integer(1), list.get(1))));
		assertEquals(false, weakIdentityMap.entrySet().remove(new EntryImpl<Integer, Integer>(new Integer(1), new Integer(1))));
		assertEquals(true, weakIdentityMap.entrySet().remove(new EntryImpl<Integer, Integer>(list.get(1), list.get(0))));
		assertEquals(false, weakIdentityMap.entrySet().remove(new EntryImpl<Integer, Integer>(list.get(1), list.get(1))));
		assertEquals(true, weakIdentityMap.entrySet().remove(new EntryImpl<Integer, Integer>(list.get(0), list.get(0))));
		assertEquals(true, weakIdentityMap.entrySet().isEmpty());
		
		list.forEach(i -> weakIdentityMap.put(i, i));
		assertEquals(false, weakIdentityMap.entrySet().isEmpty());
		weakIdentityMap.entrySet().clear();
		assertEquals(true, weakIdentityMap.keySet().isEmpty());
		assertEquals(true, weakIdentityMap.values().isEmpty());
		assertEquals(true, weakIdentityMap.entrySet().isEmpty());
		
		list.forEach(i -> weakIdentityMap.put(i, i));
		weakIdentityMap.entrySet().forEach(e -> {
			assertEquals(true, list.contains(e.getKey()));
			assertEquals(true, list.contains(e.getValue()));
		});
	}
	
	@Test(expected = NullPointerException.class)
	public void testEntrySetForEachNullAction() {
		WeakIdentityMap<Integer,Integer> weakIdentityMap = new WeakIdentityMap<>();
		weakIdentityMap.entrySet().forEach(null);
	}
	
	@Test(expected = ConcurrentModificationException.class)
	public void testEntrySetForEachConcurrentModifyAction() throws Exception {
		List<Integer> list = new ArrayList<>();
		list.add(new Integer(1));
		list.add(new Integer(2));
		list.add(new Integer(3));
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = createForSameHashCodeTest();
		list.forEach(i -> weakIdentityMap.put(i, i));
		
		Integer newKey = new Integer(1);
		weakIdentityMap.entrySet().forEach(e -> {
			weakIdentityMap.put(newKey, newKey);
		});
	}
	
	@AllArgsConstructor
	@Getter
	class EntryImpl<K,V> implements Map.Entry<K, V> {
		private K key;
		private V value;
		@Override
		public V setValue(V value) {
			V old = this.value;
			this.value = value;
			return old;
		}
	}
	
	@Test
	public void testKeyIterator() {
		Set<Integer> set = IntStream.range(1, 11).mapToObj(Integer::valueOf).collect(Collectors.toSet());
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = new WeakIdentityMap<>();
		set.forEach(i -> weakIdentityMap.put(i, i));
		
		Integer actual = weakIdentityMap.keySet().stream().reduce(Integer::sum).orElse(0);
		Integer expected = set.stream().reduce(Integer::sum).orElse(-1);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testKeyIteratorRemove() {
		Set<Integer> set = IntStream.range(1, 11).mapToObj(Integer::valueOf).collect(Collectors.toSet());
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = new WeakIdentityMap<>();
		set.forEach(i -> weakIdentityMap.put(i, i));
		
		int remove = 5;
		
		MutableInt sum = new MutableInt(0);
		
		Iterator<Integer> iterator = weakIdentityMap.keySet().iterator();
		while(iterator.hasNext()) {
			Integer next = iterator.next();
			if (next == remove) {
				iterator.remove();
			} else {
				sum.add(next);
			}
		}
		
		Integer actual = weakIdentityMap.keySet().stream().reduce(Integer::sum).orElse(0);
		Integer expected = set.stream().reduce(Integer::sum).orElse(-1) - remove;
		
		assertEquals(expected, actual);
		assertEquals(expected, sum.getValue());
	}
	
	@Test
	public void testKeyIteratorRemoveForSameHash() throws Exception {
		List<Integer> list = new ArrayList<>();
		list.add(new Integer(1));
		list.add(new Integer(2));
		list.add(new Integer(3));
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = createForAllSameHashCodeTest();
		list.forEach(i -> weakIdentityMap.put(i, i));
		
		Integer remove = list.get(1);
		
		MutableInt sum = new MutableInt(0);
		
		Iterator<Integer> iterator = weakIdentityMap.keySet().iterator();
		while(iterator.hasNext()) {
			Integer next = iterator.next();
			if (next == remove) {
				iterator.remove();
			} else {
				sum.add(next);
			}
		}
		
		Integer actual = weakIdentityMap.keySet().stream().reduce(Integer::sum).orElse(0);
		Integer expected = list.stream().reduce(Integer::sum).orElse(-1) - remove;
		
		assertEquals(expected, actual);
		assertEquals(expected, sum.getValue());
	}
	
	@Test
	public void testValueIterator() {
		Set<Integer> set = IntStream.range(1, 11).mapToObj(Integer::valueOf).collect(Collectors.toSet());
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = new WeakIdentityMap<>();
		set.forEach(i -> weakIdentityMap.put(i, i));
		
		Integer actual = weakIdentityMap.values().stream().reduce(Integer::sum).orElse(0);
		Integer expected = set.stream().reduce(Integer::sum).orElse(-1);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testEntryIterator() {
		Set<Integer> set = IntStream.range(1, 11).mapToObj(Integer::valueOf).collect(Collectors.toSet());
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = new WeakIdentityMap<>();
		set.forEach(i -> weakIdentityMap.put(i, i));
		
		Integer actual = weakIdentityMap.entrySet().stream().map(Map.Entry::getKey).reduce(Integer::sum).orElse(0);
		Integer expected = set.stream().reduce(Integer::sum).orElse(-1);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testKeySpliterator() {
		Set<Integer> set = IntStream.range(1, 11).mapToObj(Integer::valueOf).collect(Collectors.toSet());
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = new WeakIdentityMap<>();
		set.forEach(i -> weakIdentityMap.put(i, i));
		
		Integer actual = StreamSupport.stream(weakIdentityMap.keySet().spliterator(), true).reduce(Integer::sum).orElse(0);
		Integer expected = set.stream().reduce(Integer::sum).orElse(-1);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testValueSpliterator() {
		Set<Integer> set = IntStream.range(1, 11).mapToObj(Integer::valueOf).collect(Collectors.toSet());
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = new WeakIdentityMap<>();
		set.forEach(i -> weakIdentityMap.put(i, i));
		
		Integer actual = StreamSupport.stream(weakIdentityMap.values().spliterator(), true).reduce(Integer::sum).orElse(0);
		Integer expected = set.stream().reduce(Integer::sum).orElse(-1);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testEntrySpliterator() {
		Set<Integer> set = IntStream.range(1, 11).mapToObj(Integer::valueOf).collect(Collectors.toSet());
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = new WeakIdentityMap<>();
		set.forEach(i -> weakIdentityMap.put(i, i));
		
		Integer actual = StreamSupport.stream(weakIdentityMap.entrySet().spliterator(), true).map(Map.Entry::getKey).reduce(Integer::sum).orElse(0);
		Integer expected = set.stream().reduce(Integer::sum).orElse(-1);
		
		assertEquals(expected, actual);
	}
	
	@SuppressWarnings("unused")
	@Test(expected = ConcurrentModificationException.class)
	public void testKeyIteratorConcurrentModification() {
		List<Integer> list = new ArrayList<>();
		list.add(new Integer(1));
		list.add(new Integer(1));
		list.add(new Integer(1));
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = new WeakIdentityMap<>();
		weakIdentityMap.put(list.get(0), list.get(0));
		weakIdentityMap.put(list.get(1), list.get(1));
		
		for (Integer i : weakIdentityMap.keySet()) {
			weakIdentityMap.put(list.get(2), list.get(2));
		}
	}
	
	@SuppressWarnings("unused")
	@Test(expected = ConcurrentModificationException.class)
	public void testValueIteratorConcurrentModification() {
		List<Integer> list = new ArrayList<>();
		list.add(new Integer(1));
		list.add(new Integer(1));
		list.add(new Integer(1));
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = new WeakIdentityMap<>();
		weakIdentityMap.put(list.get(0), list.get(0));
		weakIdentityMap.put(list.get(1), list.get(1));
		
		for (Integer i : weakIdentityMap.values()) {
			weakIdentityMap.put(list.get(2), list.get(2));
		}
	}
	
	@SuppressWarnings("unused")
	@Test(expected = ConcurrentModificationException.class)
	public void testEntryIteratorConcurrentModification() {
		List<Integer> list = new ArrayList<>();
		list.add(new Integer(1));
		list.add(new Integer(1));
		list.add(new Integer(1));
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = new WeakIdentityMap<>();
		weakIdentityMap.put(list.get(0), list.get(0));
		weakIdentityMap.put(list.get(1), list.get(1));
		
		for (Entry<Integer, Integer> e : weakIdentityMap.entrySet()) {
			weakIdentityMap.put(list.get(2), list.get(2));
		}
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testPutNullKey() {
		WeakIdentityMap<Integer,Integer> weakIdentityMap = new WeakIdentityMap<>();
		weakIdentityMap.put(null, null);
	}
	
	@Test(expected = ConcurrentModificationException.class)
	public void testSpliteratorConcurrentModification() {
		List<Integer> list = new ArrayList<>();
		list.add(new Integer(1));
		list.add(new Integer(1));
		list.add(new Integer(1));
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = new WeakIdentityMap<>();
		list.forEach(i -> weakIdentityMap.put(i, i));
		
		Spliterator<Integer> spliterator = weakIdentityMap.keySet().spliterator();
		weakIdentityMap.remove(list.get(0));
		Integer actual = StreamSupport.stream(spliterator, true).reduce(Integer::sum).orElse(0);
		Integer expected = list.stream().reduce(Integer::sum).orElse(-1);
		
		assertEquals(expected, actual);
	}
	
	@Test(expected = ConcurrentModificationException.class)
	public void testSpliteratorConcurrentModification2() throws Exception {
		List<Integer> list = new ArrayList<>();
		list.add(new Integer(1));
		list.add(new Integer(1));
		list.add(new Integer(2));
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = createForSameHashCodeTest();
		list.forEach(i -> weakIdentityMap.put(i, i));
		
		Integer remove = list.get(2);
		
		Iterator<Integer> iterator = weakIdentityMap.keySet().iterator();
		while(iterator.hasNext()) {
			weakIdentityMap.remove(remove);
			iterator.next();
		}
	}
	
	@Test(expected = ConcurrentModificationException.class)
	public void testKeyIteratorRemoveConcurrentModification() throws Exception {
		List<Integer> list = new ArrayList<>();
		list.add(new Integer(1));
		list.add(new Integer(2));
		list.add(new Integer(3));
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = new WeakIdentityMap<>();
		list.forEach(i -> weakIdentityMap.put(i, i));
		
		Integer remove = list.get(0);
		
		Iterator<Integer> iterator = weakIdentityMap.keySet().iterator();
		while(iterator.hasNext()) {
			if (iterator.next() == remove) {
				weakIdentityMap.remove(list.get(1));
				iterator.remove();
			}
		}
		
		Integer actual = weakIdentityMap.keySet().stream().reduce(Integer::sum).orElse(0);
		Integer expected = list.stream().reduce(Integer::sum).orElse(-1) - remove;
		
		assertEquals(expected, actual);
	}
	
	@Test(expected = ConcurrentModificationException.class)
	public void testKeyIteratorRemoveConcurrentModification2() throws Exception {
		List<Integer> list = new ArrayList<>();
		list.add(new Integer(1));
		list.add(new Integer(2));
		list.add(new Integer(3));
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = new WeakIdentityMap<>();
		list.forEach(i -> weakIdentityMap.put(i, i));
		
		Integer remove = list.get(0);
		
		Iterator<Integer> iterator = weakIdentityMap.keySet().iterator();
		while(iterator.hasNext()) {
			weakIdentityMap.remove(list.get(1));
			if (iterator.next() == remove) {
				iterator.remove();
			}
		}
		
		Integer actual = weakIdentityMap.keySet().stream().reduce(Integer::sum).orElse(0);
		Integer expected = list.stream().reduce(Integer::sum).orElse(-1) - remove;
		
		assertEquals(expected, actual);
	}
	
	@Test(expected = IllegalStateException.class)
	public void testKeyIteratorRemoveDuplication() throws Exception {
		List<Integer> list = new ArrayList<>();
		list.add(new Integer(1));
		list.add(new Integer(2));
		list.add(new Integer(3));
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = new WeakIdentityMap<>();
		list.forEach(i -> weakIdentityMap.put(i, i));
		
		Integer remove = list.get(0);
		
		Iterator<Integer> iterator = weakIdentityMap.keySet().iterator();
		while(iterator.hasNext()) {
			if (iterator.next() == remove) {
				iterator.remove();
				iterator.remove();
			}
		}
		
		Integer actual = weakIdentityMap.keySet().stream().reduce(Integer::sum).orElse(0);
		Integer expected = list.stream().reduce(Integer::sum).orElse(-1) - remove;
		
		assertEquals(expected, actual);
	}
	
	@Test(expected = NoSuchElementException.class)
	public void testKeyIteratorRequireNextAfterEnd() throws Exception {
		List<Integer> list = new ArrayList<>();
		list.add(new Integer(1));
		list.add(new Integer(2));
		list.add(new Integer(3));
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = new WeakIdentityMap<>();
		list.forEach(i -> weakIdentityMap.put(i, i));
		
		Iterator<Integer> iterator = weakIdentityMap.keySet().iterator();
		while(iterator.hasNext()) {
			iterator.next();
		}
		
		iterator.next();
	}
	
	@Test(timeout = 5000)
	public void testExpunge() throws Exception {
		List<Integer> list = new ArrayList<>();
		list.add(new Integer(1));
		list.add(new Integer(1));
		list.add(new Integer(1));
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = createForSameHashCodeTest();
		list.forEach(i -> weakIdentityMap.put(i, 1));
		
		list.remove(1);
		
		while (weakIdentityMap.size() > list.size()) {
			System.gc();
			Thread.yield();
		}
		
		assertEquals(list.size(), weakIdentityMap.size());
		
		list.remove(0);
		
		while (weakIdentityMap.size() > list.size()) {
			System.gc();
			Thread.yield();
		}
		
		assertEquals(list.size(), weakIdentityMap.size());
		
		list.remove(0);
		
		while (weakIdentityMap.size() > list.size()) {
			System.gc();
			Thread.yield();
		}
		
		assertEquals(list.size(), weakIdentityMap.size());
	}
	
	@Test
	public void testNullSplitator() {
		List<Integer> list = new ArrayList<>();
		list.add(new Integer(1));
		list.add(new Integer(1));
		list.add(new Integer(1));
		
		WeakIdentityMap<Integer,Integer> weakIdentityMap = new WeakIdentityMap<>();
		list.forEach(i -> weakIdentityMap.put(i, 1));
		
		Spliterator<Integer> spliterator = weakIdentityMap.keySet().spliterator();
		int actual = sum(spliterator);
		int expected = list.stream().reduce(Integer::sum).orElse(-1);
		
		assertSame(expected, actual);
	}
	
	private int sum(Spliterator<Integer> spliterator) {
		int s = 0;
		Spliterator<Integer> sub;
		while ((sub = spliterator.trySplit()) != null) {
			System.out.println(spliterator + "," + sub);
			s += sum(sub); 
		}
		MutableInt mi = new MutableInt(0);
		spliterator.tryAdvance(mi::add);
		return s + mi.intValue();
	}
}
