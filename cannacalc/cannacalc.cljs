;; ## helper
;; ### DOM
(def log (.-log js/console))

(defn retrieve-id-and-value [input]
  (let [id (keyword (.-id input))
        value (.-value input)]
    {:id id :value value}))


;; ### math
(defn round-number
  [f]
  (/ (.round js/Math (* 100 f)) 100))



;; ## state
(defonce app-state (atom {
                          :dose-per-portion         10
                          :number-of-portions       10
                          :thc-content              10
                          :efficiency-of-extraction 80}))
(defn update-state! [app-state key value]
  (swap! app-state assoc key value))

(declare update-calculation!)

(add-watch app-state :calculator
  (fn [key atom old-val new-val]
    (update-calculation! new-val)))


;; ## business - calculating amount

(defn calculate-amount-needed [& {:keys [dose-per-portion number-of-portions thc-content efficiency-of-extraction] :as values}]
  (round-number
    (/
      (* number-of-portions
        (/ dose-per-portion 1000))
      (*
        (/ thc-content 100)
        (/ efficiency-of-extraction 100)))))


(comment
  (log (calculate-amount-needed :dose-per-portion 10 :number-of-portions 16 :thc-content 20 :efficiency-of-extraction 80))
  )


(defn update-calculation! [{:keys [dose-per-portion number-of-portions thc-content efficiency-of-extraction] :as values}]
  (let [amount (calculate-amount-needed values)
        result-element (.getElementById js/document "result")]
    (set! (.. result-element -innerText) amount)))



;; ## initialization

(defn register-handler! [inputs input]
  (.addEventListener input "input"
    (fn []
      (let [{:keys [id value]} (retrieve-id-and-value input)]
        (set! (.-value (.-nextElementSibling input)) value)
        (update-state! app-state id value)))))


(defn set-initial-values! [inputs]
  (run!
    (fn [input]
      (let [{:keys [id value]} (retrieve-id-and-value input)]
        (update-state! app-state id value)
        (set! (.-value (.-nextElementSibling input)) value)))
    inputs))

(defn init []
  (let [inputs (.getElementsByTagName js/document "input")]
    (run! #(register-handler! inputs %) inputs)
    (set-initial-values! inputs)))


;; ### kick it off
(init)
