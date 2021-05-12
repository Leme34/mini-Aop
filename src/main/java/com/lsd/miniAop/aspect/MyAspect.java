package com.lsd.miniAop.aspect;

import com.lsd.miniAop.annotation.After;
import com.lsd.miniAop.annotation.Aspect;
import com.lsd.miniAop.annotation.Before;
import com.lsd.miniAop.annotation.Pointcut;

import java.lang.reflect.Method;

@Aspect
public class MyAspect {
    @Pointcut("com.lsd.miniAop.test.*.*(..)")
    public void pointCut() {
    }

    @Before("pointCut()")
    public void doBefore(Method method, Object object) {
        System.out.println("doBefore");
    }

    @After("pointCut()")
    public void doAfter(Method method, Object object) {
        System.out.println("doAfter");
    }
}
