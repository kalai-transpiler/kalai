use crate::kalai::kalai;
use crate::kalai::kalai::*;
pub fn main() {
    let _args: std::vec::Vec<String> = std::env::args().collect();
    {
        kalai::BValue::from(kalai::NIL);
    }
}