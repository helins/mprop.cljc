;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at https://mozilla.org/MPL/2.0/.


(ns helins.mprop.test

  "Testing core features."

  {:author "Adam Helinski"}

  (:require [clojure.test.check.generators :as TC.gen]
            [clojure.test.check.properties :as TC.prop]
            [helins.mprop                  :as mprop]))


;;;;;;;;;;


(mprop/deftest deftest

  {:num-tests 1}

  (TC.prop/for-all [_ (TC.gen/return nil)]
    true))
