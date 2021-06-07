;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at https://mozilla.org/MPL/2.0/.


(ns helins.mprop

  "Multiplexing `test.check` property and tracking failure.
  
   See README for overview."

  {:author "Adam Helinski"}

  (:refer-clojure :exclude [and])
  (:require [clojure.core]
            [clojure.test.check.clojure-test :as TC.ct]
            [clojure.test.check.results      :as TC.result])
  #?(:cljs (:require-macros [helins.mprop :refer [and
                                                  check
                                                  deftest
                                                  mult]])))


(declare fail)


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
  
    "Maximum size used by [[deftest]]. Can be set using the `MPROP_MAX_SIZE` env variable.
    
     Default value is 200."
  
    (get-env "MPROP_MAX_SIZE"
             200))
  
  
  
  (def num-tests
  
    "Number of tests used by [[deftest]]. Can be set using the `MPROP_NUM_TESTS` env variable.
    
     Default value is 100."
  
    (get-env "MPROP_NUM_TESTS"
             100))))
  
  
  
  
#?(:clj (defmacro deftest
  
  "Like `clojure.test.check.clojure-test/defspec`.
  
   Difference is that the number of tests and maximum size can be easily configured
   and calibrated at the level of the whole test suite.

   `option+` is a map as accepted by `defspec`, it can notably hold `:max-size` and
   `:num-tests`.
  
   Most of the time, it is not productive fixing absolute values. During dev, number of tests
   and maximum size can be kept low in order to gain a fast feedback on what is going on.
   During actual testing, those values can be set a lot higher.

   For altering the base values, see [[max-size]] and [[num-tests]]. Each test can be
   calibrated against those base values. `option+` also accepts:

   | Key | Value |
   |---|---|
   | `:ratio-num` | Multiplies the base [[num-tests]] value |
   | `:ratio-size` | Multiplies the base [[max-size]] value |

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

    "Like Clojure's `and` but an item is considered truthy if it passes `clojure.test.check.results/pass?`.
    
     Great match for [[check]] as it allows for testing several assertions, even nested one, while keepin track
     of where failure happens."

    [& form+]

    (if (seq form+)
      (-and form+)
      true)))



(defn ^:no-doc -check

  ;; Used by [[check]], must be kept public.

  [beacon f]

  (try
     (let [x (f)]
       (if (TC.result/pass? x)
         x
         (fail beacon
               x)))
     (catch #?(:clj  Throwable
               :cljs :default) e
       (fail beacon
             e))))



(defmacro check

  "Executes form.
  
   Any failure or thrown exception is wrapped in an object that returns false on `clojure.test.check.results/pass?` with
   the following result data map attached:

   | Key | Value |
   |---|---|
   | `:mprop/path` | List of `beacon`s, contains more than one if checks were nested and shows exactly where failure happened |
   | `:mprop/value` | Value returned by `form` |
  
   Usually, checks are used with [[and]]. A `beacon` can be any value the user deems useful (often a human-readable string).

   For example (`and` being [[and]] from this namespace):

   ```clojure
   (and (check \"Some test\"
               (= 4 (+ 2 2)))

        (check \"Another test\"
               (= 3 (inc 1))))
   ```"

  [beacon form]

  `(-check ~beacon
           (fn [] ~form)))



(defn fail

  "Used by [[check]].
  
   More rarely, can be used to return an explicit failure."

  [beacon failure]

  (let [result-upstream (TC.result/result-data failure)
        path            (:mprop/path result-upstream)
        result          {:mprop/path (cons beacon
                                           path)
                         :mprop/value (if path
                                        (:mprop/value result-upstream)
                                        failure)}]
  (reify TC.result/Result

    (pass? [_]
      false)

    (result-data [_]
      result))))



(defmacro mult

  "Very common, sugar for using [[check]] with [[and]].

   Short for \"multiplex\".
  
   Replicating example in [[check]]:
  
   ```clojure
   (mult \"Some assertion\"
         (= 4 (+ 2 2))
   
        \"Another assertion\"
        (= 3 (inc 1)))
   ```"

  [& check+]

  (assert (even? (count check+)))
  `(helins.mprop/and ~@(map (fn [[beacon form]]
                              `(check ~beacon
                                      ~form))
                            (partition 2
                                       check+))))
