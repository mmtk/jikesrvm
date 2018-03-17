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

import org.vmmagic.Unboxed;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;
import org.jikesrvm.runtime.Memory;
import org.jikesrvm.runtime.Magic;
import org.mmtk.plan.Plan;

import static org.jikesrvm.runtime.SysCall.sysCall;

@Uninterruptible
public class SSContext {
    private SSContext() {}

    /*public static void setBlock(Address mmtkHandle, Address src) {
        Memory.memcopy(Magic.objectAsAddress(this), src, 32);
    }*/

    public static void setCursor(Address mmtkHandle, Address value) {
        Magic.setAddressAtOffset(mmtkHandle, Offset.fromIntSignExtend(4), value);
    }

    public static Address getCursor(Address mmtkHandle) {
        return Magic.getAddressAtOffset(mmtkHandle, Offset.fromIntSignExtend(4));
    }

    public static Address getSentinel(Address mmtkHandle) {
        return Magic.getAddressAtOffset(mmtkHandle, Offset.fromIntSignExtend(8));
    }

    public static void setImmortalCursor(Address mmtkHandle, Address value) {
        Magic.setAddressAtOffset(mmtkHandle, Offset.fromIntSignExtend(20), value);
    }

    public static Address getImmortalCursor(Address mmtkHandle) {
        return Magic.getAddressAtOffset(mmtkHandle, Offset.fromIntSignExtend(20));
    }

    public static Address getImmortalSentinel(Address mmtkHandle) {
        return Magic.getAddressAtOffset(mmtkHandle, Offset.fromIntSignExtend(24));
    }

    @Inline
    public static Address alloc(Address mmtkHandle, int bytes, int align, int offset, int allocator, int site) {
        Address region;
        Address cursor;
        Address sentinel;
        if (allocator == Plan.ALLOC_DEFAULT) {
            cursor = getCursor(mmtkHandle);
            sentinel = getSentinel(mmtkHandle);
        } else {
            cursor = getImmortalCursor(mmtkHandle);
            sentinel = getImmortalSentinel(mmtkHandle);
        }

        // Align allocation
        Word mask = Word.fromIntSignExtend(align - 1);
        Word negOff = Word.fromIntSignExtend(-offset);

        Offset delta = negOff.minus(cursor.toWord()).and(mask).toOffset();

        Address result = cursor.plus(delta);

        Address newCursor = result.plus(bytes);

        if (newCursor.GT(sentinel)) {
            region = sysCall.sysAllocSlow(mmtkHandle, bytes, align, offset, allocator);
        } else {
            if (allocator == Plan.ALLOC_DEFAULT) {
                setCursor(mmtkHandle, newCursor);
            } else {
                setImmortalCursor(mmtkHandle, newCursor);
            }
            region = result;
        }
        return region;
    }
}
