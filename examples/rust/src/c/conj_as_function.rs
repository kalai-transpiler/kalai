use crate::kalai::kalai;
use crate::kalai::kalai::*;
pub fn conj_map_map() -> i64 {
    let a: kalai::BValue = kalai::BValue::from(rpds::HashTrieMap::new().insert(
        kalai::BValue::from(String::from(":a")),
        kalai::BValue::from(1i64),
    ));
    let b: kalai::BValue = kalai::BValue::from(rpds::HashTrieMap::new().insert(
        kalai::BValue::from(String::from(":b")),
        kalai::BValue::from(2i64),
    ));
    let c: kalai::BValue = conj(a, b);
    return 3i64;
}
pub fn conj_map_vec() -> i64 {
    let a: kalai::BValue = kalai::BValue::from(rpds::HashTrieMap::new().insert(
        kalai::BValue::from(String::from(":a")),
        kalai::BValue::from(1i64),
    ));
    let b: kalai::BValue = kalai::BValue::from(
        rpds::Vector::new()
            .push_back(kalai::BValue::from(String::from(":b")))
            .push_back(kalai::BValue::from(2i64)),
    );
    let c: kalai::BValue = conj(a, b);
    return 11i64;
}
pub fn conj_set() -> i64 {
    let a: kalai::BValue = kalai::BValue::from(
        rpds::HashTrieSet::new()
            .insert(kalai::BValue::from(String::from(":a")))
            .insert(kalai::BValue::from(String::from(":b"))),
    );
    let new_value: kalai::BValue = kalai::BValue::from(String::from(":c"));
    let c: kalai::BValue = conj(a, new_value);
    return 5i64;
}
pub fn conj_vector() -> i64 {
    let a: kalai::BValue = kalai::BValue::from(
        rpds::Vector::new()
            .push_back(kalai::BValue::from(String::from(":a")))
            .push_back(kalai::BValue::from(String::from(":b"))),
    );
    let new_value: kalai::BValue = kalai::BValue::from(String::from(":c"));
    let c: kalai::BValue = conj(a, new_value);
    return 7i64;
}
pub fn type_conversions() -> i64 {
    let a: rpds::HashTrieMap<kalai::BValue, kalai::BValue> = rpds::HashTrieMap::new().insert(
        kalai::BValue::from(String::from(":a")),
        kalai::BValue::from(1i64),
    );
    let b: rpds::HashTrieMap<kalai::BValue, kalai::BValue> = rpds::HashTrieMap::new().insert(
        kalai::BValue::from(String::from(":b")),
        kalai::BValue::from(1i64),
    );
    {
        conj(kalai::BValue::from(a), kalai::BValue::from(b));
        return 4i64;
    }
}
pub fn main() {
    let _args: std::vec::Vec<String> = std::env::args().collect();
    {
        println!("{}", conj_map_map());
        println!("{}", conj_map_vec());
        println!("{}", conj_set());
        println!("{}", conj_vector());
        println!("{}", type_conversions());
    }
}
