use std::collections::hash_map::DefaultHasher;
use std::collections::HashMap;
use std::collections::HashSet;
use std::convert::TryInto;
use std::fmt;
use std::fmt::Debug;
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


// a standalone function that shows the usage of trait objects in functions
fn animal_talk(a: &dyn Animal) {
    a.talk();
}





trait Value {}

impl Value for Cat {}

impl Value for Dog {}

impl Eq for dyn Value {}

impl PartialEq for dyn Value {
    fn eq(&self, other: &Self) -> bool {
        true
    }
}

struct Null {}

impl Value for u32 {}
impl Value for u64 {}
impl Value for bool {}
impl Value for &str {}
impl Value for String {}
impl Value for Null {}

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
    use std::collections::HashMap;
    use super::{Animal, Cat, Dog};

    #[test]
    fn debug_print() {
        let v = vec![1, 2, 3,];
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
}
