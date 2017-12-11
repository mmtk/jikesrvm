#include "sys.h"
#include "../../mmtk/api/mmtk.h" // the api of the GC
#include <stdlib.h> // malloc and others
#include <errno.h> // error numbers
#include <string.h> // memcpy & memmove
#include <stdio.h> // to print things
#include <sys/mman.h> // mmap

EXTERNAL void sysMemmove(void *dst, const void *src, Extent cnt) {
  TRACE_PRINTF("%s: sysMemmove %p %p %zu\n", Me, dst, src, cnt);
  memmove(dst, src, cnt);
}

EXTERNAL void sysHelloWorld() {
  /*Offset testMethodAndDieOffset = bootRecord -> testMethodOffset;
  Address localJTOC = bootRecord -> tocRegister;
  Address testMethod = *(Address*)(localNativeThreadAddress + Thread_framePointer_offset);

  Address localNativeThreadAddress = IA32_ESI(context);
  Address localFrameAddress =  *(Address*)(localNativeThreadAddress + Thread_framePointer_offset);

  Address *sp = (Address*) IA32_ESP(context);

  sp -= __SIZEOF_POINTER__;
  *sp = localFrameAddress;

  IA32_EAX(context) = localFrameAddress;

  sp -= __SIZEOF_POINTER__;
  *sp = 0;

  IA32_ESP(context) = (greg_t) sp;

  IA32_EIP(context) = dumpStack;
  */

  int something = bootRecord -> testMethodOffset;
  int somethingelse = bootRecord -> test1MethodId;
  int a = bootRecord -> testMethodRandom;
  printf("Is this working? %d %d \n", something, a);
}


EXTERNAL void sysGCInit(int size) {
  printf("Initiating\n");
  gc_init ((size_t) size);
  printf("Initiated\n");
}

EXTERNAL void* sysAlloc(MMTk_Mutator mutator, int size, int align, int offset) {
  //printf("%d, %d, %d", size, align, offset);
  //printf("Allocating\n");
    int a = bootRecord -> testMethodRandom;
    printf("Is this working? %d \n", a);
	void* temp = alloc(mutator, (size_t) size, (size_t) align, (ssize_t) offset);
  //printf("Allocated\n");
	return temp;
}

EXTERNAL void* sysAllocSlow(MMTk_Mutator mutator, int size, int align, int offset) {
    return alloc_slow(mutator, (size_t) size, (size_t) align, (ssize_t) offset);
}

EXTERNAL MMTk_Mutator sysBindMutator(int thread_id) {
    return bind_mutator((size_t) thread_id);
}

