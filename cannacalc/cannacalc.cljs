;; ## helper
;; ### DOM
(def log (.-log js/console))


(defn get-nested-id [id-value]
  (let [[category id] (.split id-value "_")]
    [(keyword category) (keyword id)]))

(defn retrieve-id-and-value [input]
  (let [nested-id (get-nested-id (.-id input))
        value (.-value input)]
    {:nested-id nested-id :value value}))


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
  (round-number
    (/
      (/
        (*
          (* grams-of-trim
            1000)
          (*
            (/ thc-content 100)
            (/ efficiency-of-extraction 100)))
        dose-per-portion)
      size-of-portion-in-ml)))


(comment
  (log (calculate-edible-amount-needed :dose-per-portion 10 :number-of-portions 16 :thc-content 20 :efficiency-of-extraction 80))
  )


(defn update-calculation! [values]
  (let [amount (calculate-edible-amount-needed values)
        result-element (.getElementById js/document "edible_result")]
    (set! (.. result-element -innerText) amount)))
(defn update-calculation-tincture! [values]
  (let [amount (calculate-tincture-amount-needed values)
        result-element (.getElementById js/document "tincture_result")]
    (set! (.. result-element -innerText) amount)))



;; ## initialization

(defn register-handler! [input]
  (.addEventListener input "input"
    (fn []
      (let [{:keys [nested-id value]} (retrieve-id-and-value input)]
        (set! (.-value (.-nextElementSibling input)) value)
        (update-state! app-state nested-id value)))))


(defn set-initial-values! [inputs]
  (run!
    (fn [input]
      (let [{:keys [nested-id value]} (retrieve-id-and-value input)]
        (update-state! app-state nested-id value)
        (set! (.-value (.-nextElementSibling input)) value)))
    inputs))

(defn init []
  (let [inputs (.getElementsByTagName js/document "input")]
    (run! register-handler! inputs)
    (set-initial-values! inputs)))


;; ### kick it off
(init)
