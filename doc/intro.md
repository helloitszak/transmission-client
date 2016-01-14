# Introduction to transmission-client

Transmission-client provides a client to the Transmission Torrent Client's RPC protocol. The functions are very
lightweight and don't touch the inputs or outputs, the one exception being `torrent-get`.

You can find everything in the `transmission-client.get` namespace.

I highly encourage you to read the [Transmission RPC Documentation][transmission-rpc] for details on individual fields
and how they behave. 

[transmission-rpc]: https://trac.transmissionbt.com/browser/trunk/extras/rpc-spec.txt


## `transmission-get`

The exception to the light touch rule is `transmission-get` fuction, which does some output coersion. For example, any
function that returns a integer meant for enum lookup is mapped to a symbol.

The "pieces" field is also mapped to a vector of true/false values, representing the status of each piece, however it is
wise to also retrieve "pieceCount" and truncate the array to that value. This is due to transmission encoding each piece
as a bit, storing them in bytes. If you only have 4 pieces, you will have 4 garbage pieces as well you will have to truncate.


## Examples

```clojure
(ns cool.example
    (:require [transmission-client.client :as trc]))

(def transmission-server "http://example.com:9091/transmission/rpc")
(def transmission-credentials ["username" "password"])

(->> (trc/torrent-get transmission-server ["id" "name" "status"] transmission-credentials)
     :arguments
     :torrents
     (map #(println (str "The status of " (:name %) " (id: " (:id %)  ") is " (:status %) ))))


;; will produce
;; The status of My Torrent (id: 54) is :stopped
;; The status of My Other Torrent (id: 55) is :download
;; The status of My Last Torrent (id: 56) is :seed
```

