use crate::kalai::kalai;
use crate::kalai::kalai::PMap;
pub fn main() {
    let _args: std::vec::Vec<String> = std::env::args().collect();
    {
        println!("{}", std::env::var(String::from("USER")).unwrap());
    }
}
