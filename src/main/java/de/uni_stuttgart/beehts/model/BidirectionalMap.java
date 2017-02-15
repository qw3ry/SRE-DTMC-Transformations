package de.uni_stuttgart.beehts.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BidirectionalMap<Key, Value> implements Map<Key, Value> {

	private final Map<Key, Value> map;
	private final Map<Value, Key> revMap;

	public BidirectionalMap() {
		this(16, 0.75f);
	}

	public BidirectionalMap(int initialCapacity) {
		this(initialCapacity, 0.75f);
	}

	public BidirectionalMap(int initialCapacity, float loadFactor) {
		this.map = new HashMap<>(initialCapacity, loadFactor);
		this.revMap = new HashMap<>(initialCapacity, loadFactor);
	}

	private BidirectionalMap(Map<Key, Value> map, Map<Value, Key> reverseMap) {
		this.map = map;
		this.revMap = reverseMap;
	}

	@Override
	public void clear() {
		map.clear();
		revMap.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return revMap.containsKey(value);
	}

	@Override
	public Set<java.util.Map.Entry<Key, Value>> entrySet() {
		return Collections.unmodifiableSet(map.entrySet());
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Set<Key> keySet() {
		return Collections.unmodifiableSet(map.keySet());
	}

	@Override
	public void putAll(Map<? extends Key, ? extends Value> m) {
		m.entrySet().forEach(e -> put(e.getKey(), e.getValue()));
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public Collection<Value> values() {
		return Collections.unmodifiableCollection(map.values());
	}

	@Override
	public Value get(Object key) {
		return map.get(key);
	}

	@Override
	public Value put(Key key, Value value) {
		Value v = remove(key);
		getReverseView().remove(value);
		map.put(key, value);
		revMap.put(value, key);
		return v;
	}

	public Map<Value, Key> getReverseView() {
		return new BidirectionalMap<>(revMap, map);
	}

	@Override
	public Value remove(Object key) {
		if (containsKey(key)) {
			Value v = map.remove(key);
			revMap.remove(v);
			return v;
		} else {
			return null;
		}
	}

}
