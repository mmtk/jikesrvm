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
package org.mmtk.plan;

import org.vmmagic.pragma.*;

/**
 * Constraints specific to simple collectors.
 */
@Uninterruptible
public class ThirdPartyConstraints extends SimpleConstraints {
    // A third party GC may not move objects. But unless we have a way for a TPH
    // to configure this, we need this to be true to be correct.
    @Override
    public boolean movesObjects() {
      return true;
    }
    // The number doesnt matter.
    @Override
    public int gcHeaderBits() {
      return 0;
    }
    // The number doesnt matter.
    @Override
    public int gcHeaderWords() {
      return 0;
    }
}
