### Hystrix执行隔离策略的分析

Hystrix提供了两种隔离策略  
（1）信号量隔离  
（2）线程池隔离


 ![GitHub][github1]

[github1]: http://fmn.xnpic.com/fmn083/20181005/0105/large_GNWz_4462000055d51e7f.jpg "GitHub,Social Coding" 


<b>总结一下信号量和线程的优缺点</b>

#### 信号量

##### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 优点: 
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;轻量，无额外开销

##### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 缺点: 
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(1) 不支持任务排队和主动超时  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(2) 不支持异步调用

##### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 适用:
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;受信客户  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;高扇出（网关）  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;高频高速调用（cache） 

####  线程池

##### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 优点: 
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(1) 支持排队和超时   
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(1) 支持异步调用

##### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 缺点: 
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;线程池调用会带来额外的开销

##### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 适用:
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;不受信客户  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;有限扇出

----

上面过多阐述了一些理论知识，现在看一下源码是如何实现的：

####  初始化
在类 <b>AbstractCommand</b>中会初始化线程池


    private static HystrixThreadPool initThreadPool(HystrixThreadPool fromConstructor, HystrixThreadPoolKey threadPoolKey, HystrixThreadPoolProperties.Setter threadPoolPropertiesDefaults) {
        if (fromConstructor == null) {
            // get the default implementation of HystrixThreadPool
            return HystrixThreadPool
                    .Factory 
                    .getInstance(threadPoolKey, threadPoolPropertiesDefaults);
        } else {
            return fromConstructor;
        }
    }


&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;主要是根据 threadPoolKey 先从 本地缓存中(concurrentHashMap) 获取已创建的 HystrixThreadPool，获取不到，创建对应的 HystrixThreadPool 返回，并添加到 threadPool，可以理解为一个工厂方法。    

总体上和我们平常使用的线程池大同小异，就不想深纠了 =。= 
