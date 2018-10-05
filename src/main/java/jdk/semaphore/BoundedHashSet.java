package jdk.semaphore;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

/**
 * @author spuerKun
 * @date 2018/10/4.
 */
public class BoundedHashSet<T> {

    private final Set<T> set;

    private Semaphore semaphore;

    public BoundedHashSet(int bound) {
        // 注意要构建一个线程安全的类
        this.set = Collections.synchronizedSet(new HashSet<T>());
        this.semaphore = new Semaphore(bound);
    }

    public boolean add(T o) throws InterruptedException {
        semaphore.acquire();
        return set.add(o);
    }

    public boolean remove(Object o) {
        boolean wasRemoved = set.remove(o);
        if (wasRemoved) {
            semaphore.release();
        }
        return wasRemoved;
    }

    public static void main(String[] args) {

        BoundedHashSet<Integer> boundedHashSet = new BoundedHashSet<>(2);
        try {
            System.out.println(boundedHashSet.add(1));
            System.out.println(boundedHashSet.add(2));
            System.out.println(boundedHashSet.add(3));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
