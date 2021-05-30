use std::hash::Hasher;

#[derive(Debug, PartialEq, Clone)]
pub enum Value {
    Null,
    Byte(u8),
    Bool(bool),
    Float(f32),  // Remember: need to impl Eq
    Double(f64), // Remember: need to impl Eq
    Int(i32),
    Long(i64),
    String(String),
    MSet(std::collections::HashSet<Value>),
    MVector(std::vec::Vec<Value>),
    MMap(std::collections::HashMap<Value, Value>),
}

// We are informing the compiler that this is an equivalence relation,
// in particular for floating points (otherwise you can just derive).
impl Eq for Value {}

// Implementing Hash is necessary for Values to be the key of a map
impl std::hash::Hash for Value {
    fn hash<H: std::hash::Hasher>(&self, _state: &mut H) {
        match self {
            Value::MSet(x) => {
                let mut hasher = std::collections::hash_map::DefaultHasher::new();
                for key in x {
                    key.hash(&mut hasher);
                }
                hasher.finish();
            },
            Value::MVector(x) => {
                let mut hasher = std::collections::hash_map::DefaultHasher::new();
                x.hash(&mut hasher);
                hasher.finish();
            },
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
            Value::Long(x) => {
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
                let hasher = std::collections::hash_map::DefaultHasher::new();
                // x.hash(&mut hasher);
                hasher.finish();
            },
            // Floats and Doubles have different edge cases for +-0 and NaN in different languages
            // https://internals.rust-lang.org/t/f32-f64-should-implement-hash/5436/4
            // We work around Rust's desire to not hash them by hashing the bits of the float
            // https://stackoverflow.com/questions/39638363/how-can-i-use-a-hashmap-with-f64-as-key-in-rust
            // The reason we need to hash is to allow them to be keys in order to be Values
            Value::Float(x) => {
                let mut hasher = std::collections::hash_map::DefaultHasher::new();
                x.to_bits().hash(&mut hasher);
                hasher.finish();
            },
            Value::Double(x) => {
                let mut hasher = std::collections::hash_map::DefaultHasher::new();
                x.to_bits().hash(&mut hasher);
                hasher.finish();
            },
            Value::Byte(x) => {
                let mut hasher = std::collections::hash_map::DefaultHasher::new();
                x.hash(&mut hasher);
                hasher.finish();
            }
        }
    }
}

pub fn to_int(v: Value) -> i32 {
    return match v {
        Value::Int(x) => x,
        _ => panic!("not an int")
    }
}

pub fn is_int(v: Value) -> bool {
    return match v {
        Value::Int(x) => true,
        _ => false
    }
}

pub fn to_long(v: Value) -> i64 {
    return match v {
        Value::Long(x) => x,
        _ => panic!("not a long")
    }
}

pub fn is_long(v: Value) -> bool {
    return match v {
        Value::Long(x) => true,
        _ => false
    }
}

pub fn to_string(v: Value) -> String {
    return match v {
        Value::String(x) => x,
        _ => panic!("not a String")
    }
}

pub fn is_string(v: Value) -> bool {
    return match v {
        Value::String(x) => true,
        _ => false
    }
}

pub fn to_bool(v: Value) -> bool {
    return match v {
        Value::Bool(x) => x,
        _ => panic!("not a bool")
    }
}

pub fn is_bool(v: Value) -> bool {
    return match v {
        Value::Bool(x) => true,
        _ => false
    }
}

pub fn to_byte(v: Value) -> u8 {
    return match v {
        Value::Byte(x) => x,
        _ => panic!("not a byte")
    }
}

pub fn is_byte(v: Value) -> bool {
    return match v {
        Value::Byte(x) => true,
        _ => false
    }
}

pub fn to_float(v: Value) -> f32 {
    return match v {
        Value::Float(x) => x,
        _ => panic!("not a float")
    }
}

pub fn is_float(v: Value) -> bool {
    return match v {
        Value::Float(x) => true,
        _ => false
    }
}

pub fn to_double(v: Value) -> f64 {
    return match v {
        Value::Double(x) => x,
        _ => panic!("not a double")
    }
}

pub fn is_double(v: Value) -> bool {
    return match v {
        Value::Double(x) => true,
        _ => false
    }
}

pub fn to_map(v: Value) -> std::collections::HashMap<Value, Value> {
    return match v {
        Value::MMap(x) => x,
        _ => panic!("not a map")
    }
}

pub fn is_map(v: Value) -> bool {
    return match v {
        Value::MMap(x) => true,
        _ => false
    }
}

pub fn to_vector(v: Value) -> std::vec::Vec<Value> {
    return match v {
        Value::MVector(x) => x,
        _ => panic!("not a map")
    }
}

pub fn is_vector(v: Value) -> bool {
    return match v {
        Value::MVector(x) => true,
        _ => false
    }
}

pub fn to_set(v: Value) -> std::collections::HashSet<Value> {
    return match v {
        Value::MSet(x) => x,
        _ => panic!("not a map")
    }
}

pub fn is_set(v: Value) -> bool {
    return match v {
        Value::MSet(x) => true,
        _ => false
    }
}

// Demonstrates basic enum matching on Value
#[test]
pub fn enum_test() {
    let v: Value = Value::Int(3);
    println!("{:?}", &v);

    // let v3 = v + 5;
    let int_result = to_int(v) + 5;
    println!("int_result = {}", int_result);
}

// Constructing values and using them
#[test]
pub fn enum2_test() {
    let mut v: std::vec::Vec<Value> = std::vec![Value::Int(2), Value::Int(3), Value::Int(5)];
    let mut m: std::collections::HashMap<Value, Value> = std::collections::HashMap::new();
    m.insert(Value::Int(3), Value::MVector(v));
    let mval: Value = Value::MMap(m);

    let mval_map: std::collections::HashMap<Value, Value> = to_map(mval);
    let mval_vecval: Value = mval_map.get(&Value::Int(3)).unwrap().clone();
    let mval_vec: std::vec::Vec<Value> = to_vector(mval_vecval);
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
