(ns guestbook.routes.home
  (:require [guestbook.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]
    ;[ring.util.http-response :as response]
            [ring.util.response :refer [response status]]
            [clojure.java.io :as io]
            [guestbook.db.core :as db]
            [bouncer.core :as b]
            [bouncer.validators :as v]))

(defn home-page []
  (layout/render "home.html"))

(defn about-page []
  (layout/render "about.html"))

(defn validate-message [params]
  (first
    (b/validate
      params
      :name v/required
      :message [v/required [v/min-count 10]])))

(defn save-message! [{:keys [params]}]
  (if-let [errors (validate-message params)]
    (-> {:errors errors}
        response
        (status 400))
    (do
      (db/save-message!
        (assoc params :timestamp (java.util.Date.)))
        (response {:status :ok}))
    )
  )

(defroutes home-routes
           (GET "/" [] (home-page))
           (GET "/messages" [] (response (db/get-messages)))
           (POST "/add-message" request (save-message! request))
           (GET "/about" [] (about-page)))

