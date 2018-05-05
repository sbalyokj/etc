package util.data;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class WeakIdentityMap<K, V> implements Map<K, V> {
	// List<Entry> 의 길이는 대부분 1일 것이다.
	private Map<Integer, List<Entry>> hashCodeEntriesMap = new HashMap<>();
	private int size;
	private int modCount;
	
	transient private Function<Object, Integer> hashCodeMapper = System::identityHashCode;
	transient private ReferenceQueue<K> refQueue = new ReferenceQueue<K>();

	transient private Set<K> keySet;
	transient private Collection<V> values;
	transient private Set<Map.Entry<K, V>> entrySet;

	@Override
	public V get(Object key) {
		expunge();
		int hashCode = hashCodeMapper.apply(key);
		return hashCodeEntriesMap.getOrDefault(hashCode, Collections.emptyList()).stream()
				.filter(e -> e.getKey() == key)
				.map(Entry::getValue)
				.findAny().orElse(null);
	}

	@Override
	public V put(K key, V value) {
		expunge();
		if (key == null) {
			throw new IllegalArgumentException("Null key");
		}
		int hashCode = hashCodeMapper.apply(key);
		List<Entry> list = hashCodeEntriesMap.computeIfAbsent(hashCode, k -> new LinkedList<>());
		Entry entry = list.stream().filter(e -> e.getKey() == key).findAny().orElse(null);
		if (entry == null) {
			entry = new Entry(new IdentityWeakReference<K>(key, refQueue));
			list.add(entry);
			modCount++;
			size++;
		}
		return entry.setValue(value);
	}

	@Override
	public V remove(Object key) {
		expunge();
		int hashCode = hashCodeMapper.apply(key);
		Entry removed = removeMatched(hashCode, e -> e.getKey() == key);
		return removed == null ? null : removed.getValue();
	}

	@Override
	public int size() {
		expunge();
		return size;
	}

	private void expunge() {
		for (;;) {
			Reference<? extends K> ref = refQueue.poll();
			if (ref == null) {
				break;
			}
			removeMatched(ref.hashCode(), e -> e.getKeyRef() == ref);
		}
	}
	
	private Entry removeMatched(int hashCode, Predicate<Entry> match) {
		List<Entry> list = hashCodeEntriesMap.getOrDefault(hashCode, Collections.emptyList());
		Iterator<Entry> iterator = list.iterator();
		while (iterator.hasNext()) {
			Entry entry = iterator.next();
			if (match.test(entry)) {
				iterator.remove();
				if (list.isEmpty()) {
					hashCodeEntriesMap.remove(hashCode);
				}
				modCount++;
				size--;
				return entry;
			}
		}
		return null;
	}

	@Override
	public boolean isEmpty() {
		expunge();
		return size == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		expunge();
		int hashCode = hashCodeMapper.apply(key);
		return hashCodeEntriesMap.getOrDefault(hashCode, Collections.emptyList()).stream()
				.anyMatch(e -> e.getKey() == key);
	}

	@Override
	public boolean containsValue(Object value) {
		expunge();
		return hashCodeEntriesMap.values().stream()
				.anyMatch(list -> list.stream().anyMatch(e -> Objects.equals(e.getValue(), value)));
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		expunge();
		m.forEach((k, v) -> put(k, v));
	}

	@Override
	public void clear() {
		expunge();
		hashCodeEntriesMap.clear();
		modCount++;
		size = 0;
	}

	@Override
	public Set<K> keySet() {
		Set<K> ks = keySet;
		if (ks == null) {
			ks = new KeySet();
			keySet = ks;
		}
		return ks;
	}

	@Override
	public Collection<V> values() {
		Collection<V> vs = values;
		if (vs == null) {
			vs = new Values();
			values = vs;
		}
		return vs;
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		Set<Map.Entry<K, V>> es = entrySet;
		if (es == null) {
			es = new EntrySet();
			entrySet = es;
		}
		return es;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof WeakIdentityMap) {
			return Objects.equals(hashCodeEntriesMap, ((WeakIdentityMap<?, ?>) obj).hashCodeEntriesMap);
		}
		return false;
	}

	abstract class BaseIterator {
		private int expectedModCount; // for fast-fail
		private Iterator<Map.Entry<Integer, List<Entry>>> mapIterator;
		private Iterator<Entry> listIterator;
		private List<Entry> list;

		BaseIterator() {
			mapIterator = hashCodeEntriesMap.entrySet().iterator();
			expectedModCount = modCount;
		}

		public final boolean hasNext() {
			while ((listIterator == null || !listIterator.hasNext())) {
				if (mapIterator.hasNext()) {
					list = mapIterator.next().getValue();
					listIterator = list.iterator();
				} else {
					return false;
				}
			}
			return true;
		}

		final Entry nextEntry() {
			if (modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			return listIterator.next();
		}

		public final void remove() {
			if (modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}
			
			listIterator.remove();
			if (list.isEmpty()) {
				mapIterator.remove();
			}
			modCount++;
			size--;
			
			expectedModCount = modCount;
		}
	}

	final class KeyIterator extends BaseIterator implements Iterator<K> {
		public final K next() {
			return nextEntry().getKey();
		}
	}

	final class ValueIterator extends BaseIterator implements Iterator<V> {
		public final V next() {
			return nextEntry().getValue();
		}
	}

	final class EntryIterator extends BaseIterator implements Iterator<Map.Entry<K, V>> {
		public final Map.Entry<K, V> next() {
			return nextEntry();
		}
	}
	
	abstract class BaseSpliterator<T> implements Spliterator<T> {
		private int expectedModCount;
		private Spliterator<List<Entry>> listSpliterator;
		private Iterator<Entry> iterator;
		Function<Entry, T> mapper;
		
		BaseSpliterator(Function<Entry, T> mapper) {
			this(hashCodeEntriesMap.values().spliterator(), modCount, mapper);
		}

		BaseSpliterator(Spliterator<List<Entry>> listSpliterator, int expectedModCount, Function<Entry, T> mapper) {
			this.listSpliterator = listSpliterator;
			this.expectedModCount = expectedModCount;
			this.mapper = mapper;
		}

		@Override
		public boolean tryAdvance(Consumer<? super T> action) {
			while (iterator == null || !iterator.hasNext()) {
				if (!listSpliterator.tryAdvance(list -> iterator = list.iterator())) {
					return false;
				}
			}
			action.accept(mapper.apply(iterator.next()));
			if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
			return true;
		}

		@Override
		public Spliterator<T> trySplit() {
			Spliterator<List<Entry>> subListSpliterator = listSpliterator.trySplit();
			return subListSpliterator == null ? null : createSubSplitator(subListSpliterator, expectedModCount);
		}
		
		abstract BaseSpliterator<T> createSubSplitator(Spliterator<List<Entry>> listSpliterator, int expectedModCount);

		@Override
		public long estimateSize() {
			return listSpliterator.estimateSize();
		}
	}
	
	final class KeySpliterator extends BaseSpliterator<K> {
		public KeySpliterator() {
			super(Entry::getKey);
		}
		
		KeySpliterator(Spliterator<List<Entry>> listSpliterator, int expectedModCount, Function<Entry, K> mapper) {
			super(listSpliterator, expectedModCount, mapper);
		}
		
		@Override
		BaseSpliterator<K> createSubSplitator(Spliterator<List<Entry>> listSpliterator, int expectedModCount) {
			return new KeySpliterator(listSpliterator, expectedModCount, mapper);
		}

		@Override
		public int characteristics() {
			return DISTINCT;
		}
	}
	
	final class ValueSpliterator extends BaseSpliterator<V> {
		public ValueSpliterator() {
			super(Entry::getValue);
		}
		
		ValueSpliterator(Spliterator<List<Entry>> listSpliterator, int expectedModCount, Function<Entry, V> mapper) {
			super(listSpliterator, expectedModCount, mapper);
		}
		
		@Override
		BaseSpliterator<V> createSubSplitator(Spliterator<List<Entry>> listSpliterator, int expectedModCount) {
			return new ValueSpliterator(listSpliterator, expectedModCount, mapper);
		}
		
		@Override
		public int characteristics() {
			return 0;
		}
	}
	
	final class EntrySpliterator extends BaseSpliterator<Map.Entry<K, V>> {
		public EntrySpliterator() {
			super(e -> e);
		}
		
		EntrySpliterator(Spliterator<List<Entry>> listSpliterator, int expectedModCount, Function<Entry, Map.Entry<K, V>> mapper) {
			super(listSpliterator, expectedModCount, mapper);
		}
		
		@Override
		BaseSpliterator<Map.Entry<K, V>> createSubSplitator(Spliterator<List<Entry>> listSpliterator, int expectedModCount) {
			return new EntrySpliterator(listSpliterator, expectedModCount, mapper);
		}
		
		@Override
		public int characteristics() {
			return DISTINCT;
		}
	}

	final class KeySet extends AbstractSet<K> {
		public final int size() {
			return size;
		}

		public final void clear() {
			WeakIdentityMap.this.clear();
		}

		public final Iterator<K> iterator() {
			return new KeyIterator();
		}

		public final boolean contains(Object o) {
			return containsKey(o);
		}

		public final boolean remove(Object key) {
			return WeakIdentityMap.this.remove(key) != null;
		}

		public final Spliterator<K> spliterator() {
			return new KeySpliterator();
		}

		public final void forEach(Consumer<? super K> action) {
			WeakIdentityMap.this.forEach((k, v) -> action.accept(k));
		}
	}

	final class Values extends AbstractCollection<V> {
		public final int size() {
			return size;
		}

		public final void clear() {
			WeakIdentityMap.this.clear();
		}

		public final Iterator<V> iterator() {
			return new ValueIterator();
		}

		public final boolean contains(Object o) {
			return containsValue(o);
		}

		public final Spliterator<V> spliterator() {
			return new ValueSpliterator();
		}

		public final void forEach(Consumer<? super V> action) {
			WeakIdentityMap.this.forEach((k, v) -> action.accept(v));
		}
	}

	final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
		public final int size() {
			return size;
		}

		public final void clear() {
			WeakIdentityMap.this.clear();
		}

		public final Iterator<Map.Entry<K, V>> iterator() {
			return new EntryIterator();
		}

		public final boolean contains(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
			Object key = e.getKey();
			V v = get(key);
			return Objects.equals(v, e.getValue());
		}

		public final boolean remove(Object o) {
			if (o instanceof Map.Entry) {
				Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
				Object key = e.getKey();
				Object value = e.getValue();
				return WeakIdentityMap.this.remove(key, value);
			}
			return false;
		}

		public final Spliterator<Map.Entry<K, V>> spliterator() {
			return new EntrySpliterator();
		}

		public final void forEach(Consumer<? super Map.Entry<K, V>> action) {
			if (action == null) {
				throw new NullPointerException();
			}
//			int mc = modCount;
			// hashmap, list 어느곳에서든 추가 또는 삭제가 발생할 경우 에러 발생하므로 따로 체크하지 않는다.
			hashCodeEntriesMap.forEach((h, v) -> {
				v.stream().forEach(action);
			});
//			if (modCount != mc) {
//				throw new ConcurrentModificationException();
//			}
		}
	}

	public class Entry implements Map.Entry<K, V> {
		private final WeakReference<K> keyRef;
		private V value;

		public Entry(WeakReference<K> keyRef) {
			this.keyRef = keyRef;
		}

		public WeakReference<K> getKeyRef() {
			return keyRef;
		}

		@Override
		public K getKey() {
			return keyRef.get();
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			V old = this.value;
			this.value = value;
			return old;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj != null && obj instanceof Map.Entry) {
				Map.Entry<?, ?> e = (Map.Entry<?, ?>)obj;
				return getKey() == e.getKey() && Objects.equals(getValue(), e.getValue());
			}
			return false; 
		}
	}

	private class IdentityWeakReference<T> extends WeakReference<T> {
		public IdentityWeakReference(T o, ReferenceQueue<T> q) {
			super(o, q);
			this.hashCode = hashCodeMapper.apply(o);
		}

		public int hashCode() {
			return hashCode;
		}

		private final int hashCode;
	}
}
