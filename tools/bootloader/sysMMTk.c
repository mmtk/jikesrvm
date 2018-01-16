#include "sys.h"
#include "../../mmtk/api/mmtk.h" // the api of the GC
#include <stdlib.h> // malloc and others
#include <errno.h> // error numbers
#include <string.h> // memcpy & memmove
#include <stdio.h> // to print things
#include <sys/mman.h> // mmap

EXTERNAL void sysGCInit(void* jtocPtr, int size) {
  jikesrvm_gc_init (jtocPtr,(size_t) size);
}

EXTERNAL void* sysAlloc(MMTk_Mutator mutator, int size, int align, int offset, int allocator) {
	return alloc(mutator, (size_t) size, (size_t) align, (ssize_t) offset, allocator);
}

EXTERNAL void* sysAllocSlow(MMTk_Mutator mutator, int size, int align, int offset, int allocator) {
    return alloc_slow(mutator, (size_t) size, (size_t) align, (ssize_t) offset, allocator);
}

EXTERNAL MMTk_Mutator sysBindMutator(int thread_id) {
    return bind_mutator((size_t) thread_id);
}

EXTERNAL void sysStartControlCollector(int thread_id) {
    return start_control_collector ((size_t) thread_id);
}

EXTERNAL void sysReportDelayedRootEdge(MMTk_TraceLocal trace_local, void* addr) {
    report_delayed_root_edge(trace_local, addr);
}

EXTERNAL bool sysWillNotMoveInCurrentCollection(MMTk_TraceLocal trace_local, void* obj) {
    will_not_move_in_current_collection(trace_local, obj);
}

EXTERNAL void sysProcessInteriorEdge(MMTk_TraceLocal trace_local, void* target, void* slot, bool root) {
    process_interior_edge(trace_local, target, slot, root);
}

EXTERNAL bool sysWillNeverMove(void* object) {
    return will_never_move(object);
}

EXTERNAL void sysStartWorker(size_t thread_id, void* worker) {
    start_worker(thread_id, worker);
}

EXTERNAL void sysEnableCollection(size_t thread_id, size_t size) {
    enable_collection(thread_id, size);
}
