package org.jikesrvm.runtime;

import org.jikesrvm.classloader.RVMField;

import static org.jikesrvm.runtime.EntrypointHelper.getField;

public class JavaEntrypoints extends BaseEntrypoints {

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
