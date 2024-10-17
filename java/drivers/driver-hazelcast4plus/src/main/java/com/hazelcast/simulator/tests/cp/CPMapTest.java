/*
 * Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hazelcast.simulator.tests.cp;

import com.hazelcast.collection.IList;
import com.hazelcast.cp.CPMap;
import com.hazelcast.simulator.hz.HazelcastTest;
import com.hazelcast.simulator.test.BaseThreadState;
import com.hazelcast.simulator.test.annotations.AfterRun;
import com.hazelcast.simulator.test.annotations.Prepare;
import com.hazelcast.simulator.test.annotations.Setup;
import com.hazelcast.simulator.test.annotations.TimeStep;
import com.hazelcast.simulator.test.annotations.Verify;
import com.hazelcast.simulator.tests.cp.helpers.CpMapOperationCounter;
import com.hazelcast.simulator.utils.GeneratorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hazelcast.simulator.utils.CommonUtils.sleepSeconds;
import static org.junit.Assert.assertTrue;

/**
 * This test is running as part of release verification simulator test. Hence every change in this class should be
 * discussed with QE team since it can affect release verification tests.
 */
public class CPMapTest extends HazelcastTest {
    // number of cp groups to host the created maps; if (distinctMaps % maps) == 0 then there's a uniform distribution of maps
    // over cp groups, otherwise maps are allocated per-cp group in a RR-fashion. If cpGroups == 0 then all CPMap instances will
    // be hosted by the default CPGroup. When cpGroups > 0, we create and host CPMaps across non-default CP Groups.
    public int cpGroups = 0;
    // number of distinct maps to create and use during the tests
    public int maps = 1;
    // number of distinct keys to create and use per-map; key domain is [0, keys)
    public int keys = 1;
    // number of possible values
    public int valuesCount = 100;
    // size in bytes for each key's associated value
    public int valueSizeBytes = 100;
    public int keySizeBytes = 100;
    private String val;

    private List<CPMap<String, String>> mapReferences;

    private byte[][] values;
    private List<String> keyPool;

    private IList<CpMapOperationCounter> operationCounterList;

    @Setup
    public void setup() {
        val = new String(new byte[32]);
        keyPool = new ArrayList<>();

        logger.info("Generating " + keys + " keys");
        for (int i = 0; i < keys; i++) {
            keyPool.add(generateString(i));
        }
        logger.info("Generated " + keys + " keys");

        // (1) create the cp group names that will host the maps
        String[] cpGroupNames = createCpGroupNames();
        // (2) create the map names + associated proxies (maps aren't created until you actually interface with them)
        mapReferences = new ArrayList<>();
        for (int i = 0; i < maps; i++) {
            String cpGroup = cpGroupNames[i % cpGroupNames.length];
            String mapName = "map" + i + "@" + cpGroup;
            logger.info("Creating CPMap: " + mapName);
            mapReferences.add(targetInstance.getCPSubsystem().getMap(mapName));
        }

        operationCounterList = targetInstance.getList(name + "Report");
        logger.info("Sleeping for 60s to allow CP Subsystem to stabilize");
        sleepSeconds(60);
    }

    @Prepare(global = true)
    public void prepare() {
        // Determine the number of threads, e.g., based on available processors
        int threadCount = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Calculate batch size
        int batchSize = (int) Math.ceil((double) keyPool.size() / threadCount);

        // Iterate through mapReferences
        for (CPMap<String, String> map : mapReferences) {
            // Split the keyPool into batches
            for (int t = 0; t < threadCount; t++) {
                // Define the range of keys for this batch
                int start = t * batchSize;
                int end = Math.min(start + batchSize, keyPool.size());

                // Create a sublist for this thread
                List<String> keyBatch = keyPool.subList(start, end);

                // Submit each batch processing task to the executor
                executor.submit(() -> {
                    int i = 0;
                    for (String key : keyBatch) {
                        map.set(key, val);

                        // Logging every 10,000 keys
                        if (i++ % 10000 == 0) {
                            logger.info("Preloaded " + i + " keys in this batch.");
                        }
                    }
                });
            }
        }

        // Shutdown the executor service and wait for all tasks to complete
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                logger.warn("Timeout waiting for tasks to finish.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("Thread execution interrupted", e);
            executor.shutdownNow();
        }
    }

    private byte[][] createValues() {
        byte[][] valuesArray = new byte[valuesCount][valueSizeBytes];
        Random random = new Random(0);
        for (int i = 0; i < valuesArray.length; i++) {
            valuesArray[i] = GeneratorUtils.generateByteArray(random, valueSizeBytes);
        }
        return valuesArray;
    }

    private String[] createCpGroupNames() {
        if (cpGroups == 0) {
            return new String[]{"default"};
        }

        String[] cpGroupNames = new String[cpGroups];
        for (int i = 0; i < cpGroups; i++) {
            cpGroupNames[i] = "cpgroup-" + i;
        }
        return cpGroupNames;
    }

    @TimeStep(prob = 1)
<<<<<<< Updated upstream
    public void set(ThreadState state) {
        state.randomMap().set(state.randomKey(), state.randomValue());
        state.operationCounter.setCount++;
    }

    @TimeStep(prob = 0)
    public void put(ThreadState state) {
        state.randomMap().put(state.randomKey(), state.randomValue());
        state.operationCounter.putCount++;
    }

    @TimeStep(prob = 0)
    public void putIfAbsent(ThreadState state) {
        state.randomMap().putIfAbsent(state.randomKey(), state.randomValue());
        state.operationCounter.putIfAbsentCount++;
    }

    @TimeStep(prob = 0)
    public void get(ThreadState state) {
        state.randomMap().get(state.randomKey());
        state.operationCounter.getCount++;
    }

    // 'remove' and 'delete' other than their first invocation pointless -- we're just timing the logic that underpins the
    // retrieval of no value.

    @TimeStep(prob = 0)
    public void remove(ThreadState state) {
        state.randomMap().remove(state.randomKey());
        state.operationCounter.removeCount++;
    }

    @TimeStep(prob = 0)
    public void delete(ThreadState state) {
        state.randomMap().delete(state.randomKey());
        state.operationCounter.deleteCount++;
    }

    @TimeStep(prob = 0)
    public void cas(ThreadState state) {
        CPMap<Integer, byte[]> randomMap = state.randomMap();
        Integer key = state.randomKey();
        byte[] expectedValue = randomMap.get(key);
        if (expectedValue != null) {
            randomMap.compareAndSet(key, expectedValue, state.randomValue());
            state.operationCounter.casCount++;
        }
    }

    @TimeStep(prob = 0)
    public void setThenDelete(ThreadState state) {
        CPMap<Integer, byte[]> map = state.randomMap();
        int key = state.randomKey();
        map.set(key, state.randomValue());
        map.delete(key);
    }

=======
    public void putIfAbsent(ThreadState state) {
        state.getNextMap().putIfAbsent(keyPool.get(state.randomKey()), val);
        state.operationCounter.putIfAbsentCount++;
    }

>>>>>>> Stashed changes
    @AfterRun
    public void afterRun(ThreadState state) {
        operationCounterList.add(state.operationCounter);
    }

    @Verify(global = true)
    public void verify() {
        // print stats
        CpMapOperationCounter total = new CpMapOperationCounter();
        for (CpMapOperationCounter operationCounter : operationCounterList) {
            total.add(operationCounter);
        }
        logger.info(name + ": " + total + " from " + operationCounterList.size() + " worker threads");

        // basic verification
        for (CPMap<String, String> mapReference : mapReferences) {
            int entriesCount = 0;
            for (int key = 0; key < keys; key++) {
                String get = mapReference.get(keyPool.get(key));
                if (get != null) {
                    entriesCount++;
                }
            }
            // Just check that CP map after test contains any item.
            // In theory we can deliberately remove all keys but this is not expected way how we want to use this test.
            logger.info(name + ":  CP Map " + mapReference.getName() + " entries count: " + entriesCount);
            assertTrue("CP Map " + mapReference.getName() + " doesn't contain any of expected items.", entriesCount > 0);
        }
    }

    public class ThreadState extends BaseThreadState {

        final CpMapOperationCounter operationCounter = new CpMapOperationCounter();

        public int randomKey() {
            return randomInt(keys); // [0, keys)
        }

        public byte[] randomValue() {
            return values[randomInt(valuesCount)]; // [0, values)
        }

<<<<<<< Updated upstream
        public CPMap<Integer, byte[]> randomMap() {
            return mapReferences.get(randomInt(maps));
=======
        public CPMap<String, String> getNextMap() {
            if (currentMapIndex == maps) {
                currentMapIndex = 0;
            }
            return mapReferences.get(currentMapIndex++);
>>>>>>> Stashed changes
        }
    }

    private String generateString(int number) {
        String prefix = "PREFIX_";

        String numberStr = Integer.toString(number);

        int totalCharacters = keySizeBytes / 2;
        int remainingLength = totalCharacters - (prefix.length() + numberStr.length());

        Random random = new Random();
        StringBuilder randomDigits = new StringBuilder(remainingLength);
        for (int i = 0; i < remainingLength; i++) {
            randomDigits.append(random.nextInt(10));  // Random digit between 0-9
        }

        String result = prefix + numberStr + randomDigits;

        // Ensure the result is exactly the desired number of characters
        if (result.length() != totalCharacters) {
            throw new IllegalStateException("Generated string is not exactly the correct length.");
        }

        return result;
    }

}