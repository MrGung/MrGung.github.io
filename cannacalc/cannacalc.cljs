
(def log (.-log js/console))

(defn update-calculation! [inputs]
  (run!
    #(log (.-value %))
    inputs)

  )


(defn register-handler! [inputs input]
  (.addEventListener input "input"
    (fn []
      (set! (.-value (.-nextElementSibling input)) (.-value input))
      (update-calculation! inputs)
      ))
  )

(defn init []
  (let [inputs (.getElementsByTagName js/document "input")]
    (run! #(register-handler! inputs %) inputs)))

(init)
