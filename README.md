# MProp, multiplexing `test.check` properties

[![Clojars](https://img.shields.io/clojars/v/io.helins/mprop.svg)](https://clojars.org/io.helins/mprop)

[![Cljdoc](https://cljdoc.org/badge/io.helins/mprop)](https://cljdoc.org/d/io.helins/mprop)

![CircleCI](https://circleci.com/gh/helins/mprop.cljc.svg?style=shield)

Lightweight and intuitive abstraction on top of [test.check](https://github.com/clojure/test.check) for an efficient generative testing environment.

It offers a simple way for calibrating tests and writing multiple assertions at the level of one test while tracking exactly where a failure occurs.

## Usage

```clojure
(require '[helins.mprop :as mprop])
```

The following excerpts can be found and explored in the [helins.mprop.example](../main/src/example/helins/mprop/example.cljc).


### Defining and calibrating tests

Most of the time, it is not productive fixing an absolute number of tests and absolute maximum size on a test.
During development, those values can be kept low in order to quickly gain a fast feedback on what is going on
in the test suite while you are working. When actually testing, they can be set much higher, taking all the time
they need to find those sweet edge cases.

The following macro is just like `clojure.test.check.clojure-test/defspec` but accepts two additional key-values
in the option map (if provided):

| Key | Value |
|---|---|
| `:ratio-num` | Multiplies the base `num-tests` value |
| `:ratio-size` | Multiplies the base `max-size` value |

Those base values can be found in `helins.prop/max-size` and `helins.prop/num-tests`. Docstrings describes which environment variables
can be set for modyfing those values at start.

For instance, a test that runs 10 times more with half the maximum size:

```clojure
(deftest foo

  {:ratio-num  10
   :ratio-size 0.5}

  some-property)
```

In practice, `:ratio-size` is less often modified while changing `:ratio-num` is very common. The key is to think in terms of proportion. It can
be a good idea calibrating tests so that each takes roughly the same amount of time, or longer if a given test is deemed more important.


### Multiplexing properties

```clojure
(require '[clojure.test.check.generators :as TC.gen]
         '[clojure.test.check.properties :as TC.prop])
```

Suppose the following test, a classic of property-based testing:

```clojure
(mprop/deftest bad-test

  (TC.prop/for-all [x (TC.gen/vector TC.gen/large-integer)]
    (let [sorted (sort x)]
      (and (= (count x)
              (count sorted))
           (not= sorted
                 (sort sorted))))))  ;; Fails, sorting a sorted collection should be idempotent


;; Simplified output after running (bad-test)

{:total-nodes-visited 0,
 :depth 0,
 ;; Fails...
 :pass? false,
 :result false,      
 ;; But why?!
 :result-data nil
 :time-shrinking-ms 0,
 :smallest [[]]}
```

It does the job but it is not effictive: if an assertion fails, you do not know which one. Neither `:result` nor `:result-data` enlighten us and
we are left blind.


Now consider this first alternative:

```clojure
(mprop/deftest better-test

  (TC.prop/for-all [x (TC.gen/vector TC.gen/large-integer)]
    (let [sorted (sort x)]
      (mprop/and (mprop/check "Both have the same size"
                              (= (count x)
                                 (count sorted)))
                 (mprop/check "Sorting is idempotent"
                              (not= sorted
                                    (sort sorted)))))))


;; Simplified output after running (better-test)

{:total-nodes-visited 0,
 :depth 0,
 ;; Fails...
 :pass? false,
 :result false,
 ;; Oh yeah, that's why!
 :result-data #:mprop{:path ("Sorting is idempotent"), :value false},
 :time-shrinking-ms 0,
 :smallest [[]]}
```

The `mprop/check` macro creates a checkpoint which accepts any value acting as a beacon (here, a human-readable string is used) and a form to test.

THe `mprop/and` macro, akin to regular `and`, stops as soon as a result returns false on `clojure.test.results/pass?`.

In case of failure, `mprop/check` returns such a falsy result and contains the following data map:

| Key | Value |
|---|---|
| `:mprop/path` | List of `beacon`s, contains more than one if checks were nested and shows exactly where the failure happened |
| `:mprop/value` | Value returned by `form` |

In other words, in case of failure, the whole test stops but at least you can locate exactly what failed. This pattern of combining `mprop/and` and `mprop/check`
can be nested ad-libidum and is highly composable. It is so common that the `mprop/mult` macro is the sugar-coated version which would translate our
example into:

```clojure
(mprop/deftest awesome-test

  (TC.prop/for-all [x (TC.gen/vector TC.gen/large-integer)]
    (let [sorted (sort x)]
      (mprop/mult

        "Both have the same size"
        (= (count x)
           (count sorted)))

        "Sorting is idempotent"
        (not= sorted
              (sort sorted)))))
```


### Nesting properties

Property multiplexing can become somewhat complex and nesting offers good reusability while helping in locating the error.

Supposing this failing test (since 4 is not lesser than 0):

```clojure
(mprop/deftest nested

  (TC.prop/for-all [_ (TC.gen/return nil)]
    (mprop/mult

      "Yes"
      true

      "42"
      42

      "Prepare something and continue"
      (let [foo (+ 2
                   2)]
        (mprop/mult

          "Is 4"
          (= 4
             foo)

          "Below 0"
          (< foo
             0)

          "All right"
          true)))))


;; Simplified output after running (nested)

{:total-nodes-visited 0,
 :depth 0,
 :pass? false,
 :result false,
 ;; Okay, that's what's going on
 :result-data #:mprop{:path ("Prepare something and continue"
                             "Below 0"),
                      :value false},
 :time-shrinking-ms 0,
 :smallest [nil]}
```

The result data attached under `:result-data` will be:

```clojure
{:mprop/path  (list "Prepare something and continue"
                    "Below 0")
 :mprop/value false}
```


### Debugging multiplexed properties

This monadic way of multiplexing works really well most of the time. We have used it extensively, sometimes testing dozens of assertions under one `deftest`.

The so-called "beacons" which indicates where an error occured should mostlikely be human-readable strings since test errors are consumed mostly by humans.
However, they can be anything. In case of failure, they can hold contextual data which can help you understand why something is failing, maybe even produce
some useful side-effect.


## Notes

The [core BinF test namespace](https://github.com/helins/binf.cljc/blob/main/src/test/helins/binf/test.cljc) provides a real world example where `deftest` and
`mult` are used extensively.


## Development and testing <a name="develop">

This repository is organized with [Babashka](https://github.com/babashka/babashka), a wonderful tool for any Clojurist
that comes with a powerful task runner.

Listing all tasks:

```shell
$ bb tasks
```

Starting a task, for instance a REPL:

```shell
$ bb dev:clojure
```


## License

Copyright © 2021 Adam Helinski

Licensed under the term of the Mozilla Public License 2.0, see LICENSE.
