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
import org.jikesrvm.scheduler.RVMThread;

/**
 * This is empty as there are no additional entrypoints that are required for Java MMTk.
 */
public class BaseEntrypoints {

  public static final RVMField lockStateField = getField(org.jikesrvm.mm.mmtk.Lock.class, "state", int.class);
  public static final RVMField SQCFField = getField(org.mmtk.utility.deque.SharedDeque.class, "completionFlag", int.class);
  public static final RVMField SQNCField = getField(org.mmtk.utility.deque.SharedDeque.class, "numConsumers", int.class);
  public static final RVMField SQNCWField =
          getField(org.mmtk.utility.deque.SharedDeque.class, "numConsumersWaiting", int.class);
  public static final RVMField SQheadField =
          getField(org.mmtk.utility.deque.SharedDeque.class, "head", org.vmmagic.unboxed.Address.class);
  public static final RVMField SQtailField =
          getField(org.mmtk.utility.deque.SharedDeque.class, "tail", org.vmmagic.unboxed.Address.class);
  public static final RVMField SQBEField = getField(org.mmtk.utility.deque.SharedDeque.class, "bufsenqueued", int.class);
  public static final RVMField synchronizedCounterField =
          getField(org.jikesrvm.mm.mmtk.SynchronizedCounter.class, "count", int.class);

  // This function is required
  public static void postInitialize() {

  }

}
