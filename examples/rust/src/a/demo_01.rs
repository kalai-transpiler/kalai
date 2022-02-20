use crate::kalai;
use crate::kalai::PMap;
pub fn format(num: i32) -> String {
    let mut i: i32 = num;
    let mut result: std::vec::Vec<char> = std::vec::Vec::new();
    {
        while !(i == 0) {
            let quotient: i32 = (i / 10);
            let remainder: i32 = (i % 10);
            {
                result.splice(
                    0..0,
                    remainder
                        .to_string()
                        .chars()
                        .collect::<std::vec::Vec<char>>(),
                );
                i = quotient;
            }
        }
        return result.iter().collect();
    }
}
pub fn main() {
    let _args: std::vec::Vec<String> = std::env::args().collect();
    {
        format(2345);
        println!("{}", format(2345));
    }
}
