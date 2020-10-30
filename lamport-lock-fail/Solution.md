## 1. Некорректное исполнение
[![Build Status](https://travis-ci.com/ITMO-MPP-2017/lamport-lock-fail-Dogzik.svg?token=B2yLGFz6qwxKVjbLm9Ak&branch=master)](https://travis-ci.com/ITMO-MPP-2017/lamport-lock-fail-Dogzik)


## 2. Исправление алгоритма
```java
threadlocal int id       // 0..N-1 -- идентификатор потока
shared      int label[N] // заполненно нулями по умолчанию

def lock:
  0: label[id] = -1
  1: my = 1 // номер билета текущего потока
  2: for k in range(N): if k != id:
  3:     my = max(my, label[k] + 1) // должен быть больше, чем у других
  4: label[id] = my // публикуем свой номер билета для других потоков
  5: for k in range(N): if k != id:
  6:     while true: // пропускаем поток k до тех пока, пока номер его билета меньше
  7:         other = label[k] // читаем номер билета потока k
  8:         if (other != -1) and (other == 0 or (other, k) > (my, id)): break@6 // если номер его билета меньше, перестаем ждать  

def unlock:
  9: label[id] = 0
```

Мы используем отрицательное значение в label как пометку по аналогии с choosing
