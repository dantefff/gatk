package org.broadinstitute.hellbender.tools.sv;

import com.google.common.annotations.VisibleForTesting;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.util.Locatable;
import htsjdk.tribble.Feature;
import org.broadinstitute.hellbender.utils.Nucleotide;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

@VisibleForTesting
public final class LocusDepth implements Feature {
    private final SAMSequenceRecord contig;
    private final int position;
    private final int refIdx; // index into nucleotideValues
    private final int altIdx; // index into nucleotideValues
    private int totalDepth;
    private int altDepth;
    public final static String BCI_VERSION = "1.0";

    // our own private copy so that we don't make repeated array allocations
    private final static Nucleotide[] nucleotideValues = Nucleotide.values();

    public LocusDepth( final SAMSequenceDictionary dict, final Locatable loc,
                       final int refIdx, final int altIdx ) {
        this(dict, loc.getContig(), loc.getStart(), refIdx, altIdx, 0, 0);
    }

    public LocusDepth( final SAMSequenceDictionary dict,
                       final String contigName, final int position,
                       final int refIdx, final int altIdx,
                       final int totalDepth, final int altDepth ) {
        this.contig = dict.getSequence(contigName);
        this.position = position;
        this.refIdx = refIdx;
        this.altIdx = altIdx;
        this.totalDepth = totalDepth;
        this.altDepth = altDepth;
    }

    public LocusDepth( final SAMSequenceDictionary dict,
                       final DataInputStream dis ) throws IOException {
        contig = dict.getSequence(dis.readInt());
        position = dis.readInt();
        refIdx = dis.readByte();
        altIdx = dis.readByte();
        totalDepth = dis.readInt();
        altDepth = dis.readInt();
    }

    public void observe( final int idx ) {
        if ( idx == altIdx ) altDepth += 1;
        totalDepth += 1;
    }

    public SAMSequenceRecord getSequenceRecord() {
        return contig;
    }

    @Override
    public String getContig() {
        return contig.getSequenceName();
    }

    @Override
    public int getEnd() {
        return position;
    }

    @Override
    public int getStart() {
        return position;
    }

    public char getRefCall() {
        return nucleotideValues[refIdx].encodeAsChar();
    }

    public char getAltCall() {
        return nucleotideValues[altIdx].encodeAsChar();
    }

    public int getAltDepth() {
        return altDepth;
    }

    public int getTotalDepth() {
        return totalDepth;
    }

    public void write( final DataOutputStream dos ) throws IOException {
        dos.writeInt(contig.getSequenceIndex());
        dos.writeInt(position);
        dos.writeByte(refIdx);
        dos.writeByte(altIdx);
        dos.writeInt(totalDepth);
        dos.writeInt(altDepth);
    }

    public String toString() {
        return getContig() + "\t" + getStart() + "\t" + nucleotideValues[refIdx] + "\t" +
                nucleotideValues[altIdx] + "\t" + totalDepth + "\t" + altDepth;
    }
}
