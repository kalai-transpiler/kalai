fn main () {
let args: std::vec::Vec<String> = std::env::args().collect();
{
println!("{}", examples::b::required::f(1));
}
}