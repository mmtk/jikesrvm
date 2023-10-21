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
package org.jikesrvm.mm.mmtk;

import org.jikesrvm.VM;
import org.mmtk.plan.RefLifecycleTracer;
import org.mmtk.utility.deque.ObjectReferenceBuffer;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.ObjectReference;
import org.vmmagic.pragma.Inline;
import org.vmmagic.pragma.UninterruptibleNoWarn;

import static org.jikesrvm.runtime.SysCall.sysCall;

public final class RustTraceLocal implements RefLifecycleTracer {
    private Address tracer;
    private Address traceObjectCallback;

    public RustTraceLocal(Address traceObjectCallback, Address tracer) {
        this.tracer = tracer;
        this.traceObjectCallback = traceObjectCallback;
    }

    @Inline
    @UninterruptibleNoWarn
    public boolean isLive(ObjectReference object) {
        if (VM.BuildWithRustMMTk && !VM.UseBindingSideRefProc) {
            return false;
        } else {
            return sysCall.is_reachable(object);
        }
    }

    @Inline
    @UninterruptibleNoWarn
    public ObjectReference getForwardedFinalizable(ObjectReference object) {
        if (VM.BuildWithRustMMTk && !VM.UseBindingSideRefProc) {
            return Address.zero().toObjectReference();
        } else {
            return sysCall.get_forwarded_object(object);
        }
    }

    @Inline
    @UninterruptibleNoWarn
    public ObjectReference getForwardedReferent(ObjectReference object) {
        if (VM.BuildWithRustMMTk && !VM.UseBindingSideRefProc) {
            return Address.zero().toObjectReference();
        } else {
            return sysCall.get_forwarded_object(object);
        }
    }

    @Inline
    @UninterruptibleNoWarn
    public ObjectReference getForwardedReference(ObjectReference object) {
        if (VM.BuildWithRustMMTk && !VM.UseBindingSideRefProc) {
            return Address.zero().toObjectReference();
        } else {
            return sysCall.get_forwarded_object(object);
        }
    }

    @Inline
    @UninterruptibleNoWarn
    public ObjectReference retainReferent(ObjectReference object) {
        if (VM.BuildWithRustMMTk && !VM.UseBindingSideRefProc) {
            return Address.zero().toObjectReference();
        } else {
            Address obj = sysCall.sysDynamicCall2(traceObjectCallback, tracer.toWord(), object.toAddress().toWord());
            return obj.toObjectReference();
        }
    }

    @Inline
    @UninterruptibleNoWarn
    public ObjectReference retainForFinalize(ObjectReference object) {
        if (VM.BuildWithRustMMTk && !VM.UseBindingSideRefProc) {
            return Address.zero().toObjectReference();
        } else {
            Address obj = sysCall.sysDynamicCall2(traceObjectCallback, tracer.toWord(), object.toAddress().toWord());
            return obj.toObjectReference();
        }
    }
}
