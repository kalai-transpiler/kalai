use crate::kalai;
use crate::kalai::PMap;
pub fn main() {
    let _args: std::vec::Vec<String> = std::env::args().collect();
    {
        let x: std::vec::Vec<i64> = {
            let mut tmp1: std::vec::Vec<i64> = std::vec::Vec::new();
            tmp1.push(1i64);
            tmp1.push(2i64);
            tmp1.push(3i64);
            tmp1.push(4i64);
            tmp1.push(5i64);
            tmp1
        };
        println!(
            "{} {}",
            String::from("HELLO***"),
            x.clone()
                .into_iter()
                .map(|y: i64| {
                    return (y + 1i64);
                })
                .next()
                .unwrap()
                .clone()
        );
    }
}
