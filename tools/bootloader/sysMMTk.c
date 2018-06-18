#include "sys.h"
#ifdef RUST_BUILD
    #include "../../rust_mmtk/api/mmtk.h" // the api of the GC
#endif
#include <stdlib.h> // malloc and others
#include <errno.h> // error numbers
#include <string.h> // memcpy & memmove
#include <stdio.h> // to print things
#include <sys/mman.h> // mmap

EXTERNAL void sysHelloWorld() {
    printf("Hello World!\n");
}


#ifdef RUST_BUILD
EXTERNAL void alignedJikesrvmGcInit(void* jtoc, size_t heap_size) {
    jikesrvm_gc_init(jtoc, heap_size);
}
EXTERNAL void test_stack_alignment();
EXTERNAL void test_stack_alignment1(int a, int b, int c, int d, int e);
EXTERNAL void* alignedSysAlloc(MMTk_Mutator mutator, int size, int align, int offset, int allocator) {
	return alloc(mutator, (size_t) size, (size_t) align, (ssize_t) offset, allocator);
}

EXTERNAL MMTk_Mutator alignedSysBindMutator(size_t thread_id){
    return bind_mutator(thread_id);
}

EXTERNAL void* alignedSysAllocSlow(MMTk_Mutator mutator, int size, int align, int offset, int allocator) {
    return alloc_slow(mutator, (size_t) size, (size_t) align, (ssize_t) offset, allocator);
}

EXTERNAL void alignedStartControlCollector(size_t thread_id){
    start_control_collector(thread_id);
}

EXTERNAL bool alignedWillNeverMove(void* object){
    return will_never_move(object);
}

EXTERNAL void alignedReportDelayedRootEdge(MMTk_TraceLocal trace_local, void* addr){
    report_delayed_root_edge(trace_local, addr);
}

EXTERNAL bool alignedWillNotMoveInCurrentCollection(MMTk_TraceLocal trace_local, void* obj){
    return will_not_move_in_current_collection(trace_local, obj);
}

EXTERNAL void alignedProcessInteriorEdge(MMTk_TraceLocal trace_local, void* target, void* slot, bool root){
    process_interior_edge(trace_local, target, slot, root);
}

EXTERNAL void alignedStartWorker(size_t thread_id, void* worker){
    start_worker(thread_id, worker);
}

EXTERNAL void alignedEnableCollection(size_t thread_id){
    enable_collection(thread_id);
}

EXTERNAL bool alignedProcess(char* name, char* value){
    return process(name, value);
}

EXTERNAL void alignedPostAlloc(MMTk_Mutator mutator, void* refer, void* type_refer, int bytes, int allocator){
    post_alloc(mutator, refer, type_refer, bytes, allocator);
}

EXTERNAL bool alignedIsValidRef(void* ref) {
    return is_valid_ref(ref);
}
#endif
