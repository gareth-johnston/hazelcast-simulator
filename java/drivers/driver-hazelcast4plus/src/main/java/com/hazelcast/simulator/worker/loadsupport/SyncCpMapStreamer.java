package com.hazelcast.simulator.worker.loadsupport;

import com.hazelcast.cp.CPMap;

public class SyncCpMapStreamer<K, V> implements Streamer<K, V> {
    private final CPMap<K, V> map;

    SyncCpMapStreamer(CPMap<K, V> map) {
        this.map = map;
    }

    @Override
    public void pushEntry(K key, V value) {
        map.set(key, value);
    }

    @Override
    public void await() {
    }
}
