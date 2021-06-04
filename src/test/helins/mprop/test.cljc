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


;;;;;;;;;;


(defn =

  ;; Takes care of NaN not being equal to itself

  [& x+]

  (apply clojure.core/=
         (map hash
              x+)))

;;;;;;;;;;


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



(mprop/deftest deftest

  {:num-tests 1}

  (TC.prop/for-all [_ (TC.gen/return nil)]
    true))
