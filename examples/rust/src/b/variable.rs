use crate::kalai;
pub fn side_effect() -> i64 {
    let mut y: i64 = 2;
    {
        y = 3;
        (y + 4);
        return y;
    }
}
pub fn main() {
    let args: std::vec::Vec<String> = std::env::args().collect();
    {
        println!("{}", side_effect());
    }
}
