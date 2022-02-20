use crate::kalai;
use crate::kalai::PMap;
pub fn add(a: i64, b: i64) -> i64 {
    return (a + b);
}
pub fn main() {
    let _args: std::vec::Vec<String> = std::env::args().collect();
    {
        println!("{}", add(1, 2));
    }
}
