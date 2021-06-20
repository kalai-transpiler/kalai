pub fn select_str(select: TYPE_MISSING) -> TYPE_MISSING {
    return examples::clojure::string::join(String::from(", "), select);
}
pub fn from_str(from: TYPE_MISSING) -> TYPE_MISSING {
    return examples::clojure::string::join(String::from(", "), from);
}
pub fn join_str(join: TYPE_MISSING) -> TYPE_MISSING {
    return examples::clojure::string::join(String::from(", "), join);
}
pub fn where_str(join: TYPE_MISSING) -> TYPE_MISSING {
    if vector?(join) {
        let op: kalai::Value = kalai::Value::MISSING(first(join));
        let more: kalai::Value = kalai::Value::MISSING(rest(join));
        return format!(
            "{}{}{}",
            String::from("("),
            examples::clojure::string::join(interpose(
                format!("{}{}{}", String::from(" "), op, String::from(" ")),
                map(where_str, more)
            )),
            String::from(")")
        );
    } else {
        return join;
    }
}
pub fn group_by_str(join: TYPE_MISSING) -> TYPE_MISSING {
    return examples::clojure::string::join(String::from(", "), join);
}
pub fn having_str(having: TYPE_MISSING) -> TYPE_MISSING {
    return where_str(having);
}
pub fn format(query_map: std::collections::HashMap<String, std::vec::Vec<kalai::Value>>) -> String {
    let select: TYPE_MISSING = query_map.get(&String::from(":select")).unwrap().clone();
    let from: TYPE_MISSING = query_map.get(&String::from(":from")).unwrap().clone();
    let join: TYPE_MISSING = query_map.get(&String::from(":join")).unwrap().clone();
    let where_clause: TYPE_MISSING = query_map.get(&String::from(":where")).unwrap().clone();
    let group_by: TYPE_MISSING = query_map.get(&String::from(":group-by")).unwrap().clone();
    let having: TYPE_MISSING = query_map.get(&String::from(":having")).unwrap().clone();
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
