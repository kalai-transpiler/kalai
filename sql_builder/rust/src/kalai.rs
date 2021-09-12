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
    PSet(std::collections::HashSet<Value>), // TODO: use a persistent
    MVector(std::vec::Vec<Value>),
    PVector(std::vec::Vec<Value>),
    MMap(std::collections::HashMap<Value, Value>),
    PMap(std::collections::HashMap<Value, Value>),
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
            }
            Value::MVector(x) => {
                let mut hasher = std::collections::hash_map::DefaultHasher::new();
                x.hash(&mut hasher);
                hasher.finish();
            }
            Value::MMap(map) => {
                let mut hasher = std::collections::hash_map::DefaultHasher::new();
                for (key, val) in map {
                    key.hash(&mut hasher);
                    val.hash(&mut hasher);
                }
                hasher.finish();
            }
            Value::PSet(x) => {
                let mut hasher = std::collections::hash_map::DefaultHasher::new();
                for key in x {
                    key.hash(&mut hasher);
                }
                hasher.finish();
            }
            Value::PVector(x) => {
                let mut hasher = std::collections::hash_map::DefaultHasher::new();
                x.hash(&mut hasher);
                hasher.finish();
            }
            Value::PMap(map) => {
                let mut hasher = std::collections::hash_map::DefaultHasher::new();
                for (key, val) in map {
                    key.hash(&mut hasher);
                    val.hash(&mut hasher);
                }
                hasher.finish();
            }
            Value::String(x) => {
                let mut hasher = std::collections::hash_map::DefaultHasher::new();
                x.hash(&mut hasher);
                hasher.finish();
            }
            Value::Int(x) => {
                let mut hasher = std::collections::hash_map::DefaultHasher::new();
                x.hash(&mut hasher);
                hasher.finish();
            }
            Value::Long(x) => {
                let mut hasher = std::collections::hash_map::DefaultHasher::new();
                x.hash(&mut hasher);
                hasher.finish();
            }
            Value::Bool(x) => {
                let mut hasher = std::collections::hash_map::DefaultHasher::new();
                x.hash(&mut hasher);
                hasher.finish();
            }
            Value::Null => {
                let hasher = std::collections::hash_map::DefaultHasher::new();
                // x.hash(&mut hasher);
                hasher.finish();
            }
            // Floats and Doubles have different edge cases for +-0 and NaN in different languages
            // https://internals.rust-lang.org/t/f32-f64-should-implement-hash/5436/4
            // We work around Rust's desire to not hash them by hashing the bits of the float
            // https://stackoverflow.com/questions/39638363/how-can-i-use-a-hashmap-with-f64-as-key-in-rust
            // The reason we need to hash is to allow them to be keys in order to be Values
            Value::Float(x) => {
                let mut hasher = std::collections::hash_map::DefaultHasher::new();
                x.to_bits().hash(&mut hasher);
                hasher.finish();
            }
            Value::Double(x) => {
                let mut hasher = std::collections::hash_map::DefaultHasher::new();
                x.to_bits().hash(&mut hasher);
                hasher.finish();
            }
            Value::Byte(x) => {
                let mut hasher = std::collections::hash_map::DefaultHasher::new();
                x.hash(&mut hasher);
                hasher.finish();
            }
        }
    }
}

pub fn to_int(v: Value) -> i32 {
    match v {
        Value::Int(x) => x,
        _ => panic!("not an int"),
    }
}

pub fn is_int(v: Value) -> bool {
    match v {
        Value::Int(_) => true,
        _ => false,
    }
}

pub fn to_long(v: Value) -> i64 {
    match v {
        Value::Long(x) => x,
        _ => panic!("not a long"),
    }
}

pub fn is_long(v: Value) -> bool {
    match v {
        Value::Long(_) => true,
        _ => false,
    }
}

pub fn to_string(v: Value) -> String {
    match v {
        Value::String(x) => x,
        _ => panic!("not a String"),
    }
}

pub fn is_string(v: Value) -> bool {
    match v {
        Value::String(_) => true,
        _ => false,
    }
}

pub fn to_bool(v: Value) -> bool {
    match v {
        Value::Bool(x) => x,
        _ => panic!("not a bool"),
    }
}

pub fn is_bool(v: Value) -> bool {
    match v {
        Value::Bool(_) => true,
        _ => false,
    }
}

pub fn to_byte(v: Value) -> u8 {
    match v {
        Value::Byte(x) => x,
        _ => panic!("not a byte"),
    }
}

pub fn is_byte(v: Value) -> bool {
    match v {
        Value::Byte(_) => true,
        _ => false,
    }
}

pub fn to_float(v: Value) -> f32 {
    match v {
        Value::Float(x) => x,
        _ => panic!("not a float"),
    }
}

pub fn is_float(v: Value) -> bool {
    match v {
        Value::Float(_) => true,
        _ => false,
    }
}

pub fn to_double(v: Value) -> f64 {
    match v {
        Value::Double(x) => x,
        _ => panic!("not a double"),
    }
}

pub fn is_double(v: Value) -> bool {
    match v {
        Value::Double(_) => true,
        _ => false,
    }
}

pub fn to_mmap(v: Value) -> std::collections::HashMap<Value, Value> {
    match v {
        Value::MMap(x) => x,
        _ => panic!("not a map"),
    }
}

pub fn to_map(v: Value) -> std::collections::HashMap<Value, Value> {
    match v {
        Value::PMap(x) => x,
        _ => panic!("not a map"),
    }
}

pub fn is_map(v: Value) -> bool {
    match v {
        Value::MMap(_) => true,
        Value::PMap(_) => true,
        _ => false,
    }
}

pub fn to_mvector(v: Value) -> std::vec::Vec<Value> {
    match v {
        Value::MVector(x) => x,
        _ => panic!("not a vector"),
    }
}

pub fn to_vector(v: Value) -> std::vec::Vec<Value> {
    match v {
        Value::PVector(x) => x,
        _ => panic!("not a vector"),
    }
}

pub fn is_vector(v: Value) -> bool {
    match v {
        Value::MVector(_) => true,
        Value::PVector(_) => true,
        _ => false,
    }
}

pub fn to_mset(v: Value) -> std::collections::HashSet<Value> {
    match v {
        Value::MSet(x) => x,
        _ => panic!("not a set"),
    }
}

pub fn to_set(v: Value) -> std::collections::HashSet<Value> {
    match v {
        Value::PSet(x) => x,
        _ => panic!("not a set"),
    }
}

pub fn is_set(v: Value) -> bool {
    match v {
        Value::MSet(_) => true,
        Value::PSet(_) => true,
        _ => false,
    }
}

pub fn is_null(v: Value) -> bool {
    v == Value::Null
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

    let mval_map: std::collections::HashMap<Value, Value> = to_mmap(mval);
    let mval_vecval: Value = mval_map.get(&Value::Int(3)).unwrap().clone();
    let mval_vec: std::vec::Vec<Value> = to_mvector(mval_vecval);
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

pub fn where_str_testing(join: Value) -> String {
    if is_vector(join.clone()) {
        format!(
            "{}{}{}",
            String::from("("),
            to_vector(join.clone())
                .iter()
                .map(|val| where_str_testing(val.clone()))
                .collect::<Vec<String>>()
                .join(&format!(
                    "{}{}{}",
                    String::from(" "),
                    String::from("hi"),
                    String::from(" ")
                )),
            String::from(")")
        )
    } else {
        to_string(join.clone())
    }
}

#[test]
pub fn iter_test() {
    use std::iter::Iterator;
    use std::slice::Iter;

    let a = vec![1, 2, 3];
    let a_iter = a.iter();
    a_iter.map(|x| println!("{}", x));
    // let mut b = [0, 1, 2].iter().intersperse(&100);

    let b = vec!["1", "2", "3"];
    let b_join_str = b.join(", ");
    println!("b_join_str = {}", b_join_str);

    let c = vec![Value::String("1".to_string())];
    let c_join_str = c
        .iter()
        .map(|val| where_str_testing(val.clone()))
        .collect::<Vec<String>>()
        .join(",");
    println!("c_join_str = {}", c_join_str);
}
