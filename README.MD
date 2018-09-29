###微服务的容错理念

（1）为什么会有容错限流的需求？

在互联网应用中，通常一个请求会fan-out(扇出)数十个微服务，如果其中有个服务H处理请求的相应时间变慢（但是系统未down机），这样当高峰期请求的时候，所有的线程都会被hold住，导致应用的资源被耗尽，极坏的情况就会导致系统的雪崩效应。

因此我们需要容错限流。。。。

假定一个单体服务的可用性是99.99%，如果我们有30个微服务，每个可用性都是99.99%，那么总体的可用性就是 99.7% ，每个月有2小时的宕机时间 （实际情况往往更糟）


（2）业界对容错模式的总结

     - 超时：调用端自我的超时设置，是最基本也是最简单的一种模式
     - 限流：限制最大的并发数
     - 熔断：错误数达到阀值的时候，将请求断连
     - 隔离：隔离不同的依赖调用
     - 降级：服务的降级措施

 容错限流的设计模式: 
 
 <b> 断路器模式 </b>
 
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;可以理解成是一个状态机的流转，一开始是关闭状态，事件触发之后变成打开状态，一段时间之后，进入半打开状态，如果正常再进入关闭状态，否则依旧是打开状态
 
 ![GitHub][github3]

[github3]: http://fmn.xnpic.com/fmn082/20180920/0035/large_9Cup_e77200003ee31e83.jpg "GitHub,Social Coding" 
 
 
 <b> 舱壁隔离模式 </b>
 
 （3）工程师必须持有的容错素质  

     - 凡是有依赖都可能会失败     
     - 凡是资源都有限制（CPU,Memory,Threads,Queue）
     - 网络并不可靠
     - 延迟是应用稳定性杀手，会拖垮服务，耗尽资源，还不如down机来的痛苦

---

### Hystrix的设计原理

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Hystrix的自适应反馈机</b>

---

### Hystrix的执行原理

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;如下面的代码案例所示，只要实现HystrixCommand的run方法和getFallBack方法，便具备了熔断降级的功能。


 ![GitHub][github4]

[github4]: http://fmn.rrfmn.com/fmn081/20180923/2300/large_m1ND_55bc000044481e84.jpg "GitHub,Social Coding" 


&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;其中HystrixCommand提供了4个方法：execute()，queue()，observe()，toObservable()，平时只需要关注execute和queue即可。

<b>
queue()： 异步调用，返回一个Future对象，后面可以通过Future获取结果  

execute() 同步调用，直到依赖服务返回结果，或者抛出异常
</b>

``` 
public R execute() {
  try {
      return queue().get();
  } catch (Exception e) {
     throw Exceptions.sneakyThrow(decomposeException(e));
  }
}
``` 


 ![GitHub][github5]

[github5]: http://fmn.rrimg.com/fmn085/20180924/2200/large_dNEy_e7720000456f1e83.jpg "GitHub,Social Coding" 

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;通过代码逻辑上可以看出来，返回的Future对象只是对 <b>toObservable</b> 返回结果的封装代理，继续追踪<b>toObservable</b>的实现，查看<b>toObservable</b>的实现，里面充斥了一堆堆的回调，对Rxjava不熟悉的话，通过断点追踪到核心回调


&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;追踪信号量隔离的代码实现方案：

```
Observable<R> hystrixObservable = Observable.defer(applyHystrixSemantics).map(wrapWithAllOnNextHooks);
```

<b>step1:</b>

 ![GitHub][github6]

[github6]: http://fmn.rrimg.com/fmn087/20180925/2345/original_YFGU_557a000047b31e84.jpg "GitHub,Social Coding" 

 <b>step2:</b>
 
 ![GitHub][github7]

[github7]: http://fmn.xnpic.com/fmn083/20180925/2350/original_Foyv_566c000047a61e84.jpg "GitHub,Social Coding" 

 <b>step3:</b>

 ![GitHub][github8]

[github8]: http://fmn.rrimg.com/fmn086/20180925/2355/original_OLsC_e84f0000478a1e83.jpg "GitHub,Social Coding" 




&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;下面的代码逻辑可以看出来，是通过一系列的配置来判断熔断是否开启

```
@Override
public boolean attemptExecution() {

   // 强制开启
   if (properties.circuitBreakerForceOpen().get()) {
        return false;
   }

   // 强制关闭            
   if (properties.circuitBreakerForceClosed().get()) {
        return true;
   }
   
   // 熔断开关，默认为-1，请求执行正常逻辑         
   if (circuitOpened.get() == -1) {
        return true;
   } else {
        if (isAfterSleepWindow()) {
            if (status.compareAndSet(Status.OPEN, Status.HALF_OPEN)) {
                 //only the first request after sleep window should execute
                 return true;
            } else {
               return false;
            }
         } else {
               return false;
            }
        }
   }
```

(1) forceOpen   强制开启，所有请求都执行降级逻辑  
(2) forceClose  强制关闭，所有请求都执行正常逻辑  
(3) circuitOpened 熔断开关，默认为-1，请求执行正常逻辑，如果发生熔断，该值会被修改成0，请求执行降级逻辑  
(4) HALF_OPEN 熔断半开，即熔断之后，每隔一段会进行试探






















 

    



