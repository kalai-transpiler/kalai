use crate::kalai::kalai;
use crate::kalai::kalai::*;
pub fn side_effect() -> i64 {
    let mut y: i64 = 2i64;
    {
        y = 3i64;
        (y + 4i64);
        return y;
    }
}
pub fn main() {
    let _args: std::vec::Vec<String> = std::env::args().collect();
    {
        println!("{}", side_effect());
    }
}
