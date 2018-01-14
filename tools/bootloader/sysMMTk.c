#include "sys.h"
#include "../../mmtk/api/mmtk.h" // the api of the GC
#include <stdlib.h> // malloc and others
#include <errno.h> // error numbers
#include <string.h> // memcpy & memmove
#include <stdio.h> // to print things
#include <sys/mman.h> // mmap

EXTERNAL void sysHelloWorld() {
    printf("Hello World!\n");
}

EXTERNAL void sysBrokenCode() {
    broken_code();
}

EXTERNAL void sysGCInit(void* jtocPtr, int size) {
  jikesrvm_gc_init (jtocPtr,(size_t) size);
}

EXTERNAL void* sysAlloc(MMTk_Mutator mutator, int size, int align, int offset) {
	return alloc(mutator, (size_t) size, (size_t) align, (ssize_t) offset);
}

EXTERNAL void* sysAllocSlow(MMTk_Mutator mutator, int size, int align, int offset) {
    return alloc_slow(mutator, (size_t) size, (size_t) align, (ssize_t) offset);
}

EXTERNAL MMTk_Mutator sysBindMutator(int thread_id) {
    return bind_mutator((size_t) thread_id);
}

EXTERNAL void sysStartControlCollector(int thread_id) {
    return start_control_collector ((size_t) thread_id);
}

