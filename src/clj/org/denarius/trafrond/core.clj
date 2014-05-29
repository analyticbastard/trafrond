(ns org.denarius.trafrond.core
  (:gen-class)
  (:use [org.httpkit.server :only [run-server]])
  (:require [liberator.core :refer [resource]]
            [ring.middleware.params :refer [wrap-params]]
            ;[ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer [defroutes ANY GET POST]]
            [com.stuartsierra.component :as component]
            ;[taoensso.sente :as sente]
            compojure.handler
            [clojure.tools.cli :refer [parse-opts]]
            [cemerick.friend.credentials :as creds]
            [org.denarius.trafrond.middleware.auth :as auth]
            [org.denarius.trafrond.resources :as r :refer [defresource]]
            ))


(def server-component nil)


(defn valid? [login password]
  (and (= login "javier") (= password "x")))

(defn serve []
  (slurp "index.html"))

(defn validate-and-serve [login password]
  (if (valid? login password)
    (serve)))

(defresource serve-lib [type elem]
             :available-media-types ["text/html"
                                     "text/css"]
             :handle-ok (fn [_] (println type elem)
                          (let [filename (str "lib/" type "/" elem)]
                            (if (.exists (clojure.java.io/as-file filename))
                              (slurp filename)) ) ))

(defresource serve-resource [type elem]
             :available-media-types ["text/html"
                                     "text/css"]
             :handle-ok (fn [_] (println type elem)
                          (let [filename (str type "/" elem)]
                            (if (.exists (clojure.java.io/as-file filename))
                              (slurp filename)) ) ))

(defresource login-page []
             :allowed-methods [:get]
             :available-media-types ["text/html"]
             :handle-ok (fn [_] (slurp "html/login.html")))

(defresource perform-login [username password]
             :base (r/role-auth #{:user})
             :allowed-methods [:post :get]
             :post! (fn [ctx]
                      (print "POST")
                      (dosync
                        (let [body (slurp (get-in ctx [:request :body]))]
                          ;{::id id}
                          body
                          )))
             :available-media-types ["text/html"]
             :post-redirect? (fn [_] {:location "/main"})
             :handle-ok (fn [_] (print "OK!") "OK!!!!" )
             )

(defresource main []
             :base (r/role-auth #{:user})
             :allowed-methods [:get]
             :available-media-types ["text/html"]
             :handle-ok (fn [_] (slurp "html/main.html")))

(defroutes site-routes
           (ANY "/login" [login password] (perform-login login password))

           (GET "/main" [] (main))
           (GET "/lib/:type/:elem" [type elem] (serve-lib type elem))
           (GET "/:type/:elem" [type elem] (serve-resource type elem))
           (GET "/" [] (login-page)) ;(res-entity aliasid dbname basist id)
           )

(def users
  "dummy in-memory user database."
  {"root" {:username "root"
           :password (creds/hash-bcrypt "admin_password")
           :roles #{:admin}}
   "jane" {:username "jane"
           :password (creds/hash-bcrypt "user_password")
           :roles #{:user}}})

(def handler (-> site-routes (auth/friend-middleware users) (compojure.handler/api) #_(wrap-params)))


(defrecord RESTServer [port join connector-host connector-port]
  component/Lifecycle
  (start [component]
    (println ";; Starting HTTP server")
    ; Create API Java objects here
    (let [stop-server (run-server #'handler {:port port :join? join})]
      (assoc component :stop-server stop-server)))
  (stop [component]
    (println ";; Stopping HTTP server")
    (let [stop-server (:stop-server component)]
      (stop-server))
    component))

(defn new-rest-server
  "Create a new server. This does not start one. Start it with:
  (alter-var-root #'server-component component/start)
  Stop it with:
  (alter-var-root #'server-component component/stop)
  A nice way to test the server is using curl:"
  ;$ curl -H "Accept:application/edn" localhost:3000/asof?ver=1"
  [port connector-host connector-port join]
  (map->RESTServer {:port port :join? join :connector-host connector-host :connector-port connector-port }))


(defn start-server! []
  (alter-var-root #'server-component component/start))

(defn stop-server! []
  (alter-var-root #'server-component component/stop))

(defn restart-server! []
  (alter-var-root #'server-component component/stop)
  (alter-var-root #'server-component component/start))

(def program-options
  [
    ["-p" "--port PORT" "Port number to listent to"
     :default 3001
     :parse-fn #(Integer/parseInt %)
     :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
    ["-h" "--connector-host HOST" "Connector remote server IP or name"
     :default "localhost"
     :parse-fn identity]
    ["-c" "--connector-port PORT" "Connector port to connect to"
     :default 7891
     :parse-fn #(Integer/parseInt %)
     :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
    ;; A boolean option defaulting to nil
    ["-?" "--help" "Show help"]])

(defn -main [& args]
  ""
  (let [{:keys [port connector-host connector-port]} (-> args (parse-opts program-options ) :options)]
    (alter-var-root #'server-component (fn [_] (new-rest-server port connector-host connector-port false)))
    (start-server!)
    ))