#include "sys.h"
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
EXTERNAL void* alignedSysAlloc(void* mutator, int size, int align, int offset, int allocator) {
	return alloc(mutator, (size_t) size, (size_t) align, (ssize_t) offset, allocator);
}

EXTERNAL void* alignedSysBindMutator(size_t thread_id){
    return bind_mutator(thread_id);
}

EXTERNAL void* alignedSysAllocSlowBumpMonotoneImmortal(void* mutator, int size,
    int align, int offset, int allocator) {
    return jikesrvm_alloc_slow_bump_monotone_immortal(mutator, (size_t) size, (size_t) align, (ssize_t) offset, allocator);
}

EXTERNAL void* alignedSysAllocSlowBumpMonotoneCopy(void* mutator, int size,
    int align, int offset, int allocator) {
    return jikesrvm_alloc_slow_bump_monotone_copy(mutator, (size_t) size, (size_t) align, (ssize_t) offset, allocator);
}

EXTERNAL void* alignedSysAllocSlowLargeobject(void* mutator, int size,
    int align, int offset, int allocator) {
    return jikesrvm_alloc_slow_largeobject(mutator, (size_t) size, (size_t) align, (ssize_t) offset, allocator);
}

EXTERNAL void alignedStartControlCollector(size_t thread_id){
    start_control_collector(thread_id);
}

EXTERNAL bool alignedWillNeverMove(void* object){
    return will_never_move(object);
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

EXTERNAL void alignedPostAlloc(void* mutator, void* refer, void* type_refer, int bytes, int allocator){
    post_alloc(mutator, refer, type_refer, bytes, allocator);
}

EXTERNAL void alignedHandleUserCollectionRequest(size_t thread_id) {
    return handle_user_collection_request(thread_id);
}

EXTERNAL void alignedAddFinalizer(void* obj) {
    add_finalizer(obj);
}

EXTERNAL void* alignedGetFinalizedObject() {
    return get_finalized_object();
}

EXTERNAL void sysDynamicCall1(void* (*func_ptr)(void*), void* arg0){
    return func_ptr(arg0);
}

EXTERNAL void sysDynamicCall2(void* (*func_ptr)(void*, void*), void* arg0, void* arg1) {
    return func_ptr(arg0, arg1);
}
#endif
