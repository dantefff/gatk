package org.broadinstitute.hellbender.utils.codecs;

import com.google.common.base.Splitter;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.util.IOUtil;
import htsjdk.tribble.AsciiFeatureCodec;
import htsjdk.tribble.index.tabix.TabixFormat;
import htsjdk.tribble.readers.LineIterator;
import org.broadinstitute.hellbender.tools.sv.LocusDepth;
import org.broadinstitute.hellbender.utils.Nucleotide;

import java.util.List;

public class LocusDepthCodec extends AsciiFeatureCodec<LocusDepth> {
    private static SAMSequenceDictionary dict;
    private static final String FORMAT_SUFFIX = ".ld.txt";
    private static final Splitter splitter = Splitter.on("\t");

    public LocusDepthCodec() {
        super(LocusDepth.class);
    }

    public static void setDictionary( final SAMSequenceDictionary dict ) {
        LocusDepthCodec.dict = dict;
    }

    @Override public TabixFormat getTabixFormat() {
        return new TabixFormat(TabixFormat.ZERO_BASED, 1, 2, 0, '#', 0);
    }

    @Override public LocusDepth decode( final String line ) {
        final List<String> tokens = splitter.splitToList(line);
        if ( tokens.size() != 6 ) {
            throw new IllegalArgumentException("Invalid number of columns: " + tokens.size());
        }
        return new LocusDepth(dict, tokens.get(0),
                Integer.parseUnsignedInt(tokens.get(1)) + 1,
                Nucleotide.decode(tokens.get(2)).ordinal(),
                Nucleotide.decode(tokens.get(3)).ordinal(),
                Integer.parseUnsignedInt(tokens.get(4)),
                Integer.parseUnsignedInt(tokens.get(5)));
    }

    @Override public Object readActualHeader( LineIterator reader ) { return null; }

    @Override
    public boolean canDecode( final String pathArg ) {
        final String path = pathArg.toLowerCase();
        final String toDecode =
                !IOUtil.hasBlockCompressedExtension(path) ? path : path.substring(0, path.lastIndexOf('.'));
        return toDecode.endsWith(FORMAT_SUFFIX);
    }

    public static String encode( final LocusDepth locusDepth ) {
        return locusDepth.getContig() + "\t" + (locusDepth.getStart() - 1) + "\t" +
                locusDepth.getRefCall() + "\t" + locusDepth.getAltCall() + "\t" +
                locusDepth.getTotalDepth() + "\t" + locusDepth.getAltDepth();
    }
}
