use crate::kalai;
use crate::kalai::PMap;
pub fn main() {
    let _args: std::vec::Vec<String> = std::env::args().collect();
    {
        println!("{}", crate::b::required::f(1));
    }
}
