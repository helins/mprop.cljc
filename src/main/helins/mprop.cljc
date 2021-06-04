;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at https://mozilla.org/MPL/2.0/.


(ns helins.mprop

  ""

  {:author "Adam Helinski"}

  (:refer-clojure :exclude [and])
  (:require [clojure.core]
            [clojure.test.check.clojure-test :as TC.ct]
            [clojure.test.check.results      :as TC.result])
  #?(:cljs (:require-macros [helins.mprop :refer [and
                                                  deftest]])))


;;;;;;;;;; Defining tests


#?(:clj (let [get-env (fn [k default]
                        (if-some [x (not-empty (System/getenv k))]
                          (try
                            (Long/parseLong x)
                            (catch Throwable e
                                (throw (ex-info (str "While parsing env variable: "
                                                     k)
                                                {::env-var k}
                                                e))))
                            default))]
  
  (def max-size
  
    "Maximum size used by [[deftest]]. Can be set using the \"BREAK_MAX_SIZE\" env variable.
    
     Default value is 200."
  
    (get-env "BREAK_MAX_SIZE"
             200))
  
  
  
  (def num-tests
  
    "Number of tests used by [[deftest]]. Can be set using the \"BREAK_NUM_TESTS\" env variable.
    
     Default value is 100."
  
    (get-env "BREAK_NUM_TESTS"
             100))))
  
  
  
  
#?(:clj (defmacro deftest
  
  "Like `clojure.test.check.clojure-test/defspec`.
  
   Difference is that the number of tests and maximum size can be easily configured
   at the level of the whole test suite.

   `option+` is a map as accepted by `defspec`, it can notably hold `:max-size` and
   `:num-tests`.
  
   Most of the time, it is not productive fixing absolute values. During dev, number of tests
   and maximum size can be kept low in order to gain a fast feedback on what is going on.
   During actual testing, those values can be set high for thorough testing.

   For altering the base values, see [[max-size]] and [[num-tests]].

   Comparing tests, some are cheap and could be run with much higher settings, others are
   expensive and cannot have that luxury. Thus, it is important calibrating tests and it
   can be done by providing in `option+`:

   | Key | Value |
   |---|---|
   | `:ratio-num` | Multiplies the base `:num-tests` value |
   | `:ratio-size` | Multiplies the base `:max-size` value |

   For instance, a test that runs 10 times more with half the maximum size:

   ```clojure
   (deftest foo

     {:ratio-num  10
      :ratio-size 0.5}

     some-property)
   ```"
  
  ([sym prop]
  
   `(deftest ~sym
             nil
             ~prop))
  
  
  ([sym option+ prop]
  
   `(TC.ct/defspec ~sym
                  ~(-> option+
                       (update :max-size
                               #(or %
                                    (* max-size
                                       (or (:ratio-size option+)
                                           1))))
                       (update :num-tests
                               #(or %
                                    (* num-tests
                                       (or (:ratio-num option+)
                                           1)))))
                   ~prop))))


;;;;;;;;;; Testing more than one assertion


(let [-and (fn -and [[form & form-2+]]
             (if form-2+
               `(let [x# ~form]
                  (if (TC.result/pass? x#)
                    ~(-and form-2+)
                    x#))
               form))]

  (defmacro and

    "Like Clojure's `and` but an item consider truthy if it passes `clojure.test.check.results/pass?`."

    [& form+]

    (if (seq form+)
      (-and form+)
      true)))
