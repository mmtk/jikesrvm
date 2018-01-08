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
