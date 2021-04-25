fn main () {
let args: std::vec::Vec<String> = std::env::args().collect();
{
println!("{}", std::env::var(String::from("USER")).unwrap());
}
}