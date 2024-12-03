.class public Integer
.super java/lang/Object

.field public Min I
.field public Max I
.field private value I

.method public <init>(I)V
    .limit stack 2
    .limit locals 2
    aload_0
    invokespecial java/lang/Object/<init>()V
    aload_0
    iload_1
    putfield Integer/value I
    return
.end method

.method public toReal()F
    .limit stack 1
    .limit locals 1
    aload_0
    getfield Integer/value I
    i2f
    freturn
.end method

.method public toBoolean()LBoolean;
    .limit stack 3
    .limit locals 2
    aload_0
    getfield Integer/value I
    ifne TRUE
    iconst_0
    goto END
TRUE:
    iconst_1
END:
    new Boolean
    dup_x1
    swap
    invokespecial Boolean/<init>(I)V
    areturn
.end method

.method public UnaryMinus()LInteger;
    .limit stack 3
    .limit locals 2
    aload_0
    getfield Integer/value I
    ineg
    new Integer
    dup_x1
    swap
    invokespecial Integer/<init>(I)V
    areturn
.end method

.method public Plus(LInteger;)LInteger;
    .limit stack 3
    .limit locals 2
    aload_0
    getfield Integer/value I
    aload_1
    getfield Integer/value I
    iadd
    new Integer
    dup_x1
    swap
    invokespecial Integer/<init>(I)V
    areturn
.end method

.method public Minus(LInteger;)LInteger;
    .limit stack 3
    .limit locals 2
    aload_0
    getfield Integer/value I
    aload_1
    getfield Integer/value I
    isub
    new Integer
    dup_x1
    swap
    invokespecial Integer/<init>(I)V
    areturn
.end method

.method public Mult(LInteger;)LInteger;
    .limit stack 3
    .limit locals 2
    aload_0
    getfield Integer/value I
    aload_1
    getfield Integer/value I
    imul
    new Integer
    dup_x1
    swap
    invokespecial Integer/<init>(I)V
    areturn
.end method

.method public Div(LInteger;)LInteger;
    .limit stack 3
    .limit locals 2
    aload_0
    getfield Integer/value I
    aload_1
    getfield Integer/value I
    idiv
    new Integer
    dup_x1
    swap
    invokespecial Integer/<init>(I)V
    areturn
.end method

.method public Rem(LInteger;)LInteger;
    .limit stack 3
    .limit locals 2
    aload_0
    getfield Integer/value I
    aload_1
    getfield Integer/value I
    irem
    new Integer
    dup_x1
    swap
    invokespecial Integer/<init>(I)V
    areturn
.end method

.method public Less(LInteger;)LBoolean;
    .limit stack 3
    .limit locals 2
    aload_0
    getfield Integer/value I
    aload_1
    getfield Integer/value I
    if_icmplt TRUE
    iconst_0
    goto END
TRUE:
    iconst_1
END:
    new Boolean
    dup_x1
    swap
    invokespecial Boolean/<init>(I)V
    areturn
.end method

.method public Equal(LInteger;)LBoolean;
    .limit stack 3
    .limit locals 2
    aload_0
    getfield Integer/value I
    aload_1
    getfield Integer/value I
    if_icmpeq TRUE
    iconst_0
    goto END
TRUE:
    iconst_1
END:
    new Boolean
    dup_x1
    swap
    invokespecial Boolean/<init>(I)V
    areturn
.end method

.method public LessEqual(LInteger;)LBoolean;
    .limit stack 3
    .limit locals 2
    aload_0
    getfield Integer/value I
    aload_1
    getfield Integer/value I
    if_icmple TRUE
    iconst_0
    goto END
TRUE:
    iconst_1
END:
    new Boolean
    dup_x1
    swap
    invokespecial Boolean/<init>(I)V
    areturn
.end method

.method public Greater(LInteger;)LBoolean;
    .limit stack 3
    .limit locals 2
    aload_0
    getfield Integer/value I
    aload_1
    getfield Integer/value I
    if_icmpgt TRUE
    iconst_0
    goto END
TRUE:
    iconst_1
END:
    new Boolean
    dup_x1
    swap
    invokespecial Boolean/<init>(I)V
    areturn
.end method

.method public GreaterEqual(LInteger;)LBoolean;
    .limit stack 3
    .limit locals 2
    aload_0
    getfield Integer/value I
    aload_1
    getfield Integer/value I
    if_icmpge TRUE
    iconst_0
    goto END
TRUE:
    iconst_1
END:
    new Boolean
    dup_x1
    swap
    invokespecial Boolean/<init>(I)V
    areturn
.end method

.method public setValue(I)V
    .limit stack 2
    .limit locals 2
    aload_0
    iload_1
    putfield Integer/value I
    return
.end method

.method public getValue()I
    .limit stack 2
    .limit locals 2
    aload_0
    getfield Integer/value I
    ireturn
.end method