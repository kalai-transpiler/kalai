use std::any::Any;
use std::collections::hash_map::DefaultHasher;
use std::collections::{HashMap, BinaryHeap};
use std::collections::HashSet;
use std::convert::TryInto;
use std::{fmt, ops};
use std::fmt::{Debug};
use std::hash::{Hash, Hasher};
use std::vec::Vec;
use std::ops::{Deref, Add};

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
// (especially related to the `Value2` trait) that the primitives don't allow
// us to have by default. These structs effectively implement the "New Type Idiom"
// https://doc.rust-lang.org/rust-by-example/generics/new_types.html in Rust.
// We need this for things like Float, Double, Set, Map, etc. in order to create
// custom implementations that allow them to be put into keys of sets and maps, etc.

#[derive(PartialEq, Hash, Debug)]
struct Nil {}

#[derive(PartialEq, Debug)]
struct Float(f32);

#[derive(PartialEq, Debug)]
struct Double(f64);

#[derive(Debug)]
struct Set(HashSet<Box<dyn Value2>>);

#[derive(Debug)]
struct Map<K: Hash,V: Hash>(HashMap<K,V>);




// implementing Value2 trait based on SO answer at:
// https://stackoverflow.com/a/49779676

/// Implementing `hash_id` is necessary for the default Hash impl.
/// Implementing `eq_test` is necessary for the default PartialEq impl,
/// and therefore for the default Eq impl by extension.
///
/// `Set`s can contain `Set`s. This is because `Set` is a struct that
/// wraps `HashSet<Box<dyn Value2>>`, and because `Set` implements the `Value2`
/// trait. In other words, `Set` is allowed to be recursive.
///
/// For `Set`s and `Map`s to be recursive, they must implement the `Hash` trait
/// in order to be able to be put inside other `Set`s and `Map`s. Therefore,
/// the implementation of `hash_id` for those structs is required.
///
/// Note that `HashSet` does not implement the `Hash` trait, and that it is important
/// when implementing the `Hash` trait for a `Set`/`HashSet` that the hash value be
/// order-independent (regardless of the order of insertion or storage / storage iterator).
trait Value2: Debug {
    fn hash_id(&self) -> u64;
    fn as_any(&self) -> &dyn Any;
    fn eq_test(&self, other: &dyn Value2) -> bool;
}

impl Hash for dyn Value2 {
    fn hash<H>(&self, state: &mut H) where H: Hasher {
        self.hash_id().hash(state)
    }
}

impl PartialEq for dyn Value2 {
    fn eq(&self, other: &Self) -> bool {
        self.eq_test(other)
    }
}

impl Eq for dyn Value2 {}





impl Value2 for Double {
    fn hash_id(&self) -> u64
    {
        self.0.to_bits()
    }

    fn as_any(&self) -> &dyn Any {
        self
    }

    fn eq_test(&self, other: &dyn Value2) -> bool {
        match other.as_any().downcast_ref::<Double>() {
            Some(dbl) => {
                &self.0 == &dbl.0
            }
            None => false,
        }
    }
}

impl Value2 for Float {
    fn hash_id(&self) -> u64
    {
        self.0.to_bits() as u64
    }

    fn as_any(&self) -> &dyn Any {
        self
    }

    fn eq_test(&self, other: &dyn Value2) -> bool {
        match other.as_any().downcast_ref::<Float>() {
            Some(dbl) => {
                &self.0 == &dbl.0
            }
            None => false,
        }
    }
}



impl Value2 for Set {
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

    fn eq_test(&self, other: &dyn Value2) -> bool {
        match other.as_any().downcast_ref::<Set>() {
            Some(set) => {
                &self.0 == &set.0
            }
            None => false,
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

// impl Set {
//     pub fn contains<T: ?Sized>(&self, value: &T) -> bool {
//
//     }
// }



#[cfg(test)]
mod tests {
    use super::{Nil, Float, Double};

    use super::Set;
    use super::Value2;

    use std::collections::{HashMap, HashSet};
    use std::collections::hash_map::DefaultHasher;
    use std::hash::{Hash, Hasher};

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
    fn test_value2_trait() {
        let f = Float(3.14);
        let d = Double(1.414);
        let mut my_map = HashMap::<Box<dyn Value2>, i32>::new();
        my_map.insert(Box::new(f), 1);
        my_map.insert(Box::new(d), 2);

        println!("{:?}", my_map.get(&(Box::new(Float(6.28)) as Box<dyn Value2>)));
        println!("{:?}", my_map.get(&(Box::new(Float(9.42)) as Box<dyn Value2>)));
        println!("{:?}", my_map.get(&(Box::new(Double(2.828)) as Box<dyn Value2>)));
        println!("{:?}", my_map.get(&(Box::new(Double(5.656)) as Box<dyn Value2>)));

        println!("size of HashMap<Box<Value2,i32>> is {:?}", my_map.len());
        assert_eq!(2, my_map.len());
    }

    #[test]
    fn test_float_structs_with_value2_trait() {
        let f = Float(3.14);
        println!("{:?}", f);

        let d = Double(1.414);

        let v: &dyn Value2 = &d;

        let mut set: HashSet<Box<dyn Value2>> = HashSet::new();
        &set.insert(Box::new(f));
        &set.insert(Box::new(d));

        println!("size of HashSet<Box<Value2>> is {:?}", set.len());
        assert_eq!(2, set.len());

        let mut inner_set2: HashSet<Box<dyn Value2>> = HashSet::new();
        &inner_set2.insert(Box::new(Double(98.6)));
        &inner_set2.insert(Box::new(Float(37.0)));
        let set2: Set = Set(inner_set2);

        println!("size of Set (unnamed arg of type HashSet<Box<Value2>>) is {:?}", set2.0.len());
        assert_eq!(2, set2.0.len());

        // test PartialEq for Float struct
        assert_eq!(Box::new(Float(3.456)), Box::new(Float(3.456)));
        let f1: &dyn Value2 = &Float(3.456);
        let f2: &dyn Value2 = &Float(3.456);
        assert!(f1 == f2);
    }

    #[test]
    fn test_set_struct_with_value2_trait() {
        let mut set12: HashSet<Box<dyn Value2>> = HashSet::new();
        &set12.insert(Box::new(Float(1.0)));
        &set12.insert(Box::new(Double(2.0)));

        let mut set34: HashSet<Box<dyn Value2>> = HashSet::new();
        &set34.insert(Box::new(Float(3.0)));
        &set34.insert(Box::new(Double(4.0)));

        let mut set_12_34: HashSet<Box<dyn Value2>> = HashSet::new();
        println!("inserting set12 into set_12_34");
        &set_12_34.insert(Box::new(Set(set12)));
        println!("inserting set34 into set_12_34");
        &set_12_34.insert(Box::new(Set(set34)));

        let mut set12b: HashSet<Box<dyn Value2>> = HashSet::new();
        &set12b.insert(Box::new(Float(1.0)));
        &set12b.insert(Box::new(Double(2.0)));

        let mut set34b: HashSet<Box<dyn Value2>> = HashSet::new();
        &set34b.insert(Box::new(Float(3.0)));
        &set34b.insert(Box::new(Double(4.0)));

        let mut set_34_12: HashSet<Box<dyn Value2>> = HashSet::new();
        println!("inserting set34 into set_34_12");
        &set_34_12.insert(Box::new(Set(set34b)));
        println!("inserting set12 into set_34_12");
        &set_34_12.insert(Box::new(Set(set12b)));

        let sv12_34: &dyn Value2 = &Set(set_12_34); // #{#{1 2} #{3 4}}
        let sv34_12: &dyn Value2 = &Set(set_34_12); // #{#{3 4} #{1 2}}
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

    // This test proves that the Hash impl for Box<T> delegates to the Hash impl for T, and that
    // PartialEq on Box<T> is also delegating to PartialEq on T.
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

    // Note: this test does not compile because "the trait `Hash` is not implemented for `HashSet<{integer}>`"
    // which results in the follow-on error "method cannot be called on `HashSet<HashSet<{integer}>>` due to unsatisfied trait bounds"
    // for calling `.insert` with an argument of a HashSet.
    //
    // #[test]
    // fn test_nested_set_eq() {
    //     let mut set1 = HashSet::new();
    //     &set1.insert(2);
    //     &set1.insert(3);
    //     &set1.insert(5);
    //
    //     let mut set2 = HashSet::new();
    //     &set2.insert(3);
    //     &set2.insert(5);
    //     &set2.insert(2);
    //
    //     let mut set12 = HashSet::new();
    //     &set12.insert(set1);
    //     &set12.insert(set2);
    //
    //
    //     let mut set1 = HashSet::new();
    //     &set1.insert(2);
    //     &set1.insert(3);
    //     &set1.insert(5);
    //
    //     let mut set2 = HashSet::new();
    //     &set2.insert(3);
    //     &set2.insert(5);
    //     &set2.insert(2);
    //
    //     let mut set21 = HashSet::new();
    //     &set21.insert(set1);
    //     &set21.insert(set2);
    //
    //
    //     assert_eq!(set12, set21);
    // }


    /*
    let mut set = HashSet::new();
    let f = 3.14;
    &set.insert(f); // not allowed

    let mut s = Set { HashSet::new() };
    let f_box = Box::new(Float(3.14));
    &s.0.insert(f_box);

    &s.0.contains(Box::new(3.14));


    let mut m = Map { HashMap::new() };
    let k_
     */
}
