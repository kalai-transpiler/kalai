(ns clj-icu-test.emit.api
  (:require [clj-icu-test.emit.interface :as iface]
            [clj-icu-test.emit.impl.cpp :as cpp]
            [clj-icu-test.emit.impl.java :as java]))

(defn emit-const
  [ast-opts]
  (iface/emit-const ast-opts))

(defn emit-do
  [ast-opts]
  (iface/emit-do ast-opts))

(defn emit-atom
  [ast-opts]
  (iface/emit-atom ast-opts))

(defn emit-reset!
  [ast-opts]
  (iface/emit-reset! ast-opts))

(defn emit-assignment
  [ast-opts]
  (iface/emit-assignment ast-opts))

(defn emit-def
  [ast-opts]
  (iface/emit-def ast-opts))

(defn emit-binding
  [ast-opts]
  (iface/emit-binding ast-opts))

(defn emit-bindings-stanza
  [ast-opts]
  (iface/emit-bindings-stanza ast-opts))

(defn emit-let
  [ast-opts]
  (iface/emit-let ast-opts))

(defn emit-arg
  ([ast-opts]
   (iface/emit-arg ast-opts))
  ([ast-opts symb]
   (iface/emit-arg ast-opts symb)))

(defn emit-args
  [ast-opts]
  (iface/emit-args ast-opts))

(defn emit-static-call
  [ast-opts]
  (iface/emit-static-call ast-opts))

(defn emit-local
  [ast-opts]
  (iface/emit-local ast-opts))

(defn emit-var
  [ast-opts]
  (iface/emit-var ast-opts))

(defn emit-defn-arg
  [ast-opts]
  (iface/emit-defn-arg ast-opts))

(defn emit-defn-args
  [ast-opts]
  (iface/emit-defn-args ast-opts))

(defn emit-defn
  [ast-opts]
  (iface/emit-defn ast-opts))

(defn emit-defclass
  [ast-opts]
  (iface/emit-defclass ast-opts))

(defn emit-defenum
  [ast-opts]
  (iface/emit-defenum ast-opts))

(defn emit-return
  [ast-opts]
  (iface/emit-return ast-opts))

(defn emit-deref
  [ast-opts]
  (iface/emit-deref ast-opts))

(defn emit-not
  [ast-opts]
  (iface/emit-not ast-opts))

(defn emit-invoke-arg
  [ast-opts]
  (iface/emit-invoke-arg ast-opts))

(defn emit-invoke-args
  [ast-opts]
  (iface/emit-invoke-args ast-opts))

(defn emit-str-arg
  [ast-opts]
  (iface/emit-str-arg ast-opts))

(defn emit-str-args
  [ast-opts]
  (iface/emit-str-args ast-opts))

(defn emit-str
  [ast-opts]
  (iface/emit-str ast-opts))

(defn emit-println
  [ast-opts]
  (iface/emit-println ast-opts))

(defn emit-new-strbuf
  [ast-opts]
  (iface/emit-new-strbuf ast-opts))

(defn emit-prepend-strbuf
  [ast-opts]
  (iface/emit-prepend-strbuf ast-opts))

(defn emit-tostring-strbuf
  [ast-opts]
  (iface/emit-tostring-strbuf ast-opts))

(defn emit-invoke
  [ast-opts]
  (iface/emit-invoke ast-opts))

(defn emit-while
  [ast-opts]
  (iface/emit-while ast-opts))

(defn emit-loop
  [ast-opts]
  (iface/emit-loop ast-opts))

(defn emit-new
  [ast-opts]
  (iface/emit-new ast-opts))

(defn emit
  [ast-opts]
  (iface/emit ast-opts))

