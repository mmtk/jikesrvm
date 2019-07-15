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

import org.mmtk.plan.PlanConstraints;
import org.mmtk.plan.nogc.NoGCMutator;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;
import org.jikesrvm.runtime.Magic;

import static org.jikesrvm.runtime.EntrypointHelper.getField;
import static org.jikesrvm.runtime.SysCall.sysCall;
import static org.jikesrvm.runtime.UnboxedSizeConstants.BYTES_IN_WORD;

@Uninterruptible
public class ConcNoGCContext extends NoGCMutator {

    @Uninterruptible
    public static class Constraints extends PlanConstraints {
        @Override
        public int gcHeaderBits() {
          return 1;
        }
        @Override
        public int gcHeaderWords() {
          return 2;
        }
        @Override
        public boolean needsObjectReferenceWriteBarrier() {
            return true;
        }
        @Override
        public boolean needsJavaLangReferenceReadBarrier() {
            return true;
        }
    }

    @Entrypoint Address threadId;
    @Entrypoint Address cursor;
    @Entrypoint Address limit;
    @Entrypoint Address space;
    @Entrypoint Address modbuf;
    @Entrypoint Address barrierFlag;

    static final Offset threadIdOffset = getField(ConcNoGCContext.class, "threadId", Address.class).getOffset();

    @Inline
    public Address getHandle() {
      return Magic.objectAsAddress(this).plus(threadIdOffset);
    }

    @Override
    public Address alloc(int bytes, int align, int offset, int allocator, int site) {
        return sysCall.sysAlloc(getHandle(), bytes, align, offset, allocator);
    }

    public Address setBlock(Address mmtkHandle) {
        threadId    = mmtkHandle.plus(BYTES_IN_WORD * 0).loadAddress();
        cursor      = mmtkHandle.plus(BYTES_IN_WORD * 1).loadAddress();
        limit       = mmtkHandle.plus(BYTES_IN_WORD * 2).loadAddress();
        space       = mmtkHandle.plus(BYTES_IN_WORD * 3).loadAddress();
        modbuf      = mmtkHandle.plus(BYTES_IN_WORD * 4).loadAddress();
        barrierFlag = mmtkHandle.plus(BYTES_IN_WORD * 5).loadAddress();
        return Magic.objectAsAddress(this).plus(threadIdOffset);
    }

    @Override
    public void postAlloc(ObjectReference ref, ObjectReference typeRef, int bytes, int allocator) {
        sysCall.sysPostAlloc(getHandle(), ref, typeRef, bytes, allocator);
    }

    @Override
    @Inline
    public void objectReferenceWrite(ObjectReference src, Address slot, ObjectReference value, Word metaDataA, Word metaDataB, int mode) {
        sysCall.sysObjectReferenceWriteSlow(getHandle(), src, slot, value);
        org.mmtk.vm.VM.barriers.objectReferenceWrite(src, value, metaDataA, metaDataB, mode);
    }

    @Inline
    @Override
    public boolean objectReferenceTryCompareAndSwap(ObjectReference src, Address slot, ObjectReference old, ObjectReference tgt, Word metaDataA, Word metaDataB, int mode) {
      boolean result = org.mmtk.vm.VM.barriers.objectReferenceTryCompareAndSwap(src, old, tgt, metaDataA, metaDataB, mode);
      sysCall.sysObjectReferenceTryCompareAndSwapSlow(getHandle(), src, slot, old, tgt);
      return result;
    }

    @Inline
    @Override
    public ObjectReference javaLangReferenceReadBarrier(ObjectReference ref) {
        return sysCall.sysJavaLangReferenceReadSlow(getHandle(), ref);
    }
}
