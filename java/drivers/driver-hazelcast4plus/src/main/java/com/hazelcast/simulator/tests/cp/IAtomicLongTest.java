package com.hazelcast.simulator.tests.cp;

import com.hazelcast.cp.CPSubsystem;
import com.hazelcast.cp.IAtomicLong;
import com.hazelcast.simulator.hz.HazelcastTest;
import com.hazelcast.simulator.test.BaseThreadState;
import com.hazelcast.simulator.test.annotations.Setup;
import com.hazelcast.simulator.test.annotations.Teardown;
import com.hazelcast.simulator.test.annotations.TimeStep;

public class IAtomicLongTest extends HazelcastTest {

    // properties
    // the number of IAtomicLongs
    public int referenceCount = 1;
    // the number of CPGroups. 0 means that the default CPGroup is used.
    // The AtomicRefs will be placed over the different CPGroups in round robin fashion.
    public int cpGroupCount = 0;
    private IAtomicLong[] references;

    @Setup
    public void setup() {
        CPSubsystem cpSubsystem = targetInstance.getCPSubsystem();

        references = new IAtomicLong[referenceCount];
        for (int i = 0; i < referenceCount; i++) {
            String cpGroupString = cpGroupCount == 0
                    ? ""
                    : "@" + (i % cpGroupCount);
            references[i] = cpSubsystem.getAtomicLong("ref-"+i + cpGroupString);
        }
    }

    @TimeStep(prob = 1)
    public long get(ThreadState state) {
        return state.randomCounter().get();
    }

    @TimeStep(prob = 0)
    public void set(ThreadState state) {
        state.randomCounter().set(state.randomLong());
    }


    public class ThreadState extends BaseThreadState {
        private IAtomicLong randomCounter() {
            int index = randomInt(references.length);
            return references[index];
        }
    }

    @Teardown
    public void teardown() {
        // for (IAtomicLong counter : references) {
        //     counter.destroy();
        // }
    }
}
