;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC

(ns app.main.ui.workspace.viewport.rulers-config)

(def rulers-pos 15)
(def rulers-size 4)
(def rulers-width 1)
(def ruler-area-size 22)
(def ruler-clip-area 25)
(def ruler-area-half-size (/ ruler-area-size 2))
(def rulers-background "var(--panel-background-color)")
(def selection-area-color "var(--color-accent-tertiary)")
(def selection-area-opacity 0.3)
(def over-number-size 100)
(def over-number-opacity 0.8)
(def over-number-percent 0.75)

(def font-size 12)
(def font-family "worksans")
(def font-color "var(--layer-row-foreground-color)")
(def canvas-border-radius 12)

(def show-border? true)
