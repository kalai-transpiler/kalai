pub fn main() {
    let query_str: String = sql_builder::sql_builder::examples::f_1();
    println!("example query string: [{}]", query_str);
}
