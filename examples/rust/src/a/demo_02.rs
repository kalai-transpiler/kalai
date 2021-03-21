#[macro_use]
extern crate lazy_static;
use std::collections::HashMap;
use std::collections::HashSet;
use std::vec::Vec;
use std::convert::TryInto;
use std::env;
pub fn get_digits_map() -> HashMap<char,i32> {
return {
let mut tmp_1: HashMap<char,i32> = HashMap::new();
tmp_1.insert('٠', 0);
tmp_1.insert('١', 1);
tmp_1.insert('٢', 2);
tmp_1.insert('٣', 3);
tmp_1.insert('٤', 4);
tmp_1.insert('٥', 5);
tmp_1.insert('০', 0);
tmp_1.insert('٦', 6);
tmp_1.insert('১', 1);
tmp_1.insert('٧', 7);
tmp_1.insert('২', 2);
tmp_1.insert('٨', 8);
tmp_1.insert('৩', 3);
tmp_1.insert('٩', 9);
tmp_1.insert('৪', 4);
tmp_1.insert('৫', 5);
tmp_1.insert('৬', 6);
tmp_1.insert('৭', 7);
tmp_1.insert('৮', 8);
tmp_1.insert('৯', 9);
tmp_1.insert('0', 0);
tmp_1.insert('1', 1);
tmp_1.insert('2', 2);
tmp_1.insert('3', 3);
tmp_1.insert('4', 4);
tmp_1.insert('5', 5);
tmp_1.insert('6', 6);
tmp_1.insert('7', 7);
tmp_1.insert('8', 8);
tmp_1.insert('9', 9);
tmp_1
};
}
lazy_static! {
static ref digits_map: HashMap<char,i32> = get_digits_map();
}
pub fn parse(s: String) -> i32 {
let mut result: i32 = 0;
let str_length: i32 = s.chars().count().try_into().unwrap();
{
let mut i: i32 = 0;
while (i < str_length) {
{
let digit: char = s.chars().nth(i as usize).unwrap();
if digits_map.contains_key(&digit)
{
let digit_val: &i32 = digits_map.get(&digit).unwrap();
result = ((10 * result) + digit_val);
}
}
i = (i + 1);
}
return result;
}
}
pub fn get_number_systems_map() -> HashMap<String,Vec<char>> {
let m: HashMap<String,Vec<char>> = {
let mut tmp_2: HashMap<String,Vec<char>> = HashMap::new();
tmp_2.insert(String::from("ARABIC"), {
let mut tmp_3: Vec<char> = Vec::new();
tmp_3.push('٠');
tmp_3.push('١');
tmp_3.push('٢');
tmp_3.push('٣');
tmp_3.push('٤');
tmp_3.push('٥');
tmp_3.push('٦');
tmp_3.push('٧');
tmp_3.push('٨');
tmp_3.push('٩');
tmp_3
});
tmp_2.insert(String::from("LATIN"), {
let mut tmp_4: Vec<char> = Vec::new();
tmp_4.push('0');
tmp_4.push('1');
tmp_4.push('2');
tmp_4.push('3');
tmp_4.push('4');
tmp_4.push('5');
tmp_4.push('6');
tmp_4.push('7');
tmp_4.push('8');
tmp_4.push('9');
tmp_4
});
tmp_2.insert(String::from("BENGALI"), {
let mut tmp_5: Vec<char> = Vec::new();
tmp_5.push('০');
tmp_5.push('১');
tmp_5.push('২');
tmp_5.push('৩');
tmp_5.push('৪');
tmp_5.push('৫');
tmp_5.push('৬');
tmp_5.push('৭');
tmp_5.push('৮');
tmp_5.push('৯');
tmp_5
});
tmp_2
};
return m;
}
lazy_static! {
static ref number_systems_map: HashMap<String,Vec<char>> = get_number_systems_map();
}
pub fn get_grouping_separators_map() -> HashMap<String,char> {
return {
let mut tmp_6: HashMap<String,char> = HashMap::new();
tmp_6.insert(String::from("ARABIC"), '٬');
tmp_6.insert(String::from("LATIN"), ',');
tmp_6.insert(String::from("BENGALI"), ',');
tmp_6
};
}
lazy_static! {
static ref grouping_separators_map: HashMap<String,char> = get_grouping_separators_map();
}
pub fn get_separator_positions(num_length: i32, grouping_strategy: String) -> Vec<i32> {
let mut result: Vec<i32> = {
let mut tmp_7: Vec<i32> = Vec::new();
tmp_7
};
if (grouping_strategy == String::from("NONE"))
{
return result;
}
else
{
if (grouping_strategy == String::from("ON_ALIGNED_3_3"))
{
let mut i: i32 = (num_length - 3);
{
while (0 < i) {
result.push(i);
i = (i - 3);
}
return result;
}
}
else
{
if (grouping_strategy == String::from("ON_ALIGNED_3_2"))
{
let mut i: i32 = (num_length - 3);
{
while (0 < i) {
result.push(i);
i = (i - 2);
}
return result;
}
}
else
{
if (grouping_strategy == String::from("MIN_2"))
{
if (num_length <= 4)
{
return result;
}
else
{
let mut i: i32 = (num_length - 3);
{
while (0 < i) {
result.push(i);
i = (i - 3);
}
return result;
}
}
}
else
{
return result;
}
}
}
}
}
pub fn format(num: i32, number_system: String, grouping_strategy: String) -> String {
let mut i: i32 = num;
let mut result: Vec<char> = Vec::new();
{
while !(i == 0) {
let quotient: i32 = (i / 10);
let remainder: i32 = (i % 10);
let number_system_digits: &Vec<char> = number_systems_map.get(&number_system).unwrap();
let local_digit: char = *number_system_digits.get(remainder as usize).unwrap();
{
result.insert(0 as usize, local_digit);
i = quotient;
}
}
{
let sep: &char = grouping_separators_map.get(&number_system).unwrap();
let num_length: i32 = result.len() as int;
let separator_positions: Vec<i32> = get_separator_positions(num_length, grouping_strategy);
let num_positions: i32 = separator_positions.len().try_into().unwrap();
let mut idx: i32 = 0;
while (idx < num_positions) {
{
let position: i32 = *separator_positions.get(idx as usize).unwrap();
result.insert(position as usize, *sep);
}
idx = (idx + 1);
}
}
return result.iter().collect();
}
}
fn main () {
let args: Vec<String> = env::args().collect();
{
println!("{}", parse(String::from("٥٠٣٠١")));
println!("{}", parse(String::from("৫০৩০১")));
println!("{}", parse(String::from("7,654,321")));
println!("{}", parse(String::from("76,54,321")));
println!("{}", format(7654321, String::from("LATIN"), String::from("ON_ALIGNED_3_2")));
println!("{}", format(7654321, String::from("ARABIC"), String::from("ON_ALIGNED_3_3")));
println!("{}", format(7654321, String::from("BENGALI"), String::from("ON_ALIGNED_3_3")));
}
}