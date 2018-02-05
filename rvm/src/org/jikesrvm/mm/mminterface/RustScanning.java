/*
 *  This file is part of the Jikes RVM project (http://jikesrvm.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  See the COPYRIGHT.txt file distributed with this work for information
 *  regarding copyright ownership.
 */
package org.jikesrvm.mm.mminterface;

import org.jikesrvm.classloader.RVMType;
import org.jikesrvm.objectmodel.ObjectModel;
import org.vmmagic.pragma.Entrypoint;
import org.vmmagic.unboxed.ObjectReference;

public class RustScanning {

    @Entrypoint
    public static int[] getOffsetArray(Object object) {
        ObjectReference objectRef = ObjectReference.fromObject(object);
        RVMType type = ObjectModel.getObjectType(objectRef.toObject());
        return type.getReferenceOffsets();
    }
}
