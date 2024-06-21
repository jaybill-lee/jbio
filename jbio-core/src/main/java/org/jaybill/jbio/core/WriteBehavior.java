package org.jaybill.jbio.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WriteBehavior {
    private int spinCount;
    private int maxWritePerLoop;
    private int highWatermark;
    private int lowWatermark;

}
