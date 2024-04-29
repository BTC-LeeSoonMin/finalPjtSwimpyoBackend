package com.btc.swimpyo.backend.config.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
public class ExecutionTimeAop {
    @Around("@annotation(com.btc.swimpyo.backend.config.aspect.SlackNotification)")
    public Object calculateExecutionTime(ProceedingJoinPoint pjp) throws Throwable{

        StopWatch sw = new StopWatch();
        sw.start();

        Object result = pjp.proceed();

        sw.stop();
        long executionTime = sw.getTotalTimeMillis();

        String className = pjp.getTarget().getClass().getName();
        String methodName = pjp.getSignature().getName();
        String task = className + "." + methodName;

        System.out.println("[ExecutionTime] " + task + "-->" + executionTime + "(ms)");

        return result;
    }
}