package org.mmtk.plan;

import org.mmtk.plan.TraceLocal;
import org.vmmagic.unboxed.ObjectReference;

public interface FinalizableProcessorTracer {
  boolean isLive(ObjectReference object);
  ObjectReference getForwardedFinalizable(ObjectReference object);
  ObjectReference retainForFinalize(ObjectReference object);
}
