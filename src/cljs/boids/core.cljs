;;; If this namespace requires macros, remember that ClojureScript's
;;; macros are written in Clojure and have to be referenced via the
;;; :require-macros directive where the :as keyword is required, while in Clojure is optional. Even
;;; if you can add files containing macros and compile-time only
;;; functions in the :source-paths setting of the :builds, it is
;;; strongly suggested to add them to the leiningen :source-paths.
(ns boids.core
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m :include-macros true]))

(defprotocol PVec
  (add [a b])
  (subtract [a b])
  (mag [a])
  (normalise [a])
  (multiply [a b])
  (dist [a b])
  (negate [a]))

(defrecord Vector [x y]
  PVec
  (add [a b] (Vector. (+ (:x a) (:x b)) (+ (:y a) (:y b))))
  (subtract [a b] (Vector. (- (:x a) (:x b)) (- (:y a) (:y b))))
  (mag [a] (q/mag (:x a) (:y a)))
  (normalise [a]
    (let [m (mag a)]
      (Vector. (/ (:x a) m) (/ (:y a) m))))
  (multiply [a b]
    (Vector. (* (:x a) b) (* (:y a) b)))
  (dist [a b] (q/dist (:x a) (:y a) (:x b) (:y b)))
  (negate [a] (multiply a -1)))

(defrecord Boid [position velocity])

(defn gen-boid []
  (Boid. (Vector. (q/random (q/width))
                  (q/random (q/height)))
         (Vector. (q/random -5 5)
                  (q/random -5 5))))

(defn setup []
  (q/smooth)
  (q/frame-rate 60)

  (let [boids (repeatedly 100 gen-boid)]
    {:boids     boids
     :obstacles [
                 {:position (multiply (Vector. (q/width) (q/height)) 0.5)
                  :radius 50}]
     :flock? true})

  )

(defn update-boid [b flock? bs os]
  ;(.profile js/console "update")
  (let [requested []
        nearby-boids (filter (partial not= b) (filter #(< (dist (:position b) (:position %)) 100) bs))

        ;; Avoid walls

        requested (conj requested (multiply (Vector. -1 0) (/ 500 (q/sq (- (q/width) (:x (:position b)))))))
        requested (conj requested (multiply (Vector. 1 0) (/ 500 (q/sq (:x (:position b))))))
        requested (conj requested (multiply (Vector. 0 1) (/ 500 (q/sq (:y (:position b))))))
        requested (conj requested (multiply (Vector. 0 -1) (/ 500 (q/sq (- (q/height) (:y (:position b)))))))

        ;; Avoid obstacles

        requested (conj requested (reduce add (Vector. 0 0) (map (fn [n]
                                                                   (multiply (normalise (subtract (:position b) (:position n)))
                                                                             (/ 500 (q/sq (- (dist (:position b) (:position n)) (:radius n)))))) os)))

        ;; Avoid collisions with nearby boids

        requested (if (> (count nearby-boids) 0)
                    (conj requested (reduce add (Vector. 0 0) (map (fn [n]
                                                                  (multiply (normalise (subtract (:position b) (:position n)))
                                                                            (/ 500 (q/sq (dist (:position b) (:position n)))))) nearby-boids)))
                    requested)

        ;; Match velocity with nearby boids

        requested (if (and flock? (> (count nearby-boids) 0))
                    (conj requested (multiply (subtract (multiply (reduce add (map :velocity nearby-boids)) (/ 1 (count nearby-boids))) (:velocity b))
                                              0.1))
                    requested)
        ;; Attract to Center of Mass of nearby boids


        requested (if (and flock? (> (count nearby-boids) 0))
                    (let [com (multiply (reduce add (map :position nearby-boids)) (/ 1 (count nearby-boids)))]
                      (conj requested (normalise (subtract com (:position b)))))
                    requested)

        ;; Aim for a sensible speed

        requested (conj requested (multiply (:velocity b)
                                            (* 0.1 (- 5 (mag (:velocity b))))))



        new-v   (reduce add (:velocity b) requested)


        ;; Limit top speed to 5
        v-mag   (mag new-v)
        new-v   (if (> v-mag 5)
                  (multiply (normalise new-v) 5)
                  new-v)
        ]
    ;(.profileEnd js/console)
    (Boid. (add (:position b) new-v)
           new-v)))


(defn update-boids [state]
  (assoc state :boids (map (fn [b]
                             (update-boid b (:flock? state) (:boids state) (:obstacles state))) (:boids state) )))

(defn draw [state]
  (q/background 200)
  (q/stroke 0)                                 ;; Set the stroke colour to a random grey
  (q/stroke-weight 1)                           ;; Set the stroke thickness randomly
  (q/fill 255 0 0)     ;; Set the fill colour to a random grey
  (q/ellipse-mode :radius)

  (doseq [b (:boids state)]
    (let [norm (multiply (normalise (:velocity b)) 20)
          perp (multiply (normalise (Vector. (- (:y (:velocity b))) (:x (:velocity b)))) 5)
          p1 (add (:position b) norm)
          p2 (add (:position b) perp)
          p3 (subtract (:position b) perp)]
      (q/triangle (:x p1) (:y p1) (:x p2) (:y p2) (:x p3) (:y p3)))
    ;(q/ellipse (:x (:position b)) (:y (:position b)) 10 10)
    )

  (q/fill 100)
  (doseq [o (:obstacles state)]
    (q/ellipse (:x (:position o)) (:y (:position o)) (:radius o) (:radius o)))

  )

(defn mouse-moved [state e]
  (assoc state :obstacles [{:position (Vector. (:x e) (:y e))
                            :radius 50}]))

(defn mouse-clicked [state e]
  (assoc state :flock? (not (:flock? state))))

(defn boids-sketch []
  (q/sketch
    :host "boids-sketch"
    :setup setup
    :update #'update-boids
    :middleware [m/fun-mode]
    :draw #'draw
    :size [(- (aget js/window "innerWidth") 40) (- (aget js/window "innerHeight") 100)]
    :mouse-moved mouse-moved
    :mouse-clicked mouse-clicked))

(boids-sketch)
