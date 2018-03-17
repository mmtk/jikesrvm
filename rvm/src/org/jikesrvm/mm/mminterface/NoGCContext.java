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

import static org.jikesrvm.runtime.SysCall.sysCall;

@Uninterruptible
public class NoGCContext {
    private NoGCContext() {}

    /*public static void setBlock(Address mmtkHandle, Address src) {
        Memory.memcopy(mmtkHandle, src, 16);
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

    @Inline
    public static Address alloc(Address mmtkHandle, int bytes, int align, int offset, int allocator, int site) {
        Address region;
        Address cursor = getCursor(mmtkHandle);
        Address sentinel = getSentinel(mmtkHandle);

        // Align allocation
        Word mask = Word.fromIntSignExtend(align - 1);
        Word negOff = Word.fromIntSignExtend(-offset);

        Offset delta = negOff.minus(cursor.toWord()).and(mask).toOffset();

        Address result = cursor.plus(delta);

        Address newCursor = result.plus(bytes);

        if (newCursor.GT(sentinel)) {
            region = sysCall.sysAllocSlow(mmtkHandle, bytes, align, offset, allocator);
        } else {
            setCursor(mmtkHandle, newCursor);
            region = result;
        }
        return region;
    }
}
