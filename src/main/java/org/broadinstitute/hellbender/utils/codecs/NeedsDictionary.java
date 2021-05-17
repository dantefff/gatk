package org.broadinstitute.hellbender.utils.codecs;

import htsjdk.samtools.SAMSequenceDictionary;

public interface NeedsDictionary {
    void setDictionary( SAMSequenceDictionary dict );
}
