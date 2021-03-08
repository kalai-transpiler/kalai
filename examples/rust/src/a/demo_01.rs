#[macro_use]
extern crate lazy_static;
use std::collections::HashMap;
use std::collections::HashSet;
use std::vec::Vec;
use std::convert::TryInto;
use std::env;
pub fn format(num: i32) -> String {
let mut i: i32 = num;
let mut result: String = String::new();
{
while !(i == 0) {
let quotient: i32 = (i / 10);
let remainder: i32 = (i % 10);
{
result.insert_str(0, &remainder.to_string());
i = quotient;
}
}
return result;
}
}
fn main () {
let args: Vec<String> = env::args().collect();
{
format(2345);
println!("{}", format(2345));
}
}