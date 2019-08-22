(ns clj-icu-test.emit.api
  (:require [clj-icu-test.emit.interface :as iface]
            [clj-icu-test.emit.impl.cpp :as cpp]
            [clj-icu-test.emit.impl.java :as java]))

(defn emit-const
  [ast-opts]
  (iface/emit-const ast-opts))

