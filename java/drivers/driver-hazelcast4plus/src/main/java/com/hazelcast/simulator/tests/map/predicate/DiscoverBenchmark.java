package com.hazelcast.simulator.tests.map.predicate;

import com.hazelcast.map.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.PredicateBuilder;
import com.hazelcast.query.Predicates;
import com.hazelcast.simulator.hz.HazelcastTest;
import com.hazelcast.simulator.hz.IdentifiedDataSerializablePojo;
import com.hazelcast.simulator.test.annotations.Prepare;
import com.hazelcast.simulator.test.annotations.Setup;
import com.hazelcast.simulator.test.annotations.Teardown;
import com.hazelcast.simulator.test.annotations.TimeStep;
import com.hazelcast.simulator.worker.loadsupport.Streamer;
import com.hazelcast.simulator.worker.loadsupport.StreamerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Random;


public class DiscoverBenchmark extends HazelcastTest {

    // properties
    // the number of map entries
    public int alpEntryCount = 22_927_150;
    public int bcdEntryCount = 1095;
    public int compromisedEntryCount = 3_406_024;
    public int dfsEntryCount = 19_620_356;
    public int versionMapEntryCount = 4;

    private IMap<Integer, IdentifiedDataSerializablePojo> alpMap;
    private IMap<Integer, IdentifiedDataSerializablePojo> bcdMap;
    private IMap<Integer, IdentifiedDataSerializablePojo> compMap;
    private IMap<Integer, IdentifiedDataSerializablePojo> dfsMap;
    private IMap<Integer, IdentifiedDataSerializablePojo> versionMap;

    private String[] panArray = new String[alpEntryCount];

    private final int arraySize = 7;
    private Random random;
    private PanGenerator panGenerator = new PanGenerator();

    private long testStart;

    @Setup
    public void setUp() {
        this.alpMap = targetInstance.getMap("alpMap");
        this.bcdMap = targetInstance.getMap("bcdNegfileMap");
        this.compMap = targetInstance.getMap("compMap");
        this.dfsMap = targetInstance.getMap("dfsNegfileMap");
        this.versionMap = targetInstance.getMap("versionMap");
        this.random = new Random();
        this.panGenerator = new PanGenerator();
        for (int i = 0; i < panArray.length; i++) {
            panArray[i] = panGenerator.createRandomPan();
        }
    }

    @Prepare(global = true)
    public void prepare() {
        Streamer<Integer, IdentifiedDataSerializablePojo> alpStreamer = StreamerFactory.getInstance(alpMap);
        Streamer<Integer, IdentifiedDataSerializablePojo> bcdStreamer = StreamerFactory.getInstance(bcdMap);
        Streamer<Integer, IdentifiedDataSerializablePojo> compStreamer = StreamerFactory.getInstance(compMap);
        Streamer<Integer, IdentifiedDataSerializablePojo> dfsStreamer = StreamerFactory.getInstance(dfsMap);
        Streamer<Integer, IdentifiedDataSerializablePojo> versionStreamer = StreamerFactory.getInstance(versionMap);

        Integer[] sampleArray = new Integer[arraySize];
        for (int i = 0; i < arraySize; i++) {
            sampleArray[i] = i;
        }

        pushAndAwait(alpEntryCount, sampleArray, alpStreamer);
        pushAndAwait(bcdEntryCount, sampleArray, bcdStreamer);
        pushAndAwait(compromisedEntryCount, sampleArray, compStreamer);
        pushAndAwait(dfsEntryCount, sampleArray, dfsStreamer);
        pushAndAwait(versionMapEntryCount, sampleArray, versionStreamer);
        testStart = System.currentTimeMillis();

    }

    private void pushAndAwait(int entryCount, Integer[] sample, Streamer<Integer, IdentifiedDataSerializablePojo> streamer) {
        for (int i = 0; i < entryCount; i++) {
            Integer key = i;
            IdentifiedDataSerializablePojo value = new IdentifiedDataSerializablePojo(sample, panArray[i]);
            streamer.pushEntry(key, value);
        }
        streamer.await();
    }

    @TimeStep
    public void timeStep() throws Exception {
        String pan = panArray[random.nextInt(alpEntryCount)];
        PredicateBuilder.EntryObject entryObject = Predicates.newPredicateBuilder().getEntryObject();
        Predicate<Integer, IdentifiedDataSerializablePojo> predicate = entryObject.get("valueField").equal(pan);
        IMap<Integer, IdentifiedDataSerializablePojo> alpDataMap = targetInstance.getMap("alpMap");
        Collection<? extends IdentifiedDataSerializablePojo> alpValues = alpDataMap.values(predicate);
        // Query2:
        PredicateBuilder.EntryObject entryObject2 = Predicates.newPredicateBuilder().getEntryObject();
        Predicate<Integer, IdentifiedDataSerializablePojo> predicate2 = entryObject2.get("valueField").equal(pan);
        IMap<Integer, IdentifiedDataSerializablePojo> compromisedMap = targetInstance.getMap("compMap");
        List<IdentifiedDataSerializablePojo> compromisedList = (List<IdentifiedDataSerializablePojo>) compromisedMap.values(predicate2);
    }

    public static final class PanGenerator {

        private static final Random random = new Random();

        // Function to generate a random PAN
        public String createRandomPan() {
            // Generate the first 15 digits of the PAN (excluding the check digit)
            StringBuilder panBuilder = new StringBuilder();

            // Example Issuer Identification Number (IIN) - First 6 digits
            panBuilder.append(String.format("%06d", random.nextInt(999999))); // First 6 digits

            // Generate the next 9 digits randomly
            for (int i = 0; i < 9; i++) {
                panBuilder.append(random.nextInt(10)); // Append a random digit
            }

            // Now we need to calculate the check digit (16th digit) using the Luhn algorithm
            int checkDigit = calculateLuhnCheckDigit(panBuilder.toString());
            panBuilder.append(checkDigit);

            // Return the 16-digit PAN as a String
            return panBuilder.toString();
        }

        // Luhn Algorithm to calculate the check digit
        private int calculateLuhnCheckDigit(String pan) {
            int sum = 0;
            boolean alternate = false;

            // Traverse the PAN in reverse (starting from the last digit)
            for (int i = pan.length() - 1; i >= 0; i--) {
                int digit = pan.charAt(i) - '0'; // Convert char to int

                if (alternate) {
                    digit *= 2;
                    if (digit > 9) {
                        digit -= 9; // If the result is greater than 9, subtract 9
                    }
                }

                sum += digit;
                alternate = !alternate;
            }

            // Calculate the check digit to make the sum a multiple of 10
            return (10 - (sum % 10)) % 10;
        }
    }

    @Teardown
    public void tearDown() {
        alpMap.destroy();
        bcdMap.destroy();
        compMap.destroy();
        dfsMap.destroy();
        versionMap.destroy();
    }
}
