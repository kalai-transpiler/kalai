use crate::kalai;
pub fn main() {
    let args: std::vec::Vec<String> = std::env::args().collect();
    {
        println!("{}", crate::b::required::f(1));
    }
}
