;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at https://mozilla.org/MPL/2.0/.


(ns helins.mprop.dev

  "CLJC playground during dev."

  {:author           "Adam Helinski"
   :clj-kondo/config '{:linters {:unused-namespace {:level :off}}}}

  (:require [clojure.test.check.generators :as TC.gen]
            [clojure.test.check.properties :as TC.prop]
            [helins.mprop                  :as mprop]
            [helins.mprop.test]))


;;;;;;;;;;


(comment


  )
