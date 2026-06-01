;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.

(ns app.hooks
  "Extension hooks registry for enterprise features.
   Allows external modules (e.g. Contrast) to register handlers
   that are called at specific points in the application lifecycle."
  (:require
   [app.common.logging :as l]))

;; Profile enrichers: (fn [cfg profile] -> profile)
;; Called during get-profile to add license/subscription info
(defonce profile-enrichers (atom []))

;; Registration checks: (fn [cfg params] -> nil or throw)
;; Called before creating a new profile to enforce seat limits etc.
(defonce registration-checks (atom []))

(defn register-profile-enricher!
  "Register a function that enriches a profile with additional data.
   Function signature: (fn [cfg profile] -> profile)"
  [f]
  (swap! profile-enrichers conj f)
  (l/inf :hint "registered profile enricher" :fn (str f)))

(defn register-registration-check!
  "Register a function that checks registration constraints.
   Function signature: (fn [cfg params] -> nil). Should throw on failure."
  [f]
  (swap! registration-checks conj f)
  (l/inf :hint "registered registration check" :fn (str f)))

(defn enrich-profile
  "Apply all registered profile enrichers to the profile."
  [cfg profile]
  (reduce
   (fn [p enricher]
     (try
       (enricher cfg p)
       (catch Throwable cause
         (l/error :hint "profile enricher failed" :cause cause)
         p)))
   profile
   @profile-enrichers))

(defn check-registration!
  "Run all registered registration checks. Any check can throw to block registration."
  [cfg params]
  (doseq [check @registration-checks]
    (check cfg params)))

;; Load extensions if available (Contrast overlay adds app.extensions namespace)
(defn load-extensions!
  "Try to load the extensions namespace if it exists on the classpath."
  []
  (try
    (require 'app.extensions)
    (l/inf :hint "extensions loaded successfully")
    (catch java.io.FileNotFoundException _
      (l/inf :hint "no extensions found on classpath"))
    (catch Throwable cause
      (l/error :hint "failed to load extensions" :cause cause))))
