pub fn format(query_map: std::collections::HashMap<String,String>, options: std::collections::HashMap<String,String>) -> std::vec::Vec<String> {
return {
let mut tmp_1: std::vec::Vec<String> = std::vec::Vec::new();
tmp_1.push(String::from("a"));
tmp_1.push(String::from("b"));
tmp_1.push(String::from("3"));
tmp_1
};
}
pub fn format_no_opts(query_map: std::collections::HashMap<String,String>) -> std::vec::Vec<String> {
return format(query_map, {
let mut tmp_2: std::collections::HashMap<String,String> = std::collections::HashMap::new();
tmp_2
});
}