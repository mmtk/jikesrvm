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

import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.unboxed.ObjectReference;

/**
 * This class provides generic support for operations over the GC byte
 * within each object's header word. Specifically this class manages
 * global status bits which cut across policies (for example the logging bit).<p>
 *
 * The general pattern for use of the GC byte is that the high order bits
 * successively reserved for global use, as necessary.  Any GC policy may use
 * those bits that are not reserved for global use.
 */
@Uninterruptible
public class HeaderByte {
  private static final int TOTAL_BITS = 8;

  public static final boolean NEEDS_UNLOGGED_BIT = Selected.Constraints.get().needsLogBitInHeader();
  private static final int UNLOGGED_BIT_NUMBER = TOTAL_BITS - (NEEDS_UNLOGGED_BIT ? 1 : 0);
  public static final byte UNLOGGED_BIT = (byte) (1 << UNLOGGED_BIT_NUMBER);
  public static final int USED_GLOBAL_BITS = TOTAL_BITS - UNLOGGED_BIT_NUMBER;

  public static void markAsUnlogged(ObjectReference object) {
    byte value = org.jikesrvm.objectmodel.ObjectModel.readAvailableByte(object.toObject());
    org.jikesrvm.objectmodel.ObjectModel.writeAvailableByte(object, (byte) (value | UNLOGGED_BIT));
  }

  /**
   * Mark an object as logged.  Since duplicate logging does
   * not raise any correctness issues, we do <i>not</i> worry
   * about synchronization and allow threads to race to log the
   * object, potentially including it twice (unlike reference
   * counting where duplicates would lead to incorrect reference
   * counts).
   *
   * @param object The object to be marked as logged
   */
  public static void markAsLogged(ObjectReference object) {
    byte value = org.jikesrvm.objectmodel.ObjectModel.readAvailableByte(object);
    org.jikesrvm.objectmodel.ObjectModel.writeAvailableByte(object, (byte) (value & ~UNLOGGED_BIT));
  }

  /**
   * Return {@code true} if the specified object needs to be logged.
   *
   * @param object The object in question
   * @return {@code true} if the object in question needs to be logged (remembered).
   */
  public static boolean isUnlogged(ObjectReference object) {
    byte value = org.jikesrvm.objectmodel.ObjectModel.readAvailableByte(object);
    return (value & UNLOGGED_BIT) == UNLOGGED_BIT;
  }
}
