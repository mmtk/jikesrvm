package org.jikesrvm.mm.mminterface;

public class Plan {
  public static final int ALLOC_DEFAULT = 0;
  public static final int ALLOC_NON_REFERENCE = 1;
  public static final int ALLOC_NON_MOVING = 2;
  public static final int ALLOC_IMMORTAL = 3;
  public static final int ALLOC_LOS = 4;
  public static final int ALLOC_PRIMITIVE_LOS = 5;
  public static final int ALLOC_GCSPY = 6;
  public static final int ALLOC_CODE = 7;
  public static final int ALLOC_LARGE_CODE = 8;
  public static final int ALLOC_HOT_CODE =  ALLOC_DEFAULT; //todo USE_CODE_SPACE ? ALLOC_CODE : ALLOC_DEFAULT;
  public static final int ALLOC_COLD_CODE =  ALLOC_DEFAULT; //todo USE_CODE_SPACE ? ALLOC_CODE : ALLOC_DEFAULT;
  public static final int ALLOC_STACK = ALLOC_LOS;
  public static final int ALLOCATORS = 9;
  public static final int DEFAULT_SITE = -1;
}
