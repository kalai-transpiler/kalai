use crate::kalai;
use crate::kalai::PMap;
pub fn main() {
    let _args: std::vec::Vec<String> = std::env::args().collect();
    {
        let mut i1: i32 = 0i32;
        while (i1 < 10i32) {
            println!("{}", i1);
            i1 = (i1 + 1i32);
        }
        let mut i2: i64 = 0i64;
        while (i2 < 10i64) {
            println!("{}", i2);
            i2 = (i2 + 1i64);
        }
        for ii in {
            let mut tmp1: std::vec::Vec<i64> = std::vec::Vec::new();
            tmp1.push(1i64);
            tmp1.push(2i64);
            tmp1.push(3i64);
            tmp1
        } {
            println!("{}", ii);
        }
        {
            let mut x: i64 = 0i64;
            while (x < 10i64) {
                x = (x + 1i64);
                println!("{}", x);
            }
        }
    }
}
