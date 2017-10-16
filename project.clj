(defproject tweetiom "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.671"]
                 [lein-doo "0.1.7"]
                 [devcards "0.2.3"]
                 [reagent "0.6.0"]
                 [axiom-clj/axiom-cljs "0.4.1"]]
  :plugins [[lein-cljsbuild "1.1.4" :exclusions [[org.clojure/clojure]]]
            [lein-figwheel "0.5.6"]
            [lein-doo "0.1.7"]
            [lein-midje "3.1.3"]
            [axiom-clj/lein-axiom "0.4.1"]]
  :clean-targets ^{:protect false} [:target-path "out" "resources/public/cljs"]

  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[midje "1.8.3"]
                                  [axiom-clj/cloudlog-events "0.4.1"]
                                  [brosenan/reagent-query "0.2.1"]]}}

  :cljsbuild {
              :test-commands {"test" ["lein" "doo" "phantom" "test" "once"]}
              :builds [{:id "dev"             ; development configuration
                        :source-paths ["src"] ; Paths to monitor for build
                        :figwheel true        ; Enable Figwheel
                        :compiler {:main tweetiom.core     ; your main namespace
                                   :asset-path "cljs/out"                       ; Where load-dependent files will go, mind you this one is relative
                                   :output-to "resources/public/cljs/main.js"   ; Where the main file will be built
                                   :output-dir "resources/public/cljs/out"      ; Directory for temporary files
                                   :source-map-timestamp true}                  ; Sourcemaps hurray!
                        }
                       {:id "test"
                        :source-paths ["src" "test"]
                        :compiler {:main runners.doo
                                   :optimizations :none
                                   :output-to "resources/public/cljs/tests/all-tests.js"}}
                       {:id "devcards-test"
                        :source-paths ["src" "test"]
                        :figwheel {:devcards true}
                        :compiler {:main runners.browser
                                   :optimizations :none
                                   :asset-path "cljs/tests/out"
                                   :output-dir "resources/public/cljs/tests/out"
                                   :output-to "resources/public/cljs/tests/all-tests.js"
                                   :source-map-timestamp true}}]}
  :main ^:skip-aot tweetiom.core
  :target-path "target/%s"

  :axiom-run-config
  {:zookeeper-config {:url "127.0.0.1:2181"}
   :zk-plan-config {:num-threads 5
                    :parent "/my-plans"}
   :migration-config {:number-of-shards 3
                      :plan-prefix "/my-plans"
                      :clone-location "/tmp"
                      :clone-depth 10}
   :local-storm-cluster true
   :fact-spout {:include [:rabbitmq-config]}
   :store-bolt {:include [:dynamodb-event-storage-num-threads
                          :dynamodb-default-throughput
                          :dynamodb-config]}
   :output-bolt {:include [:rabbitmq-config]}
   :initlal-link-bolt {:include [:storage-local-path
                                 :storage-fetch-url]}
   :link-bolt {:include [:storage-local-path
                         :storage-fetch-url
                         :dynamodb-config
                         :dynamodb-default-throughput
                         :num-database-retriever-threads]}
   :use-dummy-authenticator true ;; Completely remove this entry to avoid the dummy authenticator
   :dummy-version "dev-705491"
   :http-config {:port 8080}}

  :axiom-deploy-config
  {:storage-local-path "/tmp/axiom-perms"
   :storage-fetch-url "https://s3.amazonaws.com/brosenan-test"
   :rabbitmq-config {:username "guest"
                     :password "guest"
                     :vhost "/"
                     :host "localhost"
                     :port 5672}
   :dynamodb-config {:access-key "STANDALONE-DB"
                     :secret-key "XXYY"
                     :endpoint "http://localhost:8006"}
   :num-database-retriever-threads 1
   :dynamodb-default-throughput {:read 1 :write 1}
   :dynamodb-event-storage-num-threads 3})
