package rx;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.concurrent.atomic.AtomicBoolean;

public class Observable<T> {
    public interface OnSubscribe<T> {
        void subscribe(Observer<T> observer);
    }

    private final OnSubscribe<T> onSubscribe;
    private Scheduler subscribeScheduler;
    private Scheduler observeScheduler;

    private Observable(OnSubscribe<T> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }

    public static <T> Observable<T> create(OnSubscribe<T> onSubscribe) {
        return new Observable<>(onSubscribe);
    }

    public Disposable subscribe(Observer<T> observer) {
        AtomicBoolean disposed = new AtomicBoolean(false);

        Observer<T> safeObserver = new Observer<T>() {
            @Override
            public void onNext(T item) {
                if (!disposed.get()) observer.onNext(item);
            }

            @Override
            public void onError(Throwable t) {
                if (!disposed.get()) observer.onError(t);
            }

            @Override
            public void onComplete() {
                if (!disposed.get()) observer.onComplete();
            }
        };

        Runnable task = () -> {
            try {
                onSubscribe.subscribe(safeObserver);
            } catch (Throwable t) {
                safeObserver.onError(t);
            }
        };

        if (subscribeScheduler != null) {
            subscribeScheduler.execute(task);
        } else {
            task.run();
        }

        return new Disposable() {
            @Override
            public void dispose() {
                disposed.set(true);
            }
            @Override
            public boolean isDisposed() {
                return disposed.get();
            }
        };
    }

    public <R> Observable<R> map(Function<T, R> mapper) {
        return new Observable<R>(observer ->
                this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T item) {
                        R mapped;
                        try {
                            mapped = mapper.apply(item);
                        } catch (Throwable t) {
                            observer.onError(t);
                            return;
                        }
                        observer.onNext(mapped);
                    }
                    @Override
                    public void onError(Throwable t) { observer.onError(t); }
                    @Override
                    public void onComplete() { observer.onComplete(); }
                })
        ).copySchedulersFrom(this);
    }

    public Observable<T> filter(Predicate<T> predicate) {
        return new Observable<T>(observer ->
                this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T item) {
                        boolean passed;
                        try {
                            passed = predicate.test(item);
                        } catch (Throwable t) {
                            observer.onError(t);
                            return;
                        }
                        if (passed) observer.onNext(item);
                    }
                    @Override
                    public void onError(Throwable t) { observer.onError(t); }
                    @Override
                    public void onComplete() { observer.onComplete(); }
                })
        ).copySchedulersFrom(this);
    }

    public <R> Observable<R> flatMap(Function<T, Observable<R>> mapper) {
        return new Observable<R>(outerObserver -> {
            this.subscribe(new Observer<T>() {
                @Override
                public void onNext(T item) {
                    Observable<R> inner;
                    try {
                        inner = mapper.apply(item);
                    } catch (Throwable t) {
                        outerObserver.onError(t);
                        return;
                    }
                    inner.subscribe(new Observer<R>() {
                        @Override
                        public void onNext(R r) { outerObserver.onNext(r); }
                        @Override
                        public void onError(Throwable t) { outerObserver.onError(t); }
                        @Override
                        public void onComplete() { /* nothing */ }
                    });
                }
                @Override
                public void onError(Throwable t) { outerObserver.onError(t); }
                @Override
                public void onComplete() { outerObserver.onComplete(); }
            });
        }).copySchedulersFrom(this);
    }

    public Observable<T> subscribeOn(Scheduler scheduler) {
        this.subscribeScheduler = scheduler;
        return this;
    }

    public Observable<T> observeOn(Scheduler scheduler) {
        Observable<T> source = this;
        return new Observable<T>(observer -> {
            source.subscribe(new Observer<T>() {
                @Override
                public void onNext(T item) {
                    scheduler.execute(() -> observer.onNext(item));
                }
                @Override
                public void onError(Throwable t) {
                    scheduler.execute(() -> observer.onError(t));
                }
                @Override
                public void onComplete() {
                    scheduler.execute(observer::onComplete);
                }
            });
        }).copySchedulersFrom(this);
    }

    private Observable<T> copySchedulersFrom(Observable<?> other) {
        this.subscribeScheduler = other.subscribeScheduler;
        this.observeScheduler = other.observeScheduler;
        return this;
    }
}