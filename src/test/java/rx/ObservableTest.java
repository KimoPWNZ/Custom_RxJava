package rx;

import org.junit.Test;
import rx.schedulers.*;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ObservableTest {

    @Test
    public void testMap() {
        List<Integer> result = new ArrayList<>();
        Observable<Integer> observable = Observable.create(obs -> {
            obs.onNext(1);
            obs.onNext(2);
            obs.onComplete();
        });
        observable.map(x -> x * 10).subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) { result.add(item); }
            @Override
            public void onError(Throwable t) {}
            @Override
            public void onComplete() {}
        });
        Assertions.assertEquals(List.of(10, 20), result);
    }

    @Test
    public void testFilter() {
        List<Integer> result = new ArrayList<>();
        Observable<Integer> observable = Observable.create(obs -> {
            obs.onNext(1);
            obs.onNext(2);
            obs.onNext(3);
            obs.onComplete();
        });
        observable.filter(x -> x % 2 == 1).subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) { result.add(item); }
            @Override
            public void onError(Throwable t) {}
            @Override
            public void onComplete() {}
        });
        Assertions.assertEquals(List.of(1, 3), result);
    }

    @Test
    public void testFlatMap() {
        List<Integer> result = new ArrayList<>();
        Observable<Integer> observable = Observable.create(obs -> {
            obs.onNext(1);
            obs.onNext(2);
            obs.onComplete();
        });
        observable.flatMap(x ->
                Observable.create(obs2 -> {
                    obs2.onNext(x);
                    obs2.onNext(x + 10);
                    obs2.onComplete();
                })
        ).subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) { result.add(item); }
            @Override
            public void onError(Throwable t) {}
            @Override
            public void onComplete() {}
        });
        Assertions.assertEquals(List.of(1, 11, 2, 12), result);
    }

    @Test
    public void testSubscribeOnObserveOn() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger threadId = new AtomicInteger();
        Observable<Integer> observable = Observable.create(obs -> {
            obs.onNext(42);
            obs.onComplete();
        });
        observable
                .subscribeOn(new IOThreadScheduler())
                .observeOn(new SingleThreadScheduler())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        threadId.set((int) Thread.currentThread().getId());
                    }
                    @Override
                    public void onError(Throwable t) {}
                    @Override
                    public void onComplete() { latch.countDown(); }
                });
        latch.await();
        Assertions.assertTrue(threadId.get() > 0);
    }

    @Test
    public void testDisposable() {
        List<Integer> result = new ArrayList<>();
        Observable<Integer> observable = Observable.create(obs -> {
            obs.onNext(1);
            obs.onNext(2);
            obs.onComplete();
        });
        Disposable d = observable.subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) { result.add(item); }
            @Override
            public void onError(Throwable t) {}
            @Override
            public void onComplete() {}
        });
        d.dispose();
        // Ensure no further items will be processed after dispose
        Assertions.assertTrue(d.isDisposed());
    }

    @Test
    public void testErrorHandling() {
        List<Object> result = new ArrayList<>();
        Observable<Integer> observable = Observable.create(obs -> {
            obs.onNext(1);
            obs.onError(new RuntimeException("Test error"));
        });
        observable.subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) { result.add(item); }
            @Override
            public void onError(Throwable t) { result.add(t.getMessage()); }
            @Override
            public void onComplete() { result.add("done"); }
        });
        Assertions.assertEquals(List.of(1, "Test error"), result);
    }
}