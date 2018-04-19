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
/**
  * This only still exists so that we can have a performance comparison of the manually aligned functions
  * versus the inbuilt alignment in syscalls.
  */

EXTERNAL void* alignedSysAlloc(MMTk_Mutator mutator, int size, int align, int offset, int allocator) {
	return alloc(mutator, (size_t) size, (size_t) align, (ssize_t) offset, allocator);
}

EXTERNAL void* alignedSysAllocSlow(MMTk_Mutator mutator, int size, int align, int offset, int allocator) {
    return alloc_slow(mutator, (size_t) size, (size_t) align, (ssize_t) offset, allocator);
}
#endif
