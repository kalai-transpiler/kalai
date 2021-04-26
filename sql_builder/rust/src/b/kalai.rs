// use std::collections::HashMap;
use std::hash::Hasher;

// TODO:
// - have convenience fns to do the matching of Value to a concrete type, for all types
// - have convenience fn(s) for doing type checking given an input of type Value (variant info elided)
//   * could be Value -> type string
//   * could be boolean?(Value) -> true/false
// - have a convenience fn for construction of Value (?)
//   * rationale: it will be recursive on construction like it is recursive on reading/casting

// TODO: add "variants" to enum to match all Kalai types in e-string
#[derive(Debug, Eq, PartialEq)]
enum Value {
    Null,
    Bool(bool),
    // Float(f32),  // Remember: need to impl Eq
    Int(i32),
    String(String),
    MVector(std::vec::Vec<Value>),
    MMap(std::collections::HashMap<Value, Value>),
}

impl std::hash::Hash for crate::b::kalai::Value {
    fn hash<H: std::hash::Hasher>(&self, state: &mut H) {
        match self {
            Value::MMap(map) => {
                let mut hasher = std::collections::hash_map::DefaultHasher::new();
                for (key, val) in map {
                    key.hash(&mut hasher);
                    val.hash(&mut hasher);
                }
                hasher.finish();
            },
            Value::String(x) => {
                let mut hasher = std::collections::hash_map::DefaultHasher::new();
                x.hash(&mut hasher);
                hasher.finish();
            },
            Value::Int(x) => {
                let mut hasher = std::collections::hash_map::DefaultHasher::new();
                x.hash(&mut hasher);
                hasher.finish();
            },
            Value::Bool(x) => {
                let mut hasher = std::collections::hash_map::DefaultHasher::new();
                x.hash(&mut hasher);
                hasher.finish();
            },
            Value::Null => {
                let mut hasher = std::collections::hash_map::DefaultHasher::new();
                // x.hash(&mut hasher);
                hasher.finish();
            },
            Value::MVector(x) => {
                let mut hasher = std::collections::hash_map::DefaultHasher::new();
                x.hash(&mut hasher);
                hasher.finish();
            },

        }
    }
}

#[test]
pub fn enum_test() {
    let v: Value = Value::Int(3);
    println!("{:?}", &v);

    // let v3 = v + 5;
     let int_result = match v {
        Value::Int(i) => i,
        _ => panic!("wrong type"),
    } + 5;
    println!("int_result = {}", int_result);
}

#[test]
pub fn enum2_test() {
    let mut v: std::vec::Vec<Value> = std::vec![Value::Int(2), Value::Int(3), Value::Int(5)];
    let mut m: std::collections::HashMap<Value, Value> = std::collections::HashMap::new();
    m.insert(Value::Int(3), Value::MVector(v));
    let mval: Value = Value::MMap(m);

    let mval_map: std::collections::HashMap<Value, Value>
        = match mval {
        Value::MMap(mmap) => {
            mmap
        },
        _ => panic!("wrong type"),
    };
    let mval_vecval: &Value = mval_map.get(&Value::Int(3)).unwrap();
    let mval_vec: &std::vec::Vec<Value>
    = match mval_vecval {
        Value::MVector(mval_v) => {
            mval_v
        },
        _ => panic!("wrong type"),
    };
    let mval_vec_first: &Value = mval_vec.get(0).unwrap();

    println!("mval_vec_first = {:?}", mval_vec_first);
}

#[test]
pub fn cast_test() {
    let x: i32 = 3;
    let f: f32 = x as f32;
    println!("f is {}", f);

    let x2: i32 = f as i32;
    println!("x2 is {}", x2);
}

