#[macro_use]
extern crate lazy_static;
use std::collections::HashMap;
use std::collections::HashSet;
use std::vec::Vec;
use std::env;
pub fn side_effect() -> i64 {
let mut y: i64 = 2;
{
y = 3;
return (y + 4);
}
}