use crate::kalai::kalai;
use crate::kalai::kalai::PMap;
pub fn test_map() -> i64 {
    let a: std::collections::HashMap<String, i64> = {
        let mut tmp1: std::collections::HashMap<String, i64> = std::collections::HashMap::new();
        tmp1.insert(String::from(":x"), 11i64);
        tmp1.insert(String::from(":y"), 13i64);
        tmp1
    };
    let b: std::collections::HashMap<kalai::BValue, kalai::BValue> = {
        let mut tmp2: std::collections::HashMap<kalai::BValue, kalai::BValue> =
            std::collections::HashMap::new();
        tmp2.insert(
            kalai::BValue::from(String::from(":x")),
            kalai::BValue::from(11i64),
        );
        tmp2.insert(
            kalai::BValue::from(String::from(":y")),
            kalai::BValue::from(13i64),
        );
        tmp2
    };
    let c: rpds::HashTrieMap<String, i64> = rpds::HashTrieMap::new()
        .insert(String::from(":x"), 11i64)
        .insert(String::from(":y"), 13i64);
    let d: rpds::HashTrieMap<kalai::BValue, kalai::BValue> = rpds::HashTrieMap::new()
        .insert(
            kalai::BValue::from(String::from(":x")),
            kalai::BValue::from(11i64),
        )
        .insert(
            kalai::BValue::from(String::from(":y")),
            kalai::BValue::from(13i64),
        );
    let e: kalai::BValue = kalai::BValue::from(
        rpds::HashTrieMap::new()
            .insert(
                kalai::BValue::from(String::from(":x")),
                kalai::BValue::from(11i64),
            )
            .insert(
                kalai::BValue::from(String::from(":y")),
                kalai::BValue::from(13i64),
            ),
    );
    {
        println!(
            "{}",
            format!(
                "{}{}",
                String::from("key :y in mutable map a returns "),
                a.get(&String::from(":y")).unwrap().clone()
            )
        );
        {
            let any_y: kalai::BValue = kalai::BValue::from(String::from(":y"));
            let get_b_any_y: i64 = i64::from(b.get(&any_y).unwrap().clone());
            println!(
                "{}",
                format!(
                    "{}{}",
                    String::from("key :y in mutable map b returns "),
                    get_b_any_y
                )
            );
        }
        println!(
            "{}",
            format!(
                "{}{}",
                String::from("key :y in persistent map c returns "),
                c.get(&String::from(":y")).unwrap().clone()
            )
        );
        {
            let any_y: kalai::BValue = kalai::BValue::from(String::from(":y"));
            let get_d_any_y: i64 = i64::from(d.get(&any_y).unwrap().clone());
            println!(
                "{}",
                format!(
                    "{}{}",
                    String::from("key :y in persistent map d returns "),
                    get_d_any_y
                )
            );
        }
        {
            let any_y: kalai::BValue = kalai::BValue::from(String::from(":y"));
            let e_map: rpds::HashTrieMap<kalai::BValue, kalai::BValue> = rpds::HashTrieMap::from(e);
            let get_e_any_y: i64 = i64::from(e_map.get(&any_y).unwrap().clone());
            println!(
                "{}",
                format!(
                    "{}{}",
                    String::from("key :y in persistent map e returns "),
                    get_e_any_y
                )
            );
        }
        return 3i64;
    }
}
pub fn test_vector() -> i64 {
    let a: std::vec::Vec<i64> = {
        let mut tmp3: std::vec::Vec<i64> = std::vec::Vec::new();
        tmp3.push(11i64);
        tmp3.push(13i64);
        tmp3
    };
    let b: std::vec::Vec<kalai::BValue> = {
        let mut tmp4: std::vec::Vec<kalai::BValue> = std::vec::Vec::new();
        tmp4.push(kalai::BValue::from(11i64));
        tmp4.push(kalai::BValue::from(13i64));
        tmp4
    };
    let c: rpds::Vector<i64> = rpds::Vector::new().push_back(11i64).push_back(13i64);
    let d: rpds::Vector<kalai::BValue> = rpds::Vector::new()
        .push_back(kalai::BValue::from(11i64))
        .push_back(kalai::BValue::from(13i64));
    {
        println!(
            "{}",
            format!(
                "{}{}",
                String::from("size of mutable vector a returns "),
                a.len() as i32
            )
        );
        println!(
            "{}",
            format!(
                "{}{}",
                String::from("size of mutable vector b returns "),
                b.len() as i32
            )
        );
        println!(
            "{}",
            format!(
                "{}{}",
                String::from("size of persistent vector c returns "),
                c.len() as i32
            )
        );
        println!(
            "{}",
            format!(
                "{}{}",
                String::from("size of persistent vector d returns "),
                d.len() as i32
            )
        );
        return 5i64;
    }
}
pub fn test_set() -> i64 {
    let a: std::collections::HashSet<i64> = {
        let mut tmp5: std::collections::HashSet<i64> = std::collections::HashSet::new();
        tmp5.insert(11i64);
        tmp5.insert(13i64);
        tmp5.insert(15i64);
        tmp5
    };
    let b: std::collections::HashSet<kalai::BValue> = {
        let mut tmp6: std::collections::HashSet<kalai::BValue> = std::collections::HashSet::new();
        tmp6.insert(kalai::BValue::from(11i64));
        tmp6.insert(kalai::BValue::from(13i64));
        tmp6.insert(kalai::BValue::from(15i64));
        tmp6
    };
    let c: rpds::HashTrieSet<i64> = rpds::HashTrieSet::new()
        .insert(11i64)
        .insert(13i64)
        .insert(15i64);
    let d: rpds::HashTrieSet<kalai::BValue> = rpds::HashTrieSet::new()
        .insert(kalai::BValue::from(11i64))
        .insert(kalai::BValue::from(13i64))
        .insert(kalai::BValue::from(15i64));
    {
        println!(
            "{}",
            format!(
                "{}{}",
                String::from("size of mutable set a returns "),
                a.len() as i32
            )
        );
        println!(
            "{}",
            format!(
                "{}{}",
                String::from("size of mutable set b returns "),
                b.len() as i32
            )
        );
        println!(
            "{}",
            format!(
                "{}{}",
                String::from("size of persistent set c returns "),
                c.size() as i32
            )
        );
        println!(
            "{}",
            format!(
                "{}{}",
                String::from("size of persistent set d returns "),
                d.size() as i32
            )
        );
        return 7i64;
    }
}
pub fn main() {
    let _args: std::vec::Vec<String> = std::env::args().collect();
    {
        println!("{}", test_map());
        println!("{}", test_vector());
        println!("{}", test_set());
    }
}
