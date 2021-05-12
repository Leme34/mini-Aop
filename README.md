_原链接：https://mp.weixin.qq.com/s/5XwH4X4xUjA0n_ymGuewrQ_

#### AOP基本概念  
在介绍AOP技术之前，我们先来理清几个基本概念点：

**Aspect(切面)**  
可以理解为是将业务代码抽出来的一个类，例如：
```
@Aspect
public class LogAspect {
  /** ... 这里面是相关方法的部分 省略大部分内容 ... **/
}
```

**JoinPoint(连接点)**  
拦截点其实可以理解为下边这个参数：
![Image text](https://mmbiz.qlogo.cn/mmbiz_png/eQPyBffYbufMnX6cab8QB3elbOC0s3uMLAU5twT4ERAUgicqLmdfDF11AAjCIt7y1NKfAFNMPjr760YgE9icMzlA/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1&retryload=2)

Spring框架目前只支持方法级别的拦截，其实aop的连接点还可以有多种方式，例如说参数，构造函数等等。


**PointCut（切入点）**  
可以理解为对各个连接点进行拦截对一个定义。
![Image text](https://mmbiz.qlogo.cn/mmbiz_png/eQPyBffYbufMnX6cab8QB3elbOC0s3uMvCWLYpAARP8Q3XQlgunOtSo0M8kLnmvkOfHN6MGlYObvmicUADdcRNA/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1&retryload=2)


**Advice(通知）**  
指拦截到连接点之后需要执行的代码。
分为了前置，后置，异常，环绕，最终
具体表现如下：
![Image text](https://mmbiz.qlogo.cn/mmbiz_png/eQPyBffYbufMnX6cab8QB3elbOC0s3uMKvHkJdF0mJM020yCBJaFsLUa4Mo6tZDWcAuyLkctbXib1ibWTj0pib9cg/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1&retryload=2)

**目标对象**  
代理的目标对象

**织入（weave）**  
将切面应用到目标对象，并且导致代理对象创建的过程。weave是一个操作过程。

**引入（introduction）**  
在不修改代码的前提下，引入可以在运行的时候动态天津一些方法或者字段。


#### Cglib如何实现接口调用的代理  
首先我们定义一个基本的业务代码对象：
```
package org.idea.spring.aop.cglib;
/**
 * @Author linhao
 * @Date created in 3:56 下午 2021/5/6
 */
public class MyBis {
    void doBus(){
        System.out.println("this is do bis");
    }
}
```
接着是目标对象的拦截：
```
package org.idea.spring.aop.cglib;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import java.lang.reflect.Method;
/**
 * 整体的一个调用流程其实就是：
 * @Author linhao
 * @Date created in 3:57 下午 2021/5/6
 */
public class TargetInterceptor implements MethodInterceptor {
    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        System.out.println("==== intercept before ====");
        //从代理实例的方法调用返回的值。
        Object result = methodProxy.invokeSuper(o,objects);
        System.out.println("==== intercept after ====");
        return result;
    }
}
```
最后是测试代码的执行。
```
package org.idea.spring.aop.cglib;
import net.sf.cglib.core.DebuggingClassWriter;
import net.sf.cglib.proxy.Enhancer;
/**
 * @Author linhao
 * @Date created in 3:59 下午 2021/5/6
 */
public class TestCglib {
    public static void main(String[] args) throws InterruptedException {
        //将cglib生成的字节码文件存放到这个目录下边，查看下会有什么东西
    System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY,"/Users/idea/IdeaProjects/framework-project/spring-framework/spring-core/spring-aop/cglib-class");
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(MyBis.class);
        enhancer.setCallback(new TargetInterceptor());
        MyBis myBis = (MyBis) enhancer.create();
        myBis.doBus();
    }
}
```
执行结果：
![Image text](https://mmbiz.qlogo.cn/mmbiz_png/eQPyBffYbufMnX6cab8QB3elbOC0s3uMWukgiafUDaOXGZvtjmiaWJM8lVnURuFI6KQUM9miboGfeialiappOicicyUibA/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1&retryload=2)
上边代码中的TargetInterceptor中的intercept方法会在目标函数被调用之后自动进行回调操作，从而实现代理调用的效果。
  
**cglib和jdk代理**
cglib的代理模式和jdk实现的代理技术本质还是会有较大差异性，jdk要求被代理对象需要实现jdk内部的InvocationHandler接口才能进行接口回调操作，但是cglib对是否实现接口方面没有强制要求，而且其性能也比JDK自带的代理要高效许多。

**cglib代理的原理**  
关于cglib的原理我只能简单地介绍一下，仔细看了下里面的内容点实在是太多了，如果一下子深入挖掘容易掉进坑，所以这里打算用些大白话的方式来介绍好了。

**cglib实现代理的基本思路**  
1.对被调用对象进行一层包装，并且对方法建立索引。  
2.当调用目标方法的时候，通过索引值去寻找并调用函数。  
这里面详细细节点可以参考这篇博客：
https://www.cnblogs.com/cruze/p/3865180.html  
根据这篇博客介绍的思路，我自己也简单实现了一个cglib类似的代理工具。代码地址见文末

**难点：**
**如何给方法建立索引？如何根据索引调用函数？**  
这里贴出我自己的一些思考和输出。  
对调用对方法名称取出hashcode，然后通过switch关键字判断需要调用对函数名称：
![Image text](https://mmbiz.qlogo.cn/mmbiz_png/eQPyBffYbufMnX6cab8QB3elbOC0s3uMEt5X9Ytm9ibDKC1g6UZ0OnFYpL1F3kegobdV2lBRLymhVa9TCtR4Rdw/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1&retryload=2)
使用起来差不多，不过很多细节方面没有做得特别完善：
![Image text](https://mmbiz.qlogo.cn/mmbiz_png/eQPyBffYbufMnX6cab8QB3elbOC0s3uMgicYAvmmIXdCE9vEhu8wleztv6iaUJNRXYu7s5DVaq4ntjqibpfPibYKoA/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1&retryload=2)

使用cglib实现代理功能，主要目的就是希望在执行某些函数之前去调用某些方法。为了实现这种方式，其实借助反射也是可以达成目的的。但是反射在多次调用的时候性能开销比较大。cglib在这块所做的优化主要是对调用方法做了一次索引的包装，生产新的字节码，实现性能上的提升。

**Cglib底层是如何生成字节码文件的**  
**ASM**  
对于需要手动操纵字节码的需求，可以使用ASM，它可以直接生产 .class字节码文件，也可以在类被加载入JVM之前动态修改类行为。ASM的应用场景有AOP（Cglib就是基于ASM）、热部署、修改其他jar包中的类等。

整体的操作流程图如下所示：
![Image text](https://mmbiz.qlogo.cn/mmbiz_png/eQPyBffYbufMnX6cab8QB3elbOC0s3uMobnX7xZVMLTX6cM7nuSjJaSL2fDZngzibDHA4J2bGwLP6ONMZazwBvg/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1&retryload=2)

先通过ClassReader读取编译好的.class文件

其通过访问者模式（Visitor）对字节码进行修改，常见的Visitor类有：对方法进行修改的MethodVisitor，或者对变量进行修改的FieldVisitor等

通过ClassWriter重新构建编译修改后的字节码文件、或者将修改后的字节码文件输出到文件中
