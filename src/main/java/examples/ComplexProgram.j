.class public ComplexProgram
.super java/lang/Object

.field private counter I
.field private message Ljava/lang/String;

.method public <init>(ILjava/lang/String;)V
    .limit stack 2
    .limit locals 5
    aload_0
    invokespecial java/lang/Object/<init>()V
    aload_0
    iload_1
    putfield ComplexProgram/counter I
    aload_0
    iload_2
    putfield ComplexProgram/message Ljava/lang/String;
    return
.end method

.method public process()V
    .limit stack 3
    .limit locals 3
getstatic java/lang/System/out Ljava/io/PrintStream;
aload_0
getfield ComplexProgram/counter I
invokevirtual java/io/PrintStream/println(I)V
    return
.end method
.method public static main([Ljava/lang/String;)V
    .limit stack 3
    .limit locals 3
new ComplexProgram
dup
iconst_1
bipush 10
ldc "text"
invokespecial ComplexProgram/<init>(ILjava/lang/String;)V
    invokevirtual ComplexProgram/process()V
    return
.end method
