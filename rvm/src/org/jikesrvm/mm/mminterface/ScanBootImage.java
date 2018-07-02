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
import static org.jikesrvm.mm.mminterface.MemoryManagerConstants.LOG_BYTES_IN_ADDRESS;
import static org.jikesrvm.mm.mminterface.MemoryManagerConstants.BYTES_IN_WORD;


public class RustScanBootImage {

    public static final boolean VALIDATE_REFS = VM.VerifyAssertions;

    private static final boolean DEBUG = false;
    private static final boolean FILTER = true;

    private static final int LOG_CHUNK_BYTES = 12;
    private static final int CHUNK_BYTES = 1 << LOG_CHUNK_BYTES;
    private static final int LONGENCODING_MASK = 0x1;
    private static final int RUN_MASK = 0x2;
    private static final int MAX_RUN = (1 << BITS_IN_BYTE) - 1;
    private static final int LONGENCODING_OFFSET_BYTES = 4;
    private static final int GUARD_REGION = LONGENCODING_OFFSET_BYTES + 1; /* long offset + run encoding */
    private static final int BITS_IN_ADDRESS = BYTES_IN_ADDRESS * BITS_IN_BYTE;

    /* statistics */
    static int roots = 0;
    static int refs = 0;


    /****************************************************************************
     *
     * Build-time encoding (assumed to be single-threaded)
     */

    /** */
    private static int lastOffset = Integer.MIN_VALUE / 2;  /* bootstrap value */
    private static int oldIndex = 0;
    private static int codeIndex = 0;

    /* statistics */
    private static int shortRefs = 0;
    private static int runRefs = 0;
    private static int longRefs = 0;
    private static int startRefs = 0;


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
                rootCursor.plus(BITS_IN_ADDRESS);
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
                        rootCursor.plus(BITS_IN_ADDRESS);
                        //trace.processRootEdge(slot, false);
                    }
                }
            }
        }
        return rootCursor;
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

    /**
     * Return true if the given address is address-aligned
     * @param address the address to be check
     * @return true if the address is address aligned.
     */
    @Uninterruptible
    public static boolean isAddressAligned(Address address) {
        return (address.toLong() >> LOG_BYTES_IN_ADDRESS) << LOG_BYTES_IN_ADDRESS == address.toLong();
    }

    /**
     * Take a bytemap encoding of all references in the boot image, and
     * produce an encoded byte array.  Return the total length of the
     * encoding.
     *
     * @param bootImageRMap space for the compressed reference map. The map
     *  is initially empty and will be filled during execution of this method.
     * @param referenceMap the (uncompressed) reference map for the bootimage
     * @param referenceMapLimit the highest index in the referenceMap that
     *  contains a reference
     * @return the total length of the encoding
     */
    public static int encodeRMap(byte[] bootImageRMap, byte[] referenceMap,
                                 int referenceMapLimit) {
        for (int index = 0; index <= referenceMapLimit; index++) {
            if (referenceMap[index] == 1) {
                addOffset(bootImageRMap, index << LOG_BYTES_IN_ADDRESS);
            }
        }
        return codeIndex + 1;
    }

    /**
     * Encode a given offset (distance from the start of the boot image)
     * into the code array.
     *
     * @param code A byte array into which the value should be encoded
     * @param offset The offset value to be encoded
     */
    private static void addOffset(byte[] code, int offset) {
        if ((codeIndex ^ (codeIndex + GUARD_REGION)) >= CHUNK_BYTES) {
            codeIndex = (codeIndex + GUARD_REGION) & ~(CHUNK_BYTES - 1);
            oldIndex = codeIndex;
            codeIndex = encodeLongEncoding(code, codeIndex, offset);
            if (DEBUG) {
                startRefs++;
                /*
                Log.write("[chunk: ", codeIndex);
                Log.write(" offset: ", offset);
                Log.write(" last offset: ", lastOffset);
                Log.writeln("]");
                */
            }
        } else {
            int delta = offset - lastOffset;
            if (VM.VerifyAssertions) VM._assert((delta & 0x3) == 0);
            if (VM.VerifyAssertions) VM._assert(delta > 0);

            int currentrun = (code[codeIndex]) & 0xff;
            if ((delta == BYTES_IN_ADDRESS) &&
                    (currentrun < MAX_RUN)) {
                currentrun++;
                code[codeIndex] = (byte) (currentrun & 0xff);
                code[oldIndex] |= RUN_MASK;
                if (DEBUG) runRefs++;
            } else {
                if (currentrun != 0) codeIndex++;
                oldIndex = codeIndex;
                if (delta < 1 << BITS_IN_BYTE) {
                    /* common case: single byte encoding */
                    code[codeIndex++] = (byte) (delta & 0xff);
                    if (DEBUG) shortRefs++;
                } else {
                    /* else four byte encoding */
                    codeIndex = encodeLongEncoding(code, codeIndex, offset);
                    if (DEBUG) longRefs++;
                }
            }
        }
        if (offset != getOffset(code, oldIndex, lastOffset)) {
            /*
            Log.writeln("offset: ", offset);
            Log.writeln("last offset: ", lastOffset);
            Log.writeln("offset: ", getOffset(code, oldIndex, lastOffset));
            Log.writeln("index: ", oldIndex);
            Log.writeln("index: ", oldIndex & (CHUNK_BYTES - 1));
            Log.writeln();
            Log.writeln("1: ", code[oldIndex]);
            Log.writeln("2: ", code[oldIndex + 1]);
            Log.writeln("3: ", code[oldIndex + 2]);
            Log.writeln("4: ", code[oldIndex + 3]);
            Log.writeln("5: ", code[oldIndex + 4]);
            */
            if (VM.VerifyAssertions)
                VM._assert(offset == getOffset(code, oldIndex, lastOffset));
        }
        lastOffset = offset;
    }

    /****************************************************************************
     *
     * Utility encoding and decoding methods
     */

    /**
     * Decode an encoded offset given the coded byte array, and index
     * into it, and the current (last) offset.
     *
     * @param code A byte array containing the encoded value
     * @param index The offset into the code array from which to
     * commence decoding
     * @param lastOffset The current (last) encoded offset
     * @return The next offset, which is either explicitly encoded in
     * the byte array or inferred from an encoded delta and the last
     * offset.
     */
    private static int getOffset(byte[] code, int index, int lastOffset) {
        if ((code[index] & RUN_MASK) == RUN_MASK) {
            return lastOffset + BYTES_IN_WORD;
        } else {
            if (((index & (CHUNK_BYTES - 1)) == 0) ||
                    ((code[index] & LONGENCODING_MASK) == LONGENCODING_MASK)) {
                return decodeLongEncoding(code, index);
            } else {
                return lastOffset + ((code[index]) & 0xff);
            }
        }
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
     * Decode a 4-byte encoding, taking a byte array and an index into
     * it and returning the encoded value as an integer
     *
     * @param code A byte array containing the encoded value
     * @param index The offset into the code array from which to
     * commence decoding
     * @return The encoded value as an integer
     */
    @Inline
    @Uninterruptible
    private static int decodeLongEncoding(byte[] code, int index) {
        int value;
        value  = (code[index])                     & 0x000000fc;
        value |= (code[index + 1] << BITS_IN_BYTE)     & 0x0000ff00;
        value |= (code[index + 2] << (2 * BITS_IN_BYTE)) & 0x00ff0000;
        value |= (code[index + 3] << (3 * BITS_IN_BYTE)) & 0xff000000;
        return value;
    }

    /**
     * Encode a 4-byte encoding, taking a byte array, the current index into
     * it, and the value to be encoded.
     *
     * @param code A byte array to contain the encoded value
     * @param index The current offset into the code array
     * @param value The value to be encoded
     * @return The updated index into the code array
     */
    private static int encodeLongEncoding(byte[] code, int index, int value) {
        code[index++] = (byte) ((value & 0xff) | LONGENCODING_MASK);
        value = value >>> BITS_IN_BYTE;
        code[index++] = (byte) (value & 0xff);
        value = value >>> BITS_IN_BYTE;
        code[index++] = (byte) (value & 0xff);
        value = value >>> BITS_IN_BYTE;
        code[index++] = (byte) (value & 0xff);
        return index;
    }

}
