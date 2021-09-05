use crate::kalai;
pub fn main() {
    let _args: std::vec::Vec<String> = std::env::args().collect();
    {
        let mut i: i64 = 0;
        while (i < 10) {
            println!("{}", i);
            i = (i + 1);
        }
        for ii in {
            let mut tmp_1: std::vec::Vec<i64> = std::vec::Vec::new();
            tmp_1.push(1);
            tmp_1.push(2);
            tmp_1.push(3);
            tmp_1
        } {
            println!("{}", ii);
        }
        {
            let mut x: i64 = 0;
            while (x < 10) {
                x = (x + 1);
                println!("{}", x);
            }
        }
    }
}
