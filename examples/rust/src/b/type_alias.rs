lazy_static::lazy_static! {
static ref x: std::collections::HashMap<i64,String> = {
let mut tmp_1: std::collections::HashMap<i64,String> = std::collections::HashMap::new();
tmp_1
};
}
pub fn f(y: std::collections::HashMap<i64, String>) -> std::collections::HashMap<i64, String> {
    let z: std::collections::HashMap<i64, String> = y;
    return z;
}
fn main() {
    let args: std::vec::Vec<String> = std::env::args().collect();
    {
        println!("{}", String::from("OK"));
    }
}
