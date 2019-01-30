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
package org.mmtk.plan.g1;

import org.mmtk.plan.TraceLocal;
import org.mmtk.plan.Trace;
import org.mmtk.policy.Space;

import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;

/**
 * This class implements the core functionality for a transitive
 * closure over the heap graph.
 */
@Uninterruptible
public class G1TraceLocal extends TraceLocal {

  public G1TraceLocal(Trace trace, boolean specialized) {
    super(specialized ? G1.SCAN_G1 : -1, trace);
  }

  /**
   * @param trace the associated global trace
   */
  public G1TraceLocal(Trace trace) {
    this(trace, true);
  }

  /****************************************************************************
   *
   * Externally visible Object processing and tracing
   */

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isLive(ObjectReference object) {
    if (object.isNull()) return false;
    if (Space.isInSpace(G1.SS0, object))
      return G1.hi ? G1.copySpace0.isLive(object) : true;
    if (Space.isInSpace(G1.SS1, object))
      return G1.hi ? true : G1.copySpace1.isLive(object);
    return super.isLive(object);
  }


  @Override
  @Inline
  public ObjectReference traceObject(ObjectReference object) {
    if (object.isNull()) return object;
    if (Space.isInSpace(G1.SS0, object))
      return G1.copySpace0.traceObject(this, object, G1.ALLOC_SS);
    if (Space.isInSpace(G1.SS1, object))
      return G1.copySpace1.traceObject(this, object, G1.ALLOC_SS);
    return super.traceObject(object);
  }

  /**
   * Will this object move from this point on, during the current trace ?
   *
   * @param object The object to query.
   * @return True if the object will not move.
   */
  @Override
  public boolean willNotMoveInCurrentCollection(ObjectReference object) {
    return (G1.hi && !Space.isInSpace(G1.SS0, object)) ||
           (!G1.hi && !Space.isInSpace(G1.SS1, object));
  }
}
