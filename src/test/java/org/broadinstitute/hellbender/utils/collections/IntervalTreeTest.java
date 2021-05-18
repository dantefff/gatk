package org.broadinstitute.hellbender.utils.collections;

import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import org.broadinstitute.hellbender.GATKBaseTest;
import org.broadinstitute.hellbender.engine.GATKPath;
import org.broadinstitute.hellbender.utils.CollatingInterval;
import org.broadinstitute.hellbender.utils.IntervalUtils;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.reference.ReferenceUtils;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class IntervalTreeTest extends GATKBaseTest {
    final static int ARRAY_SIZE = 10000000;
    final static Random rand = new Random(ARRAY_SIZE);

    @Test
    public void testSortSpeed() {
        final SAMSequenceDictionary dict = ReferenceUtils.loadFastaDictionary(new GATKPath(FULL_HG38_DICT));
        final List<CollatingInterval> arr = new ArrayList<>(ARRAY_SIZE);
        int nnn = ARRAY_SIZE;
        while ( nnn-- > 0 ) {
            final SAMSequenceRecord contig =
                    dict.getSequence(randIndex(26));
            final int start = randIndex(contig.getSequenceLength() - 151) + 1;
            arr.add(new CollatingInterval(contig, start, start + 151));
        }
        final List<CollatingInterval> testArr1 = new ArrayList<>(arr);
        long startTime1 = System.nanoTime();
        testArr1.sort(Comparator.naturalOrder());
        double elapsed1 = (System.nanoTime() - startTime1)/1000000000.;

        final List<SimpleInterval> testArr2 = new ArrayList<>(ARRAY_SIZE);
        arr.forEach(i ->
                testArr2.add(new SimpleInterval(new String(i.getContig().getBytes()), i.getStart(), i.getEnd())));
        long startTime2 = System.nanoTime();
        testArr2.sort(IntervalUtils.getDictionaryOrderComparator(dict));
        double elapsed2 = (System.nanoTime() - startTime2)/1000000000.;
        System.out.println("collating: " + elapsed1 + " secs vs. simple: " + elapsed2 + " ratio: " + elapsed2/elapsed1 + "x");
    }

    public static int randIndex( final int bound ) {
        return (rand.nextInt() & Integer.MAX_VALUE) % bound;
    }
}
