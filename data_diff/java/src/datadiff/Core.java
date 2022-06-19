package datadiff;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
public class Core {
public static final TYPE_MISSING diffAssociativeKey(final TYPE_MISSING a, final TYPE_MISSING b, final TYPE_MISSING k) {
final Object va = a.get(k);
final Object vb = b.get(k);
final Object vec18649 = datadiff.Core.diff(va, vb);
Object tmp43 = null;
if ((0L <= 0L))
{
if ((0L < vec__18649.length()))
{
tmp43 = vec__18649.get();
}
}
final Object aa = tmp43;
Object tmp44 = null;
if ((0L <= 1L))
{
if ((1L < vec__18649.length()))
{
tmp44 = vec__18649.get();
}
}
final Object bb = tmp44;
Object tmp45 = null;
if ((0L <= 2L))
{
if ((2L < vec__18649.length()))
{
tmp45 = vec__18649.get();
}
}
final Object ab = tmp45;
final Object inA = a.containsKey(k);
final Object inB = b.containsKey(k);
final TYPE_MISSING and5531Auto = inA;
"MISSING_TYPE" tmp1;
boolean tmp2 = and__5531__auto__;
if (tmp2)
{
final TYPE_MISSING and5531Auto = inB;
"MISSING_TYPE" tmp3;
boolean tmp4 = and__5531__auto__;
if (tmp4)
{
final Object or5533Auto = !(ab == null);
"MISSING_TYPE" tmp5;
boolean tmp6 = or__5533__auto__;
if (tmp6)
{
tmp5 = or__5533__auto__;
}
else
{
final boolean and5531Auto = (va == null);
"MISSING_TYPE" tmp7;
boolean tmp8 = and__5531__auto__;
if (tmp8)
{
tmp7 = (vb == null);
}
else
{
tmp7 = and__5531__auto__;
}
{
tmp5 = tmp7;
}
}
{
tmp3 = tmp5;
}
}
else
{
tmp3 = and__5531__auto__;
}
{
tmp1 = tmp3;
}
}
else
{
tmp1 = and__5531__auto__;
}
final TYPE_MISSING same = tmp1;
clojure.lang.PersistentArrayMap tmp9;
final TYPE_MISSING and5531Auto = inA;
"MISSING_TYPE" tmp11;
boolean tmp12 = and__5531__auto__;
if (tmp12)
{
final Object or5533Auto = !(aa == null);
"MISSING_TYPE" tmp13;
boolean tmp14 = or__5533__auto__;
if (tmp14)
{
tmp13 = or__5533__auto__;
}
else
{
tmp13 = !same;
}
{
tmp11 = tmp13;
}
}
else
{
tmp11 = and__5531__auto__;
}
boolean tmp10 = tmp11;
if (tmp10)
{
tmp9 = new TYPE_MISSING().put(k, aa, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS);
}
clojure.lang.PersistentArrayMap tmp15;
final TYPE_MISSING and5531Auto = inB;
"MISSING_TYPE" tmp17;
boolean tmp18 = and__5531__auto__;
if (tmp18)
{
final Object or5533Auto = !(bb == null);
"MISSING_TYPE" tmp19;
boolean tmp20 = or__5533__auto__;
if (tmp20)
{
tmp19 = or__5533__auto__;
}
else
{
tmp19 = !same;
}
{
tmp17 = tmp19;
}
}
else
{
tmp17 = and__5531__auto__;
}
boolean tmp16 = tmp17;
if (tmp16)
{
tmp15 = new TYPE_MISSING().put(k, bb, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS);
}
clojure.lang.PersistentArrayMap tmp21;
boolean tmp22 = same;
if (tmp22)
{
tmp21 = new TYPE_MISSING().put(k, ab, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS);
}
return new TYPE_MISSING().addLast(tmp9).addLast(tmp15).addLast(tmp21);
}
public static final TYPE_MISSING diffAssociative(final TYPE_MISSING a, final TYPE_MISSING b, final TYPE_MISSING ks) {
return kalai.Kalai.foldLeft(ks.stream().map((k) -> {
return datadiff.Core.diffAssociativeKey(a, b, k);
}), new TYPE_MISSING().addLast(null).addLast(null).addLast(null), (diff1, diff2) -> {
return clojure.Core.doall(clojure.Core.map(merge, diff1, diff2));
});
}
public static final TYPE_MISSING union(final TYPE_MISSING s1, final TYPE_MISSING s2) {
boolean tmp23 = (s1.length() < s2.length());
if (tmp23)
{
return kalai.Kalai.foldLeft(s1, s2, conj);
}
else
{
return kalai.Kalai.foldLeft(s2, s1, conj);
}
}
public static final TYPE_MISSING difference(final TYPE_MISSING s1, final TYPE_MISSING s2) {
boolean tmp24 = (s1.length() < s2.length());
if (tmp24)
{
return kalai.Kalai.foldLeft(s1, s1, (result, item) -> {
boolean tmp25 = s2.containsKey(item);
if (tmp25)
{
return clojure.Core.disj(result, item);
}
else
{
return result;
}
});
}
else
{
return kalai.Kalai.foldLeft(s2, s1, disj);
}
}
public static final TYPE_MISSING intersection(final TYPE_MISSING s1, final TYPE_MISSING s2) {
boolean tmp26 = (s2.length() < s1.length());
if (tmp26)
{
return recur(s2, s1);
}
else
{
return kalai.Kalai.foldLeft(s1, s1, (result, item) -> {
boolean tmp27 = s2.containsKey(item);
if (tmp27)
{
return result;
}
else
{
return clojure.Core.disj(result, item);
}
});
}
}
public static final TYPE_MISSING atomDiff(final TYPE_MISSING a, final TYPE_MISSING b) {
boolean tmp28 = (a == b);
if (tmp28)
{
return new TYPE_MISSING().addLast(null).addLast(null).addLast(a);
}
else
{
return new TYPE_MISSING().addLast(a).addLast(b).addLast(null);
}
}
public static final TYPE_MISSING equalityPartition(final TYPE_MISSING x) {
boolean tmp29 = (x instanceof Set);
if (tmp29)
{
return ":set";
}
else
{
boolean tmp30 = (x instanceof Map);
if (tmp30)
{
return ":map";
}
else
{
boolean tmp31 = (x instanceof List);
if (tmp31)
{
return ":sequence";
}
else
{
return ":atom";
}
}
}
}
public static final TYPE_MISSING mapDiff(final TYPE_MISSING a, final TYPE_MISSING b) {
final Object abKeys = datadiff.Core.union(clojure.Core.set(clojure.Core.keys(a)), clojure.Core.set(clojure.Core.keys(b)));
return datadiff.Core.diffAssociative(a, b, abKeys);
}
public static final TYPE_MISSING setDiff(final TYPE_MISSING a, final TYPE_MISSING b) {
return new TYPE_MISSING().addLast(clojure.Core.notEmpty(datadiff.Core.difference(a, b))).addLast(clojure.Core.notEmpty(datadiff.Core.difference(b, a))).addLast(clojure.Core.notEmpty(datadiff.Core.intersection(a, b)));
}
public static final TYPE_MISSING vectorize(final TYPE_MISSING m) {
boolean tmp32 = m.stream();
if (tmp32)
{
Object tmp46 = null;
if ((0L <= 0L))
{
if ((0L < vec__18692.length()))
{
tmp46 = vec__18692.get();
}
}
Object tmp47 = null;
if ((0L <= 1L))
{
if ((1L < vec__18692.length()))
{
tmp47 = vec__18692.get();
}
}
{
return kalai.Kalai.foldLeft(m, clojure.Core.vec(clojure.Core.repeat(clojure.Core.apply(max, clojure.Core.keys(m)), null)), (result, p__18690) -> {
final TYPE_MISSING vec18692 = p__18690;
final Object k = tmp46;
final Object v = tmp47;
return result.put(k, v);
});
}
}
else
{
return null;
}
}
public static final TYPE_MISSING sequenceDiff(final TYPE_MISSING a, final TYPE_MISSING b) {
"MISSING_TYPE" tmp33;
boolean tmp34 = (a instanceof List);
if (tmp34)
{
tmp33 = a;
}
else
{
tmp33 = clojure.Core.vec(a);
}
"MISSING_TYPE" tmp35;
boolean tmp36 = (b instanceof List);
if (tmp36)
{
tmp35 = b;
}
else
{
tmp35 = clojure.Core.vec(b);
}
return clojure.Core.vec(datadiff.Core.diffAssociative(tmp33, tmp35, clojure.Core.range(clojure.lang.Numbers.max(a.length(), b.length()))).stream().map(datadiff.Core::vectorize));
}
public static final TYPE_MISSING diffSimilar(final TYPE_MISSING a, final TYPE_MISSING b) {
final Object partitionA = datadiff.Core.equalityPartition(a);
final Object partitionB = datadiff.Core.equalityPartition(b);
{
boolean tmp37 = (partitionA == partitionB);
if (tmp37)
{
boolean tmp38 = (partitionA == ":set");
if (tmp38)
{
return datadiff.Core.setDiff(a, b);
}
else
{
boolean tmp39 = (partitionA == ":map");
if (tmp39)
{
return datadiff.Core.mapDiff(a, b);
}
else
{
boolean tmp40 = (partitionA == ":sequence");
if (tmp40)
{
return datadiff.Core.sequenceDiff(a, b);
}
else
{
boolean tmp41 = (partitionA == ":atom");
if (tmp41)
{
return datadiff.Core.atomDiff(a, b);
}
else
{
return null;
}
}
}
}
}
else
{
return datadiff.Core.atomDiff(a, b);
}
}
}
public static final TYPE_MISSING diff(final Object a, final Object b) {
boolean tmp42 = (a == b);
if (tmp42)
{
return new TYPE_MISSING().addLast(null).addLast(null).addLast(a);
}
else
{
return datadiff.Core.diffSimilar(a, b);
}
}
}
