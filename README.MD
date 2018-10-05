微服务的容错理念

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

[Hystrix的设计原理]（https://github.com/jbkzty/hystrix-starter/blob/45435cd2fa5666828cfa6cc748c6d2d025790879/HystrixPrinciple.md)

---

### Hystrix执行隔离策略的分析

[Hystrix执行隔离策略的分析]（https://github.com/jbkzty/hystrix-starter/blob/45435cd2fa5666828cfa6cc748c6d2d025790879/ThreadAndSemaphore.md)

---

### Hystrix为什么不适用JDK自带的信号量

[Hystrix为什么不适用JDK自带的信号量]（https://github.com/jbkzty/hystrix-starter/blob/45435cd2fa5666828cfa6cc748c6d2d025790879/DontUseSemphoreReason.md）
























 

    



