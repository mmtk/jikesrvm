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

import static org.jikesrvm.runtime.EntrypointHelper.getField;
import static org.jikesrvm.runtime.EntrypointHelper.getMethod;

import org.jikesrvm.VM;
import org.jikesrvm.classloader.RVMField;
import org.jikesrvm.classloader.RVMMethod;
import org.jikesrvm.classloader.NormalMethod;
import org.jikesrvm.mm.mminterface.RustJTOC;
import org.jikesrvm.scheduler.RVMThread;

/**
 * Fields and methods of the virtual machine that are needed by
 * compiler-generated machine code or C runtime code.
 */
public class BaseEntrypoints {
  // The usual causes for getField/Method() to fail are:
  //  1. you misspelled the class name, member name, or member signature
  //  2. the class containing the specified member didn't get compiled
  //

  //public static final NormalMethod deliverHardwareExceptionMethod =
  //        getMethod(org.jikesrvm.runtime.RuntimeEntrypoints.class, "deliverHardwareException", "(ILorg/vmmagic/unboxed/Word;)V");

  public static final NormalMethod unblockAllMutatorsForGCMethod =
    getMethod(org.jikesrvm.scheduler.RVMThread.class, "unblockAllMutatorsForGC", "()V");
  public static final NormalMethod blockAllMutatorsForGCMethod =
    getMethod(org.jikesrvm.scheduler.RVMThread.class, "blockAllMutatorsForGC", "()V");
  public static final RVMField threadBySlotField =
    getField(org.jikesrvm.scheduler.RVMThread.class, "threadBySlot", RVMThread[].class);
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
  public static final NormalMethod snipObsoleteCompiledMethodsMethod =
    getMethod(org.jikesrvm.compilers.common.CompiledMethods.class, "snipObsoleteCompiledMethods", "()V");
  public static final NormalMethod getReferenceSlotSizeMethod =
    getMethod(org.jikesrvm.runtime.Statics.class, "getReferenceSlotSize", "()I");
  public static final NormalMethod getNumberOfReferenceSlotsMethod =
    getMethod(org.jikesrvm.runtime.Statics.class, "getNumberOfReferenceSlots", "()I");
  public static final NormalMethod functionTableLengthMethod =
    getMethod(org.jikesrvm.jni.FunctionTable.class, "length", "()I");
  public static final NormalMethod implementedInJavaMethod =
    getMethod(org.jikesrvm.jni.JNIGenericHelpers.class, "implementedInJava", "(I)Z");

  public static final RVMField numThreadsField =
    getField(org.jikesrvm.scheduler.RVMThread.class, "numThreads", int.class);
  public static final RVMField movesCodeField =
    getField(org.jikesrvm.mm.mminterface.MemoryManagerConstants.class, "MOVES_CODE", boolean.class);
  public static final RVMField isCollectorField =
    getField(org.jikesrvm.mm.mminterface.ThreadContext.class, "isCollector", boolean.class);
  public static final RVMField threadsField =
    getField(org.jikesrvm.scheduler.RVMThread.class, "threads", org.jikesrvm.scheduler.RVMThread[].class);
  public static final RVMField systemThreadField =
    getField(org.jikesrvm.scheduler.RVMThread.class, "systemThread", org.jikesrvm.scheduler.SystemThread.class);
  public static final RVMField workerInstanceField =
    getField(org.jikesrvm.mm.mminterface.CollectorThread.class, "workerInstance", org.vmmagic.unboxed.Address.class);
  public static final RVMField JNIFunctionsField =
    getField(org.jikesrvm.jni.JNIEnvironment.class, "JNIFunctions", org.jikesrvm.jni.FunctionTable.class);
  public static final RVMField linkageTripletsField =
    getField(org.jikesrvm.jni.JNIEnvironment.class, "linkageTriplets", org.jikesrvm.jni.LinkageTripletTable.class);
  public static final RVMField JNIGlobalRefsField2 =
    getField(org.jikesrvm.jni.JNIGlobalRefTable.class,"JNIGlobalRefs", org.vmmagic.unboxed.AddressArray.class);
  public static final RVMField bootImageDataStartField =
    getField(org.jikesrvm.runtime.BootRecord.class, "bootImageDataStart", org.vmmagic.unboxed.Address.class);
  public static final RVMField bootImageRMapStart =
    getField(org.jikesrvm.runtime.BootRecord.class, "bootImageRMapStart", org.vmmagic.unboxed.Address.class);
  public static final RVMField bootImageRMapEnd =
    getField(org.jikesrvm.runtime.BootRecord.class, "bootImageRMapEnd", org.vmmagic.unboxed.Address.class);
  public static final RVMField activeMutatorContextField =
    getField(org.jikesrvm.scheduler.RVMThread.class, "activeMutatorContext", boolean.class);
  public static final RVMField mmtkHandleField =
    getField(org.jikesrvm.mm.mminterface.Selected.Mutator.class, "mmtkHandle",
            org.vmmagic.unboxed.Address.class);
  public static final RVMField functionTableDataField =
    getField(org.jikesrvm.jni.FunctionTable.class, "data",
            org.jikesrvm.compilers.common.CodeArray[].class);

  public static final RVMField isClassTypeField =
    getField(org.jikesrvm.classloader.RVMType.class, "isClassType", boolean.class);
  public static final RVMField isArrayTypeField =
    getField(org.jikesrvm.classloader.RVMType.class, "isArrayType", boolean.class);
  public static final RVMField isPrimitiveTypeField =
    getField(org.jikesrvm.classloader.RVMType.class, "isPrimitiveType", boolean.class);
  public static final RVMField isUnboxedTypeField =
    getField(org.jikesrvm.classloader.RVMType.class, "isUnboxedType", boolean.class);
  public static final RVMField isReferenceTypeField =
    getField(org.jikesrvm.classloader.RVMType.class, "isReferenceType", boolean.class);
  public static final RVMField instanceSizeField =
    getField(org.jikesrvm.classloader.RVMClass.class, "instanceSize", int.class);
  public static final RVMField logElementSizeField =
    getField(org.jikesrvm.classloader.RVMArray.class, "logElementSize", int.class);

  public static final RVMField booleanArrayField =
    getField(org.jikesrvm.classloader.RVMArray.class, "BooleanArray",
            org.jikesrvm.classloader.RVMArray.class);
  public static final RVMField byteArrayField =
    getField(org.jikesrvm.classloader.RVMArray.class, "ByteArray",
            org.jikesrvm.classloader.RVMArray.class);
  public static final RVMField charArrayField =
    getField(org.jikesrvm.classloader.RVMArray.class, "CharArray",
            org.jikesrvm.classloader.RVMArray.class);
  public static final RVMField shortArrayField =
    getField(org.jikesrvm.classloader.RVMArray.class, "ShortArray",
            org.jikesrvm.classloader.RVMArray.class);
  public static final RVMField intArrayField =
    getField(org.jikesrvm.classloader.RVMArray.class, "IntArray",
            org.jikesrvm.classloader.RVMArray.class);
  public static final RVMField longArrayField =
    getField(org.jikesrvm.classloader.RVMArray.class, "LongArray",
            org.jikesrvm.classloader.RVMArray.class);
  public static final RVMField floatArrayField =
    getField(org.jikesrvm.classloader.RVMArray.class, "FloatArray",
            org.jikesrvm.classloader.RVMArray.class);
  public static final RVMField doubleArrayField =
    getField(org.jikesrvm.classloader.RVMArray.class, "DoubleArray",
            org.jikesrvm.classloader.RVMArray.class);
  public static final RVMField javaLangObjectArrayField =
    getField(org.jikesrvm.classloader.RVMArray.class, "JavaLangObjectArray",
            org.jikesrvm.classloader.RVMArray.class);

  public static final RVMField rvmClassAlignment =
    getField(org.jikesrvm.classloader.RVMClass.class, "alignment", int.class);
  public static final RVMField rvmArrayAlignment =
    getField(org.jikesrvm.classloader.RVMArray.class, "alignment", int.class);
  public static final RVMField rvmArrayAcyclic =
    getField(org.jikesrvm.classloader.RVMArray.class, "acyclic", boolean.class);
  public static final RVMField rvmClassAcyclic =
    getField(org.jikesrvm.classloader.RVMClass.class, "acyclic", boolean.class);
  public static final RVMField rvmClassModifiers =
    getField(org.jikesrvm.classloader.RVMClass.class, "modifiers", short.class);

  public static void postInitialize() {
    RustJTOC.compileMMTk();
  }

}
