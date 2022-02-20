use crate::kalai;
use crate::kalai::PMap;
pub fn test_conj() -> i64 {
    let a: std::collections::HashMap<String, i64> = {
        let mut tmp1: std::collections::HashMap<String, i64> = std::collections::HashMap::new();
        tmp1.insert(String::from(":x"), 11);
        tmp1.insert(String::from(":y"), 13);
        tmp1
    };
    let b: kalai::Map = {
        let mut tmp2: kalai::Map = kalai::Map::new();
        tmp2.insert(
            kalai::BValue::from(String::from(":x")),
            kalai::BValue::from(11),
        );
        tmp2.insert(
            kalai::BValue::from(String::from(":y")),
            kalai::BValue::from(13),
        );
        tmp2
    };
    let c: rpds::HashTrieMap<String, i64> = {
        let mut tmp3: rpds::HashTrieMap<String, i64> = rpds::HashTrieMap::new();
        tmp3.insert(String::from(":x"), 11);
        tmp3.insert(String::from(":y"), 13);
        tmp3
    };
    let d: rpds::HashTrieMap<kalai::BValue, kalai::BValue> = {
        let mut tmp4: rpds::HashTrieMap<kalai::BValue, kalai::BValue> = rpds::HashTrieMap::new();
        tmp4.insert(
            kalai::BValue::from(String::from(":x")),
            kalai::BValue::from(11),
        );
        tmp4.insert(
            kalai::BValue::from(String::from(":y")),
            kalai::BValue::from(13),
        );
        tmp4
    };
    return 3;
}
pub fn main() {
    let _args: std::vec::Vec<String> = std::env::args().collect();
    {
        println!("{}", test_conj());
    }
}
