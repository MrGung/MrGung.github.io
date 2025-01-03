
(require '[clojure.string :as str])

;; ## helper
;; ### DOM
(def log (.-log js/console))


(defn get-nested-id [id-value]
  (let [[category id type] (str/split id-value #"_")]
    [(keyword category) (keyword id)]))

(defn retrieve-id-and-value [input]
  (let [id (.-id input)
        nested-id (get-nested-id id)
        value (.-value input)]
    {:nested-id nested-id :value value}))


;; #### value setter
(defn set-input-value! [result-type value]
  (let [result-element (.getElementById js/document result-type)]
    (set! (.-value result-element) value)))


(defn set-value-text! [input value]
  (let [id-value-source (.-id input)
        id-text-field (str id-value-source "_text")]
    (set-input-value! id-text-field value)))
(defn set-value-slider! [input value]
  (let [id-value-source (.-id input)
        id-slider-field (str/replace id-value-source "_text" "")]
    (set-input-value! id-slider-field value)))


;; ### math
(defn round-number
  [f]
  (/ (.round js/Math (* 100 f)) 100))



;; ## state
(defonce app-state (atom {:edible
                          {:dose-per-portion         0
                           :number-of-portions       0
                           :thc-content              0
                           :efficiency-of-extraction 0}
                          :tincture
                          {:dose-per-portion         0
                           :size-of-portion-in-ml    0
                           :thc-content              0
                           :efficiency-of-extraction 0
                           :grams-of-trim            0}}))
(defn update-state! [app-state nested-id value]
  (swap! app-state assoc-in nested-id value))

(declare update-calculation!)
(declare update-calculation-tincture!)

(add-watch app-state :dispatch-calculation
  (fn [key atom old-val new-val]
    (let [changed? (fn [category]
                     (not= (category old-val) (category new-val))
                     )]
      (cond
        (changed? :edible) (update-calculation! (:edible new-val))
        (changed? :tincture) (update-calculation-tincture! (:tincture new-val))
        ))))


;; ## business - calculating amount

(defn calculate-edible-amount-needed [& {:keys [dose-per-portion number-of-portions thc-content efficiency-of-extraction] :as values}]
  (round-number
    (/
      (* number-of-portions
        (/ dose-per-portion 1000))
      (*
        (/ thc-content 100)
        (/ efficiency-of-extraction 100)))))

(defn calculate-tincture-amount-needed [& {:keys [dose-per-portion size-of-portion-in-ml thc-content efficiency-of-extraction grams-of-trim] :as values}]
  ;; a maximum of 1 gram per 1 ml of high-percentage alcohol can be dissolved
  (let [total-amount-thc (*
                           (* grams-of-trim
                             1000)
                           (*
                             (/ thc-content 100)
                             (/ efficiency-of-extraction 100)))
        total-number-portions (/
                                total-amount-thc
                                dose-per-portion)]
    (round-number
      (*
        total-number-portions
        size-of-portion-in-ml))))


(comment
  (log (calculate-edible-amount-needed :dose-per-portion 10 :number-of-portions 16 :thc-content 20 :efficiency-of-extraction 80))
  )




(defn update-calculation! [values]
  (let [amount (calculate-edible-amount-needed values)]
    (set-input-value! "edible_result" amount)))
(defn update-calculation-tincture! [values]
  (let [amount (calculate-tincture-amount-needed values)]
    (set-input-value! "tincture_result" amount)))



;; ## initialization

(defn register-handler-slider! [input]
  (.addEventListener input "input"
    (fn []
      (let [{:keys [nested-id value]} (retrieve-id-and-value input)]
        (set-value-text! input value)
        (update-state! app-state nested-id value)))))

(defn register-handler-texts! [input]
  (.addEventListener input "input"
    (fn []
      (let [{:keys [nested-id value]} (retrieve-id-and-value input)]
        (set-value-slider! input value)
        (update-state! app-state nested-id value)))))


(defn set-initial-values! [inputs]
  (run!
    (fn [input]
      (let [{:keys [nested-id value]} (retrieve-id-and-value input)]
        (update-state! app-state nested-id value)
        (set-value-text! input value)))
    inputs))


(defn filter-inputs-by-type [inputs type]
  (filter (fn [input] (= type (.-type input))) inputs))

(defn init []
  (let [inputs (.getElementsByTagName js/document "input")
        sliders (filter-inputs-by-type inputs "range")
        texts (filter-inputs-by-type inputs "text")]
    (run! register-handler-slider! sliders)
    (run! register-handler-texts! texts)
    (set-initial-values! sliders)))


;; ### kick it off
(init)
