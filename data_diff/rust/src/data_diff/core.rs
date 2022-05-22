use crate::kalai;
use crate::kalai::PMap;
pub fn diff_associative_key(a: TYPE_MISSING, b: TYPE_MISSING, k: TYPE_MISSING) -> TYPE_MISSING {
let va: kalai::BValue = a.get(&k).unwrap().clone();
let vb: kalai::BValue = b.get(&k).unwrap().clone();
let vec_18500: kalai::BValue = diff(va, vb);
let a*: kalai::BValue = clojure.lang._rt/nth(vec_18500, 0i64, kalai::BValue::from(kalai::NIL));
let b*: kalai::BValue = clojure.lang._rt/nth(vec_18500, 1i64, kalai::BValue::from(kalai::NIL));
let ab: kalai::BValue = clojure.lang._rt/nth(vec_18500, 2i64, kalai::BValue::from(kalai::NIL));
let in_a: kalai::BValue = a.contains_key(&k);
let in_b: kalai::BValue = b.contains_key(&k);
let same = {
let and_5531_auto = in_a;
if and_5531_auto
{
let and_5531_auto = in_b;
if and_5531_auto
{
let or_5533_auto: kalai::BValue = !ab.is_type("Nil");
if or_5533_auto
{
or_5533_auto
}
else
{
let and_5531_auto: bool = va.is_type("Nil");
if and_5531_auto
{
vb.is_type("Nil")
}
else
{
and_5531_auto
}
}
}
else
{
and_5531_auto
}
}
else
{
and_5531_auto
}
};
return TYPE_MISSING::new().push_back(if {
let and_5531_auto = in_a;
if and_5531_auto
{
let or_5533_auto: kalai::BValue = !a*.is_type("Nil");
if or_5533_auto
{
or_5533_auto
}
else
{
!same
}
}
else
{
and_5531_auto
}
}
{
TYPE_MISSING::new().insert(k.clone(), a*.clone())
}.clone()).push_back(if {
let and_5531_auto = in_b;
if and_5531_auto
{
let or_5533_auto: kalai::BValue = !b*.is_type("Nil");
if or_5533_auto
{
or_5533_auto
}
else
{
!same
}
}
else
{
and_5531_auto
}
}
{
TYPE_MISSING::new().insert(k.clone(), b*.clone())
}.clone()).push_back(if same
{
TYPE_MISSING::new().insert(k.clone(), ab.clone())
}.clone());
}
pub fn diff_associative(a: TYPE_MISSING, b: TYPE_MISSING, ks: TYPE_MISSING) -> TYPE_MISSING {
return reduce(fn*([diff1_diff2_](doall(map(merge, diff1, diff2)))), TYPE_MISSING::new().push_back(kalai::BValue::from(kalai::NIL).clone()).push_back(kalai::BValue::from(kalai::NIL).clone()).push_back(kalai::BValue::from(kalai::NIL).clone()), ks.clone().map(|kalai_elem|clojure.lang._lazy_seq@32_a64517(kalai_elem.clone())));
}
pub fn union(s1: TYPE_MISSING, s2: TYPE_MISSING) -> TYPE_MISSING {
if (s1.len() as i32 < s2.len() as i32)
{
return reduce(conj, s2, s1);
}
else
{
return reduce(conj, s1, s2);
}
}
pub fn difference(s1: TYPE_MISSING, s2: TYPE_MISSING) -> TYPE_MISSING {
if (s1.len() as i32 < s2.len() as i32)
{
return reduce(fn*([result_item](if s2.contains_key(&item)
{
disj(result, item)
}
else
{
result
})), s1, s1);
}
else
{
return reduce(disj, s1, s2);
}
}
pub fn intersection(s1: TYPE_MISSING, s2: TYPE_MISSING) -> TYPE_MISSING {
if (s2.len() as i32 < s1.len() as i32)
{
return recur(s2, s1);
}
else
{
return reduce(fn*([result_item](if s2.contains_key(&item)
{
result
}
else
{
disj(result, item)
})), s1, s1);
}
}
pub fn atom_diff(a: TYPE_MISSING, b: TYPE_MISSING) -> TYPE_MISSING {
if (a == b)
{
return TYPE_MISSING::new().push_back(kalai::BValue::from(kalai::NIL).clone()).push_back(kalai::BValue::from(kalai::NIL).clone()).push_back(a.clone());
}
else
{
return TYPE_MISSING::new().push_back(a.clone()).push_back(b.clone()).push_back(kalai::BValue::from(kalai::NIL).clone());
}
}
pub fn equality_partition(x: TYPE_MISSING) -> TYPE_MISSING {
if x.is_type("Set")
{
return String::from(":set");
}
else
{
if (x.is_type("Map") || x.is_type("PMap"))
{
return String::from(":map");
}
else
{
if (x.is_type("Vector") || x.is_type("Vec"))
{
return String::from(":sequence");
}
else
{
return String::from(":atom");
}
}
}
}
pub fn map_diff(a: TYPE_MISSING, b: TYPE_MISSING) -> TYPE_MISSING {
let ab_keys: kalai::BValue = union(set(keys(a)), set(keys(b)));
return diff_associative(a, b, ab_keys);
}
pub fn set_diff(a: TYPE_MISSING, b: TYPE_MISSING) -> TYPE_MISSING {
return TYPE_MISSING::new().push_back(not_empty(difference(a, b)).clone()).push_back(not_empty(difference(b, a)).clone()).push_back(not_empty(intersection(a, b)).clone());
}
pub fn vectorize(m: TYPE_MISSING) -> TYPE_MISSING {
if m.clone().iter()
{
return reduce(fn*([result_p_18541_]({
let vec_18543 = p_18541;
let k: kalai::BValue = clojure.lang._rt/nth(vec_18543, 0i64, kalai::BValue::from(kalai::NIL));
let v: kalai::BValue = clojure.lang._rt/nth(vec_18543, 1i64, kalai::BValue::from(kalai::NIL));
assoc(result, k, v)
})), vec(repeat(apply(max, keys(m)), kalai::BValue::from(kalai::NIL))), m);
}
else
{
return kalai::BValue::from(kalai::NIL);
}
}
pub fn sequence_diff(a: TYPE_MISSING, b: TYPE_MISSING) -> TYPE_MISSING {
return vec(diff_associative(if (a.is_type("Vector") || a.is_type("Vec"))
{
a
}
else
{
vec(a)
}, if (b.is_type("Vector") || b.is_type("Vec"))
{
b
}
else
{
vec(b)
}, range(clojure.lang._numbers/max(a.len() as i32, b.len() as i32))).clone().map(|kalai_elem|vectorize(kalai_elem.clone())));
}
pub fn diff_similar(a: TYPE_MISSING, b: TYPE_MISSING) -> TYPE_MISSING {
let partition_a: kalai::BValue = equality_partition(a);
let partition_b: kalai::BValue = equality_partition(b);
if (partition_a == partition_b)
{
if (partition_a == String::from(":set"))
{
return set_diff(a, b);
}
else
{
if (partition_a == String::from(":map"))
{
return map_diff(a, b);
}
else
{
if (partition_a == String::from(":sequence"))
{
return sequence_diff(a, b);
}
else
{
if (partition_a == String::from(":atom"))
{
return atom_diff(a, b);
}
else
{
return kalai::BValue::from(kalai::NIL);
}
}
}
}
}
else
{
return atom_diff(a, b);
}
}
pub fn diff(a: kalai::BValue, b: kalai::BValue) -> TYPE_MISSING {
if (a == b)
{
return TYPE_MISSING::new().push_back(kalai::BValue::from(kalai::NIL).clone()).push_back(kalai::BValue::from(kalai::NIL).clone()).push_back(a.clone());
}
else
{
return diff_similar(a, b);
}
}