#[macro_use]
extern crate lazy_static;
use std::collections::HashMap;
use std::collections::HashSet;
use std::vec::Vec;
use std::convert::TryInto;
use std::env;
lazy_static! {
static ref x: HashMap<i64,String> = ();
}
pub fn f(y: HashMap<i64,String>) -> HashMap<i64,String> {
let z: HashMap<i64,String> = y;
return z;
}