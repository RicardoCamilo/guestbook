(ns guestbook.core
    (:require [reagent.core :as  reagent :refer [atom]]
              [ajax.core :refer [GET]]
              [guestbook.ws :as ws]))

(defn message-list [messages]
    [:ul.content
     (for [{:keys [timestamp message name]} @messages]
         ^{:key timestamp}
         [:li
          [:time (.toLocaleString timestamp)]
          [:p message]
          [:p " - " name]])])

(defn get-messages [messages]
  (GET "/messages"
       {:headers {"Accept" "application/transit+json"}
        :handler #(reset! messages (vec %))}))

(comment
  (defn send-message! [fields errors messages]
    (POST "/add-message"
          {:headers {"Accept" "application/transit+json"
                     "x-csrf-token" (.-value (.getElementById js/document "token"))}
           :params @fields
           :handler #(do
                       (reset! errors nil)
                       (swap! messages conj (assoc @fields :timestamp (js/Date.))))
           :error-handler #(do
                             (.log js/console (str %))
                             (reset! errors (get-in % [:response :errors])))})))

(defn errors-component [errors id]
    (when-let [error (id @errors)]
        [:div.alert.alert-danger (clojure.string/join error)]))

(comment
  (defn message-form [messages]
    (let [fields (atom {})
          errors (atom nil)]
      (fn []
        [:div.content
         [:div.form-group
          [errors-component errors :name]
          [:p "Name:"
           [:input.form-control
            {:type :text
             :name :name
             :on-change #(swap! fields assoc :name (-> % .-target .-value))
             :value (:name @fields)}]]
          [errors-component errors :message]
          [:p "Message:"
           [:textarea.form-control
            {:rows 4
             :cols 50
             :name :message
             :value (:message @fields)
             :on-change #(swap! fields assoc :message (-> % .-target .-value))}]]
          [:input.btn.btn-primary
           {:type :submit
            :on-click #(send-message! fields errors messages)
            :value "comment"}]]]))))

(defn message-form [fields errors]
  [:div.content
   [:div.form-group
    [errors-component errors :name]
    [:p "Name:"
     [:input.form-control
      {:type :text
       :on-change #(swap! fields assoc :name (-> % .-target .-value))
       :value (:name @fields)}]]
    [errors-component errors :message]
    [:p "Message:"
     [:textarea.form-control
      {:rows 4
       :cols 50
       :value (:message @fields)
       :on-change #(swap! fields assoc :message (-> % .-target .-value))}]]
    [:input.btn.btn-primary
     {:type :submit
      :on-click #(ws/send-message! @fields)
      :value "comment"}]]])

(defn response-handler [messages fields errors]
  (fn [message]
    (if-let [response-errors (:errors message)]
      (reset! errors response-errors)
      (do
        (reset! errors nil)
        (reset! fields nil)
        (swap! messages conj message)))))

(defn home []
    (let [messages (atom nil)
          errors (atom nil)
          fields (atom nil)]
        (ws/connect! (str "ws://" (.-host js/location) "/ws")
                     (response-handler messages fields errors))
        (get-messages messages)
        (fn []
            [:div
             [:div.row
              [:div.span12
               [message-list messages]]]
             [:div.row
              [:div.span12
               [message-form fields errors]]]])))

(reagent/render
    [home]
    (.getElementById js/document "content"))
