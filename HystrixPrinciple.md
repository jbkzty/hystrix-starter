### Hystrix的设计原理

##### Hystrix的自适应反馈机

从作者比较原始的版本中找了一张流程图，应该会相对比较好理解：


 ![GitHub][github1]

[github1]: https://raw.githubusercontent.com/wiki/Netflix/Hystrix/images/hystrix-command-flow-chart.png "GitHub,Social Coding" 

step1 : 请求会被封装成hystrixCommand类  
step2 : 执行command，比如同步执行execute(),异步执行queue()  
step3 : 是否存在缓存，如果存在便返回结果  
step4 : 电路是否短路  
step5 : 线程池、队列、信号量是否满  
step6 : 上述条件都未命中，便执行 HystrixCommand.run()   
step7 : 对响应进行监控（metrics） 

######  参考 ： https://github.com/Netflix/Hystrix/wiki/How-it-Works


<b>
第七步的 calculate metrics health 是整个熔断的脑袋，会实时的判断熔断是否需要打开</b>

---

### Hystrix的断路器分析（HystrixCircuitBreaker）


 ![GitHub][github4]

[github4]: http://fmn.rrimg.com/fmn084/20181005/1445/large_SKtR_43160000564e1e7f.jpg "GitHub,Social Coding" 


&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;HystrixCircuitBreaker 有三种状态  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; (1) CLOSE  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; (2) OPEN  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; (3) HALF_OPEN  

其中，断路器处于 OPEN 状态时，链路处于非健康状态，命令执行时，直接调用回退逻辑，跳过正常逻辑



    public interface HystrixCircuitBreaker {

    // 每一个请求都会执行改方法来判断是否需要执行，不会将断路器修改状态( CLOSE => HALF-OPEN )
    boolean allowRequest();
    
    // 每一个请求都会执行改方法来判断是否需要执行，会将断路器修改状态( CLOSE => HALF-OPEN )
    boolean attemptExecution();

    // 断路器是否打开
    boolean isOpen();

    void markSuccess();

    void markNonSuccess();
    

 ---   
HystrixCircuitBreaker 有两个子类实现 ：  
（1）NoOpCircuitBreaker ：空的断路器实现，用于不开启断路器功能的情况  
（2）HystrixCircuitBreakerImpl ：完整的断路器实现

<b>AbstractCommand</b>在创建的时候，会初始化HystrixCircuitBreaker

    this.circuitBreaker = initCircuitBreaker(this.properties.circuitBreakerEnabled().get(), circuitBreaker, this.commandGroup, this.commandKey, this.properties, this.metrics);
    

    private static HystrixCircuitBreaker initCircuitBreaker(boolean enabled, HystrixCircuitBreaker fromConstructor,
                                                            HystrixCommandGroupKey groupKey, HystrixCommandKey commandKey,
                                                            HystrixCommandProperties properties, HystrixCommandMetrics metrics) {
        if (enabled) {
            if (fromConstructor == null) {
                // 当 HystrixCommandProperties.circuitBreakerEnabled = true 时，即断路器功能开启，使用Factory获得HystrixCircuitBreakerImpl对象
                return HystrixCircuitBreaker.Factory.getInstance(commandKey, groupKey, properties, metrics);
            } else {
                return fromConstructor;
            }
        } else {
            // 当 HystrixCommandProperties.circuitBreakerEnabled = false 时，即断路器功能关闭，创建 NoOpCircuitBreaker 对象
            return new NoOpCircuitBreaker();
        }
    }    


<b> 重点来了，这个hystrix完整的断路器实现就存在 HystrixCircuitBreakerImpl 中 </b>   

 ![GitHub][github2]

[github2]: http://fmn.rrimg.com/fmn086/20181005/1425/original_l6AH_5656000056931e84.jpg "GitHub,Social Coding" 

使用 #subscribeToStream() 向 Hystrix Metrics 对请求量统计 Observable 的发起订阅

 ![GitHub][github3]

[github3]: http://fmn.rrimg.com/fmn086/20181005/1440/large_yemd_a5dd000056461e80.jpg "GitHub,Social Coding" 



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


