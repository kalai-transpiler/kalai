use crate::kalai;
use crate::kalai::PMap;
pub fn get_digits_map() -> std::collections::HashMap<char, i32> {
    return {
        let mut tmp1: std::collections::HashMap<char, i32> = std::collections::HashMap::new();
        tmp1.insert('0'.clone(), 0i32);
        tmp1.insert('1'.clone(), 1i32);
        tmp1.insert('2'.clone(), 2i32);
        tmp1.insert('3'.clone(), 3i32);
        tmp1.insert('4'.clone(), 4i32);
        tmp1.insert('5'.clone(), 5i32);
        tmp1.insert('6'.clone(), 6i32);
        tmp1.insert('7'.clone(), 7i32);
        tmp1.insert('8'.clone(), 8i32);
        tmp1.insert('9'.clone(), 9i32);
        tmp1.insert('٠'.clone(), 0i32);
        tmp1.insert('١'.clone(), 1i32);
        tmp1.insert('٢'.clone(), 2i32);
        tmp1.insert('٣'.clone(), 3i32);
        tmp1.insert('٤'.clone(), 4i32);
        tmp1.insert('٥'.clone(), 5i32);
        tmp1.insert('٦'.clone(), 6i32);
        tmp1.insert('٧'.clone(), 7i32);
        tmp1.insert('٨'.clone(), 8i32);
        tmp1.insert('٩'.clone(), 9i32);
        tmp1.insert('০'.clone(), 0i32);
        tmp1.insert('১'.clone(), 1i32);
        tmp1.insert('২'.clone(), 2i32);
        tmp1.insert('৩'.clone(), 3i32);
        tmp1.insert('৪'.clone(), 4i32);
        tmp1.insert('৫'.clone(), 5i32);
        tmp1.insert('৬'.clone(), 6i32);
        tmp1.insert('৭'.clone(), 7i32);
        tmp1.insert('৮'.clone(), 8i32);
        tmp1.insert('৯'.clone(), 9i32);
        tmp1
    };
}
lazy_static::lazy_static! {
static ref digits_map: std::collections::HashMap<char,i32> = get_digits_map();
}
pub fn parse(s: String) -> i32 {
    let mut result: i32 = 0i32;
    let str_length: i32 = s.chars().count() as i32;
    {
        let mut i: i32 = 0i32;
        while (i < str_length) {
            {
                let digit: char = s.chars().nth(i as usize).unwrap();
                if digits_map.contains_key(&digit) {
                    let digit_val: i32 = digits_map.get(&digit).unwrap().clone();
                    result = ((10i32 * result) + digit_val);
                }
            }
            i = (i + 1i32);
        }
        return result;
    }
}
pub fn get_number_systems_map() -> std::collections::HashMap<String, std::vec::Vec<char>> {
    let m: std::collections::HashMap<String, std::vec::Vec<char>> = {
        let mut tmp2: std::collections::HashMap<String, std::vec::Vec<char>> =
            std::collections::HashMap::new();
        tmp2.insert(
            String::from("ARABIC"),
            {
                let mut tmp3: std::vec::Vec<char> = std::vec::Vec::new();
                tmp3.push('٠'.clone());
                tmp3.push('١'.clone());
                tmp3.push('٢'.clone());
                tmp3.push('٣'.clone());
                tmp3.push('٤'.clone());
                tmp3.push('٥'.clone());
                tmp3.push('٦'.clone());
                tmp3.push('٧'.clone());
                tmp3.push('٨'.clone());
                tmp3.push('٩'.clone());
                tmp3
            }
            .clone(),
        );
        tmp2.insert(
            String::from("BENGALI"),
            {
                let mut tmp4: std::vec::Vec<char> = std::vec::Vec::new();
                tmp4.push('০'.clone());
                tmp4.push('১'.clone());
                tmp4.push('২'.clone());
                tmp4.push('৩'.clone());
                tmp4.push('৪'.clone());
                tmp4.push('৫'.clone());
                tmp4.push('৬'.clone());
                tmp4.push('৭'.clone());
                tmp4.push('৮'.clone());
                tmp4.push('৯'.clone());
                tmp4
            }
            .clone(),
        );
        tmp2.insert(
            String::from("LATIN"),
            {
                let mut tmp5: std::vec::Vec<char> = std::vec::Vec::new();
                tmp5.push('0'.clone());
                tmp5.push('1'.clone());
                tmp5.push('2'.clone());
                tmp5.push('3'.clone());
                tmp5.push('4'.clone());
                tmp5.push('5'.clone());
                tmp5.push('6'.clone());
                tmp5.push('7'.clone());
                tmp5.push('8'.clone());
                tmp5.push('9'.clone());
                tmp5
            }
            .clone(),
        );
        tmp2
    };
    return m;
}
lazy_static::lazy_static! {
static ref number_systems_map: std::collections::HashMap<String,std::vec::Vec<char>> = get_number_systems_map();
}
pub fn get_grouping_separators_map() -> std::collections::HashMap<String, char> {
    return {
        let mut tmp6: std::collections::HashMap<String, char> = std::collections::HashMap::new();
        tmp6.insert(String::from("ARABIC"), '٬'.clone());
        tmp6.insert(String::from("BENGALI"), ','.clone());
        tmp6.insert(String::from("LATIN"), ','.clone());
        tmp6
    };
}
lazy_static::lazy_static! {
static ref grouping_separators_map: std::collections::HashMap<String,char> = get_grouping_separators_map();
}
pub fn get_separator_positions(num_length: i32, grouping_strategy: String) -> std::vec::Vec<i32> {
    let mut result: std::vec::Vec<i32> = std::vec::Vec::new();
    if (grouping_strategy == String::from("NONE")) {
        return result;
    } else {
        if (grouping_strategy == String::from("ON_ALIGNED_3_3")) {
            let mut i: i32 = (num_length - 3i32);
            {
                while (0i32 < i) {
                    result.push(i.clone());
                    i = (i - 3i32);
                }
                return result;
            }
        } else {
            if (grouping_strategy == String::from("ON_ALIGNED_3_2")) {
                let mut i: i32 = (num_length - 3i32);
                {
                    while (0i32 < i) {
                        result.push(i.clone());
                        i = (i - 2i32);
                    }
                    return result;
                }
            } else {
                if (grouping_strategy == String::from("MIN_2")) {
                    if (num_length <= 4i32) {
                        return result;
                    } else {
                        let mut i: i32 = (num_length - 3i32);
                        {
                            while (0i32 < i) {
                                result.push(i.clone());
                                i = (i - 3i32);
                            }
                            return result;
                        }
                    }
                } else {
                    return result;
                }
            }
        }
    }
}
pub fn format(num: i32, number_system: String, grouping_strategy: String) -> String {
    let mut i: i32 = num;
    let mut result: std::vec::Vec<char> = std::vec::Vec::new();
    {
        while !(i == 0i32) {
            let quotient: i32 = (i / 10i32);
            let remainder: i32 = (i % 10i32);
            let number_system_digits: std::vec::Vec<char> =
                number_systems_map.get(&number_system).unwrap().clone();
            let local_digit: char = number_system_digits
                .get(remainder as usize)
                .unwrap()
                .clone();
            {
                result.insert(0i32 as usize, local_digit);
                i = quotient;
            }
        }
        {
            let sep: char = grouping_separators_map.get(&number_system).unwrap().clone();
            let num_length: i32 = result.len() as i32;
            let separator_positions: std::vec::Vec<i32> =
                get_separator_positions(num_length, grouping_strategy);
            let num_positions: i32 = separator_positions.len() as i32;
            let mut idx: i32 = 0i32;
            while (idx < num_positions) {
                {
                    let position: i32 = separator_positions.get(idx as usize).unwrap().clone();
                    result.insert(position as usize, sep);
                }
                idx = (idx + 1i32);
            }
        }
        return result.iter().collect();
    }
}
pub fn main() {
    let _args: std::vec::Vec<String> = std::env::args().collect();
    {
        println!("{}", parse(String::from("٥٠٣٠١")));
        println!("{}", parse(String::from("৫০৩০১")));
        println!("{}", parse(String::from("7,654,321")));
        println!("{}", parse(String::from("76,54,321")));
        println!(
            "{}",
            format(
                7654321i32,
                String::from("LATIN"),
                String::from("ON_ALIGNED_3_2")
            )
        );
        println!(
            "{}",
            format(
                7654321i32,
                String::from("ARABIC"),
                String::from("ON_ALIGNED_3_3")
            )
        );
        println!(
            "{}",
            format(
                7654321i32,
                String::from("BENGALI"),
                String::from("ON_ALIGNED_3_3")
            )
        );
    }
}
