package org.jaybill.jbio.core;

import lombok.Data;

@Data
public class ReadBehavior {
    private int maxReadCountPerLoop;
    private ByteBufferAllocateStrategy strategy;
}
