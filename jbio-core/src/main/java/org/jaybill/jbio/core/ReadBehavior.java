package org.jaybill.jbio.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadBehavior {
    private int maxReadCountPerLoop;
    private ByteBufferAllocateStrategy strategy;
}
