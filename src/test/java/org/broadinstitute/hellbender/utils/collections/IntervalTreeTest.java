package org.broadinstitute.hellbender.utils.collections;

import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import org.broadinstitute.hellbender.GATKBaseTest;
import org.broadinstitute.hellbender.engine.GATKPath;
import org.broadinstitute.hellbender.utils.CollatingInterval;
import org.broadinstitute.hellbender.utils.IntervalUtils;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.reference.ReferenceUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
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
        double elapsed1 = timeSortingCollatingIntervals(arr);
        double elapsed2 = timeSortingSimpleIntervals(arr, dict, false);
        double elapsed3 = timeSortingSimpleIntervals(arr, dict, true);
        elapsed3 = (elapsed3 + timeSortingSimpleIntervals(arr, dict, true)) / 2;
        elapsed2 = (elapsed2 + timeSortingSimpleIntervals(arr, dict, false)) / 2;
        elapsed1 = (elapsed1 + timeSortingCollatingIntervals(arr)) / 2;
        System.out.println("collating: " + elapsed1 + " secs");
        System.out.println("interned:  " + elapsed2 + " ratio: " + elapsed2/elapsed1 + "x");
        System.out.println("novel str: " + elapsed3 + " ratio: " + elapsed3/elapsed1 + "x");
        Assert.assertTrue(elapsed2 > elapsed1);
        Assert.assertTrue(elapsed3 > elapsed1);
    }

    public static double timeSortingCollatingIntervals( final List<CollatingInterval> inputs ) {
        final List<CollatingInterval> testArr1 = new ArrayList<>(inputs);
        long startTime1 = System.nanoTime();
        testArr1.sort(Comparator.naturalOrder());
        return (System.nanoTime() - startTime1)/1000000000.;
    }

    public static double timeSortingSimpleIntervals( final List<CollatingInterval> inputs,
                                                     final SAMSequenceDictionary dict,
                                                     final boolean createNovelString ) {
        final List<SimpleInterval> testArr2 = new ArrayList<>(ARRAY_SIZE);
        inputs.forEach(i ->
                testArr2.add(new SimpleInterval(
                        createNovelString ? new String(i.getContig().getBytes()) : i.getContig(),
                        i.getStart(), i.getEnd())));
        long startTime2 = System.nanoTime();
        testArr2.sort(IntervalUtils.getDictionaryOrderComparator(dict));
        return (System.nanoTime() - startTime2)/1000000000.;
    }

    public static int randIndex( final int bound ) {
        return (rand.nextInt() & Integer.MAX_VALUE) % bound;
    }
}
