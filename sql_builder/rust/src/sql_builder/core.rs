pub fn select_str(select: std::vec::Vec<kalai::Value>) -> String {
    return select.join(String::from(", "));
}
pub fn from_str(from: std::vec::Vec<kalai::Value>) -> String {
    return from.join(String::from(", "));
}
pub fn join_str(join: std::vec::Vec<kalai::Value>) -> String {
    return join.join(String::from(", "));
}
pub fn where_str(join: std::vec::Vec<kalai::Value>) -> String {
    if vector?(join) {
        let op: kalai::Value = kalai::Value::MISSING(first(join));
        let more: kalai::Value = kalai::Value::MISSING(rest(join));
        return format!(
            "{}{}{}",
            String::from("("),
            more.map(where_str)
                .join(format!("{}{}{}", String::from(" "), op, String::from(" "))),
            String::from(")")
        );
    } else {
        return join;
    }
}
pub fn group_by_str(join: std::vec::Vec<kalai::Value>) -> String {
    return join.join(String::from(", "));
}
pub fn having_str(having: std::vec::Vec<kalai::Value>) -> String {
    return where_str(having);
}
pub fn format(query_map: std::collections::HashMap<String, std::vec::Vec<kalai::Value>>) -> String {
    let select: std::vec::Vec<kalai::Value> =
        query_map.get(&String::from(":select")).unwrap().clone();
    let from: std::vec::Vec<kalai::Value> = query_map.get(&String::from(":from")).unwrap().clone();
    let join: std::vec::Vec<kalai::Value> = query_map.get(&String::from(":join")).unwrap().clone();
    let where_clause: std::vec::Vec<kalai::Value> =
        query_map.get(&String::from(":where")).unwrap().clone();
    let group_by: std::vec::Vec<kalai::Value> =
        query_map.get(&String::from(":group-by")).unwrap().clone();
    let having: std::vec::Vec<kalai::Value> =
        query_map.get(&String::from(":having")).unwrap().clone();
    return format!(
        "{}{}{}{}{}{}",
        if select {
            format!("{}{}", String::from("SELECT "), select_str(select))
        },
        if from {
            format!("{}{}", String::from(" FROM "), from_str(from))
        },
        if join {
            format!("{}{}", String::from(" JOIN "), join_str(join))
        },
        if where_clause {
            format!("{}{}", String::from(" WHERE "), where_str(where_clause))
        },
        if group_by {
            format!("{}{}", String::from(" GROUP BY "), group_by_str(group_by))
        },
        if having {
            format!("{}{}", String::from(" HAVING "), having_str(having))
        }
    );
}
