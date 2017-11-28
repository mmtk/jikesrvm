#include "sys.h"
#include "../../../mmtk/api/mmtk.h" // the api of the GC
#include <stdlib.h> // malloc and others
#include <errno.h> // error numbers
#include <string.h> // memcpy & memmove
#include <stdio.h> // to print things
#include <sys/mman.h> // mmap

EXTERNAL void sysMemmove(void *dst, const void *src, Extent cnt)
{
  TRACE_PRINTF("%s: sysMemmove %p %p %zu\n", Me, dst, src, cnt);
  memmove(dst, src, cnt);
}

EXTERNAL void sysHelloWorld()
{
  printf("Hello, World!\n");
}


EXTERNAL void sysGCInit(int size){
  printf("Initiated\n");
  gc_init ((size_t) size);
}

EXTERNAL void* sysAlloc(int size, int align, int offset){
  //printf("%d, %d, %d", size, align, offset);
  //printf("Allocating\n");
	void* temp = alloc ((size_t) size, (size_t) align, (ssize_t) offset);
  //printf("Allocated\n");
	return temp;
}


