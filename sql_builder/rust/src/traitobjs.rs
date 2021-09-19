use std::collections::hash_map::DefaultHasher;
use std::collections::HashMap;
use std::collections::HashSet;
use std::convert::TryInto;
use std::fmt;
use std::fmt::{Debug};
use std::hash::{Hash, Hasher};
use std::vec::Vec;

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

// `Animal` is a trait. Is an example of how we might be able to migrate the `Value` enum to be
// a trait that can be used for trait objects in collections.

trait Animal {
    fn talk(&self);

    fn hash(&self, state: &mut std::collections::hash_map::DefaultHasher);
}

// Structs don't need fields, per se. (And traits don't need methods, per se, ex: Rust's Eq).

struct Cat {}

struct Dog {}

impl Animal for Cat {
    fn talk(&self) {
        println!("meow");
    }

    // We need to implement std::hash::Hash for Animal because Values can be the key for
    // HashSet and HashMap. In order to allow users to maintain the ability to extend the trait
    // for their own types, they need to define how the hashing works on their type (struct). This
    // is also necessary because trait implementations for a struct must exist in the same module as
    // the struct definition itself.
    fn hash(&self, _state: &mut std::collections::hash_map::DefaultHasher) {
        let mut hasher = std::collections::hash_map::DefaultHasher::new();
        hasher.write(b"cat");
        hasher.finish();
    }
}

impl Animal for Dog {
    fn talk(&self) {
        println!("bark");
    }
    fn hash(&self, _state: &mut std::collections::hash_map::DefaultHasher) {
        let mut hasher = std::collections::hash_map::DefaultHasher::new();
        hasher.write(b"cat");
        hasher.finish();
    }
}

// for debugging / testing purposes only
impl Debug for dyn Animal {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "dyn Animal: {}", "I'm talking!")
    }
}

impl Eq for dyn Animal {}

impl PartialEq for dyn Animal {
    fn eq(&self, other: &Self) -> bool {
        true
    }
}

// as stated above, needed for when Values are used in sets and in the keys of maps.
impl std::hash::Hash for dyn Animal {
    fn hash<H: std::hash::Hasher>(&self, _state: &mut H) {
        // ignore the concrete type of the dyn Animal for now...
        // maybe this would help for when we do: https://stackoverflow.com/questions/33687447/how-to-get-a-reference-to-a-concrete-type-from-a-trait-object
        let mut hasher = std::collections::hash_map::DefaultHasher::new();
        // &self.hash(&mut hasher);
        // TODO: uncomment this and see if it works
        // hasher.write(&self);
        hasher.finish();
    }
}

// impl std::hash::Hash for Cat {
//     fn hash<H: std::hash::Hasher>(&self, _state: &mut H) {
//         let mut hasher = std::collections::hash_map::DefaultHasher::new();
//         &self.hash(&mut hasher);
//         hasher.finish();
//     }
// }

// impl std::hash::Hash for Dog {
//     fn hash<H: std::hash::Hasher>(&self, _state: &mut H) {
//         let mut hasher = std::collections::hash_map::DefaultHasher::new();
//         &self.hash(&mut hasher);
//         hasher.finish();
//     }
// }

// // a standalone function that shows the usage of trait objects in functions
// fn animal_talk(a: &dyn Animal) {
//     a.talk();
// }

trait Value: Hash+PartialEq {}

#[derive(PartialEq, Hash, Debug)]
struct Nil {}

#[derive(PartialEq, Debug)]
struct Float(f32);

#[derive(PartialEq, Debug)]
struct Double(f64);

#[derive(Debug)]
struct Set<T: Hash+PartialEq>(HashSet<T>);

#[derive(Debug)]
struct Map<K: Hash+PartialEq,V: Hash+PartialEq>(HashMap<K,V>);

impl Value for i32 {}
impl Value for i64 {}
// Floats and Doubles have different edge cases for +-0 and NaN in different languages
// https://internals.rust-lang.org/t/f32-f64-should-implement-hash/5436/4
// We work around Rust's desire to not hash them by hashing the bits of the float
// https://stackoverflow.com/questions/39638363/how-can-i-use-a-hashmap-with-f64-as-key-in-rust
// The reason we need to hash is to allow them to be keys in order to be Values
impl Hash for Float {
    fn hash<H>(&self, hasher: &mut H)
        where
            H: Hasher,
    {
        self.0.to_bits().hash(hasher);
        hasher.finish();
    }
}
impl Value for Float {}

impl Hash for Double {
    fn hash<H>(&self, hasher: &mut H)
        where
            H: Hasher,
    {
        self.0.to_bits().hash(hasher);
        hasher.finish();
    }
}
impl Value for Double {}

impl Value for bool {}
impl Value for &str {}
impl Value for String {}
impl Value for Nil {}

impl<T: Hash+PartialEq> Hash for Set<T> {
    fn hash<H>(&self, hasher: &mut H)
        where
            H: Hasher,
    {
        for key in &self.0 {
            key.hash(hasher);
        }
        hasher.finish();
    }
}
// impl<T: PartialEq> Value for Set<T> {}

impl<K: Hash+PartialEq,V: Hash+PartialEq> Hash for Map<K,V> {
    fn hash<H>(&self, hasher: &mut H)
        where
            H: Hasher,
    {
        for (key, val) in &self.0 {
            key.hash(hasher);
            val.hash(hasher);
        }
        hasher.finish();
    }
}
// impl<K: PartialEq,V: PartialEq> Value for Map<K,V> {}

// TODO: figure out null check helper fn
// fn is_null(x: &dyn Value) -> bool {
//     let null_inst = Null {};
//     if x == null_inst {
//         true
//     } else {
//         false
//     }
// }

#[cfg(test)]
mod tests {
    use super::{Animal, Cat, Dog};
    use super::{Value, Nil, Float, Double};
    use std::collections::{HashMap, HashSet};
    use std::collections::hash_map::DefaultHasher;
    use std::hash::{Hash, Hasher};

    #[test]
    fn debug_print() {
        let v = vec![1, 2, 3];
        println!("{:?}", v);
    }

    #[test]
    fn coll_of_trait_objs() {
        let d = Dog {};
        let c = Cat {};

        // creates a collection type for trait objects
        let v2: Vec<&dyn Animal> = vec![&c, &d];
        println!("{:?}", v2);

        // creates a collection with trait objects as keys, thereby exercising the Hash trait
        // implementations defined.
        // TODO: does our hashing code for `dyn Animal` actually work? (Does it dispatch to the
        // hashing of Cat and Dog?)
        // TODO: does our simplistic hashing for Cat and Dog hold up if we add 2 dogs and 2 cats?
        let mut m1: HashMap<&dyn Animal, &str> = HashMap::new();
        m1.insert(&d, "dog");
        println!("{:?}", m1);
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

        /*
        TODO: resolve the compiler error below.
        Note: Value is a trait, and Double is a struct
        Note: think of trait=Clojure protocol, struct=Clojure record
        Note: Maybe we can search for "constrain trait type parameter using PartialEq"
        TODO: consider replacing `T: Hash+PartialEq` with `T: Value` in the definition of Set and Hash wrapper structs

error[E0038]: the trait `traitobjs::Value` cannot be made into an object
   --> src/traitobjs.rs:262:17
    |
262 |         let v: &dyn Value = &d;
    |                 ^^^^^^^^^ `traitobjs::Value` cannot be made into an object
    |
note: for a trait to be "object safe" it needs to allow building a vtable to allow the call to be resolvable dynamically; for more information visit <https://doc.rust-lang.org/reference/items/traits.html#object-safety>
   --> src/traitobjs.rs:113:19
    |
113 | trait Value: Hash+PartialEq {}
    |       -----       ^^^^^^^^^ ...because it uses `Self` as a type parameter
    |       |
    |       this trait cannot be made into an object...
         */
        let v: &dyn Value = &d;



        // let mut set: HashSet<Box<dyn Value>> = HashSet::new();
        // &set.insert(Box::new(f));
        // &set.insert(Box::new(d));
        //
        // println!("size of HashSet<Value> is {:?}", set.size());
    }
}
