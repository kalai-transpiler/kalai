package a;
import java.util.Vector;
import java.util.Map;
import java.util.HashSet;
public class demo02 {
final TYPE_MISSING CI;
public static final CI getDigitsMap() {
final PersistentMap tmp1 = new PersistentMap();
tmp1.put('٠', 0);
tmp1.put('١', 1);
tmp1.put('٢', 2);
tmp1.put('٣', 3);
tmp1.put('٤', 4);
tmp1.put('٥', 5);
tmp1.put('০', 0);
tmp1.put('٦', 6);
tmp1.put('১', 1);
tmp1.put('٧', 7);
tmp1.put('২', 2);
tmp1.put('٨', 8);
tmp1.put('৩', 3);
tmp1.put('٩', 9);
tmp1.put('৪', 4);
tmp1.put('৫', 5);
tmp1.put('৬', 6);
tmp1.put('৭', 7);
tmp1.put('৮', 8);
tmp1.put('৯', 9);
tmp1.put('0', 0);
tmp1.put('1', 1);
tmp1.put('2', 2);
tmp1.put('3', 3);
tmp1.put('4', 4);
tmp1.put('5', 5);
tmp1.put('6', 6);
tmp1.put('7', 7);
tmp1.put('8', 8);
tmp1.put('9', 9);
final CI m = tmp1;
return m;
}
final Map<Java.lang.character,Integer> digitsMap = getDigitsMap();
public static final Integer parse(final String s) {
long result = 0;
final MISSING_TYPE strLength = clojure.lang.RT/count(s);
{
int i = 0;
while ((i < strLength)) {
{
final Character digit = clojure.lang.RT/nth(s, i);
if (digitsMap.contains(digit))
{
final Integer digitVal = clojure.lang.RT/get(digitsMap, digit);
result = ((10 * result) + digitVal);
}
}
i = ++i;
}
return result;
}
}
final TYPE_MISSING SLC;
public static final SLC getNumberSystemsMap() {
final PersistentMap tmp2 = new PersistentMap();
final PersistentVector tmp3 = new PersistentVector();
tmp3.add('٠');
tmp3.add('١');
tmp3.add('٢');
tmp3.add('٣');
tmp3.add('٤');
tmp3.add('٥');
tmp3.add('٦');
tmp3.add('٧');
tmp3.add('٨');
tmp3.add('٩');
tmp2.put("ARABIC", tmp3);
final PersistentVector tmp4 = new PersistentVector();
tmp4.add('0');
tmp4.add('1');
tmp4.add('2');
tmp4.add('3');
tmp4.add('4');
tmp4.add('5');
tmp4.add('6');
tmp4.add('7');
tmp4.add('8');
tmp4.add('9');
tmp2.put("LATIN", tmp4);
final PersistentVector tmp5 = new PersistentVector();
tmp5.add('০');
tmp5.add('১');
tmp5.add('২');
tmp5.add('৩');
tmp5.add('৪');
tmp5.add('৫');
tmp5.add('৬');
tmp5.add('৭');
tmp5.add('৮');
tmp5.add('৯');
tmp2.put("BENGALI", tmp5);
final SLC m = tmp2;
return m;
}
final Map<String,Type_missing> numberSystemsMap = getNumberSystemsMap();
final TYPE_MISSING SC;
public static final SC getGroupingSeparatorsMap() {
final PersistentMap tmp6 = new PersistentMap();
tmp6.put("ARABIC", '٬');
tmp6.put("LATIN", ',');
tmp6.put("BENGALI", ',');
final SC m = tmp6;
return m;
}
final Map<String,Java.lang.character> groupingSeparatorsMap = getGroupingSeparatorsMap();
final TYPE_MISSING LI;
public static final LI getSeparatorPositions(final Integer numLength, final String groupingStrategy) {
final PersistentVector tmp7 = new PersistentVector();
List<Integer> result = tmp7;
if ((groupingStrategy == "NONE"))
{
return result;
}
else
{
if ((groupingStrategy == "ON_ALIGNED_3_3"))
{
MISSING_TYPE i = (numLength - 3);
{
while ((0 < i)) {
result = result.add(i);
i = (i - 3);
}
return result;
}
}
else
{
if ((groupingStrategy == "ON_ALIGNED_3_2"))
{
MISSING_TYPE i = (numLength - 3);
{
while ((0 < i)) {
result = result.add(i);
i = (i - 2);
}
return result;
}
}
else
{
if ((groupingStrategy == "MIN_2"))
{
if ((numLength <= 4))
{
return result;
}
else
{
MISSING_TYPE i = (numLength - 3);
{
while ((0 < i)) {
result = result.add(i);
i = (i - 3);
}
return result;
}
}
}
else
{
if (":else")
{
return result;
}
}
}
}
}
}
public static final String format(final Integer num, final String numberSystem, final String groupingStrategy) {
MISSING_TYPE i = num;
final StringBuffer result = new StringBuffer();
{
while (!(i == 0)) {
final Integer quotient = (i / 10);
final Integer remainder = (i % 10);
final List<Character> numberSystemDigits = clojure.lang.RT/get(numberSystemsMap, numberSystem);
final Character localDigit = clojure.lang.RT/nth(numberSystemDigits, remainder);
{
result.insert(0, localDigit);
i = quotient;
}
}
{
final Character sep = clojure.lang.RT/get(groupingSeparatorsMap, numberSystem);
final MISSING_TYPE numLength = result.length();
final LI separatorPositions = getSeparatorPositions(numLength, groupingStrategy);
final MISSING_TYPE numPositions = clojure.lang.RT/count(separatorPositions);
int idx = 0;
while ((idx < numPositions)) {
{
final Integer position = clojure.lang.RT/nth(separatorPositions, idx);
result = result.insert(position, sep);
}
idx = ++idx;
}
}
return result.toString();
}
}
}