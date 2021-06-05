;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at https://mozilla.org/MPL/2.0/.


(ns helins.mprop.example

  "Testing the same assertions differently.

   They are failing on purpose so that the user can run them and compare the outputs."

  {:author "Adam Helinski"}

  (:require [clojure.pprint]
            [clojure.test.check.generators :as TC.gen]
            [clojure.test.check.properties :as TC.prop]
            [helins.mprop                  :as mprop]))


;;;;;;;;;; Tests


(mprop/deftest bad-test

  ;; As usual

  (TC.prop/for-all [x (TC.gen/vector TC.gen/large-integer)]
    (let [sorted (sort x)]
      (and (= (count x)
              (count sorted))
           (not= sorted
                 (sort sorted))))))



(mprop/deftest better-test

  ;; Using MProp.

  (TC.prop/for-all [x (TC.gen/vector TC.gen/large-integer)]
    (let [sorted (sort x)]
      (mprop/and (mprop/check "Both have the same size"
                              (= (count x)
                                 (count sorted)))
                 (mprop/check "Sorting is idempotent"
                              (not= sorted
                                    (sort sorted)))))))



(mprop/deftest awesome-test

  ;; Like previous example but cleaner.

  (TC.prop/for-all [x (TC.gen/vector TC.gen/large-integer)]
    (let [sorted (sort x)]
      (mprop/mult

        "Both have the same size"
        (= (count x)
           (count sorted))

        "Sorting is idempotent"
        (not= sorted
              (sort sorted))))))



(mprop/deftest nested

  ;; Nested example, hint at how composable the API is.

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


;;;;;;;;;; To run


(comment


  ;; Usual, poor way
  ;;
  (clojure.pprint/pprint (bad-test))


  ;; Improved ways, see the `:result-data` key-value
  ;;
  (clojure.pprint/pprint (better-test))


  ;; Like previous run, just syntactic sugar
  ;;
  (clojure.pprint/pprint (awesome-test))


  ;; Nested example, see how convenient it is having the path in `:result-data`
  ;;
  (clojure.pprint/pprint (nested))



  )
