#[macro_use]
extern crate lazy_static;
use std::collections::HashMap;
use std::collections::HashSet;
use std::vec::Vec;
use std::convert::TryInto;
use std::env;
pub fn format(num: i32) -> String {
let mut i: i32 = num;
let result: String = String::new();
{
while !(i == 0) {
let quotient: i32 = (i / 10);
let remainder: i32 = (i % 10);
{
result.insert(0, remainder);
i = quotient;
}
}
return result;
}
}
fn main () {
let args: Vec<String> = env::args().collect();
{
a.demo_01.format(2345);
println!("{}", a.demo_01.format(2345));
}
}