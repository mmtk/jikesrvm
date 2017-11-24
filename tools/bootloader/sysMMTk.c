#include "sys.h"

#include <stdlib.h> // malloc and others
#include <errno.h> // error numbers
#include <string.h> // memcpy & memmove
#include <sys/mman.h> // mmap

EXTERNAL void sysMemmove(void *dst, const void *src, Extent cnt)
{
  printf("Test2\n");
  TRACE_PRINTF("%s: sysMemmove %p %p %zu\n", Me, dst, src, cnt);
  memmove(dst, src, cnt);
}

EXTERNAL void sysHelloWorld()
{
  printf("Hello, World!");
}