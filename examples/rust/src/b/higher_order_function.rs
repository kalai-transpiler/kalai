use crate::kalai::kalai;
use crate::kalai::kalai::PMap;
pub fn main() {
    let _args: std::vec::Vec<String> = std::env::args().collect();
    {
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
        {
            let y: std::vec::Vec<i64> = {
                let mut tmp2: std::vec::Vec<i64> = std::vec::Vec::new();
                tmp2.push(1i64);
                tmp2.push(2i64);
                tmp2.push(3i64);
                tmp2.push(4i64);
                tmp2.push(5i64);
                tmp2
            };
            let z: i64 = y
                .clone()
                .into_iter()
                .reduce(|a, b| {
                    return (a + b);
                })
                .unwrap();
            let z2: String = y.clone().into_iter().fold(String::from(""), |a, b| {
                return format!("{}{}", a, b);
            });
            {
                println!("{} {}", String::from("z ="), z);
                println!("{} {}", String::from("z2 ="), z2);
            }
        }
    }
}
