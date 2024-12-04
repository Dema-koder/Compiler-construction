.class public Array
.super java/lang/Object

.field private array [Ljava/lang/Object;
.field private length I

.method public <init>(I)V
    .limit stack 2
    .limit locals 2

    aload_0
    invokespecial java/lang/Object/<init>()V

    aload_0
    iload_1
    anewarray java/lang/Object
    putfield Array/array [Ljava/lang/Object;

    aload_0
    iload_1
    putfield Array/length I

    return
.end method

.method public Length()LInteger;
    .limit stack 100
    .limit locals 100

    aload_0
    getfield Array/length I
    new Integer
    dup_x1
    swap
    invokespecial Integer/<init>(I)V
    areturn
.end method

.method public get(I)Ljava/lang/Object;
    .limit stack 3
    .limit locals 2

    aload_0
    getfield Array/array [Ljava/lang/Object;
    iload_1
    aaload
    areturn
.end method

.method public set(ILjava/lang/Object;)V
    .limit stack 3
    .limit locals 3

    aload_0
    getfield Array/array [Ljava/lang/Object;
    iload_1
    aload_2
    aastore
    return
.end method

.method public toList()Ljava/util/List;
    .limit stack 3
    .limit locals 1

    aload_0
    getfield Array/array [Ljava/lang/Object;

    invokestatic java/util/Arrays/asList([Ljava/lang/Object;)Ljava/util/List;
    areturn
.end method