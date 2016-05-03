(defproject misabogados-workflow "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [selmer "1.0.0"]
                 [markdown-clj "0.9.88"]
                 [luminus/config "0.5"]
                 [ring-middleware-format "0.7.0"]
                 [metosin/ring-http-response "0.6.5"]
                 [bouncer "1.0.0"]
                 [cljsjs/bootstrap "3.3.6-0"]
                 [org.webjars/font-awesome "4.5.0"]
                 [org.webjars.bower/tether "1.1.1"]
                 [cljsjs/jquery "2.1.4-0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [com.taoensso/tower "3.0.2"]
                 [compojure "1.4.0"]
                 [ring-webjars "0.1.1"]
                 [ring/ring-defaults "0.1.5"]
                 [ring "1.4.0" :exclusions [ring/ring-jetty-adapter]]
                 [ring/ring-json "0.4.0"]
                 [ring-test "0.1.3"]
                 [mount "0.1.8"]
                 [luminus-nrepl "0.1.2"]
                 [buddy "0.10.0"]
                 [com.novemberain/monger "3.0.0-rc2"]
                 [org.clojure/clojurescript "1.7.228" :scope "provided"]
                 [reagent "0.5.1"]
                 [reagent-forms "0.5.13"]
                 [reagent-utils "0.1.7"]
                 [secretary "1.2.3"]
                 [org.clojure/core.async "0.2.374"]
                 [cljs-ajax "0.5.3"]
                 [org.webjars/webjars-locator-jboss-vfs "0.1.0"]
                 [luminus-immutant "0.1.0"]
                 [luminus-log4j "0.1.2"]
                 [hiccup "1.0.5"]
                 [camel-snake-kebab "0.3.2"]
                 [com.draines/postal "1.11.3"]
                 [midje "1.8.3"]
                 [reagent-forms "0.5.22"]
                 [gws/clj-mandrill "0.4.2"]
                 [clj-time "0.11.0"]
                 [inflections "0.12.1"]
                 [clj-recaptcha "0.0.2"]]

  :min-lein-version "2.0.0"
  :uberjar-name "misabogados-workflow.jar"
  :jvm-opts ["-server"]
  :target-path "target/%s/"
  :resource-paths ["resources" "target/cljsbuild"]

  :main misabogados-workflow.core

  :plugins [[lein-environ "1.0.1"]
            [lein-cljsbuild "1.1.1"]
            [lein-uberwar "0.1.0"]
            [lein-sass "0.3.0"]
            [lein-scss "0.2.0"]]

  :uberwar {:handler misabogados-workflow.handler/app
            :init misabogados-workflow.handler/init
            :destroy misabogados-workflow.handler/destroy
            :name "misabogados-workflow.war"}

  :sass {:src "resources/scss"
         :output-directory "resources/public/css"

         :source-maps true
         :style :nested
         }

  :scss {:builds
         {:develop    {:source-dir "resources/scss/"
                       :dest-dir   "resources/public/css/"
                       :executable "sassc"
                       :args       ["-t" "nested"]}
          :production {:source-dir "scss/"
                       :dest-dir   "resources/public/css/"
                       :executable "sassc"
                       :args       ["-I" "resources/scss/" "-t" "compressed"]}}}

  :clean-targets ^{:protect false} [:target-path [:cljsbuild :builds :app :compiler :output-dir] [:cljsbuild :builds :app :compiler :output-to]]
  :cljsbuild
  {:builds
   {:app
    {:source-paths ["src-cljs"]
     :compiler
     {:output-to "target/cljsbuild/public/js/app.js"
      :output-dir "target/cljsbuild/public/js/out"
      :externs ["react/externs/react.js"]
      :pretty-print true}}}}

  :figwheel {:reload-clj-files {:clj true
                                :cljc true}}

  :profiles
  {:uberjar {:omit-source true
             :env {:production true
                   :log-path "log/misabogados.log"
                   :database-url "mongodb://127.0.0.1/misabogados_workflow"}
             :prep-tasks ["compile" ["cljsbuild" "once"]]
             :cljsbuild
             {:builds
              {:app
               {:source-paths ["env/prod/cljs"]
                :compiler
                {:externs ["resoureces/public/js/externs/externs.js"]
                 :optimizations :advanced
                 :pretty-print false
                 :pseudo-names false
                 :closure-warnings
                 {:externs-validation :off :non-standard-jsdoc :off}}}}}

             :aot :all
             :source-paths ["env/prod/clj"]
             :resource-paths ["env/prod/resources"]}
   :production [:project/production :profiles/production]
   :dev           [:project/dev :profiles/dev]
   :test          [:project/test :profiles/test]
   :project/dev  {:dependencies [[prone "1.0.1"]
                                 [ring/ring-mock "0.3.0"]
                                 [ring/ring-devel "1.4.0"]
                                 [pjstadig/humane-test-output "0.7.1"]
                                 [lein-figwheel "0.5.0-6"]
                                 [com.cemerick/piggieback "0.2.2-SNAPSHOT"]
                                 [spyscope "0.1.5"]]
                  :plugins [[lein-figwheel "0.5.2"] [org.clojure/clojurescript "1.8.51"]]
                   :cljsbuild
                   {:builds
                    {:app
                     {:source-paths ["env/dev/cljs"]
                      :compiler
                      {:main "misabogados-workflow.app"
                       :asset-path "/js/out"
                       :optimizations :none
                       :source-map true}}}}

                  :figwheel
                  {:http-server-root "public"
                   :server-port 3449
                   :nrepl-port 7002
                   :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"]
                   :css-dirs ["resources/public/css"]
                   :ring-handler misabogados-workflow.handler/app}

                  :source-paths ["env/dev/clj"]
                  :resource-paths ["env/dev/resources"]
                  :repl-options {:init-ns user}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)
                               (require 'spyscope.core)]
                  ;;when :nrepl-port is set the application starts the nREPL server on load
                  :env {:dev        true
                        :country "cl"
                        :port       3000
                        :nrepl-port 7000
                        :payment-system "payu"
                        :currency "COP"
                        :log-path "log/misabogados.log"
                        :database-url "mongodb://127.0.0.1/misabogados_workflow_dev"
                        :settings-database-url "mongodb://127.0.0.1/misabogados_workflow_settings_dev"}
                  }
   :project/test {:env {:test       true
                        :port       3001
                        :nrepl-port 7001}}
   :project/production {:env {:production true
                              :log-path "log/misabogados.log"
                              :database-url "mongodb://127.0.0.1/misabogados_workflow"}}
   :profiles/production {}
   :profiles/dev {}
   :profiles/test {}})
