;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at https://mozilla.org/MPL/2.0/.


(ns helins.mprop.test

  "Testing core features."

  {:author "Adam Helinski"}

  (:refer-clojure :exclude [=])
  (:require [clojure.test                  :as T]
            [clojure.test.check.generators :as TC.gen]
            [clojure.test.check.properties :as TC.prop]
            [clojure.test.check.results    :as TC.result]
            [helins.mprop                  :as mprop]))


;;;;;;;;;; Helpers


(defn =

  ;; Takes care of NaN not being equal to itself

  [& x+]

  (apply clojure.core/=
         (map hash
              x+)))


;;;;;;;;;; Generators


(def gen-falsy
     (TC.gen/elements #{nil
                        false
                        (reify TC.result/Result (pass? [_] false))}))



(def gen-truthy
     (TC.gen/frequency [[9 TC.gen/any
                         1 (TC.gen/return (reify TC.result/Result (pass? [_] true)))]]))


;;;;;;;;;; Tests


#?(:clj (mprop/deftest and-

  {:ratio-num  0.5
   :ratio-size 0.3}

  (TC.prop/for-all [x+ (TC.gen/vector TC.gen/any
                                      1
                                      16)]
    (= (if-some [[fail] (some (fn [x]
                                (when-not (TC.result/pass? x)
                                  [x]))
                              x+)]
         fail
         (last x+))
       (eval `(mprop/and ~@(map (fn [x]
                                  `(quote ~x))
                                x+)))))))



(T/deftest and--true

  (T/is (true? (mprop/and)))

  (T/is (true? (mprop/and true
                          true
                          true))))



(mprop/deftest check--failure

  (TC.prop/for-all [path     (TC.gen/vector TC.gen/any
                                            1
                                            8)
                    [failure
                     value]  (TC.gen/one-of [(TC.gen/let [x (TC.gen/elements [false
                                                                              nil])]
                                               [(constantly x)
                                                x])
                                             (TC.gen/return (let [e (Error.)]
                                                            [#(throw e)
                                                             e]))
                                             (TC.gen/let [x TC.gen/any]
                                               (let [x-2 (reify TC.result/Result
        
                                                           (pass? [_]
                                                             false)
        
                                                           (result-data [_]
                                                             x))]
                                                 [(constantly x-2)
                                                  x-2]))])]
    (let [{path-2  :mprop/path
           value-2 :mprop/value} (TC.result/result-data (let [f (fn f [[x & x+]]
                                                          (if x+
                                                            (let [upstream (f x+)]
                                                              (fn []
                                                                (mprop/check x
                                                                             (upstream))))
                                                            (fn []
                                                              (mprop/check x
                                                                           (failure)))))]
                                                  ((f path))))]
      (mprop/mult

        "Path"
        (= path
           path-2)

        "Value"
        (= value
           value-2)))))



(mprop/deftest check--success

  (TC.prop/for-all [path  (TC.gen/vector TC.gen/any
                                         1
                                         8)
                    value (TC.gen/such-that boolean
                                            TC.gen/any)]
    (mprop/mult "Output is input"
                (= value
                   (let [f (fn f [[x & x+]]
                             (if x+
                               (let [upstream (f x+)]
                                 (fn []
                                   (mprop/check x
                                                (upstream))))
                               (fn []
                                 (mprop/check x
                                              value))))]
                     ((f path)))))))
