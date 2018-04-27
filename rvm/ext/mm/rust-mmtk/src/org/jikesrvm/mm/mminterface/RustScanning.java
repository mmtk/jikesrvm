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

import org.jikesrvm.VM;
import org.jikesrvm.classloader.RVMType;
import org.jikesrvm.objectmodel.ObjectModel;
import org.jikesrvm.runtime.BootRecord;
import org.jikesrvm.scheduler.RVMThread;
import org.vmmagic.pragma.Entrypoint;
import org.vmmagic.pragma.Inline;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.ObjectReference;
import org.vmmagic.unboxed.Offset;

import static org.jikesrvm.mm.mminterface.MemoryManagerConstants.BYTES_IN_ADDRESS;
import static org.jikesrvm.mm.mminterface.MemoryManagerConstants.BITS_IN_BYTE;
import static org.mmtk.utility.Constants.LOG_BYTES_IN_ADDRESS;

public class RustScanning {

    public static final boolean VALIDATE_REFS = VM.VerifyAssertions;

    private static final boolean DEBUG = false;
    private static final boolean FILTER = true;

    private static final int LOG_CHUNK_BYTES = 12;
    private static final int LONGENCODING_MASK = 0x1;
    private static final int RUN_MASK = 0x2;
    private static final int LONGENCODING_OFFSET_BYTES = 4;
    private static final int ADDRESS_LENGTH = BYTES_IN_ADDRESS * 8;

    /* statistics */
    static int roots = 0;
    static int refs = 0;

    @Entrypoint
    public static int[] getOffsetArray(Object object) {
        ObjectReference objectRef = ObjectReference.fromObject(object);
        RVMType type = ObjectModel.getObjectType(objectRef.toObject());
        return type.getReferenceOffsets();
    }

    @Inline
    @Uninterruptible
    @Entrypoint
    public static void scanBootImage(Address root) {
        /* establish sentinels in map & image */
        Address mapStart = BootRecord.the_boot_record.bootImageRMapStart;
        Address mapEnd = BootRecord.the_boot_record.bootImageRMapEnd;
        Address imageStart = BootRecord.the_boot_record.bootImageDataStart;

        /* establish roots */
        Address rootCursor = root;

        /* figure out striding */
        int stride = 1 << LOG_CHUNK_BYTES;
        int start = 0 << LOG_CHUNK_BYTES;
        Address cursor = mapStart.plus(start);

        /* statistics */
        roots = 0;
        refs = 0;

        /* process chunks in parallel till done */
        while (cursor.LT(mapEnd)) {
            rootCursor = processChunk(cursor, imageStart, mapStart, mapEnd, rootCursor);
            cursor = cursor.plus(stride);
        }

        /* print some debugging stats */
        /*
        Log.write("<boot image");
        Log.write(" roots: ", roots);
        Log.write(" refs: ", refs);
        Log.write(">");
        */
    }

    @Inline
    @Uninterruptible
    static Address processChunk(Address chunkStart, Address imageStart,
                                    Address mapStart, Address mapEnd, Address rootCursor) {
        int value;
        Offset offset = Offset.zero();
        Address cursor = chunkStart;
        while ((value = (cursor.loadByte() & 0xff)) != 0) {
            /* establish the offset */
            if ((value & LONGENCODING_MASK) != 0) {
                offset = decodeLongEncoding(cursor);
                cursor = cursor.plus(LONGENCODING_OFFSET_BYTES);
            } else {
                offset = offset.plus(value & 0xfc);
                cursor = cursor.plus(1);
            }
            /* figure out the length of the run, if any */
            int runlength = 0;
            if ((value & RUN_MASK) != 0) {
                runlength = cursor.loadByte() & 0xff;
                cursor = cursor.plus(1);
            }
            /* enqueue the specified slot or slots */
            if (VM.VerifyAssertions) VM._assert(isAddressAligned(offset));
            Address slot = imageStart.plus(offset);
            if (DEBUG) refs++;
            if (!FILTER || slot.loadAddress().GT(mapEnd)) {
                if (DEBUG) roots++;
                rootCursor.store(slot);
                rootCursor.plus(ADDRESS_LENGTH);
                //trace.processRootEdge(slot, false);
            }
            if (runlength != 0) {
                for (int i = 0; i < runlength; i++) {
                    offset = offset.plus(BYTES_IN_ADDRESS);
                    slot = imageStart.plus(offset);
                    if (VM.VerifyAssertions) VM._assert(isAddressAligned(slot));
                    if (DEBUG) refs++;
                    if (!FILTER || slot.loadAddress().GT(mapEnd)) {
                        if (DEBUG) roots++;
                        if (VALIDATE_REFS) checkReference(slot);
                        rootCursor.store(slot);
                        rootCursor.plus(ADDRESS_LENGTH);
                        //trace.processRootEdge(slot, false);
                    }
                }
            }
        }
        return rootCursor;
    }

    /**
     * Decode a 4-byte encoding, taking a pointer to the first byte of
     * the encoding, and returning the encoded value as an <code>Offset</code>
     *
     * @param cursor A pointer to the first byte of encoded data
     * @return The encoded value as an <code>Offset</code>
     */
    @Inline
    @Uninterruptible
    public static Offset decodeLongEncoding(Address cursor) {
        int value;
        value  = (cursor.loadByte())                                              & 0x000000fc;
        value |= (cursor.loadByte(Offset.fromIntSignExtend(1)) << BITS_IN_BYTE)     & 0x0000ff00;
        value |= (cursor.loadByte(Offset.fromIntSignExtend(2)) << (2 * BITS_IN_BYTE)) & 0x00ff0000;
        value |= (cursor.loadByte(Offset.fromIntSignExtend(3)) << (3 * BITS_IN_BYTE)) & 0xff000000;
        return Offset.fromIntSignExtend(value);
    }


    /**
     * Check that a reference encountered during scanning is valid.  If
     * the reference is invalid, dump stack and die.
     *
     * @param refaddr The address of the reference in question.
     */
    @Uninterruptible
    public static void checkReference(Address refaddr) {
        ObjectReference ref = Selected.Plan.get().loadObjectReference(refaddr);
        if (!MemoryManager.validRef(ref)) {
            //Log.writeln();
            //Log.writeln("Invalid ref reported while scanning boot image");
            //Log.writeln();
            //Log.write(refaddr);
            //Log.write(":");
            //Log.flush();
            MemoryManager.dumpRef(ref);
            //Log.writeln();
            //Log.writeln("Dumping stack:");
            RVMThread.dumpStack();
            VM.sysFail("\n\nScanStack: Detected bad GC map; exiting RVM with fatal error");
        }
    }

    /**
     * Return true if the given offset is address-aligned
     * @param offset the offset to be check
     * @return true if the offset is address aligned.
     */
    @Uninterruptible
    public static boolean isAddressAligned(Offset offset) {
        return (offset.toLong() >> LOG_BYTES_IN_ADDRESS) << LOG_BYTES_IN_ADDRESS == offset.toLong();
    }

}
