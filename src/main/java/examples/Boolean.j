.class public Boolean
.super java/lang/Object

.field private value Z

.method public <init>(I)V
    .limit stack 2
    .limit locals 2
    aload_0
    invokespecial java/lang/Object/<init>()V
    aload_0
    iload_1
    ifeq Lfalse
    iconst_1
    goto LsetValue
Lfalse:
    iconst_0
LsetValue:
    putfield Boolean/value Z
    return
.end method

.method public toInteger()LInteger;
    .limit stack 3
    .limit locals 1
    aload_0
    getfield Boolean/value Z
    ifeq Lfalse
    iconst_1
    goto Lcreate
Lfalse:
    iconst_0
Lcreate:
    new Integer
    dup_x1
    swap
    invokespecial Integer/<init>(I)V
    areturn
.end method

.method public Or(LBoolean;)LBoolean;
    .limit stack 5
    .limit locals 5
    aload_0
    getfield Boolean/value Z
    aload_1
    getfield Boolean/value Z
    ior
    istore_2
    new Boolean
    dup
    iload_2
    invokespecial Boolean/<init>(Z)V
    areturn
.end method

.method public And(LBoolean;)LBoolean;
    .limit stack 4
    .limit locals 5
    aload_0
    getfield Boolean/value Z
    aload_1
    getfield Boolean/value Z
    iand
    istore_2
    new Boolean
    dup
    iload_2
    invokespecial Boolean/<init>(Z)V
    areturn
.end method

.method public Xor(LBoolean;)LBoolean;
    .limit stack 5
    .limit locals 5
    aload_0
    getfield Boolean/value Z
    aload_1
    getfield Boolean/value Z
    ixor
    istore_2
    new Boolean
    dup
    iload_2
    invokespecial Boolean/<init>(Z)V
    areturn
.end method

.method public Not()LBoolean;
    .limit stack 5
    .limit locals 5
    aload_0
    getfield Boolean/value Z
    iconst_1
    ixor
    istore_2
    new Boolean
    dup
    iload_2
    invokespecial Boolean/<init>(Z)V
    areturn
.end method

.method public getBool()Ljava/lang/String;
    .limit stack 2
    .limit locals 1
    aload_0
    getfield Boolean/value Z
    ifeq Lfalse
    ldc "true"
    goto Lend
Lfalse:
    ldc "false"
Lend:
    areturn
.end method
