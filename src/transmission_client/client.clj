(ns transmission-client.client
  (:require [clj-http.client :as client]
            [slingshot.slingshot :refer [try+]]))

(defn rpc
  ([server method args] (rpc server method args nil ""))
  ([server method args auth] (rpc server method args auth ""))
  ([server method args auth sessionid]
   (try+
    (let [req {:content-type :json
               :form-params {"arguments" args "method" method}
               :basic-auth auth
               :headers {"X-Transmission-Session-Id" sessionid}
               :as :json}]
      (client/post server req))
    (catch [:status 409] {:keys [headers]}
      (rpc server method args auth (get headers "X-Transmission-Session-Id"))))))

(defmacro ^{:private true} defrpc [name params rpc-name rpc-in & body]
  `(defn ~name
     ([~'server ~@params] (~name ~'server ~@params nil ""))
     ([~'server ~@params ~'auth] (~name ~'server ~@params ~'auth ""))
     ([~'server ~@params ~'auth ~'sessionid]
      (let [~'result (->
                    (rpc ~'server ~rpc-name ~rpc-in ~'auth ~'sessionid)
                    :body)]
        ~(if body `(do ~@body) `~'result)))))

(defrpc torrent-start [ids] "torrent-start" {"ids" ids})

(defrpc torrent-start-now [ids] "torrent-start-now" {"ids" ids})

(defrpc torrent-stop [ids] "torrent-stop" {"ids" ids})

(defrpc torrent-verify [ids] "torrent-verify" {"ids" ids})

(defrpc torrent-reannounce [ids] "torrent-reannounce" {"ids" ids}) 

(defrpc torrent-set [ids params] "torrent-set" (merge {"ids" ids} params))

(def ^{:private true}
  status_map
  {0 :stopped
   1 :check_wait
   2 :check
   3 :download_wait
   4 :download
   5 :seed_wait
   6 :seed})

(def ^{:private true}
  tracker_state
  {0 :inactive
   1 :waiting
   2 :queued
   3 :active})

(def ^{:private true}
  error_type
  {0 :ok
   1 :tracker_warning
   2 :tracker_error
   3 :local_error})

(def ^{:private true}
  eta_map
  {-1 :not_available
   -2 :unknown})

(def ^{:private true}
  ratio_map
  {-1 :not_applicable
   -2 :infinite})

(def mappers ^{:private true}
  {:status status_map
   :error error_type
   :eta eta_map
   :uploadRatio ratio_map
   :etaIdle eta_map})

(defn- parse-torrent-get [result]
  (assoc-in result [:arguments :torrents]
            (->> (get-in result [:arguments :torrents])
                 (map parse-torrent-get-item)
                 (into []))))

(defn- parse-torrent-get-item [item]
  (->>
   item
   (map (fn [pair]
          (let [[key val] pair
                replacement (or (get (get mappers key nil) val) val)]
            (assoc pair 1 replacement))))
   (into {})))

(defrpc torrent-get [fields] "torrent-get" {"fields" fields} (parse-torrent-get result))

(defrpc torrent-get-ids [ids fields] "torrent-get" {"ids" ids "fields" fields})

(defrpc torrent-add-file [path params] "torrent-add" (merge {"filename" path} params))

(defrpc torrent-add-metainfo [metainfo params] "torrent-add"
  (merge {"metainfo" metainfo} params))

(defrpc torrent-remove [ids delete-local-data] "torrent-remove"
  {"ids" ids "delete-local-data" delete-local-data})

(defrpc torrent-set-location [ids location move] "torrent-set-location"
  {"ids" ids "location" location "move" move})

(defrpc torrent-rename-path [id path name] "torrent-rename-path"
  {"ids" [id] "path" path "name" name})

(defrpc session-set [params] "session-set" params)

(defrpc session-get nil "session-get" nil)

(defrpc session-stats nil "session-stats" nil)

(defrpc blocklist-update nil "blocklist-update" nil)

(defrpc port-test nil "port-test" nil)

(defrpc session-close nil "session-close" nil)

(defrpc queue-move-top [ids] "queue-move-top" {"ids" ids})
(defrpc queue-move-up [ids] "queue-move-up" {"ids" ids})
(defrpc queue-move-down [ids] "queue-move-down" {"ids" ids})
(defrpc queue-move-bottom [ids] "queue-move-botom" {"ids" ids})

(defrpc free-space [path] "free-space" {"path" path})
