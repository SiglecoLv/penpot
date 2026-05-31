;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC

(ns app.main.ui.ds.product.loader
  (:require-macros
   [app.common.data.macros :as dm]
   [app.main.style :as stl])
  (:require
   [app.common.data :as d]
   [app.common.math :as mth]
   [app.util.i18n :as i18n :refer [tr]]
   [beicon.v2.core :as rx]
   [rumext.v2 :as mf]))

;; ---------- Tips ----------
(defn- get-tips
  []
  [{:title (tr "loader.tips.01.title")
    :message (tr "loader.tips.01.message")}
   {:title (tr "loader.tips.02.title")
    :message (tr "loader.tips.02.message")}
   {:title (tr "loader.tips.03.title")
    :message (tr "loader.tips.03.message")}
   {:title (tr "loader.tips.04.title")
    :message (tr "loader.tips.04.message")}
   {:title (tr "loader.tips.05.title")
    :message (tr "loader.tips.05.message")}
   {:title (tr "loader.tips.06.title")
    :message (tr "loader.tips.06.message")}
   {:title (tr "loader.tips.07.title")
    :message (tr "loader.tips.07.message")}
   {:title (tr "loader.tips.08.title")
    :message (tr "loader.tips.08.message")}
   {:title (tr "loader.tips.09.title")
    :message (tr "loader.tips.09.message")}
   {:title (tr "loader.tips.10.title")
    :message (tr "loader.tips.10.message")}])

;; ---------- SVG paths (new) ----------
(def ^:private svg:loader-static
  ;; All static parts of the loader icon (everything except the spinning arc)
  "M116.537 75.6746L109.063 83.4141L177.503 149.506L184.977 141.766L116.537 75.6746Z M176.372 2.00061L184.428 10.0576L100.636 93.8502L92.5789 85.7932L176.372 2.00061Z M80.0463 2H66.3732V151.265H80.0463V2Z M48.7371 6.5576C48.7371 9.07475 50.7776 11.1153 53.2948 11.1153C55.812 11.1153 57.8525 9.07475 57.8525 6.5576C57.8525 4.04045 55.812 1.9999 53.2948 1.9999C50.7776 1.9999 48.7371 4.04045 48.7371 6.5576ZM48.7371 168.926C48.7371 171.443 50.7776 173.484 53.2948 173.484C55.812 173.484 57.8525 171.443 57.8525 168.926C57.8525 166.409 55.812 164.369 53.2948 164.369C50.7776 164.369 48.7371 166.409 48.7371 168.926ZM53.2948 6.5576H52.4402V168.926H53.2948H54.1494V6.5576H53.2948Z M117.67 81.6813L187.176 150.234V150.125H203.128V166.077H187.176V150.234L117.67 81.6813V81.7593H106.276V70.3651H117.67V81.6813Z M198.664 161.566L198.616 154.588L191.639 154.636L191.687 161.614L198.664 161.566ZM108.46 72.5976L108.509 79.5749L115.486 79.5268L115.438 72.5495L108.46 72.5976ZM117.67 81.7593V82.6139H118.525V81.7593H117.67ZM117.67 70.3651H118.525V69.5105H117.67V70.3651ZM106.276 70.3651V69.5105H105.422V70.3651H106.276ZM106.276 81.7593H105.422V82.6139H106.276V81.7593ZM187.176 150.125V149.271H186.321V150.125H187.176ZM187.176 166.077H186.321V166.932H187.176V166.077ZM203.128 166.077V166.932H203.982V166.077H203.128ZM203.128 150.125H203.982V149.271H203.128V150.125ZM195.152 158.101L195.752 157.493L187.776 149.626L187.176 150.234L186.576 150.843L194.552 158.71L195.152 158.101ZM187.176 150.234L187.776 149.626L118.27 81.0729L117.67 81.6813L117.07 82.2897L186.576 150.843L187.176 150.234ZM117.67 81.6813L118.27 81.0729L112.573 75.4538L111.973 76.0622L111.373 76.6706L117.07 82.2897L117.67 81.6813ZM117.67 81.7593H118.525V81.6813H117.67H116.816V81.7593H117.67ZM117.67 81.6813H118.525V70.3651H117.67H116.816V81.6813H117.67ZM117.67 70.3651V69.5105H106.276V70.3651V71.2196H117.67V70.3651ZM106.276 70.3651H105.422V81.7593H106.276H107.131V70.3651H106.276ZM106.276 81.7593V82.6139H117.67V81.7593V80.9048H106.276V81.7593ZM187.176 150.125H186.321V150.234H187.176H188.03V150.125H187.176ZM187.176 150.234H186.321V166.077H187.176H188.03V150.234H187.176ZM187.176 166.077V166.932H203.128V166.077V165.223H187.176V166.077ZM203.128 166.077H203.982V150.125H203.128H202.273V166.077H203.128ZM203.128 150.125V149.271H187.176V150.125V150.98H203.128V150.125Z")

(def ^:private svg:loader-spin
  ;; The arc that will spin
  "M149.496 114.94C144.955 125.1 138.005 133.999 129.248 140.867C120.491 147.735 110.192 152.364 99.2428 154.353C88.2935 156.343 77.0242 155.632 66.4113 152.284C55.7983 148.936 46.1618 143.051 38.3365 135.138C30.5112 127.225 24.7332 117.524 21.5029 106.874C18.2727 96.225 17.6877 84.9485 19.7986 74.022C21.9096 63.0954 26.6527 52.8483 33.6172 44.1684C40.5817 35.4885 49.5575 28.6375 59.767 24.209")

;; ---------- Loader icon component ----------
(mf/defc loader-icon*
  {::mf/private true}
  [{:keys [width height title] :rest props}]
  (let [class (stl/css :loader)
        props (mf/spread-props props {:viewBox "0 0 204 175"  ;; new viewBox
                                      :role "status"
                                      :width width
                                      :height height
                                      :class class})]
    [:> :svg props
     [:title title]
     ;; Static part
     [:path {:d svg:loader-static
             :fill "#F7F7F7"
             :stroke "#F7F7F7"
             :stroke-width "1.13943"}]
     ;; Spinning arc (CSS animation is defined in the stylesheet)
     [:path {:d svg:loader-spin
             :fill "none"
             :stroke "#47FECE"
             :stroke-width "1.71"
             :stroke-linecap "square"
             :stroke-dasharray "6.42 9.5"
             :class (stl/css :loader-spin)}]]))

;; ---------- Schema ----------
(def ^:private schema:loader
  [:map
   [:class {:optional true} :string]
   [:width {:optional true} :int]
   [:height {:optional true} :int]
   [:title {:optional true} :string]
   [:overlay {:optional true} :boolean]
   [:file-loading {:optional true} :boolean]])

;; ---------- Main loader component ----------
(mf/defc loader*
  {::mf/schema schema:loader}
  [{:keys [class width height title overlay children file-loading] :rest props}]
  (let [width  (or width (when (some? height) (mth/ceil (* height (/ 204 175)))) 204)
        height (or height (when (some? width) (mth/ceil (* width (/ 175 204)))) 175)

        class  (dm/str (d/nilv class "") " "
                       (stl/css-case :wrapper true
                                     :wrapper-overlay overlay
                                     :file-loading file-loading))

        title  (or title (tr "labels.loading"))
        tips   (mf/use-memo get-tips)

        tip*   (mf/use-state nil)
        tip    (deref tip*)]

    (mf/with-effect [file-loading tips]
      (when file-loading
        (let [sub (->> (rx/timer 1000 4000)
                       (rx/subs! #(reset! tip* (rand-nth tips))))]
          (partial rx/dispose! sub))))

    [:> :div {:class class}
     [:div {:class (stl/css :loader-content)}
      [:> loader-icon* {:title title
                        :width width
                        :height height}]
      (when (and file-loading tip)
        [:div {:class (stl/css :tips-container)}
         [:div {:class (stl/css :tip-title)}
          (get tip :title)]
         [:div {:class (stl/css :tip-message)}
          (get tip :message)]])]

     children]))