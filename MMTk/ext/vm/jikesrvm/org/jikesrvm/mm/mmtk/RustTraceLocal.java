package org.mmtk.mmtk;

import org.mmtk.plan.TraceLocal;

import org.vmmagic.unboxed.Address;

public final class RustTraceLocal /* extends TraceLocal */ {
    public Address addr;

    public RustTraceLocal(Address traceAddress) {
        this.addr = addr;
    }

    @Inline
    public boolean isLive(ObjectReference object) {
        return sysIsLiveObject(object); // sysislive
    }

    public ObjectReference getForwardedFinalizable(ObjectReference object) {
        return getForwardedReference(object);
    }

    @Inline
    public ObjectReference getForwardedReference(ObjectReference object) {
        return traceObject(object);
    }

    @Inline
    public ObjectReference traceObject(ObjectReference object) {
        return ObjectReference.nullReference();
    }

    public ObjectReference retainForFinalize(ObjectReference object) {
        return traceObject(object);
    }
}
