# Курсовая работа: Многопоточное и асинхронное программирование на Java. Custom RxJava.

## Описание

Этот проект реализует собственную версию библиотеки реактивного программирования RxJava на языке Java. Цель — предоставить минимальный, но функциональный набор инструментов для работы с реактивными потоками данных, операторов для их трансформации, управления подписками и потоками исполнения.

---

## Архитектура

### Основные компоненты

- **Observable<T>**  
  Источник данных. Позволяет подписываться на поток событий, создавать новые Observable через статический метод `create()`, а также использовать операторы `map`, `filter`, `flatMap` и др.

- **Observer<T>**  
  Получатель данных. Содержит методы:
  - `onNext(T item)` — вызывается при получении нового элемента.
  - `onError(Throwable t)` — вызывается при ошибке в потоке.
  - `onComplete()` — вызывается при завершении потока.

- **Disposable**  
  Позволяет отменить подписку и прекратить получение событий.

- **Scheduler**  
  Интерфейс для планирования исполнения задач в различных потоках.

---

## Операторы

- **map(Function<T, R>)**  
  Преобразует каждый элемент потока по переданной функции.

- **filter(Predicate<T>)**  
  Пропускает только те элементы, которые удовлетворяют условию.

- **flatMap(Function<T, Observable<R>>)**  
  Для каждого элемента исходного потока создает новый Observable, элементы которых включаются в результирующий поток.

---

## Управление потоками (Schedulers)

Реализованы три типа Scheduler:

- **IOThreadScheduler**  
  Использует `Executors.newCachedThreadPool()`. Подходит для операций ввода-вывода, где требуется много потоков, короткие задачи.

- **ComputationScheduler**  
  Использует `Executors.newFixedThreadPool(n)`, где `n = Runtime.getRuntime().availableProcessors()`. Хорошо подходит для CPU-bound задач.

- **SingleThreadScheduler**  
  Использует один поток (`Executors.newSingleThreadExecutor()`). Гарантирует последовательную обработку событий.

**Методы:**
- `subscribeOn(Scheduler)` — выполнение подписки происходит в заданном Scheduler.
- `observeOn(Scheduler)` — события (`onNext`, `onError`, `onComplete`) передаются Observer в потоке Scheduler.

---

## Обработка ошибок

- Любая ошибка в Observable или операторе передается в метод `onError()` Observer'а.
- После вызова `onError()` дальнейшие события не поступают Observer'у.

---

## Примеры использования

### 1. Простое создание Observable и подписка

```java
Observable<Integer> observable = Observable.create(obs -> {
    obs.onNext(1);
    obs.onNext(2);
    obs.onComplete();
});

observable.subscribe(new Observer<Integer>() {
    public void onNext(Integer item) { System.out.println(item); }
    public void onError(Throwable t) { t.printStackTrace(); }
    public void onComplete() { System.out.println("done"); }
});
```

### 2. Использование операторов

```java
observable
    .map(x -> x * 2)
    .filter(x -> x > 2)
    .subscribe(new Observer<Integer>() {
        public void onNext(Integer item) { System.out.println(item); }
        public void onError(Throwable t) { }
        public void onComplete() { }
    });
// Выведет: 4
```

### 3. flatMap

```java
observable
    .flatMap(x -> Observable.create(obs2 -> {
        obs2.onNext(x);
        obs2.onNext(x + 10);
        obs2.onComplete();
    }))
    .subscribe(new Observer<Integer>() {
        public void onNext(Integer item) { System.out.println(item); }
        public void onError(Throwable t) { }
        public void onComplete() { }
    });
// Выведет: 1 11 2 12
```

### 4. Переключение потоков

```java
observable
    .subscribeOn(new IOThreadScheduler())
    .observeOn(new SingleThreadScheduler())
    .subscribe(new Observer<Integer>() {
        public void onNext(Integer item) { System.out.println("Thread: " + Thread.currentThread().getId()); }
        public void onError(Throwable t) { }
        public void onComplete() { }
    });
```

---

## Процесс тестирования

- Для каждого ключевого компонента и оператора (Observable, Observer, Disposable, map, filter, flatMap, Schedulers) написаны unit-тесты (см. `src/test/java/rx/ObservableTest.java`).
- Тесты покрывают:
  - Корректность передачи данных через цепочки операторов.
  - Обработку ошибок.
  - Многопоточность и корректную работу Schedulers.
  - Механизм отписки через Disposable.

---

## Примеры тестов

```java
// Проверка map и filter
Observable<Integer> observable = Observable.create(obs -> {
    obs.onNext(1);
    obs.onNext(2);
    obs.onComplete();
});
List<Integer> result = new ArrayList<>();
observable.map(x -> x * 10)
    .filter(x -> x > 10)
    .subscribe(new Observer<Integer>() {
        public void onNext(Integer item) { result.add(item); }
        public void onError(Throwable t) { }
        public void onComplete() { }
    });
// result = [20]
```

---

## Заключение

В проекте реализованы все основные концепции реактивного программирования:
- Поток данных (Observable)
- Наблюдатель (Observer)
- Операторы трансформации (map, filter, flatMap)
- Управление подписью (Disposable)
- Управление потоками исполнения (Schedulers)
- Обработка ошибок

Система покрыта тестами, хорошо масштабируется и легко расширяется для новых операторов и источников данных.
