### 为什么Hystrix不使用JDK自带的信号量？


Hystrix 定义的信号量接口：


    static interface TryableSemaphore {

        /**
         * if (s.tryAcquire()) {
         * try {
         *   
         *  } finally {
         *   s.release();
         *  }
         * }
         */
        public abstract boolean tryAcquire();

        /**
         * ONLY call release if tryAcquire returned true.
         * 
         * if (s.tryAcquire()) {
         * try {
         *   
         * } finally {
         *   s.release();
         *  }
         * }
         */
        public abstract void release();

        public abstract int getNumberOfPermitsUsed();

    }
    

TryableSemaphore 共有两个子类实现 ：

（1）TryableSemaphoreNoOp   
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;有兴趣的同学可以看一下这逻辑，tryAcquire方法返回的永远是true，配合线程池隔离模式来使用的  

（2）TryableSemaphoreActual  


     static class TryableSemaphoreActual implements TryableSemaphore {

        protected final HystrixProperty<Integer> numberOfPermits;

        private final AtomicInteger count = new AtomicInteger(0);

        public TryableSemaphoreActual(HystrixProperty<Integer> numberOfPermits) {
            this.numberOfPermits = numberOfPermits;
        }

        @Override
        public boolean tryAcquire() {
            int currentCount = count.incrementAndGet();
            if (currentCount > numberOfPermits.get()) {
                count.decrementAndGet();
                return false;
            } else {
                return true;
            }
        }

        @Override
        public void release() {
            count.decrementAndGet();
        }

        @Override
        public int getNumberOfPermitsUsed() {
            return count.get();
        }
    }
    
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;这个的逻辑处理更像是一个计数器，<b>和JDK的计数器比起来，这边可以使用numberOfPermits这个参数来动态的调整信号量的上限</b>  ，另外一个原因是由于这个没有阻塞获取信号的需求，因此使用AtomicInteger可以达到更轻量级的实现。

---

### getExecutionSemaphore 

 ![GitHub][github1]

[github1]: http://fmn.xnpic.com/fmn083/20181004/1525/large_APOj_562a000055141e84.jpg "GitHub,Social Coding" 


---
### 扩展 - J.U.C semaphore的使用 

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;semaphore管理着一组虚拟的许可（permit），该值在初始化的时候会指定，在执行操作的时候，首先会获取许可，并且在使用过后释放许可，如果没有多于的许可，acquire将阻塞直到有许可，或者被中断或者操作超时（semaphore也可以作为互斥锁来使用）

核心理念：


    final int nonfairTryAcquireShared(int acquires) {
    for (;;) {
        int available = getState();
        int remaining = available - acquires;
        if (remaining < 0 || compareAndSetState(available, remaining))
            return remaining;
       }
    }

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;获取当前的state，以此state减去需要获取的信号个数作为剩余的个数，如果结果小于零，则返回这个值，如果大于零，则基于CAS将state的值设置为剩余的个数，当前步骤只有在结果小于零或者设置state值成功的情况下才会退出，否则一直循环。  

充分的使用CAS来尽量避免锁操作
