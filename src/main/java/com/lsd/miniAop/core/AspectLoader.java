package com.lsd.miniAop.core;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.lsd.miniAop.annotation.After;
import com.lsd.miniAop.annotation.Aspect;
import com.lsd.miniAop.annotation.Before;
import com.lsd.miniAop.annotation.Pointcut;
import com.lsd.miniAop.test.TestMethod;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AspectLoader {
    /**
     * 配置扫描aop的aspect基础包路径，真实场景应作为可配置项
     */
    public static final String PACKAGE_NAME = "com.lsd.miniAop";
    /**
     * 模拟ioc容器
     */
    public Map<String, Object> beanContainer = new HashMap<>();

    public AspectLoader() {
        this.beanContainer.put("TestMethod", new TestMethod());
    }

    public static void main(String[] args) {
        AspectLoader aspectLoader = new AspectLoader();
        aspectLoader.init();
        TestMethod testMethod = (TestMethod) aspectLoader.beanContainer.get("TestMethod");
        testMethod.doTest();
    }

    /**
     * 初始化aop的配置相关
     */
    private void init() {
        try {
            //获取标注了@Aspect注解的切面类
            List<Class> targetsWithAspectJAnnotationList = this.getAspectClass();
            for (Class targetsWithAspectJAnnotation : targetsWithAspectJAnnotationList) {
                Method beforeMethod = this.getBeforeMethod(targetsWithAspectJAnnotation);//Aspect类中声明的前置通知
                Pointcut pointcut = (Pointcut) this.getMethodAnnotation(targetsWithAspectJAnnotation, Pointcut.class);//Aspect类中声明的切点@Pointcut信息
                Method afterMethod = this.getAfterMethod(targetsWithAspectJAnnotation);//Aspect类中声明的后置通知
                // 获取 @Pointcut 的 value 参数指定包下的所有类
                List<Class> classList = this.getClassFromPackage(pointcut.value().substring(0, pointcut.value().indexOf("*") - 1));
                // 通过 cglib 为 每个被切方法 创建 实现before方法与after方法环绕通知 的 代理对象，最后再放入ioc容器
                for (Class sourceClass : classList) {
                    Object aspectObject = targetsWithAspectJAnnotation.newInstance();
                    Enhancer enhancer = new Enhancer();
                    enhancer.setSuperclass(sourceClass);
                    enhancer.setCallback(new MethodInterceptor() {
                        @Override
                        public Object intercept(Object obj, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                            beforeMethod.invoke(aspectObject, method, obj);
                            methodProxy.invokeSuper(obj, objects);
                            afterMethod.invoke(aspectObject, method, obj);
                            return obj;
                        }
                    });
                    Object proxyObj = enhancer.create();
                    // 放入ioc容器
                    this.beanContainer.put(sourceClass.getSimpleName(), proxyObj);
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }

    /**
     * 使用guava提供的类加载工具，扫描并收集 {@link AspectLoader#PACKAGE_NAME} 下所有标注了 {@link Aspect} 注解的目标类
     *
     * @return 目标类集合
     */
    private List<Class> getAspectClass() throws ClassNotFoundException, IOException {
        final ClassPath classPath = ClassPath.from(AspectLoader.class.getClassLoader());
        List<Class> aspectClass = new ArrayList<>();
        ImmutableSet<ClassPath.ClassInfo> clazz = classPath.getAllClasses();
        List<ClassPath.ClassInfo> list = clazz.asList();
        for (ClassPath.ClassInfo classInfo : list) {
            if (classInfo.getName() != null && classInfo.getPackageName().contains(PACKAGE_NAME)) {
                Class clazzTemp = Class.forName(classInfo.getName());
                if (clazzTemp.isAnnotationPresent(Aspect.class)) {
                    aspectClass.add(clazzTemp);
                }
            }
        }
        return aspectClass;
    }

    /**
     * 获取 {@param packageName}下的所有类
     *
     * @param packageName 包名
     * @return 目标类集合
     */
    private List<Class> getClassFromPackage(String packageName) {
        List<Class> classList = new ArrayList<>();
        final ClassPath classPath;
        try {
            classPath = ClassPath.from(AspectLoader.class.getClassLoader());
            ImmutableSet<ClassPath.ClassInfo> clazz = classPath.getAllClasses();
            List<ClassPath.ClassInfo> list = clazz.asList();
            for (ClassPath.ClassInfo classInfo : list) {
                if (classInfo.getName() != null && classInfo.getPackageName().contains(packageName)) {
                    classList.add(Class.forName(classInfo.getName()));
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return classList;
    }

    private Annotation getMethodAnnotation(Class source, Class annotationClass) {
        Method[] methods = source.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(annotationClass)) {
                Annotation[] beforeArr = method.getAnnotationsByType(annotationClass);
                if (beforeArr.length > 0) {
                    return beforeArr[0];
                }
            }
        }
        return null;
    }

    private Method getBeforeMethod(Class source) {
        Method[] methods = source.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Before.class)) {
                return method;
            }
        }
        return null;
    }

    /**
     * 遍历Aspect类所有方法，找到标注了{@link After}的方法
     *
     * @param source Aspect类
     * @return 标注了@After的方法
     */
    private Method getAfterMethod(Class source) {
        Method[] methods = source.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(After.class)) {
                return method;
            }
        }
        return null;
    }
}
