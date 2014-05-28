(defproject trafrond "0.1.0-SNAPSHOT"
  :description "Denarius Trading Frontend"
  :url "https://github.com/denarius-exchange/trafrond"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.cli "0.3.1"]
                 [liberator "0.10.0"]
                 [compojure "1.1.3"]
                 [com.stuartsierra/component "0.2.1"]
                 [http-kit "2.0.0"]
                 [com.taoensso/sente "0.14.0"]]
  :source-paths ["src/clj" "src/cljs"]
  :test-paths ["test"]
  :main org.denarius.trafrond.core)
