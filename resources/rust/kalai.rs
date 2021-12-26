use std::{any, any::Any};
use std::collections::hash_map::DefaultHasher;
use std::collections::{HashMap, BinaryHeap};
use std::collections::HashSet;
use std::convert::TryInto;
use std::{fmt, ops};
use std::fmt::{Debug};
use std::hash::{Hash, Hasher};
use std::vec::Vec;
use std::ops::{Deref, Add};
use std::borrow::Borrow;

/// Because we want to insert values that implement the Value trait (in order to
/// be added to collection types in an extensible way that is accessible to users),
/// we are effectively inserting Value trait objects into the collections. Since
/// Rust only allows collections to be defined with a fixed-size type, we cannot
/// have, for example, `Set<Value>`. Instead, we must use the `Box` type as the
/// type of the collection. (`Box` represents a pointer, which has a fixed size.
/// The pointer is to the location in the memory heap  that `Box` allocates for
/// the object / trait object.) Therefore, when dealing with collections and our
/// `Value` trait, we have to deal with the `BValue` representation of
/// our value. (And in fact, we must upcast(?) a concrete type like `Float` into
/// `Value` (ex: `let f: Box<dyn Value = Box::new(Float(3.14));`) before being
/// able to use that concrete-typed value).
pub type BValue = Box<dyn Value>;


// Trait objects provide dynamic dispatch in Rust. But they don't allow / need OOP inheritance
// for the dispatch.
//
// Dynamic dispatch is similar to other statically typed target languages (ex: Java) in how they
// represent heterogeneous collections (ex: Set<Object>). Dynamically typed languages implicitly
// do the same thing.
//
// So we want to explore switching from an enum, which cannot be extended once it is defined, into
// a trait, which can be extended on new types, similar to Clojure protocols. This would allow users
// to have their own types and have the ability to extend the trait on their own types thesmelves.

// Note: the following structs are wrapping primitives to allow us functionality
// (especially related to the `Value` trait) that the primitives don't allow
// us to have by default. These structs effectively implement the "New Type Idiom"
// https://doc.rust-lang.org/rust-by-example/generics/new_types.html in Rust.
// We need this for things like Float, Double, Set, Map, etc. in order to create
// custom implementations that allow them to be put into keys of sets and maps, etc.

#[derive(PartialEq, Hash, Debug, Clone)]
pub struct Nil(i32);

const NIL: Nil = Nil(0);

#[derive(PartialEq, Debug, Clone)]
pub struct Float(pub f32);

#[derive(PartialEq, Debug, Clone)]
pub struct Double(pub f64);

#[derive(Debug, Clone)]
pub struct Set(pub HashSet<BValue>);

#[derive(Debug, Clone)]
pub struct Map(pub HashMap<BValue,BValue>);

#[derive(Debug, Clone)]
pub struct Vector(pub Vec<BValue>);


// implementing Value trait based on SO answer at:
// https://stackoverflow.com/a/49779676

/// Implementing `hash_id` is necessary for the default Hash impl.
/// Implementing `eq_test` is necessary for the default PartialEq impl,
/// and therefore for the default Eq impl by extension.
///
/// `Set`s can contain `Set`s. This is because `Set` is a struct that
/// wraps `HashSet<BValue>`, and because `Set` implements the `Value`
/// trait. In other words, `Set` is allowed to be recursive.
///
/// For `Set`s and `Map`s to be recursive, they must implement the `Hash` trait
/// in order to be able to be put inside other `Set`s and `Map`s. Therefore,
/// the implementation of `hash_id` for those structs is required.
///
/// Note that `HashSet` does not implement the `Hash` trait, and that it is important
/// when implementing the `Hash` trait for a `Set`/`HashSet` that the hash value be
/// order-independent (regardless of the order of insertion or storage / storage iterator).
///
///
/// ```
/// use std::collections::HashSet;
/// use sql_builder::kalai::{Set, Float, Value, BValue};
///
/// let mut set = HashSet::<f32>::new();
/// let f = 3.14;
/// // &set.insert(f); // not allowed
///
/// let mut s = Set::default();
/// s.insert_f32(3.14);
///
/// // assert!(s.contains_f32(3.14)); // autogenerated by Kalai
///
/// let f_box_2 = BValue::from(6.28);  // use this for .contains() querying
/// assert!(!s.contains(&f_box_2));
///
/// let f_box_3 = BValue::from(6.28);  // put this into set
/// s.insert(f_box_3);
/// assert!(s.contains(&f_box_2));
/// ```
pub trait Value: Debug + CloneValue {
    fn type_name(&self) -> &'static str;

    fn hash_id(&self) -> u64;
    fn as_any(&self) -> &dyn Any;
    fn eq_test(&self, other: &dyn Value) -> bool;

    fn is_type(&self, type_str: &str) -> bool {
        type_str == self.type_name()
    }
}


impl Hash for dyn Value {
    fn hash<H>(&self, state: &mut H) where H: Hasher {
        self.hash_id().hash(state)
    }
}

impl PartialEq for dyn Value {
    fn eq(&self, other: &Self) -> bool {
        self.eq_test(other)
    }
}

impl Eq for dyn Value {}


// TODO: we should investigate whether it is better to implement Copy instead
// of Clone, and which to be more choosy in implementing for the Value trait

// TODO: See if we can do something similar for replacing hash_id() with a helper
// trait like here, and move our existing implementations for types that don't
// have a hashing function to custom impls of Hash, but allow ourselves to not have to
// re-implement existing default impls of Hash

pub trait CloneValue {
    fn clone_value<'a>(&self) -> Box<dyn Value>;
}

impl<T> CloneValue for T
    where
        T: Value + Clone + 'static,
{
    fn clone_value(&self) -> Box<dyn Value> {
        Box::new(self.clone())
    }
}

impl Clone for Box<dyn Value> {
    fn clone(&self) -> Self {
        self.clone_value()
    }
}

//
// implementing Value for our custom-defined type wrapper structs
//

impl Value for Double {
    fn type_name(&self) -> &'static str {
        "Double"
    }

    fn hash_id(&self) -> u64
    {
        self.0.to_bits()
    }

    fn as_any(&self) -> &dyn Any {
        self
    }

    fn eq_test(&self, other: &dyn Value) -> bool {
        match other.as_any().downcast_ref::<Double>() {
            Some(dbl) => {
                &self.0 == &dbl.0
            }
            None => false,
        }
    }
}

impl Value for Float {
    fn type_name(&self) -> &'static str {
        "Float"
    }

    fn hash_id(&self) -> u64
    {
        self.0.to_bits() as u64
    }

    fn as_any(&self) -> &dyn Any {
        self
    }

    fn eq_test(&self, other: &dyn Value) -> bool {
        match other.as_any().downcast_ref::<Float>() {
            Some(dbl) => {
                &self.0 == &dbl.0
            }
            None => false,
        }
    }
}

impl Value for Set {
    fn type_name(&self) -> &'static str {
        "Set"
    }

    fn hash_id(&self) -> u64
    {
        // TODO: find a more efficient way to create a deterministic contents/value-based hash for a Set (or any collection)
        // TODO: look into how Clojure hashes collections (ex: map, set)
        // Note: we use BinaryHeap to order the hash values because hashing is stateful, and therefore, order-dependent.
        let elem_hashes: BinaryHeap<u64> = self.0.iter().map(|e| e.deref().hash_id()).collect();
        let sorted_hashes: Vec<u64> = elem_hashes.into_sorted_vec();

        let mut hasher = DefaultHasher::new();
        for eh in sorted_hashes.iter() {
            eh.hash(&mut hasher);
        }
        let result = hasher.finish();
        result
    }

    fn as_any(&self) -> &dyn Any {
        self
    }

    fn eq_test(&self, other: &dyn Value) -> bool {
        match other.as_any().downcast_ref::<Set>() {
            Some(set) => {
                &self.0 == &set.0
            }
            None => false,
        }
    }
}

impl<T> Value for Vec<T>
where T: PartialEq + Value + Clone + 'static {
    fn type_name(&self) -> &'static str {
    // TODO: think about this...
    // This isn't as important as it sounds... it's just to check if `vector?` should return true
        "Vector"
    }

    fn hash_id(&self) -> u64
    {
        // TODO: find a more efficient way to create a deterministic contents/value-based hash for a Set (or any collection)
        // TODO: look into how Clojure hashes collections (ex: map, set)
        // Note: we use BinaryHeap to order the hash values because hashing is stateful, and therefore, order-dependent.
        let elem_hashes: BinaryHeap<u64> = self.iter().map(|e| e.deref().hash_id()).collect();
        let sorted_hashes: Vec<u64> = elem_hashes.into_sorted_vec();

        let mut hasher = DefaultHasher::new();
        for eh in sorted_hashes.iter() {
            eh.hash(&mut hasher);
        }
        let result = hasher.finish();
        result
    }

    fn as_any(&self) -> &dyn Any {
        self
    }

    fn eq_test(&self, other: &dyn Value) -> bool {
        match other.as_any().downcast_ref::<Vec<T>>() {
            Some(vector) => {
                *self == *vector
            }
            None => false,
        }
    }
}


impl Value for Vector {
    fn type_name(&self) -> &'static str {
        "Vector"
    }

    fn hash_id(&self) -> u64
    {
        // TODO: find a more efficient way to create a deterministic contents/value-based hash for a Set (or any collection)
        // TODO: look into how Clojure hashes collections (ex: map, set)
        // Note: we use BinaryHeap to order the hash values because hashing is stateful, and therefore, order-dependent.
        let elem_hashes: BinaryHeap<u64> = self.0.iter().map(|e| e.deref().hash_id()).collect();
        let sorted_hashes: Vec<u64> = elem_hashes.into_sorted_vec();

        let mut hasher = DefaultHasher::new();
        for eh in sorted_hashes.iter() {
            eh.hash(&mut hasher);
        }
        let result = hasher.finish();
        result
    }

    fn as_any(&self) -> &dyn Any {
        self
    }

    fn eq_test(&self, other: &dyn Value) -> bool {
        match other.as_any().downcast_ref::<Vector>() {
            Some(vector) => {
                &self.0 == &vector.0
            }
            None => false,
        }
    }
}


impl Value for bool {
    fn type_name(&self) -> &'static str {
        "bool"
    }

    fn hash_id(&self) -> u64
    {
        *self as u64
    }

    fn as_any(&self) -> &dyn Any {
        self
    }

    fn eq_test(&self, other: &dyn Value) -> bool {
        match other.as_any().downcast_ref::<bool>() {
            Some(x) => {
                &self == &x
            }
            None => false,
        }
    }
}

impl Value for i8 {
    fn type_name(&self) -> &'static str {
        "i8"
    }

    fn hash_id(&self) -> u64
    {
        *self as u64
    }

    fn as_any(&self) -> &dyn Any {
        self
    }

    fn eq_test(&self, other: &dyn Value) -> bool {
        match other.as_any().downcast_ref::<i8>() {
            Some(x) => {
                &self == &x
            }
            None => false,
        }
    }
}

impl Value for char {
    fn type_name(&self) -> &'static str {
        "char"
    }

    fn hash_id(&self) -> u64
    {
        *self as u64
    }

    fn as_any(&self) -> &dyn Any {
        self
    }

    fn eq_test(&self, other: &dyn Value) -> bool {
        match other.as_any().downcast_ref::<char>() {
            Some(x) => {
                &self == &x
            }
            None => false,
        }
    }
}

impl Value for i32 {
    fn type_name(&self) -> &'static str {
        "i32"
    }

    fn hash_id(&self) -> u64
    {
        *self as u64
    }

    fn as_any(&self) -> &dyn Any {
        self
    }

    fn eq_test(&self, other: &dyn Value) -> bool {
        match other.as_any().downcast_ref::<i32>() {
            Some(x) => {
                &self == &x
            }
            None => false,
        }
    }
}


impl Value for i64 {
    fn type_name(&self) -> &'static str {
        "i64"
    }

    fn hash_id(&self) -> u64
    {
        *self as u64
    }

    fn as_any(&self) -> &dyn Any {
        self
    }

    fn eq_test(&self, other: &dyn Value) -> bool {
        match other.as_any().downcast_ref::<i64>() {
            Some(x) => {
                &self == &x
            }
            None => false,
        }
    }
}

impl Value for Nil {
    fn type_name(&self) -> &'static str {
        "Nil"
    }

    fn hash_id(&self) -> u64 {
        0
    }

    fn as_any(&self) -> &dyn Any {
        self
    }

    fn eq_test(&self, other: &dyn Value) -> bool {
        // should be the same as: as_any().downcast_ref::<Nil>().is_some()
        other.is_type("Nil")
    }
}

// impl Value for HashSet<i32> {
//     const TYPE_NAME: &'static str = "HashSet<i32>";
//
//     fn hash_id(&self) -> u64 {
//         let elem_hashes: BinaryHeap<u64> = self.iter().map(|e| (*e) as u64).collect();
//         let sorted_hashes: Vec<u64> = elem_hashes.into_sorted_vec();
//
//         let mut hasher = DefaultHasher::new();
//         for eh in sorted_hashes.iter() {
//             eh.hash(&mut hasher);
//         }
//         let result = hasher.finish();
//         result
//     }
//
//     fn as_any(&self) -> &dyn Any {
//         self
//     }
//
//     fn eq_test(&self, other: &dyn Value) -> bool {
//         match other.as_any().downcast_ref::<HashSet<i32>>() {
//             Some(set) => {
//                 self == set
//             }
//             None => false,
//         }
//     }
// }

// impl Value for HashSet<BValue> {
//     const TYPE_NAME: &'static str = "HashSet<BValue>";
//
//     fn hash_id(&self) -> u64 {
//         let elem_hashes: BinaryHeap<u64> = self.iter().map(|e| e.hash_id()).collect();
//         let sorted_hashes: Vec<u64> = elem_hashes.into_sorted_vec();
//
//         let mut hasher = DefaultHasher::new();
//         for eh in sorted_hashes.iter() {
//             eh.hash(&mut hasher);
//         }
//         let result = hasher.finish();
//         result
//     }
//
//     fn as_any(&self) -> &dyn Any {
//         self
//     }
//
//     fn eq_test(&self, other: &dyn Value) -> bool {
//         match other.as_any().downcast_ref::<HashSet<BValue>>() {
//             Some(set) => {
//                 self == set
//             }
//             None => false,
//         }
//     }
// }

// impl Value for HashMap<String, f32> {
//     const TYPE_NAME: &'static str = "HashMap<String, f32>";
//
//     fn hash_id(&self) -> u64 {
//         let elem_hashes: BinaryHeap<u64> =
//             self.iter()
//                 .flat_map(
//                     |(k,v)|
//                         [(*k).hash_id(), Float(*v).hash_id()]
//                             .iter()
//                             .cloned()
//                             .collect::<Vec<u64>>())
//                 .collect();
//         let sorted_hashes: Vec<u64> = elem_hashes.into_sorted_vec();
//
//         let mut hasher = DefaultHasher::new();
//         for eh in sorted_hashes.iter() {
//             eh.hash(&mut hasher);
//         }
//         let result = hasher.finish();
//         result
//     }
//
//     fn as_any(&self) -> &dyn Any {
//         self
//     }
//
//     fn eq_test(&self, other: &dyn Value) -> bool {
//         match other.as_any().downcast_ref::< HashMap<String, f32> >() {
//             Some(set) => {
//                 self == set
//             }
//             None => false,
//         }
//     }
// }

impl Value for Map {
    fn type_name(&self) -> &'static str {
        "Map"
    }

    fn hash_id(&self) -> u64
    {
        // TODO: find a more efficient way to create a deterministic contents/value-based hash for a Set (or any collection)
        // TODO: look into how Clojure hashes collections (ex: map, set)
        // Note: we use BinaryHeap to order the hash values because hashing is stateful, and therefore, order-dependent.
        let elem_hashes: BinaryHeap<u64> =
            self.0.iter()
                .flat_map(
                    |(k,v)|
                        [k.deref().hash_id(), v.deref().hash_id()]
                            .iter()
                            .cloned()
                            .collect::<Vec<u64>>())
                .collect();
        let sorted_hashes: Vec<u64> = elem_hashes.into_sorted_vec();

        let mut hasher = DefaultHasher::new();
        for eh in sorted_hashes.iter() {
            eh.hash(&mut hasher);
        }
        let result = hasher.finish();
        result
    }

    fn as_any(&self) -> &dyn Any {
        self
    }

    fn eq_test(&self, other: &dyn Value) -> bool {
        match other.as_any().downcast_ref::<Map>() {
            Some(map) => {
                &self.0 == &map.0
            }
            None => false,
        }
    }
}

impl Value for String {
    fn type_name(&self) -> &'static str {
        "String"
    }

    fn hash_id(&self) -> u64
    {
        let mut hasher = DefaultHasher::new();
        self.to_string().hash(&mut hasher);
        hasher.finish()
    }

    fn as_any(&self) -> &dyn Any {
        self
    }

    fn eq_test(&self, other: &dyn Value) -> bool {
        match other.as_any().downcast_ref::<String>() {
            Some(s) => {
                self == s
            }
            None => false,
        }
    }
}


//
// BValue to/from impls
//

// f32

impl From<f32> for BValue {
    fn from(x: f32) -> Self {
        let b: BValue = Box::new(Float(x));
        b
    }
}

impl From<BValue> for f32 {
    fn from(v: BValue) -> f32 {
        if let Some(float) = v.as_any().downcast_ref::<Float>() {
            float.deref().0
        } else {
            panic!("Could not downcast Value into Float!");
        }
    }
}


impl From<&BValue> for f32 {
    fn from(v: &BValue) -> f32 {
        if let Some(float) = v.as_any().downcast_ref::<Float>() {
            float.deref().0
        } else {
            panic!("Could not downcast Value into Float!");
        }
    }
}

// f64

impl From<f64> for BValue {
    fn from(x: f64) -> Self {
        let b: BValue = Box::new(Double(x));
        b
    }
}

impl From<BValue> for f64 {
    fn from(v: BValue) -> f64 {
        if let Some(double) = v.as_any().downcast_ref::<Double>() {
            double.deref().0
        } else {
            panic!("Could not downcast Value into Double!");
        }
    }
}

impl From<&BValue> for f64 {
    fn from(v: &BValue) -> f64 {
        if let Some(double) = v.as_any().downcast_ref::<Double>() {
            double.deref().0
        } else {
            panic!("Could not downcast Value into Double!");
        }
    }
}

// String

impl From<String> for BValue {
    fn from(x: String) -> Self {
        let b: BValue = Box::new(x);
        b
    }
}


impl From<BValue> for String {
    fn from(v: BValue) -> String {
        if let Some(s) = v.as_any().downcast_ref::<String>() {
            s.deref().to_string()
        } else {
            panic!("Could not downcast Value into String!");
        }
    }
}

impl From<&BValue> for String {
    fn from(v: &BValue) -> String {
        if let Some(s) = v.as_any().downcast_ref::<String>() {
            s.deref().to_string()
        } else {
            panic!("Could not downcast Value into String!");
        }
    }
}

// bool

impl From<bool> for BValue {
    fn from(x: bool) -> Self {
        let b: BValue = Box::new(x);
        b
    }
}


impl From<BValue> for bool {
    fn from(v: BValue) -> bool {
        if let Some(b) = v.as_any().downcast_ref::<bool>() {
            *b.deref()
        } else {
            panic!("Could not downcast Value into bool!");
        }
    }
}

impl From<&BValue> for bool {
    fn from(v: &BValue) -> bool {
        if let Some(b) = v.as_any().downcast_ref::<bool>() {
            *b.deref()
        } else {
            panic!("Could not downcast Value into bool!");
        }
    }
}

// i8

impl From<i8> for BValue {
    fn from(x: i8) -> Self {
        let b: BValue = Box::new(x);
        b
    }
}


impl From<BValue> for i8 {
    fn from(v: BValue) -> i8 {
        if let Some(x) = v.as_any().downcast_ref::<i8>() {
            *x.deref()
        } else {
            panic!("Could not downcast Value into i8!");
        }
    }
}

impl From<&BValue> for i8 {
    fn from(v: &BValue) -> i8 {
        if let Some(x) = v.as_any().downcast_ref::<i8>() {
            *x.deref()
        } else {
            panic!("Could not downcast Value into i8!");
        }
    }
}

// char

impl From<char> for BValue {
    fn from(x: char) -> Self {
        let b: BValue = Box::new(x);
        b
    }
}


impl From<BValue> for char {
    fn from(v: BValue) -> char {
        if let Some(x) = v.as_any().downcast_ref::<char>() {
            *x.deref()
        } else {
            panic!("Could not downcast Value into char!");
        }
    }
}

impl From<&BValue> for char {
    fn from(v: &BValue) -> char {
        if let Some(x) = v.as_any().downcast_ref::<char>() {
            *x.deref()
        } else {
            panic!("Could not downcast Value into char!");
        }
    }
}

// i32

impl From<i32> for BValue {
    fn from(x: i32) -> Self {
        let b: BValue = Box::new(x);
        b
    }
}


impl From<BValue> for i32 {
    fn from(v: BValue) -> i32 {
        if let Some(x) = v.as_any().downcast_ref::<i32>() {
            *x.deref()
        } else {
            panic!("Could not downcast Value into i32!");
        }
    }
}

impl From<&BValue> for i32 {
    fn from(v: &BValue) -> i32 {
        if let Some(x) = v.as_any().downcast_ref::<i32>() {
            *x.deref()
        } else {
            panic!("Could not downcast Value into i32!");
        }
    }
}

// i64

impl From<i64> for BValue {
    fn from(x: i64) -> Self {
        let b: BValue = Box::new(x);
        b
    }
}


impl From<BValue> for i64 {
    fn from(v: BValue) -> i64 {
        if let Some(x) = v.as_any().downcast_ref::<i64>() {
            *x.deref()
        } else {
            panic!("Could not downcast Value into i64!");
        }
    }
}

impl From<&BValue> for i64 {
    fn from(v: &BValue) -> i64 {
        if let Some(x) = v.as_any().downcast_ref::<i64>() {
            *x.deref()
        } else {
            panic!("Could not downcast Value into i64!");
        }
    }
}

// Nil

impl From<Nil> for BValue {
    fn from(x: Nil) -> Self {
        let b: BValue = Box::new(x);
        b
    }
}


impl From<BValue> for Nil {
    fn from(v: BValue) -> Nil {
        if let Some(x) = v.as_any().downcast_ref::<Nil>() {
            NIL
        } else {
            panic!("Could not downcast Value into Nil!");
        }
    }
}

impl From<&BValue> for Nil {
    fn from(v: &BValue) -> Nil {
        if let Some(x) = v.as_any().downcast_ref::<Nil>() {
            NIL
        } else {
            panic!("Could not downcast Value into Nil!");
        }
    }
}

// // HashSet<i32>
//
// impl From<HashSet<i32>> for BValue {
//     fn from(x: HashSet<i32>) -> Self {
//         let b: BValue = Box::new(x);
//         b
//     }
// }
//
// impl From<BValue> for HashSet<i32> {
//     fn from(v: BValue) -> HashSet<i32> {
//         if let Some(set) = v.as_any().downcast_ref::<HashSet<i32>>() {
//             set.clone()
//         } else {
//             panic!("Could not downcast Value into HashSet<i32>!");
//         }
//     }
// }
//
// // HashMap<String,f32>
//
// impl From<BValue> for HashMap<String, f32> {
//     fn from(v: BValue) -> HashMap<String, f32> {
//         if let Some(set) = v.as_any().downcast_ref::<HashMap<String, f32>>() {
//             set.clone()
//         } else {
//             panic!("Could not downcast Value into HashMap<String, f32>!");
//         }
//     }
// }


// impl From<HashSet<BValue>> for BValue {
//     fn from(x: HashSet<BValue>) -> Self {
//         let b: BValue = Box::new(Set(x));
//         b
//     }
// }
//
// impl From<BValue> for HashSet<BValue> {
//     fn from(v: BValue) -> HashSet<BValue> {
//         if let Some(set) = v.as_any().downcast_ref::<HashSet<BValue>>() {
//             let temp_set: HashSet<BValue> = *set.clone();
//             temp_set
//         } else {
//             panic!("Could not downcast Value into HashSet<BValue>!");
//         }
//     }
// }


impl From<BValue> for Map {
    fn from(v: BValue) -> Map {
        if let Some(map) = v.as_any().downcast_ref::<Map>() {
            map.clone()
        } else {
            panic!("Could not downcast Value into Map!");
        }
    }
}

impl From<BValue> for Set {
    fn from(v: BValue) -> Set {
        if let Some(set) = v.as_any().downcast_ref::<Set>() {
            set.clone()
        } else {
            panic!("Could not downcast Value into Set!");
        }
    }
}

impl From<BValue> for Vector {
    fn from(v: BValue) -> Vector {
        if let Some(vector) = v.as_any().downcast_ref::<Vector>() {
            vector.clone()
        } else {
            panic!("Could not downcast Value into Vector!");
        }
    }
}

impl From<Map> for BValue {
    fn from(m: Map) -> BValue {
        if let Some(value) = m.as_any().downcast_ref::<BValue>() {
            value.clone()
        } else {
            panic!("Could not downcast Map into BValue!");
        }
    }
}

impl From<Set> for BValue {
    fn from(s: Set) -> BValue {
        if let Some(value) = s.as_any().downcast_ref::<BValue>() {
            value.clone()
        } else {
            panic!("Could not downcast Set into BValue!");
        }
    }
}

impl From<Vector> for BValue {
    fn from(v: Vector) -> BValue {
        if let Some(value) = v.as_any().downcast_ref::<BValue>() {
            value.clone()
        } else {
            panic!("Could not downcast Vector into BValue!");
        }
    }
}



//
// Float impls
//

impl ops::Add<Float> for Float {
    type Output = Float;

    fn add(self, rhs: Float) -> Self::Output {
        Float(self.0 + rhs.0)
    }
}
// ^ Here, we have Add(Float,Float).
// Option 1: Now we need {Add, Subtract, Multiply, Divide, Modulo?}({Numerical}x{Numerical})
// for "Numerical"={u8, i32, i64, f32, f64, Float, Double}. => 4x7x7 = ~200 impl blocks, which could
// perhaps be reduced using Rust macros
// Pros: Apply operators on Double/Float/Rust primitive num types like before
//   Floating point type results are also Values
// Cons: Users will have to know that Double/Float are not primitives and access the primitives with
// .0
// Option 2: Get the .insert/.get/.contains/.remove operations on Set(HashSet<T>),
// Map(HashMap<K,V>), Vec<T> to convert to and from f32/f64 <-> Float/Double at those "boundaries"
// Pros: Users would not need to deal with Float/Double directly
// Cons: This may not actually work
// When calling .get and the return is a Value, we need to be able to know the concrete type is
// Double/Float and do the conversion back to f64/f32



//
// Set impls
//

impl Default for Set {
    fn default() -> Set {
        Set(
            HashSet::<BValue>::new()
        )
    }
}

// We are auto-generating this in Kalai now. We must also comment out
// associated unit tests.
//
// impl Set {
//     pub fn contains_f32(&self, x: f32) -> bool {
//         self.contains(&BValue::from(x))
//     }
//
//     pub fn insert_f32(&mut self, x: f32) -> bool {
//         self.insert(BValue::from(x))
//     }
// }

impl Set {
    pub fn contains(&self, x: &BValue) -> bool {
        self.0.contains(x)
    }

    pub fn insert(&mut self, x: BValue) -> bool {
        self.0.insert(x)
    }

    pub fn new() -> Self {
        Self::default()
    }
}

//
// Vector impls
//

impl Default for Vector {
    fn default() -> Vector {
        Vector(
            Vec::<BValue>::new()
        )
    }
}

impl Vector {
    pub fn contains(&self, x: &BValue) -> bool {
        self.0.contains(x)
    }

    pub fn push(&mut self, x: BValue) -> () {
        self.0.push(x)
    }

    pub fn insert(&mut self, idx: usize, x: BValue) -> () {
        self.0.insert(idx, x)
    }

    pub fn new() -> Self {
        Self::default()
    }
}

//
// Map impls
//


impl Default for Map {
    fn default() -> Map {
        Map(
            HashMap::<BValue, BValue>::new()
        )
    }
}

// impl Map {
//     pub fn get_string_f32(&self, k: String) -> Option<f32> {
//         let v = self.get(&BValue::from(k));
//         v.map(|bval| f32::from(bval))
//     }
//
//     pub fn insert_string_f32(&mut self, k: String, v: f32) -> Option<BValue> {
//         self.insert(BValue::from(k), BValue::from(v))
//     }
// }

impl Map {
    pub fn insert(&mut self, k: BValue, v: BValue) -> Option<BValue> {
        self.0.insert(k, v)
    }

    pub fn get(&self, k: &BValue) -> Option<&BValue> {
        self.0.get(k)
    }

    pub fn new() -> Map {
        Self::default()
    }
}


#[cfg(test)]
mod tests {
    use super::{Nil, Float, Double};

    use super::{Set, Map};
    use super::Value;

    use std::collections::{HashMap, HashSet};
    use std::collections::hash_map::DefaultHasher;
    use std::hash::{Hash, Hasher};
    use crate::kalai::BValue;
    use std::ops::Deref;

    #[test]
    fn debug_print() {
        let v = vec![1, 2, 3];
        println!("{:?}", v);
    }

    #[test]
    fn nil_hash() {
        let nil1 = Nil {};
        let nil2 = Nil {};
        assert_eq!(nil1, nil2);


        let mut hasher = DefaultHasher::new();
        nil1.hash(&mut hasher);
        let nil1_hash = hasher.finish();
        println!("nil1 hash = {}", nil1_hash);

        let mut hasher = DefaultHasher::new();
        nil2.hash(&mut hasher);
        let nil2_hash = hasher.finish();
        println!("nil2 hash = {}", nil2_hash);

        assert_eq!(nil1_hash, nil2_hash);

    }

    #[test]
    fn test_float_structs() {
        let f = Float(3.14);
        println!("{:?}", f);

        let d = Double(1.414);

        // let v: &dyn Value = &d;

        // TODO: make this work
        // let mut set: HashSet<Box<dyn Value>> = HashSet::new();
        // &set.insert(Box::new(f));
        // &set.insert(Box::new(d));
        //
        // println!("size of HashSet<Box<dyn Value>> is {:?}", set.len());
        // assert_eq!(2, set.len());
    }

    #[test]
    fn test_value_trait() {
        let f = Float(3.14);
        let d = Double(1.414);
        let mut my_map = HashMap::<BValue, i32>::new();
        my_map.insert(Box::new(f), 1);
        my_map.insert(Box::new(d), 2);

        println!("{:?}", my_map.get(&(Box::new(Float(6.28)) as BValue)));
        println!("{:?}", my_map.get(&(Box::new(Float(9.42)) as BValue)));
        println!("{:?}", my_map.get(&(Box::new(Double(2.828)) as BValue)));
        println!("{:?}", my_map.get(&(Box::new(Double(5.656)) as BValue)));

        println!("size of HashMap<Box<Value,i32>> is {:?}", my_map.len());
        assert_eq!(2, my_map.len());
    }

    #[test]
    fn test_float_structs_with_value_trait() {
        let f = Float(3.14);
        println!("{:?}", f);

        let d = Double(1.414);

        let v: &dyn Value = &d;

        let mut set: HashSet<BValue> = HashSet::new();
        &set.insert(Box::new(f));
        &set.insert(Box::new(d));

        println!("size of HashSet<BValue> is {:?}", set.len());
        assert_eq!(2, set.len());

        let mut inner_set2: HashSet<BValue> = HashSet::new();
        &inner_set2.insert(Box::new(Double(98.6)));
        &inner_set2.insert(Box::new(Float(37.0)));
        let set2: Set = Set(inner_set2);

        println!("size of Set (unnamed arg of type HashSet<BValue>) is {:?}", set2.0.len());
        assert_eq!(2, set2.0.len());

        // test PartialEq for Float struct
        assert_eq!(Box::new(Float(3.456)), Box::new(Float(3.456)));
        let f1: &dyn Value = &Float(3.456);
        let f2: &dyn Value = &Float(3.456);
        assert!(f1 == f2);
    }

    #[test]
    fn test_set_struct_with_value_trait() {
        let mut set12: HashSet<BValue> = HashSet::new();
        &set12.insert(Box::new(Float(1.0)));
        &set12.insert(Box::new(Double(2.0)));

        let mut set34: HashSet<BValue> = HashSet::new();
        &set34.insert(Box::new(Float(3.0)));
        &set34.insert(Box::new(Double(4.0)));

        let mut set_12_34: HashSet<BValue> = HashSet::new();
        println!("inserting set12 into set_12_34");
        &set_12_34.insert(Box::new(Set(set12)));
        println!("inserting set34 into set_12_34");
        &set_12_34.insert(Box::new(Set(set34)));

        let mut set12b: HashSet<BValue> = HashSet::new();
        &set12b.insert(Box::new(Float(1.0)));
        &set12b.insert(Box::new(Double(2.0)));

        let mut set34b: HashSet<BValue> = HashSet::new();
        &set34b.insert(Box::new(Float(3.0)));
        &set34b.insert(Box::new(Double(4.0)));

        let mut set_34_12: HashSet<BValue> = HashSet::new();
        println!("inserting set34 into set_34_12");
        &set_34_12.insert(Box::new(Set(set34b)));
        println!("inserting set12 into set_34_12");
        &set_34_12.insert(Box::new(Set(set12b)));

        let sv12_34: &dyn Value = &Set(set_12_34); // #{#{1 2} #{3 4}}
        let sv34_12: &dyn Value = &Set(set_34_12); // #{#{3 4} #{1 2}}
        println!("comparing equality of Set(set_12_34) == Set(set_34_12)");
        assert!(sv12_34 == sv34_12);
        // assert!(Box::new(Set(set_1_2)) == Box::new(Set(set_2_1)));
    }

    #[test]
    fn test_set_eq() {
        let mut set1 = HashSet::new();
        &set1.insert(2);
        &set1.insert(3);
        &set1.insert(5);

        let mut set2 = HashSet::new();
        &set2.insert(3);
        &set2.insert(5);
        &set2.insert(2);

        assert_eq!(set1, set2);
    }

    #[test]
    fn test_float_val_eq() {
        let f1 = Float(3.0);
        let f2 = Float(3.0);

        assert_eq!(f1, f2);
    }

    // This test proves that the Hash impl for Box<T> delegates to the Hash impl for T, and that
    // PartialEq on Box<T> is also delegating to PartialEq on T.
    //
    // Remember: we do this because Rust does not implement Eq on floating-points (f32, f64), which
    // means that you cannot have HashSet<f32>, or for that matter, any Hash{Set,Map}<f{32,64}>.
    #[test]
    fn test_set_box_val_eq() {
        let mut set1 = HashSet::new();
        &set1.insert(Box::new(2));
        &set1.insert(Box::new(3));
        &set1.insert(Box::new(5));

        let mut set2 = HashSet::new();
        &set2.insert(Box::new(3));
        &set2.insert(Box::new(5));
        &set2.insert(Box::new(2));

        assert_eq!(set1, set2);
    }

    #[test]
    fn test_float_op_add_overload() {
        let f1 = Float(1.0);
        let f2 = Float(2.0);

        assert_eq!(f1+f2, Float(3.0));
    }

    #[test]
    fn test_value_to_concrete_struct() {
        let v = BValue::from(3.14);
        assert_eq!(f64::from(v), 3.14);

        let v = BValue::from(3.14f32);
        assert_eq!(f32::from(v), 3.14);
    }

    #[test]
    fn test_map_get() {
        let mut map1 = HashMap::new();
        &map1.insert(BValue::from("two".to_string()), BValue::from(2.0f32));
        &map1.insert(BValue::from("three".to_string()), BValue::from(3.0f32));
        &map1.insert(BValue::from("five".to_string()), BValue::from(5.0f32));

        let lookup_key = BValue::from("two".to_string());
        let deux: Option<&BValue> = map1.get(&lookup_key);

        if let Some(bval) = deux {
            assert_eq!(bval, &BValue::from(2.0f32)); // get back a BValue == "kalai type :any"
            assert_eq!(2.0, f32::from(bval));
        } else {
            assert!(false, "value returned by map is None!")
        }

        let mut m1 = Map(map1);

        &m1.insert(BValue::from("eight".to_string()), BValue::from(8.0f32));

        let lookup_key = BValue::from("eight".to_string());
        let ocho: Option<&BValue> = m1.get(&lookup_key);

        if let Some(bval) = ocho {
            assert_eq!(bval, &BValue::from(8.0f32)); // get back a BValue == "kalai type :any"
            assert_eq!(8.0, f32::from(bval));
        } else {
            assert!(false, "value returned by map is None!")
        }

        let eight_f32 = &m1.get_string_f32("eight".to_string());
        assert_eq!(eight_f32, &Some(8.0f32));
        let ocho_f32 = &m1.get_string_f32("ocho".to_string());
        assert_eq!(ocho_f32, &None);
    }

    #[test]
    fn test_map_of_map_get() {
        let mut map1 = HashMap::new();
        &map1.insert(BValue::from("two".to_string()), BValue::from(2.0f32));

        let mut m1 = Map(map1);

        let mut map_parent: HashMap<BValue, BValue> = HashMap::new();
        &map_parent.insert(BValue::from("map1".to_string()), Box::new(m1));

        let mut m_parent = Map(map_parent);

        let lookup_key = BValue::from("map1".to_string());
        let map_child_bval = m_parent.get(&lookup_key).unwrap();

        let map_child_deref: BValue = map_child_bval.clone();

        let map_child = Map::from(map_child_deref);
        assert_eq!(map_child.get_string_f32("two".to_string()), Some(2.0f32));
    }
}
