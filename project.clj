(defproject boids "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}

  :min-lein-version "2.3.4"

  ;; We need to add src/cljs too, because cljsbuild does not add its
  ;; source-paths to the project source-paths
  :source-paths ["src/clj" "src/cljs"]
  :resources-paths ["dev-resources"]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3196"]
                 [quil "2.2.5" :exclusions [org.clojure/clojure]]]

  :plugins [[lein-cljsbuild "1.0.5"]]

  :repl-options {:port 45678}

  :cljsbuild
  {:builds {;; This build is only used for including any cljs source
            ;; in the packaged jar when you issue lein jar command and
            ;; any other command that depends on it
            :boids
            {:source-paths ["src/cljs"]
             :compiler
                           {:verbose true
                            :output-to  "dev-resources/public/js/boids.js"
                            :output-dir "dev-resources/public/js"
                            :source-map "dev-resources/public/js/boids.js.map"
                            :main "boids.core"
                            :asset-path "/js"
                            :optimizations :advanced
                            :pretty-print true}}
            }})
