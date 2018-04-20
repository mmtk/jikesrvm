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
package org.jikesrvm.runtime;

import static org.jikesrvm.runtime.EntrypointHelper.getField;
import static org.jikesrvm.runtime.EntrypointHelper.getMethod;

import org.jikesrvm.VM;
import org.jikesrvm.classloader.RVMField;
import org.jikesrvm.classloader.RVMMethod;
import org.jikesrvm.classloader.NormalMethod;
import org.jikesrvm.scheduler.RVMThread;

/**
 * Fields and methods of the virtual machine that are needed by
 * compiler-generated machine code or C runtime code.
 */
public abstract class RustEntrypoints{
    // The usual causes for getField/Method() to fail are:
    //  1. you misspelled the class name, member name, or member signature
    //  2. the class containing the specified member didn't get compiled
    //
    public static final NormalMethod blockForGCMethod =
            getMethod(org.jikesrvm.mm.mminterface.MemoryManager.class, "blockForGC", "()V");
    public static final NormalMethod prepareMutatorMethod =
            getMethod(org.jikesrvm.mm.mminterface.MemoryManager.class, "prepareMutator",
                    "(Lorg/jikesrvm/scheduler/RVMThread;)V");
    public static final NormalMethod scanBootImageMethod =
            getMethod(org.jikesrvm.mm.mminterface.RustScanning.class, "scanBootImage", "(Lorg/vmmagic/unboxed/Address;)V");
    public static final NormalMethod testMethod = VM.BuildWithRustMMTk ?
            getMethod(org.jikesrvm.mm.mminterface.MemoryManager.class, "test", "(I)I") :
            getMethod(org.jikesrvm.runtime.RuntimeEntrypoints.class, "raiseIllegalAccessError", "()V");
    public static final NormalMethod test1Method = VM.BuildWithRustMMTk ?
            getMethod(org.jikesrvm.mm.mminterface.MemoryManager.class, "test1", "()V") :
            getMethod(org.jikesrvm.runtime.RuntimeEntrypoints.class, "raiseIllegalAccessError", "()V");
    public static final NormalMethod test2Method = VM.BuildWithRustMMTk ?
            getMethod(org.jikesrvm.mm.mminterface.MemoryManager.class, "test2", "(II)I") :
            getMethod(org.jikesrvm.runtime.RuntimeEntrypoints.class, "raiseIllegalAccessError", "()V");
    public static final NormalMethod test3Method = VM.BuildWithRustMMTk ?
            getMethod(org.jikesrvm.mm.mminterface.MemoryManager.class, "test3", "(IIII)I") :
            getMethod(org.jikesrvm.runtime.RuntimeEntrypoints.class, "raiseIllegalAccessError", "()V");
    public static final NormalMethod getOffsetArrayMethod =
            getMethod(org.jikesrvm.mm.mminterface.RustScanning.class, "getOffsetArray","(Ljava/lang/Object;)[I");
    public static final NormalMethod scanThreadMethod =
            getMethod(org.jikesrvm.mm.mminterface.RustScanThread.class, "scanThread",
                    "(Lorg/jikesrvm/scheduler/RVMThread;Lorg/vmmagic/unboxed/Address;ZZ)V");
    public static final NormalMethod dumpRefMethod =
            getMethod(org.jikesrvm.mm.mminterface.DebugUtil.class, "dumpRef",
                    "(Lorg/vmmagic/unboxed/ObjectReference;)V");
    public static final NormalMethod spawnCollectorThreadMethod =
            getMethod(org.jikesrvm.mm.mminterface.MemoryManager.class, "spawnCollectorThread",
                    "(Lorg/vmmagic/unboxed/Address;)V");
}
